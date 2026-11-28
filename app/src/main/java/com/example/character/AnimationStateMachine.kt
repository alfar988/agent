package com.example.character

import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.ToolType
import com.example.render.gltf.GlbAnimationClip
import com.example.render.gltf.GlbBoneNode
import com.example.render.gltf.GlbChannelPath
import com.example.weapons.FirearmCatalog
import com.example.weapons.WeaponActionState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class CharacterAnimState {
    IDLE,
    WALKING,
    JOGGING,
    SPRINTING,
    JUMPING,
    FALLING,
    LANDING,
    CROUCHING,
    INTERACTING,
    PICKING_UP,
    HOLDING,
    RELEASING,
    TOOL_USING,
    INSPECTING
}

data class CharacterAnimInput(
    val horizontalSpeed: Float = 0f,
    val forwardDot: Float = 0f,
    val rightDot: Float = 0f,
    val verticalVel: Float = 0f,
    val onGround: Boolean = true,
    val isCrouching: Boolean = false,
    val isSprinting: Boolean = false,
    val isGrabbing: Boolean = false,
    val grabbedWorldPos: Vec3? = null,
    val targetedWorldPos: Vec3? = null,
    val selectedTool: ToolType = ToolType.GRAB,
    val activeCatalogToolId: String = "HAMMER",
    val toolActionPulse: Float = 0f,
    val landingImpulse: Float = 0f,
    val interactPulse: Float = 0f,
    val releasePulse: Float = 0f,
    val cameraYaw: Float = 0f,
    val cameraPitch: Float = 0f,
    val lookDirWeight: Float = 1f,
    val weaponActionState: WeaponActionState = WeaponActionState.HOLSTERED,
    val isAiming: Boolean = false,
    val recoilVisualOffset: Float = 0f
)

class AnimationStateMachine {

    var baseState: CharacterAnimState = CharacterAnimState.IDLE
        private set
    var upperState: CharacterAnimState = CharacterAnimState.IDLE
        private set

    private var prevClipName: String = "Idle_Neutral"
    private var activeClipName: String = "Idle_Neutral"
    private var blendWeight: Float = 1f
    private var clipTime: Float = 0f
    private var prevClipTime: Float = 0f

    private var upperClipTime: Float = 0f
    private var blinkTimer: Float = 2.4f
    private var blinkDurationLeft: Float = 0f
    private var idleCycleTime: Float = 0f
    private var wasOnGround: Boolean = true
    private var landingHoldTimer: Float = 0f
    private var pickupHoldTimer: Float = 0f
    private var interactHoldTimer: Float = 0f
    private var releaseHoldTimer: Float = 0f
    private var prevGrabbing: Boolean = false

    private val scratchTrans = Array(BoneIds.TOTAL_BONES) { Vec3.ZERO }
    private val scratchRots = Array(BoneIds.TOTAL_BONES) { Quat.IDENTITY }

    fun reset() {
        baseState = CharacterAnimState.IDLE
        upperState = CharacterAnimState.IDLE
        activeClipName = "Idle_Neutral"
        prevClipName = "Idle_Neutral"
        blendWeight = 1f
        clipTime = 0f
        prevClipTime = 0f
        upperClipTime = 0f
        blinkTimer = 2.4f
        blinkDurationLeft = 0f
        landingHoldTimer = 0f
        pickupHoldTimer = 0f
        interactHoldTimer = 0f
        releaseHoldTimer = 0f
        wasOnGround = true
        prevGrabbing = false
    }

