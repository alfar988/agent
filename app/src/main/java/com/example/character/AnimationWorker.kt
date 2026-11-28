package com.example.character

import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.CameraMode
import com.example.physics.ToolType
import com.example.physics.WorldRenderSnapshot
import com.example.render.gltf.GlbAnimationClip
import com.example.render.gltf.GlbBoneNode
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.cos

data class CharacterPoseSnapshot(
    val baseState: CharacterAnimState,
    val upperState: CharacterAnimState,
    val boneSkinMatrices: FloatArray,
    val rightHandWorldMatrix: FloatArray,
    val leftHandWorldMatrix: FloatArray,
    val rightHandPos: Vec3,
    val rightHandRot: Quat,
    val blinkLeft: Float,
    val blinkRight: Float,
    val browFocus: Float,
    val mouthOpen: Float,
    val characterWorldPos: Vec3,
    val characterYawRad: Float,
    val isFirstPerson: Boolean,
    val equippedToolId: String,
    val toolActionPulse: Float,
    val toolSpinAngle: Float,
    val crouchFraction: Float
)

/**
 * Dedicated background animation thread evaluating the 14-state AnimationStateMachine,
 * 65-bone forward kinematics, Foot/Hand/Look/Two-Hand IK, and morph weights at ~60 Hz
 * without blocking the Main/UI thread, OpenGL thread, or Physics solver thread.
 */
