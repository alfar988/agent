package com.example.math

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Quat(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val w: Float = 1f) {

    operator fun times(q: Quat): Quat = Quat(
        w * q.x + x * q.w + y * q.z - z * q.y,
        w * q.y - x * q.z + y * q.w + z * q.x,
        w * q.z + x * q.y - y * q.x + z * q.w,
        w * q.w - x * q.x - y * q.y - z * q.z
    )

    fun normalized(): Quat {
        val len = sqrt(x * x + y * y + z * z + w * w)
        return if (len > 1e-6f) Quat(x / len, y / len, z / len, w / len) else IDENTITY
    }

    fun conjugate(): Quat = Quat(-x, -y, -z, w)

    fun rotate(v: Vec3): Vec3 {
        val qx = x; val qy = y; val qz = z; val qw = w
        val ix = qw * v.x + qy * v.z - qz * v.y
        val iy = qw * v.y + qz * v.x - qx * v.z
        val iz = qw * v.z + qx * v.y - qy * v.x
        val iw = -qx * v.x - qy * v.y - qz * v.z
        return Vec3(
            ix * qw + iw * -qx + iy * -qz - iz * -qy,
            iy * qw + iw * -qy + iz * -qx - ix * -qz,
            iz * qw + iw * -qz + ix * -qy - iy * -qx
        )
    }

    fun integrateAngularVelocity(omega: Vec3, dt: Float): Quat {
        val wx = omega.x * dt * 0.5f
        val wy = omega.y * dt * 0.5f
        val wz = omega.z * dt * 0.5f
        val dq = Quat(
            wx * w + wy * z - wz * y,
            wy * w + wz * x - wx * z,
            wz * w + wx * y - wy * x,
            -wx * x - wy * y - wz * z
        )
        return Quat(x + dq.x, y + dq.y, z + dq.z, w + dq.w).normalized()
    }

    fun toMat3(): Mat3 {
        val xx = x * x; val yy = y * y; val zz = z * z
        val xy = x * y; val xz = x * z; val yz = y * z
        val wx = w * x; val wy = w * y; val wz = w * z
        return Mat3(
            floatArrayOf(
                1f - 2f * (yy + zz), 2f * (xy + wz), 2f * (xz - wy),
                2f * (xy - wz), 1f - 2f * (xx + zz), 2f * (yz + wx),
                2f * (xz + wy), 2f * (yz - wx), 1f - 2f * (xx + yy)
            )
        )
    }

    fun writeModelMatrix(pos: Vec3, scale: Vec3, out: FloatArray) {
        val xx = x * x; val yy = y * y; val zz = z * z
        val xy = x * y; val xz = x * z; val yz = y * z
        val wx = w * x; val wy = w * y; val wz = w * z

        out[0] = (1f - 2f * (yy + zz)) * scale.x
        out[1] = (2f * (xy + wz)) * scale.x
        out[2] = (2f * (xz - wy)) * scale.x
        out[3] = 0f

        out[4] = (2f * (xy - wz)) * scale.y
        out[5] = (1f - 2f * (xx + zz)) * scale.y
        out[6] = (2f * (yz + wx)) * scale.y
        out[7] = 0f

        out[8] = (2f * (xz + wy)) * scale.z
        out[9] = (2f * (yz - wx)) * scale.z
        out[10] = (1f - 2f * (xx + yy)) * scale.z
        out[11] = 0f

        out[12] = pos.x
        out[13] = pos.y
        out[14] = pos.z
        out[15] = 1f
    }

    fun slerp(target: Quat, t: Float): Quat {
        var dot = x * target.x + y * target.y + z * target.z + w * target.w
        var tx = target.x; var ty = target.y; var tz = target.z; var tw = target.w
        if (dot < 0f) {
            dot = -dot; tx = -tx; ty = -ty; tz = -tz; tw = -tw
        }
        if (dot > 0.9995f) {
            return Quat(
                x + t * (tx - x),
                y + t * (ty - y),
                z + t * (tz - z),
                w + t * (tw - w)
            ).normalized()
        }
        val theta0 = acos(dot.coerceIn(-1f, 1f))
        val theta = theta0 * t
        val sinTheta = sin(theta)
        val sinTheta0 = sin(theta0)
        val s0 = cos(theta) - dot * sinTheta / sinTheta0
        val s1 = sinTheta / sinTheta0
        return Quat(
            s0 * x + s1 * tx,
            s0 * y + s1 * ty,
            s0 * z + s1 * tz,
            s0 * w + s1 * tw
        ).normalized()
    }

    fun toEulerDegrees(): Vec3 {
        val radToDeg = 180f / PI.toFloat()
        val sinPitch = (2f * (w * y - z * x)).coerceIn(-1f, 1f)
        val pitch = asin(sinPitch)
        val yaw = atan2(2f * (w * z + x * y), 1f - 2f * (y * y + z * z))
        val roll = atan2(2f * (w * x + y * z), 1f - 2f * (x * x + y * y))
        return Vec3(pitch * radToDeg, yaw * radToDeg, roll * radToDeg)
    }

    companion object {
        val IDENTITY = Quat(0f, 0f, 0f, 1f)

        fun fromAxisAngle(axis: Vec3, radians: Float): Quat {
            val n = axis.normalized()
            val half = radians * 0.5f
            val s = sin(half)
            return Quat(n.x * s, n.y * s, n.z * s, cos(half)).normalized()
        }

        fun fromEuler(pitch: Float, yaw: Float, roll: Float): Quat {
            val cy = cos(yaw * 0.5f); val sy = sin(yaw * 0.5f)
            val cp = cos(pitch * 0.5f); val sp = sin(pitch * 0.5f)
            val cr = cos(roll * 0.5f); val sr = sin(roll * 0.5f)
            return Quat(
                sr * cp * cy - cr * sp * sy,
                cr * sp * cy + sr * cp * sy,
                cr * cp * sy - sr * sp * cy,
                cr * cp * cy + sr * sp * sy
            ).normalized()
        }
    }
}
