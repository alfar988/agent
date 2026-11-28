package com.example.render

import com.example.physics.PhysicsMaterial
import com.example.physics.TexturePattern
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class MaterialTextureData(
    val size: Int,
    val albedoRgba: ByteBuffer,
    val normalRgba: ByteBuffer,
    val mraRgba: ByteBuffer // R = Metallic, G = Roughness, B = AO, A = 255
)

object ProceduralTextureGenerator {

    fun generateAllMaterials(): Map<PhysicsMaterial, MaterialTextureData> {
        val map = HashMap<PhysicsMaterial, MaterialTextureData>(PhysicsMaterial.entries.size)
        for (mat in PhysicsMaterial.entries) {
            map[mat] = generateMaterialTextures(mat, size = 256)
        }
        return map
    }

    fun generateAllPatterns(size: Int = 256): Map<TexturePattern, MaterialTextureData> {
        val map = HashMap<TexturePattern, MaterialTextureData>(TexturePattern.entries.size)
        for (pat in TexturePattern.entries) {
            map[pat] = generatePatternTextures(pat, size)
        }
        return map
    }

    private fun generatePatternTextures(pat: TexturePattern, size: Int): MaterialTextureData {
        val albedo = ByteBuffer.allocateDirect(size * size * 4)
        val normal = ByteBuffer.allocateDirect(size * size * 4)
        val mra = ByteBuffer.allocateDirect(size * size * 4)

        val heightField = FloatArray(size * size)
        val roughField = FloatArray(size * size)
        val metalField = FloatArray(size * size)
        val aoField = FloatArray(size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val u = x.toFloat() / size
                val v = y.toFloat() / size
                val idx = y * size + x

                val n1 = valueNoise(u * 14f, v * 14f, pat.ordinal * 23 + 3)
                val n2 = valueNoise(u * 42f, v * 42f, pat.ordinal * 41 + 9)
                val seamDist = minOf(minOf(u, 1f - u), minOf(v, 1f - v))
                val subtleChamfer = if (seamDist < 0.018f) -0.14f else 0f

                var h = 0.5f + subtleChamfer * 0.25f
                var rVar = 0.5f
                var mVar = 0.5f
                var ao = if (seamDist < 0.022f) 0.90f else 1.0f

                when (pat) {
                    TexturePattern.BRUSHED_METAL -> {
                        val brush = sin(u * 140f) * 0.05f + n2 * 0.06f
                        h += brush
                        rVar = (0.24f + n1 * 0.12f).coerceIn(0.12f, 0.65f)
                        mVar = 0.92f
                        ao *= (0.93f + n1 * 0.07f)
                    }
                    TexturePattern.WOOD_GRAIN -> {
                        val grain = sin((v * 28f + n1 * 2.4f) * PI.toFloat()) * 0.14f
                        val plankSeam = if (abs((v * 4f) % 1f - 0.5f) > 0.482f) -0.24f else 0f
                        h += grain + plankSeam + n2 * 0.04f
                        rVar = (0.64f + n2 * 0.14f).coerceIn(0.45f, 0.94f)
                        mVar = 0.0f
                        ao *= if (plankSeam < 0f) 0.76f else (0.91f + grain * 0.25f)
                    }
                    TexturePattern.SPECKLED_STONE -> {
                        val speckle = (n1 * 0.52f + n2 * 0.48f) - 0.5f
                        val tileLine = if (abs((u * 2f) % 1f - 0.5f) > 0.488f || abs((v * 2f) % 1f - 0.5f) > 0.488f) -0.16f else 0f
                        h += speckle * 0.30f + tileLine
                        rVar = (0.80f + speckle * 0.14f).coerceIn(0.58f, 0.97f)
                        mVar = 0.02f
                        ao *= if (tileLine < 0f) 0.84f else (0.88f + n1 * 0.12f)
                    }
                    TexturePattern.WEAVE_FABRIC -> {
                        val warp = sin(u * 56f * PI.toFloat())
                        val weft = sin(v * 56f * PI.toFloat())
                        val weave = (warp * weft) * 0.14f
                        h += weave + n2 * 0.05f
                        rVar = (0.86f + n1 * 0.08f).coerceIn(0.70f, 0.98f)
                        mVar = 0.0f
                        ao *= (0.85f + (weave + 0.14f) * 0.40f).coerceIn(0.70f, 1.0f)
                    }
                    TexturePattern.CELLULAR_FOAM -> {
                        val cell = abs(n1 - n2)
                        h += (cell - 0.25f) * 0.36f
                        rVar = (0.86f + cell * 0.12f).coerceIn(0.70f, 0.99f)
                        mVar = 0.0f
                        ao *= (0.80f + cell * 0.30f).coerceIn(0.65f, 1.0f)
                    }
                    TexturePattern.CARBON_TWILL -> {
                        val twill = sin((u * 32f + v * 32f) * PI.toFloat()) * 0.12f
                        val cross = cos((u * 32f - v * 32f) * PI.toFloat()) * 0.07f
                        h += twill + cross
                        rVar = (0.26f + (twill + 0.12f) * 0.38f).coerceIn(0.15f, 0.62f)
                        mVar = 0.38f
                        ao *= (0.88f + (twill + 0.12f) * 0.30f).coerceIn(0.74f, 1.0f)
                    }
                    TexturePattern.GLASS_SMOOTH -> {
                        h += n1 * 0.02f
                        rVar = (0.06f + n2 * 0.04f).coerceIn(0.04f, 0.20f)
                        mVar = 0.08f
                    }
                    TexturePattern.CORRUGATED_RUST -> {
                        val rib = sin(u * 20f * PI.toFloat()) * 0.16f
                        val rust = n1 * 0.65f + n2 * 0.35f
                        h += rib + (rust - 0.5f) * 0.32f
                        rVar = (0.48f + rust * 0.42f).coerceIn(0.32f, 0.96f)
                        mVar = (0.78f - rust * 0.52f).coerceIn(0.20f, 0.90f)
                        ao *= (0.80f + rust * 0.20f)
                    }
                    TexturePattern.CARDBOARD_KRAFT -> {
                        val flutes = sin(u * 40f * PI.toFloat()) * 0.08f
                        h += flutes + (n1 - 0.5f) * 0.18f + n2 * 0.06f
                        rVar = (0.86f + n1 * 0.08f).coerceIn(0.72f, 0.98f)
                        mVar = 0.0f
                        ao *= (0.88f + n1 * 0.12f)
                    }
                    TexturePattern.ICE_CRYSTAL -> {
                        val facet = abs(sin((u * 16f + n1 * 2f) * PI.toFloat()) * cos((v * 16f - n2 * 2f) * PI.toFloat()))
                        h += (facet - 0.4f) * 0.20f
                        rVar = (0.09f + facet * 0.12f).coerceIn(0.05f, 0.30f)
                        mVar = 0.05f
                        ao *= (0.91f + facet * 0.09f)
                    }
                    TexturePattern.DIAMOND_PLATE -> {
                        val cellU = (u * 8f) % 1f - 0.5f
                        val cellV = (v * 8f) % 1f - 0.5f
                        val parity = ((u * 8f).toInt() + (v * 8f).toInt()) % 2
                        val d = if (parity == 0) abs(cellU + cellV) * 0.65f + abs(cellU - cellV) * 1.4f
                        else abs(cellU - cellV) * 0.65f + abs(cellU + cellV) * 1.4f
                        val lug = if (d < 0.34f) (0.34f - d) * 0.85f else 0f
                        h += lug + n2 * 0.04f
                        rVar = (if (lug > 0.05f) 0.22f else 0.36f + n1 * 0.10f).coerceIn(0.15f, 0.72f)
                        mVar = 0.90f
                        ao *= if (lug > 0.02f) 0.96f else 0.84f
                    }
                    TexturePattern.EPOXY_FLOOR_TILE -> {
                        val tileU = abs((u * 4f) % 1f - 0.5f)
                        val tileV = abs((v * 4f) % 1f - 0.5f)
                        val grout = if (tileU > 0.478f || tileV > 0.478f) -0.34f else 0f
                        val fleck = (n2 - 0.5f) * 0.14f
                        h += grout + fleck
                        rVar = if (grout < 0f) 0.88f else (0.32f + n1 * 0.12f).coerceIn(0.20f, 0.65f)
                        mVar = 0.04f
                        ao *= if (grout < 0f) 0.72f else (0.92f + n1 * 0.08f)
                    }
                    TexturePattern.CEILING_PANEL_GRID -> {
                        val gridU = abs((u * 3f) % 1f - 0.5f)
                        val gridV = abs((v * 3f) % 1f - 0.5f)
                        val rail = if (gridU > 0.46f || gridV > 0.46f) 0.18f else 0f
                        val acoustic = ((sin(u * 96f) * sin(v * 96f)) * 0.5f + 0.5f) * -0.10f
                        h += rail + acoustic
                        rVar = if (rail > 0f) 0.45f else 0.86f
                        mVar = if (rail > 0f) 0.65f else 0.0f
                        ao *= if (rail > 0f) 0.88f else (0.86f + n2 * 0.14f)
                    }
                    TexturePattern.BRICK_MASONRY -> {
                        val row = (v * 6f).toInt()
                        val shiftedU = (u * 3f + (row % 2) * 0.5f) % 1f
                        val localV = (v * 6f) % 1f
                        val mortar = if (shiftedU < 0.06f || shiftedU > 0.94f || localV < 0.11f || localV > 0.89f) -0.36f else 0f
                        val clay = (n1 * 0.6f + n2 * 0.4f - 0.5f) * 0.24f
                        h += mortar + clay
                        rVar = if (mortar < 0f) 0.94f else (0.78f + n2 * 0.14f)
                        mVar = 0.0f
                        ao *= if (mortar < 0f) 0.72f else (0.89f + n1 * 0.11f)
                    }
                    TexturePattern.ASPHALT_PAVEMENT -> {
                        val agg = (n2 * 0.65f + valueNoise(u * 96f, v * 96f, 77) * 0.35f) - 0.5f
                        val tarCrack = if (abs(n1 - 0.5f) < 0.015f) -0.22f else 0f
                        h += agg * 0.38f + tarCrack
                        rVar = (0.86f + agg * 0.12f).coerceIn(0.68f, 0.98f)
                        mVar = 0.0f
                        ao *= (0.82f + agg * 0.22f).coerceIn(0.60f, 1.0f)
                    }
                    TexturePattern.GRAVEL_EARTH -> {
                        val pebble = abs(n2 - 0.5f) * 0.55f + (valueNoise(u * 78f, v * 78f, 91) - 0.5f) * 0.40f
                        h += (pebble - 0.18f) * 0.48f
                        rVar = 0.94f
                        mVar = 0.0f
                        ao *= (0.76f + pebble * 0.35f).coerceIn(0.58f, 1.0f)
                    }
                }

                heightField[idx] = h
                roughField[idx] = rVar
                metalField[idx] = mVar
                aoField[idx] = ao.coerceIn(0.55f, 1.0f)
            }
        }

