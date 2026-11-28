package com.example.render

import android.graphics.Bitmap
import android.opengl.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.MapId
import com.example.physics.MaterialLibrary
import com.example.world.MapBuilder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Software 3D perspective Z-buffer renderer that produces authentic 16:9 thumbnails
 * directly from each MapId's reconstructed geometry, lighting, and atmosphere.
 */
object MapPreviewRenderer {

    private val cache = ConcurrentHashMap<MapId, ImageBitmap>()
    private val bitmapCache = ConcurrentHashMap<MapId, Bitmap>()

    fun getOrRenderPreview(mapId: MapId, width: Int = 480, height: Int = 270): ImageBitmap {
        return cache.getOrPut(mapId) {
            renderMapBitmap(mapId, width, height).asImageBitmap()
        }
    }

    fun renderMapBitmap(mapId: MapId, width: Int = 480, height: Int = 270): Bitmap {
        bitmapCache[mapId]?.let { existing ->
            if (existing.width == width && existing.height == height) return existing
        }

        val built = MapBuilder.buildMap(mapId)
        val meshes = ProceduralMeshGenerator.buildAllMeshes()
        val (eye, target) = cameraPresetFor(mapId)

        val view = FloatArray(16)
        val proj = FloatArray(16)
        val vp = FloatArray(16)
        val model = FloatArray(16)
        val mvp = FloatArray(16)

        Matrix.setLookAtM(view, 0, eye.x, eye.y, eye.z, target.x, target.y, target.z, 0f, 1f, 0f)
        Matrix.perspectiveM(proj, 0, 58f, width.toFloat() / height.toFloat(), 0.25f, 110f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)

        val pixels = IntArray(width * height)
        val zBuf = FloatArray(width * height) { Float.POSITIVE_INFINITY }

        // 1. Sky / atmosphere gradient background
        val skyTop = mapId.skyColor
        val skyHor = mapId.fogColor
        for (y in 0 until height) {
            val t = (1f - y.toFloat() / height).pow(0.8f)
            val r = lerp(skyHor.x, skyTop.x, t)
            val g = lerp(skyHor.y, skyTop.y, t)
            val b = lerp(skyHor.z, skyTop.z, t)
            val argb = packSrgb(r, g, b)
            val rowOff = y * width
            for (x in 0 until width) {
                pixels[rowOff + x] = argb
            }
        }

        val sunDir = mapId.sunDir.normalized()
        val fillDir = Vec3(-sunDir.x * 0.6f, 0.55f, -sunDir.z * 0.6f).normalized()
        val sunCol = mapId.sunColor

        data class DrawCall(
            val level: MeshLevel,
            val mMatrix: FloatArray,
            val rot: Quat,
            val tint: Vec3,
            val alpha: Float,
            val emissive: Vec3,
            val distSq: Float
        )

        val opaques = ArrayList<DrawCall>(built.statics.size + built.dynamics.size)
        val transparents = ArrayList<DrawCall>(12)

        for (s in built.statics) {
            if (!s.isVisible) continue
            val level = meshes[s.meshId]?.lod2 ?: continue
            val spec = MaterialLibrary.fromLegacy(s.material)
            val tint = s.colorOverride ?: Vec3(spec.tintR, spec.tintG, spec.tintB)
            val alpha = s.alphaOverride ?: spec.alpha
            val m = FloatArray(16)
            s.orientation.writeModelMatrix(s.position, s.halfExtents, m)
            val emis = s.emissiveColor * s.emissiveIntensity
            val dSq = (s.position - eye).lengthSq()
            val dc = DrawCall(level, m, s.orientation, tint, alpha, emis, dSq)
            if (alpha < 0.99f) transparents.add(dc) else opaques.add(dc)
        }

        for (d in built.dynamics) {
            val level = meshes[d.type.meshId]?.lod2 ?: continue
            val spec = d.materialSpec
            val tint = Vec3(spec.tintR, spec.tintG, spec.tintB)
            val m = FloatArray(16)
            d.orientation.writeModelMatrix(d.position, Vec3(d.scale, d.scale, d.scale), m)
            val emis = d.emissiveColor * d.emissiveIntensity
            val dSq = (d.position - eye).lengthSq()
            val dc = DrawCall(level, m, d.orientation, tint, spec.alpha, emis, dSq)
            if (spec.alpha < 0.99f) transparents.add(dc) else opaques.add(dc)
        }

        fun rasterizeCall(dc: DrawCall, isTransparent: Boolean) {
            Matrix.multiplyMM(mvp, 0, vp, 0, dc.mMatrix, 0)
            val verts = dc.level.vertices
            val inds = dc.level.indices
            var i = 0
            while (i + 2 < inds.size) {
                val i0 = inds[i].toInt() and 0xFFFF
                val i1 = inds[i + 1].toInt() and 0xFFFF
                val i2 = inds[i + 2].toInt() and 0xFFFF
                i += 3

                val b0 = i0 * 11
                val b1 = i1 * 11
                val b2 = i2 * 11

                val cx0 = mvp[0] * verts[b0] + mvp[4] * verts[b0 + 1] + mvp[8] * verts[b0 + 2] + mvp[12]
                val cy0 = mvp[1] * verts[b0] + mvp[5] * verts[b0 + 1] + mvp[9] * verts[b0 + 2] + mvp[13]
                val cz0 = mvp[2] * verts[b0] + mvp[6] * verts[b0 + 1] + mvp[10] * verts[b0 + 2] + mvp[14]
                val cw0 = mvp[3] * verts[b0] + mvp[7] * verts[b0 + 1] + mvp[11] * verts[b0 + 2] + mvp[15]

                val cx1 = mvp[0] * verts[b1] + mvp[4] * verts[b1 + 1] + mvp[8] * verts[b1 + 2] + mvp[12]
                val cy1 = mvp[1] * verts[b1] + mvp[5] * verts[b1 + 1] + mvp[9] * verts[b1 + 2] + mvp[13]
                val cz1 = mvp[2] * verts[b1] + mvp[6] * verts[b1 + 1] + mvp[10] * verts[b1 + 2] + mvp[14]
                val cw1 = mvp[3] * verts[b1] + mvp[7] * verts[b1 + 1] + mvp[11] * verts[b1 + 2] + mvp[15]

                val cx2 = mvp[0] * verts[b2] + mvp[4] * verts[b2 + 1] + mvp[8] * verts[b2 + 2] + mvp[12]
                val cy2 = mvp[1] * verts[b2] + mvp[5] * verts[b2 + 1] + mvp[9] * verts[b2 + 2] + mvp[13]
                val cz2 = mvp[2] * verts[b2] + mvp[6] * verts[b2 + 1] + mvp[10] * verts[b2 + 2] + mvp[14]
                val cw2 = mvp[3] * verts[b2] + mvp[7] * verts[b2 + 1] + mvp[11] * verts[b2 + 2] + mvp[15]

                if (cw0 <= 0.25f || cw1 <= 0.25f || cw2 <= 0.25f) continue

                val invW0 = 1f / cw0
                val invW1 = 1f / cw1
                val invW2 = 1f / cw2

                val sx0 = (cx0 * invW0 * 0.5f + 0.5f) * width
                val sy0 = (1f - (cy0 * invW0 * 0.5f + 0.5f)) * height
                val sz0 = cz0 * invW0

                val sx1 = (cx1 * invW1 * 0.5f + 0.5f) * width
                val sy1 = (1f - (cy1 * invW1 * 0.5f + 0.5f)) * height
                val sz1 = cz1 * invW1

                val sx2 = (cx2 * invW2 * 0.5f + 0.5f) * width
                val sy2 = (1f - (cy2 * invW2 * 0.5f + 0.5f)) * height
                val sz2 = cz2 * invW2

                // Screen-space area & backface cull (negative area for CCW in Y-down screen space)
                val area = (sx1 - sx0) * (sy2 - sy0) - (sy1 - sy0) * (sx2 - sx0)
                if (!isTransparent && area >= -0.05f) continue
                if (kotlin.math.abs(area) < 0.05f) continue

                val minX = max(0, kotlin.math.floor(min(sx0, min(sx1, sx2))).toInt())
                val maxX = min(width - 1, kotlin.math.ceil(max(sx0, max(sx1, sx2))).toInt())
                val minY = max(0, kotlin.math.floor(min(sy0, min(sy1, sy2))).toInt())
                val maxY = min(height - 1, kotlin.math.ceil(max(sy0, max(sy1, sy2))).toInt())
                if (minX > maxX || minY > maxY) continue

                // Flat-shaded world normal from vertex average
                val ln = Vec3(
                    verts[b0 + 3] + verts[b1 + 3] + verts[b2 + 3],
                    verts[b0 + 4] + verts[b1 + 4] + verts[b2 + 4],
                    verts[b0 + 5] + verts[b1 + 5] + verts[b2 + 5]
                )
                val wn = dc.rot.rotate(ln).normalized()
                val ndl = max(0f, wn.dot(sunDir))
                val fill = max(0f, wn.dot(fillDir)) * 0.32f
                val hemi = wn.y * 0.5f + 0.5f
                val ambR = lerp(mapId.groundAmbient.x, skyTop.x, hemi) * 0.85f
                val ambG = lerp(mapId.groundAmbient.y, skyTop.y, hemi) * 0.85f
                val ambB = lerp(mapId.groundAmbient.z, skyTop.z, hemi) * 0.85f

                val shadeR = dc.tint.x * (ambR + sunCol.x * (ndl * 0.95f + fill)) + dc.emissive.x
                val shadeG = dc.tint.y * (ambG + sunCol.y * (ndl * 0.95f + fill)) + dc.emissive.y
                val shadeB = dc.tint.z * (ambB + sunCol.z * (ndl * 0.95f + fill)) + dc.emissive.z

                val distEst = (cw0 + cw1 + cw2) * 0.333f
                val fog = (1f - kotlin.math.exp(-distEst * mapId.fogDensity * 1.15f)).coerceIn(0f, 0.7f)
                val fr = lerp(shadeR, skyHor.x, fog)
                val fg = lerp(shadeG, skyHor.y, fog)
                val fb = lerp(shadeB, skyHor.z, fog)
                val shadedArgb = packSrgb(fr, fg, fb)

                val invArea = 1f / area
                for (py in minY..maxY) {
                    val pyc = py + 0.5f
                    val rowOff = py * width
                    for (px in minX..maxX) {
                        val pxc = px + 0.5f
                        val w0 = ((sx1 - pxc) * (sy2 - pyc) - (sy1 - pyc) * (sx2 - pxc)) * invArea
                        val w1 = ((sx2 - pxc) * (sy0 - pyc) - (sy2 - pyc) * (sx0 - pxc)) * invArea
                        val w2 = 1f - w0 - w1
                        if (w0 >= -0.001f && w1 >= -0.001f && w2 >= -0.001f) {
                            val z = w0 * sz0 + w1 * sz1 + w2 * sz2
                            val pIdx = rowOff + px
                            if (z >= -1f && z < zBuf[pIdx]) {
                                if (!isTransparent) {
                                    zBuf[pIdx] = z
                                    pixels[pIdx] = shadedArgb
                                } else {
                                    pixels[pIdx] = blendArgb(pixels[pIdx], shadedArgb, dc.alpha)
                                }
                            }
                        }
                    }
                }
            }
        }

        for (dc in opaques) rasterizeCall(dc, isTransparent = false)
        transparents.sortByDescending { it.distSq }
        for (dc in transparents) rasterizeCall(dc, isTransparent = true)

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmapCache[mapId] = bmp
        return bmp
    }

