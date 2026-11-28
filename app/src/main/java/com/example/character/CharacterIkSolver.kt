package com.example.character

import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.CollisionDetector
import com.example.physics.StaticProp
import com.example.render.gltf.GlbBoneNode
import com.example.weapons.FirearmCatalog
import com.example.weapons.WeaponActionState
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class IkApplyResult(
    val leftFootHitDist: Float? = null,
    val rightFootHitDist: Float? = null,
    val leftHandReached: Boolean = false,
    val rightHandReached: Boolean = false
)

object CharacterIkSolver {

    /**
     * Applies Look IK, Foot IK (ground/stair/slope adaptation), Hand/Reach IK, and Two-Handed Tool IK
     * onto an evaluated SkeletonPose and updates world + GPU skinning matrices.
     */
    fun solveFullBodyIk(
        pose: SkeletonPose,
        bones: List<GlbBoneNode>,
        characterWorldPos: Vec3,
        characterYawRad: Float,
        input: CharacterAnimInput,
        staticProps: List<StaticProp> = emptyList(),
        isFirstPerson: Boolean = false
    ): IkApplyResult {
        val invCharRot = Quat.fromAxisAngle(Vec3.UP, characterYawRad)
        val charRot = Quat.fromAxisAngle(Vec3.UP, -characterYawRad)

        // 1. Look IK (Head, Neck, Chest aim toward pitch/target without over-twisting torso)
        val pitchAim = input.cameraPitch.coerceIn(-0.85f, 0.85f) * input.lookDirWeight
        if (abs(pitchAim) > 0.005f) {
            pose.localRotations[BoneIds.SPINE_02] = (pose.localRotations[BoneIds.SPINE_02] *
                Quat.fromAxisAngle(Vec3.RIGHT, pitchAim * 0.16f)).normalized()
            pose.localRotations[BoneIds.NECK] = (pose.localRotations[BoneIds.NECK] *
                Quat.fromAxisAngle(Vec3.RIGHT, pitchAim * 0.28f)).normalized()
            pose.localRotations[BoneIds.HEAD] = (pose.localRotations[BoneIds.HEAD] *
                Quat.fromAxisAngle(Vec3.RIGHT, pitchAim * 0.45f)).normalized()
        }

        // Subtle eye gaze tracking toward targeted object or camera look direction
        if (input.targetedWorldPos != null) {
            val toTargetChar = invCharRot.rotate(input.targetedWorldPos - characterWorldPos)
            val eyeOrigin = pose.boneWorldPosition(BoneIds.LEFT_EYE).lerp(pose.boneWorldPosition(BoneIds.RIGHT_EYE), 0.5f)
            val dirLocal = (toTargetChar - eyeOrigin).normalized()
            val eyeYaw = (-dirLocal.x).coerceIn(-0.28f, 0.28f)
            val eyePitch = (dirLocal.y).coerceIn(-0.22f, 0.22f)
            val eyeQ = Quat.fromAxisAngle(Vec3.UP, eyeYaw) * Quat.fromAxisAngle(Vec3.RIGHT, eyePitch)
            pose.localRotations[BoneIds.LEFT_EYE] = eyeQ
            pose.localRotations[BoneIds.RIGHT_EYE] = eyeQ
        } else {
            val lookEye = Quat.fromAxisAngle(Vec3.RIGHT, (pitchAim * 0.18f).coerceIn(-0.20f, 0.20f))
            pose.localRotations[BoneIds.LEFT_EYE] = lookEye
            pose.localRotations[BoneIds.RIGHT_EYE] = lookEye
        }

        pose.updateWorldAndSkinMatrices(bones)

        // 2. Foot IK on ground / stairs / slopes (skip while airborne or in first-person arms view)
        var leftFootHitDist: Float? = null
        var rightFootHitDist: Float? = null
        if (input.onGround && !isFirstPerson && staticProps.isNotEmpty()) {
            val lFootChar = pose.boneWorldPosition(BoneIds.LEFT_FOOT)
            val rFootChar = pose.boneWorldPosition(BoneIds.RIGHT_FOOT)
            val lRayOrigin = characterWorldPos + charRot.rotate(Vec3(lFootChar.x, 0.72f, lFootChar.z))
            val rRayOrigin = characterWorldPos + charRot.rotate(Vec3(rFootChar.x, 0.72f, rFootChar.z))

            val lHit = CollisionDetector.raycastBodiesAndStatics(lRayOrigin, Vec3.DOWN, 1.35f, emptyList(), staticProps)
            val rHit = CollisionDetector.raycastBodiesAndStatics(rRayOrigin, Vec3.DOWN, 1.35f, emptyList(), staticProps)

            val lGroundRelY = lHit?.point?.let { it.y - characterWorldPos.y } ?: 0f
            val rGroundRelY = rHit?.point?.let { it.y - characterWorldPos.y } ?: 0f
            if (lHit != null) leftFootHitDist = lHit.distance
            if (rHit != null) rightFootHitDist = rHit.distance

            // Lower pelvis smoothly to reach lowest foot without stretching legs
            val pelvisDrop = min(lGroundRelY, rGroundRelY).coerceIn(-0.26f, 0.06f)
            if (abs(pelvisDrop) > 0.005f) {
                pose.localTranslations[BoneIds.PELVIS] =
                    pose.localTranslations[BoneIds.PELVIS] + Vec3(0f, pelvisDrop, 0f)
                pose.updateWorldAndSkinMatrices(bones)
            }

            // Bend higher foot leg via 2-bone IK to plant cleanly on step/slope
            if (lHit != null && abs(lGroundRelY - pelvisDrop) > 0.015f) {
                val targetFootLocal = Vec3(lFootChar.x, lGroundRelY + 0.042f, lFootChar.z)
                solveTwoBoneLegIk(pose, bones, isLeft = true, targetAnkleLocal = targetFootLocal)
            }
            if (rHit != null && abs(rGroundRelY - pelvisDrop) > 0.015f) {
                val targetFootLocal = Vec3(rFootChar.x, rGroundRelY + 0.042f, rFootChar.z)
                solveTwoBoneLegIk(pose, bones, isLeft = false, targetAnkleLocal = targetFootLocal)
            }
            pose.updateWorldAndSkinMatrices(bones)
        }

        // 3. Hand IK & Two-Handed Tool IK
        var rightReached = false
        var leftReached = false

        val eyeLocal = Vec3(0f, if (input.isCrouching) 1.08f else 1.54f, -0.08f)
        val lookLocal = Quat.fromAxisAngle(Vec3.RIGHT, pitchAim).rotate(Vec3.FORWARD)

        if (input.isGrabbing && input.grabbedWorldPos != null) {
            val grabbedChar = invCharRot.rotate(input.grabbedWorldPos - characterWorldPos)
            val reachDir = (grabbedChar - eyeLocal).normalized()
            val reachClamped = eyeLocal + reachDir * min(0.68f, (grabbedChar - eyeLocal).length())
            val rightTarget = reachClamped + Vec3(0.14f, -0.06f, 0.02f)
            val leftTarget = reachClamped + Vec3(-0.14f, -0.06f, 0.02f)
            rightReached = solveTwoBoneArmIk(pose, bones, isRight = true, targetHandLocal = rightTarget, blend = 0.88f)
            leftReached = solveTwoBoneArmIk(pose, bones, isRight = false, targetHandLocal = leftTarget, blend = 0.85f)
        } else if (input.selectedTool == com.example.physics.ToolType.CATALOG_TOOL) {
            val isFirearm = FirearmCatalog.isFirearmTool(input.activeCatalogToolId)
            val firearmHolstered = isFirearm && (input.weaponActionState == WeaponActionState.HOLSTERED || input.weaponActionState == WeaponActionState.HOLSTERING)
            if (!firearmHolstered) {
                val grip = ToolAttachmentRegistry.getSpec(input.activeCatalogToolId)
                val toolHoldBase = if (isFirearm) {
                    val kick = input.recoilVisualOffset * 0.04f
                    when {
                        input.weaponActionState == WeaponActionState.RELOADING ->
                            eyeLocal + lookLocal * 0.42f + Vec3(0.10f, -0.26f, 0f)
                        input.isAiming ->
                            eyeLocal + lookLocal * (0.44f - kick) + Vec3(0.06f, -0.11f, 0f)
                        else ->
                            eyeLocal + lookLocal * (0.48f - kick) + Vec3(0.16f, -0.18f, 0f)
                    }
                } else {
                    val swing = sin(input.toolActionPulse * Math.PI.toFloat())
                    eyeLocal + lookLocal * (0.48f + swing * 0.14f) + Vec3(0.19f, -0.22f + swing * 0.06f, 0f)
                }
                val rBlend = when {
                    isFirstPerson -> 0.95f
                    isFirearm && input.isAiming -> 0.92f
                    else -> 0.72f
                }
                rightReached = solveTwoBoneArmIk(pose, bones, isRight = true, targetHandLocal = toolHoldBase, blend = rBlend)
                if (grip.isTwoHanded) {
                    val secLocal = toolHoldBase + grip.secondaryLocalGripOffset
                    leftReached = solveTwoBoneArmIk(pose, bones, isRight = false, targetHandLocal = secLocal, blend = if (isFirstPerson) 0.95f else 0.85f)
                }
            }
        } else if (input.interactPulse > 0.15f && input.targetedWorldPos != null) {
            val tgtChar = invCharRot.rotate(input.targetedWorldPos - characterWorldPos)
            val dir = (tgtChar - eyeLocal).normalized()
            val reachPt = eyeLocal + dir * min(0.66f, (tgtChar - eyeLocal).length())
            rightReached = solveTwoBoneArmIk(pose, bones, isRight = true, targetHandLocal = reachPt, blend = input.interactPulse.coerceIn(0f, 1f))
        }

        pose.updateWorldAndSkinMatrices(bones)
        return IkApplyResult(leftFootHitDist, rightFootHitDist, leftReached, rightReached)
    }