        for (y in 0 until size) {
            for (x in 0 until size) {
                val idx = y * size + x
                val xm = (x - 1 + size) % size
                val xp = (x + 1) % size
                val ym = (y - 1 + size) % size
                val yp = (y + 1) % size

                val dX = (heightField[y * size + xp] - heightField[y * size + xm]) * 2.2f
                val dY = (heightField[yp * size + x] - heightField[ym * size + x]) * 2.2f
                var nx = -dX
                var ny = -dY
                var nz = 1.0f
                val nLen = sqrt(nx * nx + ny * ny + nz * nz)
                nx /= nLen; ny /= nLen; nz /= nLen

                // Neutral modulated grayscale albedo so PBR shader tint works uniformly across 105 materials
                val shade = (0.86f + (heightField[idx] - 0.5f) * 0.28f).coerceIn(0.52f, 1.0f)
                val lum = (shade * 255f).toInt().toByte()
                albedo.put(lum); albedo.put(lum); albedo.put(lum); albedo.put(255.toByte())

                normal.put((((nx * 0.5f + 0.5f) * 255f).toInt()).toByte())
                normal.put((((ny * 0.5f + 0.5f) * 255f).toInt()).toByte())
                normal.put((((nz * 0.5f + 0.5f) * 255f).toInt()).toByte())
                normal.put(255.toByte())

                mra.put((metalField[idx] * 255f).toInt().toByte())
                mra.put((roughField[idx] * 255f).toInt().toByte())
                mra.put((aoField[idx] * 255f).toInt().toByte())
                mra.put(255.toByte())
            }
        }

