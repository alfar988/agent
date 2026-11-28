package com.example.render

import com.example.math.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MeshLevel(
    val vertices: FloatArray, // Interleaved: pos(3), normal(3), tangent(3), uv(2) = 11 floats
    val indices: ShortArray
) {
    val vertexCount: Int get() = vertices.size / 11
    val indexCount: Int get() = indices.size
}

class MeshAsset(
    val lod0: MeshLevel,
    val lod1: MeshLevel,
    val lod2: MeshLevel,
    val boundingRadius: Float
) {
    fun selectLod(distanceSq: Float): MeshLevel = when {
        distanceSq < 18f * 18f -> lod0
        distanceSq < 38f * 38f -> lod1
        else -> lod2
    }
}

/** Mutable mesh builder supporting compound sub-parts, bevels, lathes, coils, and icospheres. */
class MeshBuilder {
    private val verts = ArrayList<Float>(1024)
    private val inds = ArrayList<Short>(1024)

    fun addVertex(p: Vec3, n: Vec3, t: Vec3, u: Float, v: Float): Short {
        val idx = (verts.size / 11).toShort()
        verts.add(p.x); verts.add(p.y); verts.add(p.z)
        verts.add(n.x); verts.add(n.y); verts.add(n.z)
        verts.add(t.x); verts.add(t.y); verts.add(t.z)
        verts.add(u); verts.add(v)
        return idx
    }

    fun addQuad(
        p0: Vec3, p1: Vec3, p2: Vec3, p3: Vec3,
        n: Vec3, uScale: Float = 1f, vScale: Float = 1f
    ) {
        val t = (p1 - p0).normalized().let { if (it.lengthSq() < 0.01f) Vec3.RIGHT else it }
        val i0 = addVertex(p0, n, t, 0f, 0f)
        val i1 = addVertex(p1, n, t, uScale, 0f)
        val i2 = addVertex(p2, n, t, uScale, vScale)
        val i3 = addVertex(p3, n, t, 0f, vScale)
        inds.add(i0); inds.add(i1); inds.add(i2)
        inds.add(i0); inds.add(i2); inds.add(i3)
    }

    fun addTri(p0: Vec3, p1: Vec3, p2: Vec3, uv0: Pair<Float, Float>, uv1: Pair<Float, Float>, uv2: Pair<Float, Float>) {
        val e1 = p1 - p0
        val e2 = p2 - p0
        val n = e1.cross(e2).normalized()
        val t = e1.normalized().let { if (it.lengthSq() < 0.01f) Vec3.RIGHT else it }
        val i0 = addVertex(p0, n, t, uv0.first, uv0.second)
        val i1 = addVertex(p1, n, t, uv1.first, uv1.second)
        val i2 = addVertex(p2, n, t, uv2.first, uv2.second)
        inds.add(i0); inds.add(i1); inds.add(i2)
    }

    fun addBox(cx: Float, cy: Float, cz: Float, hx: Float, hy: Float, hz: Float, uvTile: Float = 1f) {
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz
        // +Z
        addQuad(Vec3(x0, y0, z1), Vec3(x1, y0, z1), Vec3(x1, y1, z1), Vec3(x0, y1, z1), Vec3(0f, 0f, 1f), hx * 2f * uvTile, hy * 2f * uvTile)
        // -Z
        addQuad(Vec3(x1, y0, z0), Vec3(x0, y0, z0), Vec3(x0, y1, z0), Vec3(x1, y1, z0), Vec3(0f, 0f, -1f), hx * 2f * uvTile, hy * 2f * uvTile)
        // +X
        addQuad(Vec3(x1, y0, z1), Vec3(x1, y0, z0), Vec3(x1, y1, z0), Vec3(x1, y1, z1), Vec3(1f, 0f, 0f), hz * 2f * uvTile, hy * 2f * uvTile)
        // -X
        addQuad(Vec3(-hx + cx, y0, z0), Vec3(-hx + cx, y0, z1), Vec3(-hx + cx, y1, z1), Vec3(-hx + cx, y1, z0), Vec3(-1f, 0f, 0f), hz * 2f * uvTile, hy * 2f * uvTile)
        // +Y
        addQuad(Vec3(x0, y1, z1), Vec3(x1, y1, z1), Vec3(x1, y1, z0), Vec3(x0, y1, z0), Vec3(0f, 1f, 0f), hx * 2f * uvTile, hz * 2f * uvTile)
        // -Y
        addQuad(Vec3(x0, y0, z0), Vec3(x1, y0, z0), Vec3(x1, y0, z1), Vec3(x0, y0, z1), Vec3(0f, -1f, 0f), hx * 2f * uvTile, hz * 2f * uvTile)
    }