    fun update(
        dt: Float,
        input: CharacterAnimInput,
        bones: List<GlbBoneNode>,
        clips: Map<String, GlbAnimationClip>,
        outPose: SkeletonPose
    ) {
        val safeDt = dt.coerceIn(0f, 0.08f)
        outPose.resetToBind(bones)

        // Track ground impact transitions
        if (!wasOnGround && input.onGround && (input.landingImpulse > 0.4f || abs(input.verticalVel) > 1.8f)) {
            landingHoldTimer = 0.28f
        }
        wasOnGround = input.onGround
        if (landingHoldTimer > 0f) landingHoldTimer = (landingHoldTimer - safeDt).coerceAtLeast(0f)

        // Track pickup/release/interact one-shots
        if (!prevGrabbing && input.isGrabbing) pickupHoldTimer = 0.42f
        if (prevGrabbing && !input.isGrabbing) releaseHoldTimer = 0.30f
        prevGrabbing = input.isGrabbing

        if (input.interactPulse > 0.6f) interactHoldTimer = 0.35f
        if (input.releasePulse > 0.6f) releaseHoldTimer = 0.30f

        pickupHoldTimer = (pickupHoldTimer - safeDt).coerceAtLeast(0f)
        interactHoldTimer = (interactHoldTimer - safeDt).coerceAtLeast(0f)
        releaseHoldTimer = (releaseHoldTimer - safeDt).coerceAtLeast(0f)

        // 1. Resolve Lower-Body / Locomotion Base State
        val spd = input.horizontalSpeed
        val nextBase = when {
            !input.onGround && input.verticalVel > 1.0f -> CharacterAnimState.JUMPING
            !input.onGround -> CharacterAnimState.FALLING
            landingHoldTimer > 0.01f && spd < 1.5f -> CharacterAnimState.LANDING
            input.isCrouching -> CharacterAnimState.CROUCHING
            spd < 0.22f -> CharacterAnimState.IDLE
            input.isSprinting && spd > 6.2f -> CharacterAnimState.SPRINTING
            spd > 4.2f -> CharacterAnimState.JOGGING
            else -> CharacterAnimState.WALKING
        }
        baseState = nextBase

        val firearmHolstered = FirearmCatalog.isFirearmTool(input.activeCatalogToolId) &&
            input.weaponActionState == WeaponActionState.HOLSTERED

        // 2. Resolve Upper-Body Interaction / Tool State
        upperState = when {
            pickupHoldTimer > 0.01f -> CharacterAnimState.PICKING_UP
            input.isGrabbing -> CharacterAnimState.HOLDING
            releaseHoldTimer > 0.01f -> CharacterAnimState.RELEASING
            !firearmHolstered && input.toolActionPulse > 0.05f && input.selectedTool == ToolType.CATALOG_TOOL -> CharacterAnimState.TOOL_USING
            interactHoldTimer > 0.01f -> CharacterAnimState.INTERACTING
            input.selectedTool == ToolType.INSPECT -> CharacterAnimState.INSPECTING
            else -> nextBase
        }

        // 3. Choose Base Clip & Playback Speed Proportional to Ground Speed
        val targetClip = when (baseState) {
            CharacterAnimState.IDLE -> {
                idleCycleTime += safeDt
                if ((idleCycleTime % 11.0f) > 7.8f) "Idle_Shift" else "Idle_Neutral"
            }
            CharacterAnimState.WALKING -> {
                val fwd = input.forwardDot
                val rgt = input.rightDot
                if (abs(rgt) > abs(fwd) * 1.15f) {
                    if (rgt > 0f) "Walk_Right" else "Walk_Left"
                } else {
                    if (fwd >= -0.15f) "Walk_Fwd" else "Walk_Back"
                }
            }
            CharacterAnimState.JOGGING -> "Jog_Fwd"
            CharacterAnimState.SPRINTING -> "Sprint_Fwd"
            CharacterAnimState.JUMPING -> "Jump_Start"
            CharacterAnimState.FALLING -> "Jump_Air"
            CharacterAnimState.LANDING -> "Landing"
            CharacterAnimState.CROUCHING -> if (spd > 0.22f) "Crouch_Walk" else "Crouch_Idle"
            else -> "Idle_Neutral"
        }

        if (targetClip != activeClipName) {
            prevClipName = activeClipName
            prevClipTime = clipTime
            activeClipName = targetClip
            clipTime = 0f
            blendWeight = 0f
        }
        blendWeight = (blendWeight + safeDt * 7.5f).coerceIn(0f, 1f)

        val playbackScale = when (baseState) {
            CharacterAnimState.WALKING -> (spd / 3.2f).coerceIn(0.45f, 1.45f)
            CharacterAnimState.JOGGING -> (spd / 5.4f).coerceIn(0.75f, 1.35f)
            CharacterAnimState.SPRINTING -> (spd / 7.8f).coerceIn(0.85f, 1.40f)
            CharacterAnimState.CROUCHING -> if (spd > 0.22f) (spd / 2.2f).coerceIn(0.5f, 1.3f) else 1f
            else -> 1f
        }

        val activeClip = clips[activeClipName]
        val prevClip = clips[prevClipName]
        clipTime += safeDt * playbackScale
        prevClipTime += safeDt * playbackScale

        val loopActive = baseState != CharacterAnimState.JUMPING && baseState != CharacterAnimState.LANDING
        if (activeClip != null) {
            sampleClip(activeClip, clipTime, loopActive, bones, scratchTrans, scratchRots)
            for (i in 0 until min(outPose.boneCount, bones.size)) {
                outPose.localTranslations[i] = scratchTrans[i]
                outPose.localRotations[i] = scratchRots[i]
            }
        }
        if (blendWeight < 0.995f && prevClip != null) {
            sampleClip(prevClip, prevClipTime, true, bones, scratchTrans, scratchRots)
            for (i in 0 until min(outPose.boneCount, bones.size)) {
                outPose.localTranslations[i] = scratchTrans[i].lerp(outPose.localTranslations[i], blendWeight)
                outPose.localRotations[i] = scratchRots[i].slerp(outPose.localRotations[i], blendWeight)
            }
        }

        // 4. Overlay Upper-Body Clip for Carrying / Tool Use / Inspection / Interaction
        val upperClipName = when (upperState) {
            CharacterAnimState.PICKING_UP -> "Reach_Pick"
            CharacterAnimState.HOLDING -> "Hold_Carry"
            CharacterAnimState.RELEASING -> "Button_Press"
            CharacterAnimState.TOOL_USING -> "Tool_Use"
            CharacterAnimState.INTERACTING -> "Button_Press"
            CharacterAnimState.INSPECTING -> "Inspect_Item"
            else -> null
        }
        if (upperClipName != null) {
            upperClipTime += safeDt
            val uClip = clips[upperClipName]
            if (uClip != null) {
                sampleClip(uClip, upperClipTime, upperState == CharacterAnimState.HOLDING || upperState == CharacterAnimState.INSPECTING, bones, scratchTrans, scratchRots)
                val upperWeight = if (upperState == CharacterAnimState.TOOL_USING) input.toolActionPulse.coerceIn(0.35f, 1f) else 0.92f
                for (bi in UPPER_BODY_BONES) {
                    if (bi < outPose.boneCount) {
                        outPose.localRotations[bi] = outPose.localRotations[bi].slerp(scratchRots[bi], upperWeight)
                    }
                }
            }
        } else {
            upperClipTime = 0f
        }

        // 5. Continuous Equipped Tool Ready Pose & Articulated 28-Finger Hand Pose
        applyEquippedHandAndArmPosture(input, outPose)

        // 6. Natural Organic Eye Blinking + Breathing Micro-Motion
        blinkTimer -= safeDt
        if (blinkTimer <= 0f && blinkDurationLeft <= 0f) {
            blinkDurationLeft = 0.16f
            blinkTimer = 2.8f + ((idleCycleTime * 1.7f) % 2.6f)
        }
        if (blinkDurationLeft > 0f) {
            blinkDurationLeft = (blinkDurationLeft - safeDt).coerceAtLeast(0f)
            val bPhase = sin((1f - blinkDurationLeft / 0.16f) * PI.toFloat()).coerceIn(0f, 1f)
            outPose.blinkLeft = bPhase
            outPose.blinkRight = bPhase
        } else {
            outPose.blinkLeft = 0f
            outPose.blinkRight = 0f
        }
        outPose.browFocus = if (input.toolActionPulse > 0.1f || input.isGrabbing) 0.55f else 0.12f

        // Subtle torso breathing on Chest/Spine_02
        val breath = sin(idleCycleTime * 1.9f) * 0.015f
        outPose.localRotations[BoneIds.CHEST] = (outPose.localRotations[BoneIds.CHEST] *
            Quat.fromAxisAngle(Vec3.RIGHT, breath)).normalized()

        outPose.updateWorldAndSkinMatrices(bones)
    }

