package com.example.math

import kotlin.math.sqrt

/** Column-major 3x3 matrix for world-space inertia tensor transforms. */
class Mat3(val m: FloatArray = FloatArray(9)) {

    fun multiplyVec(v: Vec3): Vec3 = Vec3(
        m[0] * v.x + m[3] * v.y + m[6] * v.z,
        m[1] * v.x + m[4] * v.y + m[7] * v.z,
        m[2] * v.x + m[5] * v.y + m[8] * v.z
    )

    fun axisX(): Vec3 = Vec3(m[0], m[1], m[2])
    fun axisY(): Vec3 = Vec3(m[3], m[4], m[5])
    fun axisZ(): Vec3 = Vec3(m[6], m[7], m[8])

    companion object {
        val IDENTITY = Mat3(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f))

        /** Computes R * diag(d) * R^T for world inverse inertia tensor. */
        fun computeWorldInvInertia(rotMat: Mat3, localInvDiag: Vec3): Mat3 {
            val r = rotMat.m
            val dx = localInvDiag.x
            val dy = localInvDiag.y
            val dz = localInvDiag.z

            val t00 = r[0] * dx; val t01 = r[3] * dy; val t02 = r[6] * dz
            val t10 = r[1] * dx; val t11 = r[4] * dy; val t12 = r[7] * dz
            val t20 = r[2] * dx; val t21 = r[5] * dy; val t22 = r[8] * dz

            return Mat3(
                floatArrayOf(
                    t00 * r[0] + t01 * r[3] + t02 * r[6],
                    t10 * r[0] + t11 * r[3] + t12 * r[6],
                    t20 * r[0] + t21 * r[3] + t22 * r[6],

                    t00 * r[1] + t01 * r[4] + t02 * r[7],
                    t10 * r[1] + t11 * r[4] + t12 * r[7],
                    t20 * r[1] + t21 * r[4] + t22 * r[7],

                    t00 * r[2] + t01 * r[5] + t02 * r[8],
                    t10 * r[2] + t11 * r[5] + t12 * r[8],
                    t20 * r[2] + t21 * r[5] + t22 * r[8]
                )
            )
        }
    }
}

/** 6-plane frustum extracted from column-major ViewProjection 4x4 matrix. */
class Frustum {
    private val planes = Array(6) { FloatArray(4) }

    fun updateFromViewProj(vp: FloatArray) {
        // Left: row3 + row0
        setPlane(0, vp[3] + vp[0], vp[7] + vp[4], vp[11] + vp[8], vp[15] + vp[12])
        // Right: row3 - row0
        setPlane(1, vp[3] - vp[0], vp[7] - vp[4], vp[11] - vp[8], vp[15] - vp[12])
        // Bottom: row3 + row1
        setPlane(2, vp[3] + vp[1], vp[7] + vp[5], vp[11] + vp[9], vp[15] + vp[13])
        // Top: row3 - row1
        setPlane(3, vp[3] - vp[1], vp[7] - vp[5], vp[11] - vp[9], vp[15] - vp[13])
        // Near: row3 + row2
        setPlane(4, vp[3] + vp[2], vp[7] + vp[6], vp[11] + vp[10], vp[15] + vp[14])
        // Far: row3 - row2
        setPlane(5, vp[3] - vp[2], vp[7] - vp[6], vp[11] - vp[10], vp[15] - vp[14])
    }

    private fun setPlane(i: Int, a: Float, b: Float, c: Float, d: Float) {
        val len = sqrt(a * a + b * b + c * c)
        if (len > 1e-6f) {
            planes[i][0] = a / len
            planes[i][1] = b / len
            planes[i][2] = c / len
            planes[i][3] = d / len
        }
    }

    fun isSphereVisible(center: Vec3, radius: Float): Boolean {
        for (i in 0 until 6) {
            val p = planes[i]
            val dist = p[0] * center.x + p[1] * center.y + p[2] * center.z + p[3]
            if (dist < -radius) return false
        }
        return true
    }
}