    /** Chamfered box with beveled outer edges so specular highlights catch every corner. */
    fun addChamferedBox(cx: Float, cy: Float, cz: Float, hx: Float, hy: Float, hz: Float, bevel: Float) {
        val b = bevel.coerceAtMost(minOf(hx, hy, hz) * 0.35f)
        val ix = hx - b; val iy = hy - b; val iz = hz - b
        // Core 6 faces inset
        addQuad(Vec3(cx - ix, cy - iy, cz + hz), Vec3(cx + ix, cy - iy, cz + hz), Vec3(cx + ix, cy + iy, cz + hz), Vec3(cx - ix, cy + iy, cz + hz), Vec3(0f, 0f, 1f))
        addQuad(Vec3(cx + ix, cy - iy, cz - hz), Vec3(cx - ix, cy - iy, cz - hz), Vec3(cx - ix, cy + iy, cz - hz), Vec3(cx + ix, cy + iy, cz - hz), Vec3(0f, 0f, -1f))
        addQuad(Vec3(cx + hx, cy - iy, cz + iz), Vec3(cx + hx, cy - iy, cz - iz), Vec3(cx + hx, cy + iy, cz - iz), Vec3(cx + hx, cy + iy, cz + iz), Vec3(1f, 0f, 0f))
        addQuad(Vec3(cx - hx, cy - iy, cz - iz), Vec3(cx - hx, cy - iy, cz + iz), Vec3(cx - hx, cy + iy, cz + iz), Vec3(cx - hx, cy + iy, cz - iz), Vec3(-1f, 0f, 0f))
        addQuad(Vec3(cx - ix, cy + hy, cz + iz), Vec3(cx + ix, cy + hy, cz + iz), Vec3(cx + ix, cy + hy, cz - iz), Vec3(cx - ix, cy + hy, cz - iz), Vec3(0f, 1f, 0f))
        addQuad(Vec3(cx - ix, cy - hy, cz - iz), Vec3(cx + ix, cy - hy, cz - iz), Vec3(cx + ix, cy - hy, cz + iz), Vec3(cx - ix, cy - hy, cz + iz), Vec3(0f, -1f, 0f))

        // 12 beveled edges
        val invSqrt2 = 0.70710677f
        // Top 4 edges
        addQuad(Vec3(cx - ix, cy + iy, cz + hz), Vec3(cx + ix, cy + iy, cz + hz), Vec3(cx + ix, cy + hy, cz + iz), Vec3(cx - ix, cy + hy, cz + iz), Vec3(0f, invSqrt2, invSqrt2))
        addQuad(Vec3(cx + ix, cy + iy, cz - hz), Vec3(cx - ix, cy + iy, cz - hz), Vec3(cx - ix, cy + hy, cz - iz), Vec3(cx + ix, cy + hy, cz - iz), Vec3(0f, invSqrt2, -invSqrt2))
        addQuad(Vec3(cx + hx, cy + iy, cz + iz), Vec3(cx + hx, cy + iy, cz - iz), Vec3(cx + ix, cy + hy, cz - iz), Vec3(cx + ix, cy + hy, cz + iz), Vec3(invSqrt2, invSqrt2, 0f))
        addQuad(Vec3(cx - hx, cy + iy, cz - iz), Vec3(cx - hx, cy + iy, cz + iz), Vec3(cx - ix, cy + hy, cz + iz), Vec3(cx - ix, cy + hy, cz - iz), Vec3(-invSqrt2, invSqrt2, 0f))

        // Bottom 4 edges
        addQuad(Vec3(cx - ix, cy - hy, cz + iz), Vec3(cx + ix, cy - hy, cz + iz), Vec3(cx + ix, cy - iy, cz + hz), Vec3(cx - ix, cy - iy, cz + hz), Vec3(0f, -invSqrt2, invSqrt2))
        addQuad(Vec3(cx + ix, cy - hy, cz - iz), Vec3(cx - ix, cy - hy, cz - iz), Vec3(cx - ix, cy - iy, cz - hz), Vec3(cx + ix, cy - iy, cz - hz), Vec3(0f, -invSqrt2, -invSqrt2))
        addQuad(Vec3(cx + ix, cy - hy, cz + iz), Vec3(cx + ix, cy - hy, cz - iz), Vec3(cx + hx, cy - iy, cz - iz), Vec3(cx + hx, cy - iy, cz + iz), Vec3(invSqrt2, -invSqrt2, 0f))
        addQuad(Vec3(cx - ix, cy - hy, cz - iz), Vec3(cx - ix, cy - hy, cz + iz), Vec3(cx - hx, cy - iy, cz + iz), Vec3(cx - hx, cy - iy, cz - iz), Vec3(-invSqrt2, -invSqrt2, 0f))

        // Vertical 4 edges
        addQuad(Vec3(cx + ix, cy - iy, cz + hz), Vec3(cx + hx, cy - iy, cz + iz), Vec3(cx + hx, cy + iy, cz + iz), Vec3(cx + ix, cy + iy, cz + hz), Vec3(invSqrt2, 0f, invSqrt2))
        addQuad(Vec3(cx - hx, cy - iy, cz + iz), Vec3(cx - ix, cy - iy, cz + hz), Vec3(cx - ix, cy + iy, cz + hz), Vec3(cx - hx, cy + iy, cz + iz), Vec3(-invSqrt2, 0f, invSqrt2))
        addQuad(Vec3(cx + hx, cy - iy, cz - iz), Vec3(cx + ix, cy - iy, cz - hz), Vec3(cx + ix, cy + iy, cz - hz), Vec3(cx + hx, cy + iy, cz - iz), Vec3(invSqrt2, 0f, -invSqrt2))
        addQuad(Vec3(cx - ix, cy - iy, cz - hz), Vec3(cx - hx, cy - iy, cz - iz), Vec3(cx - hx, cy + iy, cz - iz), Vec3(cx - ix, cy + iy, cz - hz), Vec3(-invSqrt2, 0f, -invSqrt2))
    }