    private fun applyEquippedHandAndArmPosture(input: CharacterAnimInput, outPose: SkeletonPose) {
        if (input.isGrabbing) {
            HandPoseLibrary.applyHandPose(outPose, isRightHand = true, HandPoseId.CARRY_HOLD, 1f)
            HandPoseLibrary.applyHandPose(outPose, isRightHand = false, HandPoseId.OPEN_PALM_SUPPORT, 1f)
            return
        }

        when (input.selectedTool) {
            ToolType.CATALOG_TOOL -> {
                if (FirearmCatalog.isFirearmTool(input.activeCatalogToolId) &&
                    (input.weaponActionState == WeaponActionState.HOLSTERED || input.weaponActionState == WeaponActionState.HOLSTERING)
                ) {
                    HandPoseLibrary.applyHandPose(outPose, isRightHand = true, HandPoseId.RELAXED_NATURAL, 1f)
                    HandPoseLibrary.applyHandPose(outPose, isRightHand = false, HandPoseId.RELAXED_NATURAL, 1f)
                    return
                }
                val grip = ToolAttachmentRegistry.getSpec(input.activeCatalogToolId)
                HandPoseLibrary.applyHandPose(outPose, isRightHand = true, grip.primaryHandPose, 1f)
                if (grip.isTwoHanded && grip.secondaryHandPose != null) {
                    HandPoseLibrary.applyHandPose(outPose, isRightHand = false, grip.secondaryHandPose, 1f)
                } else {
                    HandPoseLibrary.applyHandPose(outPose, isRightHand = false, HandPoseId.RELAXED_NATURAL, 1f)
                }
                // Raise right forearm comfortably when carrying catalog tool (or aiming firearm)
                if (baseState == CharacterAnimState.IDLE || baseState == CharacterAnimState.WALKING ||
                    input.toolActionPulse > 0.02f || input.isAiming
                ) {
                    val raiseAmt = when {
                        input.isAiming -> 0.96f
                        input.toolActionPulse > 0.02f -> 0.95f
                        else -> 0.68f
                    }
                    val targetArm = Quat.fromAxisAngle(Vec3.RIGHT, 0.36f) *
                        Quat.fromAxisAngle(Vec3.UP, 0.18f) *
                        Quat.fromAxisAngle(Vec3(0f, 0f, 1f), -0.44f)
                    val targetElbow = Quat.fromAxisAngle(Vec3.RIGHT, -0.78f - input.toolActionPulse * 0.25f)
                    outPose.localRotations[BoneIds.RIGHT_UPPER_ARM] =
                        outPose.localRotations[BoneIds.RIGHT_UPPER_ARM].slerp(targetArm, raiseAmt)
                    outPose.localRotations[BoneIds.RIGHT_ELBOW] =
                        outPose.localRotations[BoneIds.RIGHT_ELBOW].slerp(targetElbow, raiseAmt)
                }
            }
            ToolType.INSPECT -> {
                HandPoseLibrary.applyHandPose(outPose, isRightHand = true, HandPoseId.POINTING_INDEX, 1f)
                HandPoseLibrary.applyHandPose(outPose, isRightHand = false, HandPoseId.RELAXED_NATURAL, 1f)
            }
            ToolType.CONSTRAINT -> {
                HandPoseLibrary.applyHandPose(outPose, isRightHand = true, HandPoseId.PINCH_PICKUP, 1f)
                HandPoseLibrary.applyHandPose(outPose, isRightHand = false, HandPoseId.OPEN_PALM_SUPPORT, 0.8f)
            }
            ToolType.IMPULSE -> {
                HandPoseLibrary.applyHandPose(outPose, isRightHand = true, HandPoseId.OPEN_PALM_SUPPORT, 1f)
                HandPoseLibrary.applyHandPose(outPose, isRightHand = false, HandPoseId.RELAXED_NATURAL, 1f)
            }
            ToolType.GRAB -> {
                HandPoseLibrary.applyHandPose(outPose, isRightHand = true, HandPoseId.RELAXED_NATURAL, 1f)
                HandPoseLibrary.applyHandPose(outPose, isRightHand = false, HandPoseId.RELAXED_NATURAL, 1f)
            }
        }
    }