        albedo.flip()
        normal.flip()
        mra.flip()
        return MaterialTextureData(size, albedo, normal, mra)
    }

    private fun generateMaterialTextures(mat: PhysicsMaterial, size: Int): MaterialTextureData {
        val albedo = ByteBuffer.allocateDirect(size * size * 4)
        val normal = ByteBuffer.allocateDirect(size * size * 4)
        val mra = ByteBuffer.allocateDirect(size * size * 4)

        val heightField = FloatArray(size * size)
        val roughField = FloatArray(size * size)
        val metalField = FloatArray(size * size)
        val aoField = FloatArray(size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val u = x.toFloat() / size
                val v = y.toFloat() / size
                val idx = y * size + x

                val n1 = valueNoise(u * 12f, v * 12f, mat.ordinal * 17 + 1)
                val n2 = valueNoise(u * 32f, v * 32f, mat.ordinal * 31 + 7)
                val panelEdge = minOf(
                    minOf(u, 1f - u),
                    minOf(v, 1f - v)
                )
                val borderGroove = if (panelEdge < 0.045f) -0.45f else 0f

                var h = 0.5f + borderGroove * 0.4f
                var rVar = mat.roughness
                var mVar = mat.metallic
                var ao = if (panelEdge < 0.05f) 0.72f else 1.0f

                when (mat) {
                    PhysicsMaterial.STEEL, PhysicsMaterial.COPPER -> {
                        val brush = sin(u * 90f) * 0.06f + n2 * 0.08f
                        h += brush
                        rVar = (mat.roughness + n1 * 0.10f - 0.04f).coerceIn(0.12f, 0.65f)
                        ao *= (0.92f + n1 * 0.08f)
                    }
                    PhysicsMaterial.RUBBER -> {
                        val tread = sin((u + v) * 24f * PI.toFloat()) * 0.14f
                        h += tread + n2 * 0.10f
                        rVar = (mat.roughness + n1 * 0.08f).coerceIn(0.65f, 0.98f)
                        ao *= (0.85f + (tread + 0.14f) * 0.4f).coerceIn(0.7f, 1f)
                    }
                    PhysicsMaterial.WOOD -> {
                        val grain = sin((v * 22f + n1 * 2.5f) * PI.toFloat()) * 0.14f
                        val plankSeam = if (abs((v * 4f) % 1f - 0.5f) > 0.47f) -0.35f else 0f
                        h += grain + plankSeam + n2 * 0.05f
                        rVar = (mat.roughness + n2 * 0.12f).coerceIn(0.50f, 0.95f)
                        ao *= if (plankSeam < 0f) 0.65f else (0.9f + grain * 0.3f)
                    }
                    PhysicsMaterial.CONCRETE -> {
                        val tileU = abs((u * 3f) % 1f - 0.5f)
                        val tileV = abs((v * 3f) % 1f - 0.5f)
                        val speckle = (n1 * 0.58f + n2 * 0.42f) - 0.5f
                        val tileLine = if (tileU > 0.478f || tileV > 0.478f) -0.30f else 0f
                        h += speckle * 0.34f + tileLine
                        rVar = (mat.roughness + speckle * 0.14f).coerceIn(0.66f, 0.98f)
                        ao *= if (tileLine < 0f) 0.72f else (0.86f + n1 * 0.14f)
                    }
                    PhysicsMaterial.GLASS, PhysicsMaterial.ICE -> {
                        h += n1 * 0.05f
                        rVar = (mat.roughness + n2 * 0.05f).coerceIn(0.04f, 0.30f)
                    }
                    PhysicsMaterial.RUSTED_IRON -> {
                        val rust = n1 * 0.7f + n2 * 0.3f
                        h += (rust - 0.5f) * 0.45f
                        rVar = (0.45f + rust * 0.48f).coerceIn(0.35f, 0.98f)
                        mVar = (0.85f - rust * 0.65f).coerceIn(0.15f, 0.90f)
                        ao *= (0.78f + rust * 0.22f)
                    }
                }

                heightField[idx] = h
                roughField[idx] = rVar
                metalField[idx] = mVar
                aoField[idx] = ao.coerceIn(0.45f, 1.0f)
            }
        }

        for (y in 0 until size) {
            for (x in 0 until size) {
                val idx = y * size + x
                val xm = (x - 1 + size) % size
                val xp = (x + 1) % size
                val ym = (y - 1 + size) % size
                val yp = (y + 1) % size

                val dX = (heightField[y * size + xp] - heightField[y * size + xm]) * 2.4f
                val dY = (heightField[yp * size + x] - heightField[ym * size + x]) * 2.4f
                var nx = -dX
                var ny = -dY
                var nz = 1.0f
                val nLen = sqrt(nx * nx + ny * ny + nz * nz)
                nx /= nLen; ny /= nLen; nz /= nLen

                val shade = 0.85f + (heightField[idx] - 0.5f) * 0.30f
                val ar = ((mat.tintR * shade).coerceIn(0f, 1f) * 255f).toInt().toByte()
                val ag = ((mat.tintG * shade).coerceIn(0f, 1f) * 255f).toInt().toByte()
                val ab = ((mat.tintB * shade).coerceIn(0f, 1f) * 255f).toInt().toByte()
                val aa = (mat.alpha * 255f).toInt().toByte()
                albedo.put(ar); albedo.put(ag); albedo.put(ab); albedo.put(aa)

                normal.put((((nx * 0.5f + 0.5f) * 255f).toInt()).toByte())
                normal.put((((ny * 0.5f + 0.5f) * 255f).toInt()).toByte())
                normal.put((((nz * 0.5f + 0.5f) * 255f).toInt()).toByte())
                normal.put(255.toByte())

                mra.put((metalField[idx] * 255f).toInt().toByte())
                mra.put((roughField[idx] * 255f).toInt().toByte())
                mra.put((aoField[idx] * 255f).toInt().toByte())
                mra.put(255.toByte())
            }
        }

        albedo.flip()
        normal.flip()
        mra.flip()
        return MaterialTextureData(size, albedo, normal, mra)
    }

    private fun hash2(ix: Int, iy: Int, seed: Int): Float {
        var n = ix * 374761393 + iy * 668265263 + seed * 1442695041
        n = (n xor (n shr 13)) * 1274126177
        return ((n xor (n shr 16)) and 0x7fffffff) / 2147483647.0f
    }

    private fun valueNoise(x: Float, y: Float, seed: Int): Float {
        val ix = kotlin.math.floor(x).toInt()
        val iy = kotlin.math.floor(y).toInt()
        val fx = x - ix
        val fy = y - iy
        val ux = fx * fx * (3f - 2f * fx)
        val uy = fy * fy * (3f - 2f * fy)

        val a = hash2(ix, iy, seed)
        val b = hash2(ix + 1, iy, seed)
        val c = hash2(ix, iy + 1, seed)
        val d = hash2(ix + 1, iy + 1, seed)

        val ab = a + (b - a) * ux
        val cd = c + (d - c) * ux
        return ab + (cd - ab) * uy
    }
}