    private fun cameraPresetFor(mapId: MapId): Pair<Vec3, Vec3> = when (mapId) {
        MapId.LAB -> Vec3(-6.4f, 4.4f, 7.6f) to Vec3(2.2f, 1.8f, -5.2f)
        MapId.FACTORY -> Vec3(-8.8f, 6.0f, 11.4f) to Vec3(2.8f, 2.0f, -3.5f)
        MapId.WAREHOUSE -> Vec3(-7.4f, 5.2f, 11.8f) to Vec3(2.4f, 1.8f, -3.5f)
        MapId.CONSTRUCTION -> Vec3(14.5f, 8.8f, 15.2f) to Vec3(-1.5f, 2.8f, -3.0f)
        MapId.FACILITY -> Vec3(-8.2f, 5.6f, 13.0f) to Vec3(2.8f, 2.0f, -4.5f)
        MapId.UNDERGROUND_MAINTENANCE -> Vec3(-7.0f, 4.6f, 11.5f) to Vec3(1.5f, 1.8f, -4.0f)
        MapId.OUTDOOR_TESTING_YARD -> Vec3(15.0f, 9.2f, 16.5f) to Vec3(-1.5f, 2.4f, -3.0f)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun packSrgb(r: Float, g: Float, b: Float): Int {
        // ACES Filmic curve + gamma matching PBR shader
        fun tone(v: Float): Int {
            val x = v.coerceAtLeast(0f)
            val mapped = ((x * (2.51f * x + 0.03f)) / (x * (2.43f * x + 0.59f) + 0.14f)).coerceIn(0f, 1f)
            return (mapped.pow(1f / 2.2f) * 255f).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (tone(r) shl 16) or (tone(g) shl 8) or tone(b)
    }

    private fun blendArgb(dst: Int, src: Int, alpha: Float): Int {
        val inv = 1f - alpha
        val r = (((dst ushr 16) and 0xFF) * inv + ((src ushr 16) and 0xFF) * alpha).toInt().coerceIn(0, 255)
        val g = (((dst ushr 8) and 0xFF) * inv + ((src ushr 8) and 0xFF) * alpha).toInt().coerceIn(0, 255)
        val b = ((dst and 0xFF) * inv + (src and 0xFF) * alpha).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