    private fun sampleClip(
        clip: GlbAnimationClip,
        rawTime: Float,
        loop: Boolean,
        bones: List<GlbBoneNode>,
        outTrans: Array<Vec3>,
        outRots: Array<Quat>
    ) {
        for (i in 0 until min(outTrans.size, bones.size)) {
            outTrans[i] = bones[i].bindLocalTranslation
            outRots[i] = Quat.IDENTITY
        }
        val dur = clip.durationSec.coerceAtLeast(0.05f)
        val t = if (loop) (rawTime % dur + dur) % dur else rawTime.coerceIn(0f, dur)

        for (ch in clip.channels) {
            val times = ch.times
            if (times.isEmpty() || ch.boneIndex !in outTrans.indices) continue
            var k0 = 0
            var k1 = 0
            var frac = 0f
            if (t <= times[0]) {
                k0 = 0; k1 = 0
            } else if (t >= times.last()) {
                k0 = times.lastIndex; k1 = times.lastIndex
            } else {
                for (k in 0 until times.lastIndex) {
                    if (t >= times[k] && t <= times[k + 1]) {
                        k0 = k; k1 = k + 1
                        val span = (times[k1] - times[k0]).coerceAtLeast(1e-5f)
                        frac = ((t - times[k0]) / span).coerceIn(0f, 1f)
                        break
                    }
                }
            }
            when (ch.path) {
                GlbChannelPath.TRANSLATION -> {
                    val v0 = Vec3(ch.values[k0 * 3], ch.values[k0 * 3 + 1], ch.values[k0 * 3 + 2])
                    val v1 = Vec3(ch.values[k1 * 3], ch.values[k1 * 3 + 1], ch.values[k1 * 3 + 2])
                    outTrans[ch.boneIndex] = v0.lerp(v1, frac)
                }
                GlbChannelPath.ROTATION -> {
                    val q0 = Quat(ch.values[k0 * 4], ch.values[k0 * 4 + 1], ch.values[k0 * 4 + 2], ch.values[k0 * 4 + 3]).normalized()
                    val q1 = Quat(ch.values[k1 * 4], ch.values[k1 * 4 + 1], ch.values[k1 * 4 + 2], ch.values[k1 * 4 + 3]).normalized()
                    outRots[ch.boneIndex] = q0.slerp(q1, frac)
                }
            }
        }
    }

    companion object {
        private val UPPER_BODY_BONES = intArrayOf(
            BoneIds.SPINE_01, BoneIds.SPINE_02, BoneIds.CHEST, BoneIds.NECK, BoneIds.HEAD,
            BoneIds.LEFT_SHOULDER, BoneIds.LEFT_UPPER_ARM, BoneIds.LEFT_ELBOW, BoneIds.LEFT_FOREARM, BoneIds.LEFT_WRIST, BoneIds.LEFT_HAND,
            BoneIds.RIGHT_SHOULDER, BoneIds.RIGHT_UPPER_ARM, BoneIds.RIGHT_ELBOW, BoneIds.RIGHT_FOREARM, BoneIds.RIGHT_WRIST, BoneIds.RIGHT_HAND
        )
    }
}