    /**
     * Analytical 2-bone Arm IK using law of cosines + pole hinge bias toward natural elbow drop.
     */
    fun solveTwoBoneArmIk(
        pose: SkeletonPose,
        bones: List<GlbBoneNode>,
        isRight: Boolean,
        targetHandLocal: Vec3,
        blend: Float = 1f
    ): Boolean {
        val shIdx = if (isRight) BoneIds.RIGHT_UPPER_ARM else BoneIds.LEFT_UPPER_ARM
        val elIdx = if (isRight) BoneIds.RIGHT_ELBOW else BoneIds.LEFT_ELBOW
        val wrIdx = if (isRight) BoneIds.RIGHT_WRIST else BoneIds.LEFT_WRIST

        pose.updateWorldAndSkinMatrices(bones)
        val pRoot = pose.boneWorldPosition(shIdx)
        val pMid = pose.boneWorldPosition(elIdx)
        val pEnd = pose.boneWorldPosition(wrIdx)

        val l1 = (pMid - pRoot).length().coerceAtLeast(0.05f)
        val l2 = (pEnd - pMid).length().coerceAtLeast(0.05f)
        val poleHint = Vec3(if (isRight) 0.35f else -0.35f, -0.7f, 0.35f) // elbows flare back/down

        val solved = solveTwoBonePositions(pRoot, pMid, pEnd, targetHandLocal, l1, l2, poleHint) ?: return false
        val (newRootDir, newMidDir) = solved

        // Compute upper arm delta rotation in world/character space and map back to parent local
        val parentRot = pose.boneWorldRotation(bones[shIdx].parentBoneIndex, bones)
        val curUpperDir = (pMid - pRoot).normalized()
        val deltaUpperWorld = quatFromTo(curUpperDir, newRootDir)
        val curUpperWorld = pose.boneWorldRotation(shIdx, bones)
        val targetUpperLocal = parentRot.conjugate() * deltaUpperWorld * curUpperWorld
        pose.localRotations[shIdx] = pose.localRotations[shIdx].slerp(targetUpperLocal.normalized(), blend)

        pose.updateWorldAndSkinMatrices(bones)
        val pMidAfter = pose.boneWorldPosition(elIdx)
        val pEndAfter = pose.boneWorldPosition(wrIdx)
        val curLowerDir = (pEndAfter - pMidAfter).normalized()
        val deltaLowerWorld = quatFromTo(curLowerDir, newMidDir)
        val curLowerWorld = pose.boneWorldRotation(elIdx, bones)
        val elParentWorld = pose.boneWorldRotation(bones[elIdx].parentBoneIndex, bones)
        val targetLowerLocal = elParentWorld.conjugate() * deltaLowerWorld * curLowerWorld
        pose.localRotations[elIdx] = pose.localRotations[elIdx].slerp(targetLowerLocal.normalized(), blend)

        return true
    }

