package com.example.character

import com.example.math.Quat
import com.example.math.Vec3

enum class HandPoseId(val displayName: String) {
    RELAXED_NATURAL("Relaxed Natural"),
    POWER_GRIP("Power Grip"),
    PRECISION_PINCH("Precision Pinch"),
    TRIGGER_GRIP("Trigger Grip"),
    OPEN_PALM_SUPPORT("Open Palm Support"),
    TWO_HAND_GRIP("Two-Handed Grip"),
    POINTING_INDEX("Pointing Index"),
    PINCH_PICKUP("Pinch Pickup"),
    FLAT_HAND_REST("Flat Hand Rest"),
    CARRY_HOLD("Carry Hold")
}

data class FingerCurlProfile(
    val thumbCurl: Float,
    val thumbAbduct: Float,
    val indexCurl: Float,
    val middleCurl: Float,
    val ringCurl: Float,
    val littleCurl: Float,
    val spread: Float = 0.04f
)

object HandPoseLibrary {

    fun profileFor(pose: HandPoseId): FingerCurlProfile = when (pose) {
        HandPoseId.RELAXED_NATURAL -> FingerCurlProfile(
            thumbCurl = 0.18f, thumbAbduct = 0.15f,
            indexCurl = 0.20f, middleCurl = 0.25f, ringCurl = 0.30f, littleCurl = 0.35f, spread = 0.03f
        )
        HandPoseId.POWER_GRIP -> FingerCurlProfile(
            thumbCurl = 0.65f, thumbAbduct = 0.48f,
            indexCurl = 0.92f, middleCurl = 0.96f, ringCurl = 0.96f, littleCurl = 0.92f, spread = 0.01f
        )
        HandPoseId.PRECISION_PINCH -> FingerCurlProfile(
            thumbCurl = 0.52f, thumbAbduct = 0.42f,
            indexCurl = 0.62f, middleCurl = 0.48f, ringCurl = 0.70f, littleCurl = 0.78f, spread = 0.02f
        )
        HandPoseId.TRIGGER_GRIP -> FingerCurlProfile(
            thumbCurl = 0.55f, thumbAbduct = 0.40f,
            indexCurl = 0.46f, middleCurl = 0.94f, ringCurl = 0.96f, littleCurl = 0.94f, spread = 0.02f
        )
        HandPoseId.OPEN_PALM_SUPPORT -> FingerCurlProfile(
            thumbCurl = 0.08f, thumbAbduct = 0.34f,
            indexCurl = 0.08f, middleCurl = 0.08f, ringCurl = 0.10f, littleCurl = 0.12f, spread = 0.07f
        )
        HandPoseId.TWO_HAND_GRIP -> FingerCurlProfile(
            thumbCurl = 0.62f, thumbAbduct = 0.45f,
            indexCurl = 0.86f, middleCurl = 0.90f, ringCurl = 0.92f, littleCurl = 0.90f, spread = 0.01f
        )
        HandPoseId.POINTING_INDEX -> FingerCurlProfile(
            thumbCurl = 0.48f, thumbAbduct = 0.30f,
            indexCurl = 0.05f, middleCurl = 0.88f, ringCurl = 0.92f, littleCurl = 0.92f, spread = 0.02f
        )
        HandPoseId.PINCH_PICKUP -> FingerCurlProfile(
            thumbCurl = 0.48f, thumbAbduct = 0.38f,
            indexCurl = 0.56f, middleCurl = 0.46f, ringCurl = 0.58f, littleCurl = 0.66f, spread = 0.03f
        )
        HandPoseId.FLAT_HAND_REST -> FingerCurlProfile(
            thumbCurl = 0.04f, thumbAbduct = 0.18f,
            indexCurl = 0.04f, middleCurl = 0.04f, ringCurl = 0.05f, littleCurl = 0.06f, spread = 0.03f
        )
        HandPoseId.CARRY_HOLD -> FingerCurlProfile(
            thumbCurl = 0.36f, thumbAbduct = 0.36f,
            indexCurl = 0.54f, middleCurl = 0.58f, ringCurl = 0.60f, littleCurl = 0.58f, spread = 0.04f
        )
    }

    /**
     * Applies finger curl rotations across all 14 finger joints on either Left or Right hand.
     * In bind pose, Left palm points down-left (-X, -Y) so curling fingers rotates inward toward +X/+Y (positive Z curl for Left, negative Z for Right).
     */
    fun applyHandPose(pose: SkeletonPose, isRightHand: Boolean, poseId: HandPoseId, blend: Float = 1f) {
        val prof = profileFor(poseId)
        val curlSign = if (isRightHand) -1f else 1f
        val baseIdx = if (isRightHand) BoneIds.RIGHT_THUMB_1 else BoneIds.LEFT_THUMB_1
        val b = blend.coerceIn(0f, 1f)

        fun blendRot(boneIdx: Int, target: Quat) {
            pose.localRotations[boneIdx] = pose.localRotations[boneIdx].slerp(target, b)
        }

        // Thumb (2 phalanges): opposes palm inward (-Z/-Y toward fingers)
        val t1Rot = Quat.fromAxisAngle(Vec3(0f, 0f, 1f), curlSign * prof.thumbCurl * 0.55f) *
            Quat.fromAxisAngle(Vec3(1f, 0f, 0f), prof.thumbAbduct * 0.65f)
        val t2Rot = Quat.fromAxisAngle(Vec3(0f, 0f, 1f), curlSign * prof.thumbCurl * 0.72f)
        blendRot(baseIdx + 0, t1Rot)
        blendRot(baseIdx + 1, t2Rot)

        // 4 fingers x 3 phalanges
        val curls = floatArrayOf(prof.indexCurl, prof.middleCurl, prof.ringCurl, prof.littleCurl)
        val spreads = floatArrayOf(-1.5f, -0.4f, 0.5f, 1.4f)
        for (fi in 0 until 4) {
            val fStart = baseIdx + 2 + fi * 3
            val c = curls[fi]
            val mcp = Quat.fromAxisAngle(Vec3(0f, 0f, 1f), curlSign * c * 0.88f) *
                Quat.fromAxisAngle(Vec3(0f, 1f, 0f), curlSign * spreads[fi] * prof.spread)
            val pip = Quat.fromAxisAngle(Vec3(0f, 0f, 1f), curlSign * c * 1.12f)
            val dip = Quat.fromAxisAngle(Vec3(0f, 0f, 1f), curlSign * c * 0.75f)
            blendRot(fStart + 0, mcp)
            blendRot(fStart + 1, pip)
            blendRot(fStart + 2, dip)
        }
    }
}