    fun addCylinderY(cx: Float, cy: Float, cz: Float, rBottom: Float, rTop: Float, height: Float, segs: Int) {
        val y0 = cy - height * 0.5f
        val y1 = cy + height * 0.5f
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segs) * (2f * PI.toFloat())
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)

            val p00 = Vec3(cx + c0 * rBottom, y0, cz + s0 * rBottom)
            val p10 = Vec3(cx + c1 * rBottom, y0, cz + s1 * rBottom)
            val p11 = Vec3(cx + c1 * rTop, y1, cz + s1 * rTop)
            val p01 = Vec3(cx + c0 * rTop, y1, cz + s0 * rTop)

            val n = Vec3((c0 + c1) * 0.5f, 0f, (s0 + s1) * 0.5f).normalized()
            addQuad(p00, p10, p11, p01, n, 1f / segs, 1f)

            // Caps
            addTri(Vec3(cx, y1, cz), p01, p11, 0.5f to 0.5f, 0.5f + c0 * 0.5f to 0.5f + s0 * 0.5f, 0.5f + c1 * 0.5f to 0.5f + s1 * 0.5f)
            addTri(Vec3(cx, y0, cz), p10, p00, 0.5f to 0.5f, 0.5f + c1 * 0.5f to 0.5f + s1 * 0.5f, 0.5f + c0 * 0.5f to 0.5f + s0 * 0.5f)
        }
    }

    fun addTorusXZ(cx: Float, cy: Float, cz: Float, majorR: Float, minorR: Float, majorSegs: Int, minorSegs: Int, rotateToVerticalX: Boolean = false) {
        fun transformPt(v: Vec3): Vec3 = if (rotateToVerticalX) Vec3(v.y, v.x, v.z) else v
        for (i in 0 until majorSegs) {
            val u0 = (i.toFloat() / majorSegs) * (2f * PI.toFloat())
            val u1 = ((i + 1).toFloat() / majorSegs) * (2f * PI.toFloat())
            for (j in 0 until minorSegs) {
                val v0 = (j.toFloat() / minorSegs) * (2f * PI.toFloat())
                val v1 = ((j + 1).toFloat() / minorSegs) * (2f * PI.toFloat())

                fun torusPt(u: Float, v: Float): Vec3 {
                    val r = majorR + minorR * cos(v)
                    return transformPt(Vec3(r * cos(u), minorR * sin(v), r * sin(u))) + Vec3(cx, cy, cz)
                }

                val p00 = torusPt(u0, v0)
                val p10 = torusPt(u1, v0)
                val p11 = torusPt(u1, v1)
                val p01 = torusPt(u0, v1)
                val cMid = transformPt(Vec3(majorR * cos((u0 + u1) * 0.5f), 0f, majorR * sin((u0 + u1) * 0.5f))) + Vec3(cx, cy, cz)
                val n = ((p00 + p10 + p11 + p01) * 0.25f - cMid).normalized()
                addQuad(p00, p10, p11, p01, n)
            }
        }
    }

    fun addUvSphere(cx: Float, cy: Float, cz: Float, radius: Float, rings: Int, sectors: Int) {
        for (r in 0 until rings) {
            val phi0 = (r.toFloat() / rings) * PI.toFloat()
            val phi1 = ((r + 1).toFloat() / rings) * PI.toFloat()
            for (s in 0 until sectors) {
                val th0 = (s.toFloat() / sectors) * (2f * PI.toFloat())
                val th1 = ((s + 1).toFloat() / sectors) * (2f * PI.toFloat())

                fun sp(phi: Float, th: Float): Vec3 = Vec3(
                    cx + radius * sin(phi) * cos(th),
                    cy + radius * cos(phi),
                    cz + radius * sin(phi) * sin(th)
                )

                val p00 = sp(phi0, th0)
                val p10 = sp(phi1, th0)
                val p11 = sp(phi1, th1)
                val p01 = sp(phi0, th1)
                val center = Vec3(cx, cy, cz)
                val n = ((p00 + p10 + p11 + p01) * 0.25f - center).normalized()
                if (r == 0) {
                    addTri(p00, p10, p11, 0.5f to 0f, 0f to 1f, 1f to 1f)
                } else if (r == rings - 1) {
                    addTri(p00, p10, p01, 0f to 0f, 0.5f to 1f, 1f to 0f)
                } else {
                    addQuad(p00, p11, p01, p10, n)
                }
            }
        }
    }

    fun addHelicalCoilY(cx: Float, cy: Float, cz: Float, coilR: Float, wireR: Float, height: Float, turns: Float, steps: Int) {
        val sides = 6
        val yStart = cy - height * 0.5f
        for (i in 0 until steps) {
            val t0 = i.toFloat() / steps
            val t1 = (i + 1).toFloat() / steps
            val a0 = t0 * turns * 2f * PI.toFloat()
            val a1 = t1 * turns * 2f * PI.toFloat()
            val c0 = Vec3(cx + coilR * cos(a0), yStart + t0 * height, cz + coilR * sin(a0))
            val c1 = Vec3(cx + coilR * cos(a1), yStart + t1 * height, cz + coilR * sin(a1))
            val tan = (c1 - c0).normalized()
            val binorm = tan.cross(Vec3.UP).let { if (it.lengthSq() < 0.01f) Vec3.RIGHT else it.normalized() }
            val norm = binorm.cross(tan).normalized()

            for (s in 0 until sides) {
                val sa0 = (s.toFloat() / sides) * 2f * PI.toFloat()
                val sa1 = ((s + 1).toFloat() / sides) * 2f * PI.toFloat()
                val off0 = (binorm * cos(sa0) + norm * sin(sa0)) * wireR
                val off1 = (binorm * cos(sa1) + norm * sin(sa1)) * wireR
                val n = (off0 + off1).normalized()
                addQuad(c0 + off0, c1 + off0, c1 + off1, c0 + off1, n)
            }
        }
    }

    fun build(): MeshLevel {
        val vArr = FloatArray(verts.size)
        for (i in verts.indices) vArr[i] = verts[i]
        val iArr = ShortArray(inds.size)
        for (i in inds.indices) iArr[i] = inds[i]
        return MeshLevel(vArr, iArr)
    }
}