    /**
     * Analytical 2-bone Leg IK (Hip/Thigh -> Knee -> Ankle) with forward knee pole (-Z).
     */
    fun solveTwoBoneLegIk(
        pose: SkeletonPose,
        bones: List<GlbBoneNode>,
        isLeft: Boolean,
        targetAnkleLocal: Vec3,
        blend: Float = 0.85f
    ): Boolean {
        val thIdx = if (isLeft) BoneIds.LEFT_THIGH else BoneIds.RIGHT_THIGH
        val knIdx = if (isLeft) BoneIds.LEFT_KNEE else BoneIds.RIGHT_KNEE
        val akIdx = if (isLeft) BoneIds.LEFT_ANKLE else BoneIds.RIGHT_ANKLE

        pose.updateWorldAndSkinMatrices(bones)
        val pRoot = pose.boneWorldPosition(thIdx)
        val pMid = pose.boneWorldPosition(knIdx)
        val pEnd = pose.boneWorldPosition(akIdx)

        val l1 = (pMid - pRoot).length().coerceAtLeast(0.05f)
        val l2 = (pEnd - pMid).length().coerceAtLeast(0.05f)
        val poleHint = Vec3(0f, 0.25f, -1.0f) // knees bend forward (-Z)

        val solved = solveTwoBonePositions(pRoot, pMid, pEnd, targetAnkleLocal, l1, l2, poleHint) ?: return false
        val (newThighDir, newShinDir) = solved

        val thParent = pose.boneWorldRotation(bones[thIdx].parentBoneIndex, bones)
        val curThighWorld = pose.boneWorldRotation(thIdx, bones)
        val delta1 = quatFromTo((pMid - pRoot).normalized(), newThighDir)
        pose.localRotations[thIdx] = pose.localRotations[thIdx].slerp((thParent.conjugate() * delta1 * curThighWorld).normalized(), blend)

        pose.updateWorldAndSkinMatrices(bones)
        val pMid2 = pose.boneWorldPosition(knIdx)
        val pEnd2 = pose.boneWorldPosition(akIdx)
        val knParent = pose.boneWorldRotation(bones[knIdx].parentBoneIndex, bones)
        val curShinWorld = pose.boneWorldRotation(knIdx, bones)
        val delta2 = quatFromTo((pEnd2 - pMid2).normalized(), newShinDir)
        pose.localRotations[knIdx] = pose.localRotations[knIdx].slerp((knParent.conjugate() * delta2 * curShinWorld).normalized(), blend)
        return true
    }

