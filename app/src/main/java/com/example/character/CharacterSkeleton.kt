package com.example.character

import com.example.math.Quat
import com.example.math.Vec3
import com.example.render.gltf.GlbBoneNode

object BoneIds {
    const val ROOT = 0
    const val PELVIS = 1
    const val SPINE = 2
    const val SPINE_01 = 3
    const val SPINE_02 = 4
    const val CHEST = 5
    const val NECK = 6
    const val HEAD = 7
    const val JAW = 8
    const val LEFT_EYE = 9
    const val RIGHT_EYE = 10

    const val LEFT_SHOULDER = 11
    const val LEFT_UPPER_ARM = 12
    const val LEFT_ELBOW = 13
    const val LEFT_FOREARM = 14
    const val LEFT_WRIST = 15
    const val LEFT_HAND = 16
    const val LEFT_THUMB_1 = 17
    const val LEFT_THUMB_2 = 18
    const val LEFT_INDEX_1 = 19
    const val LEFT_INDEX_2 = 20
    const val LEFT_INDEX_3 = 21
    const val LEFT_MIDDLE_1 = 22
    const val LEFT_MIDDLE_2 = 23
    const val LEFT_MIDDLE_3 = 24
    const val LEFT_RING_1 = 25
    const val LEFT_RING_2 = 26
    const val LEFT_RING_3 = 27
    const val LEFT_LITTLE_1 = 28
    const val LEFT_LITTLE_2 = 29
    const val LEFT_LITTLE_3 = 30

    const val RIGHT_SHOULDER = 31
    const val RIGHT_UPPER_ARM = 32
    const val RIGHT_ELBOW = 33
    const val RIGHT_FOREARM = 34
    const val RIGHT_WRIST = 35
    const val RIGHT_HAND = 36
    const val RIGHT_THUMB_1 = 37
    const val RIGHT_THUMB_2 = 38
    const val RIGHT_INDEX_1 = 39
    const val RIGHT_INDEX_2 = 40
    const val RIGHT_INDEX_3 = 41
    const val RIGHT_MIDDLE_1 = 42
    const val RIGHT_MIDDLE_2 = 43
    const val RIGHT_MIDDLE_3 = 44
    const val RIGHT_RING_1 = 45
    const val RIGHT_RING_2 = 46
    const val RIGHT_RING_3 = 47
    const val RIGHT_LITTLE_1 = 48
    const val RIGHT_LITTLE_2 = 49
    const val RIGHT_LITTLE_3 = 50

    const val LEFT_HIP = 51
    const val LEFT_THIGH = 52
    const val LEFT_KNEE = 53
    const val LEFT_CALF = 54
    const val LEFT_ANKLE = 55
    const val LEFT_FOOT = 56
    const val LEFT_TOES = 57

    const val RIGHT_HIP = 58
    const val RIGHT_THIGH = 59
    const val RIGHT_KNEE = 60
    const val RIGHT_CALF = 61
    const val RIGHT_ANKLE = 62
    const val RIGHT_FOOT = 63
    const val RIGHT_TOES = 64

    const val TOTAL_BONES = 65
}

class SkeletonPose(val boneCount: Int = BoneIds.TOTAL_BONES) {
    val localTranslations = Array(boneCount) { Vec3.ZERO }
    val localRotations = Array(boneCount) { Quat.IDENTITY }

    // Computed character-space world transforms & GPU skinning matrices (column-major 4x4)
    val worldMatrices = Array(boneCount) { identityMat4() }
    val skinMatrices = FloatArray(boneCount * 16)

    // Facial morph target weights
    var blinkLeft: Float = 0f
    var blinkRight: Float = 0f
    var browFocus: Float = 0f
    var mouthOpen: Float = 0f

    fun resetToBind(bones: List<GlbBoneNode>) {
        val count = minOf(boneCount, bones.size)
        for (i in 0 until count) {
            localTranslations[i] = bones[i].bindLocalTranslation
            localRotations[i] = Quat.IDENTITY
        }
        blinkLeft = 0f
        blinkRight = 0f
        browFocus = 0f
        mouthOpen = 0f
    }

    fun updateWorldAndSkinMatrices(bones: List<GlbBoneNode>) {
        val count = minOf(boneCount, bones.size)
        val localMat = FloatArray(16)
        for (i in 0 until count) {
            val bone = bones[i]
            localRotations[i].writeModelMatrix(localTranslations[i], Vec3.ONE, localMat)
            if (bone.parentBoneIndex >= 0 && bone.parentBoneIndex < i) {
                mulMat4(worldMatrices[bone.parentBoneIndex], localMat, worldMatrices[i])
            } else {
                System.arraycopy(localMat, 0, worldMatrices[i], 0, 16)
            }
            val skinTmp = FloatArray(16)
            mulMat4(worldMatrices[i], bone.inverseBindMatrix, skinTmp)
            System.arraycopy(skinTmp, 0, skinMatrices, i * 16, 16)
        }
    }

    fun boneWorldPosition(boneIdx: Int): Vec3 {
        if (boneIdx !in 0 until boneCount) return Vec3.ZERO
        val m = worldMatrices[boneIdx]
        return Vec3(m[12], m[13], m[14])
    }

    fun boneWorldRotation(boneIdx: Int, bones: List<GlbBoneNode>): Quat {
        var acc = Quat.IDENTITY
        var cur = boneIdx
        val chain = IntArray(16)
        var depth = 0
        while (cur in bones.indices && depth < chain.size) {
            chain[depth++] = cur
            cur = bones[cur].parentBoneIndex
        }
        for (k in depth - 1 downTo 0) {
            acc = (acc * localRotations[chain[k]]).normalized()
        }
        return acc
    }

    companion object {
        fun identityMat4(): FloatArray = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )

        fun mulMat4(a: FloatArray, b: FloatArray, out: FloatArray) {
            for (col in 0..3) {
                val c4 = col * 4
                val b0 = b[c4]
                val b1 = b[c4 + 1]
                val b2 = b[c4 + 2]
                val b3 = b[c4 + 3]
                out[c4] = a[0] * b0 + a[4] * b1 + a[8] * b2 + a[12] * b3
                out[c4 + 1] = a[1] * b0 + a[5] * b1 + a[9] * b2 + a[13] * b3
                out[c4 + 2] = a[2] * b0 + a[6] * b1 + a[10] * b2 + a[14] * b3
                out[c4 + 3] = a[3] * b0 + a[7] * b1 + a[11] * b2 + a[15] * b3
            }
        }
    }
}