class AnimationWorker {

    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "KineticLab-AnimationThread").apply { priority = Thread.NORM_PRIORITY }
    }

    private val running = AtomicBoolean(false)
    private val isShutdown = AtomicBoolean(false)
    private val stateMachine = AnimationStateMachine()
    private val pose = SkeletonPose(BoneIds.TOTAL_BONES)

    @Volatile private var skeletonBones: List<GlbBoneNode> = emptyList()
    @Volatile private var animationClips: Map<String, GlbAnimationClip> = emptyMap()
    @Volatile private var latestWorldSnapshot: WorldRenderSnapshot? = null
    @Volatile var previewModeActive: Boolean = false
    @Volatile var ikEnabled: Boolean = true

    val latestPoseRef = AtomicReference<CharacterPoseSnapshot?>(null)
    val evalStepMsRef = AtomicReference(0f)

    private var lastTickNs: Long = 0L

    fun setCharacterRigData(bones: List<GlbBoneNode>, clips: Map<String, GlbAnimationClip>) {
        synchronized(this) {
            skeletonBones = bones
            animationClips = clips
            pose.resetToBind(bones)
            pose.updateWorldAndSkinMatrices(bones)
        }
    }

    fun submitWorldSnapshot(snapshot: WorldRenderSnapshot) {
        latestWorldSnapshot = snapshot
    }

    fun start() {
        if (isShutdown.get() || executor.isShutdown) return
        if (running.compareAndSet(false, true)) {
            lastTickNs = System.nanoTime()
            try {
                executor.scheduleAtFixedRate({
                    try {
                        val now = System.nanoTime()
                        val dt = ((now - lastTickNs) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
                        lastTickNs = now
                        evaluateStep(dt)
                    } catch (_: Throwable) {
                        // Keep worker thread resilient
                    }
                }, 0L, 16666L, TimeUnit.MICROSECONDS)
            } catch (_: java.util.concurrent.RejectedExecutionException) {
                running.set(false)
            }
        }
    }

    fun shutdown() {
        isShutdown.set(true)
        running.set(false)
        executor.shutdownNow()
    }

    /**
     * Synchronous deterministic step helper usable both inside the worker loop and in JVM tests.
     */
    fun stepSync(dt: Float, snapshot: WorldRenderSnapshot): CharacterPoseSnapshot? {
        latestWorldSnapshot = snapshot
        return evaluateStep(dt)
    }

    private fun evaluateStep(dt: Float): CharacterPoseSnapshot? {
        val bones = skeletonBones
        val clips = animationClips
        val snap = latestWorldSnapshot ?: return null
        if (bones.isEmpty()) return null

        val startNs = System.nanoTime()
        val result: CharacterPoseSnapshot
        synchronized(this) {
            val horizSpeed = hypot(snap.playerVel.x, snap.playerVel.z)
            val fwd = Vec3(sin(snap.cameraYaw), 0f, -cos(snap.cameraYaw))
            val rgt = Vec3(cos(snap.cameraYaw), 0f, sin(snap.cameraYaw))
            val fwdDot = snap.playerVel.x * fwd.x + snap.playerVel.z * fwd.z
            val rgtDot = snap.playerVel.x * rgt.x + snap.playerVel.z * rgt.z

            val animInput = if (previewModeActive) {
                CharacterAnimInput(
                    horizontalSpeed = 0f,
                    onGround = true,
                    selectedTool = ToolType.GRAB,
                    lookDirWeight = 0.25f
                )
            } else {
                CharacterAnimInput(
                    horizontalSpeed = horizSpeed,
                    forwardDot = fwdDot,
                    rightDot = rgtDot,
                    verticalVel = snap.playerVel.y,
                    onGround = snap.playerOnGround,
                    isCrouching = snap.isCrouching,
                    isSprinting = snap.isSprinting,
                    isGrabbing = snap.isGrabbing,
                    grabbedWorldPos = snap.grabbedObjectPos,
                    targetedWorldPos = snap.targetedWorldPos,
                    selectedTool = snap.selectedTool,
                    activeCatalogToolId = snap.activeCatalogToolId,
                    toolActionPulse = snap.equippedToolActionPulse,
                    landingImpulse = snap.landingImpulse,
                    interactPulse = snap.interactPulse,
                    releasePulse = snap.releasePulse,
                    cameraYaw = snap.cameraYaw,
                    cameraPitch = snap.cameraPitch,
                    lookDirWeight = 1f,
                    weaponActionState = snap.weaponActionState,
                    isAiming = snap.isAiming,
                    recoilVisualOffset = snap.recoilVisualOffset
                )
            }

            stateMachine.update(dt, animInput, bones, clips, pose)

            val isFp = !previewModeActive && snap.cameraMode == CameraMode.FIRST_PERSON
            if (ikEnabled && !previewModeActive) {
                CharacterIkSolver.solveFullBodyIk(
                    pose = pose,
                    bones = bones,
                    characterWorldPos = snap.playerPos,
                    characterYawRad = snap.cameraYaw,
                    input = animInput,
                    staticProps = snap.staticProps,
                    isFirstPerson = isFp
                )
            } else {
                pose.updateWorldAndSkinMatrices(bones)
            }

            val skinCopy = FloatArray(pose.skinMatrices.size)
            System.arraycopy(pose.skinMatrices, 0, skinCopy, 0, skinCopy.size)
            val rHandMat = pose.worldMatrices[BoneIds.RIGHT_HAND].copyOf()
            val lHandMat = pose.worldMatrices[BoneIds.LEFT_HAND].copyOf()
            val rHandPos = pose.boneWorldPosition(BoneIds.RIGHT_HAND)
            val rHandRot = pose.boneWorldRotation(BoneIds.RIGHT_HAND, bones)

            result = CharacterPoseSnapshot(
                baseState = stateMachine.baseState,
                upperState = stateMachine.upperState,
                boneSkinMatrices = skinCopy,
                rightHandWorldMatrix = rHandMat,
                leftHandWorldMatrix = lHandMat,
                rightHandPos = rHandPos,
                rightHandRot = rHandRot,
                blinkLeft = pose.blinkLeft,
                blinkRight = pose.blinkRight,
                browFocus = pose.browFocus,
                mouthOpen = pose.mouthOpen,
                characterWorldPos = snap.playerPos,
                characterYawRad = snap.cameraYaw,
                isFirstPerson = isFp,
                equippedToolId = snap.activeCatalogToolId,
                toolActionPulse = snap.equippedToolActionPulse,
                toolSpinAngle = snap.equippedToolSpinAngle,
                crouchFraction = snap.crouchFraction
            )
            latestPoseRef.set(result)
        }
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000f
        evalStepMsRef.set(evalStepMsRef.get() * 0.8f + elapsedMs * 0.2f)
        return result
    }
}