    private fun solveTwoBonePositions(
        pRoot: Vec3,
        pMid: Vec3,
        pEnd: Vec3,
        target: Vec3,
        l1: Float,
        l2: Float,
        poleBiasDir: Vec3
    ): Pair<Vec3, Vec3>? {
        val toTarget = target - pRoot
        val rawDist = toTarget.length()
        if (rawDist < 1e-4f) return null
        val maxReach = (l1 + l2) * 0.992f
        val minReach = abs(l1 - l2) + 0.02f
        val dist = rawDist.coerceIn(minReach, maxReach)
        val dir = toTarget / rawDist

        // Law of cosines for angle at root
        val cosA = ((l1 * l1 + dist * dist - l2 * l2) / (2f * l1 * dist)).coerceIn(-1f, 1f)
        val sinA = sqrt(max(0f, 1f - cosA * cosA))

        // Orthonormal bend plane toward poleBiasDir
        var bendAxis = poleBiasDir - dir * dir.dot(poleBiasDir)
        if (bendAxis.lengthSq() < 1e-5f) {
            bendAxis = ((pMid - pRoot) - dir * dir.dot(pMid - pRoot))
        }
        if (bendAxis.lengthSq() < 1e-5f) bendAxis = Vec3.FORWARD
        bendAxis = bendAxis.normalized()

        val desiredMid = pRoot + dir * (l1 * cosA) + bendAxis * (l1 * sinA)
        val clampedEnd = pRoot + dir * dist
        return (desiredMid - pRoot).normalized() to (clampedEnd - desiredMid).normalized()
    }

    private fun quatFromTo(from: Vec3, to: Vec3): Quat {
        val a = from.normalized()
        val b = to.normalized()
        val d = a.dot(b).coerceIn(-1f, 1f)
        if (d > 0.9999f) return Quat.IDENTITY
        if (d < -0.9999f) {
            val ortho = if (abs(a.x) < 0.8f) Vec3.RIGHT.cross(a) else Vec3.UP.cross(a)
            return Quat.fromAxisAngle(ortho.normalized(), Math.PI.toFloat())
        }
        val axis = a.cross(b)
        val angle = acos(d)
        return Quat.fromAxisAngle(axis.normalized(), angle)
    }
}
