package com.example.math

import kotlin.math.sqrt

data class Vec3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    operator fun div(s: Float): Vec3 = if (s != 0f) Vec3(x / s, y / s, z / s) else ZERO
    operator fun unaryMinus() = Vec3(-x, -y, -z)

    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z

    fun cross(o: Vec3): Vec3 = Vec3(
        y * o.z - z * o.y,
        z * o.x - x * o.z,
        x * o.y - y * o.x
    )

    fun lengthSq(): Float = x * x + y * y + z * z
    fun length(): Float = sqrt(lengthSq())

    fun normalized(): Vec3 {
        val len = length()
        return if (len > 1e-6f) Vec3(x / len, y / len, z / len) else ZERO
    }

    fun abs(): Vec3 = Vec3(kotlin.math.abs(x), kotlin.math.abs(y), kotlin.math.abs(z))

    fun lerp(target: Vec3, t: Float): Vec3 = Vec3(
        x + (target.x - x) * t,
        y + (target.y - y) * t,
        z + (target.z - z) * t
    )

    fun componentMin(o: Vec3) = Vec3(minOf(x, o.x), minOf(y, o.y), minOf(z, o.z))
    fun componentMax(o: Vec3) = Vec3(maxOf(x, o.x), maxOf(y, o.y), maxOf(z, o.z))
    fun clamp(min: Vec3, max: Vec3) = Vec3(
        x.coerceIn(min.x, max.x),
        y.coerceIn(min.y, max.y),
        z.coerceIn(min.z, max.z)
    )

    fun isFinite(): Boolean = x.isFinite() && y.isFinite() && z.isFinite()

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val ONE = Vec3(1f, 1f, 1f)
        val UP = Vec3(0f, 1f, 0f)
        val DOWN = Vec3(0f, -1f, 0f)
        val RIGHT = Vec3(1f, 0f, 0f)
        val FORWARD = Vec3(0f, 0f, -1f)
    }
}

class MutableVec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun set(nx: Float, ny: Float, nz: Float): MutableVec3 {
        x = nx; y = ny; z = nz; return this
    }
    fun set(v: Vec3): MutableVec3 {
        x = v.x; y = v.y; z = v.z; return this
    }
    fun addScaled(v: Vec3, scale: Float): MutableVec3 {
        x += v.x * scale; y += v.y * scale; z += v.z * scale; return this
    }
    fun toVec3() = Vec3(x, y, z)
}
