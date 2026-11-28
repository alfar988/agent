package com.example.render.gltf

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.Matrix
import com.example.math.Quat
import com.example.math.Vec3
import com.example.render.MeshAsset
import com.example.render.MeshLevel
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class GlbChannelPath {
    TRANSLATION,
    ROTATION
}

data class GlbMorphTarget(
    val name: String,
    val positionDeltas: FloatArray // xyz * vertexCount
)

data class GlbPrimitiveData(
    val meshName: String,
    val materialIndex: Int,
    val positions: FloatArray, // 3 * vCount
    val normals: FloatArray,   // 3 * vCount
    val tangents: FloatArray,  // 4 * vCount
    val uvs: FloatArray,       // 2 * vCount
    val joints: IntArray,      // 4 * vCount (bone indices 0..64)
    val weights: FloatArray,   // 4 * vCount
    val indices: ShortArray,
    val morphTargets: List<GlbMorphTarget>
) {
    val vertexCount: Int get() = positions.size / 3
    val triangleCount: Int get() = indices.size / 3
}

data class GlbTextureBlob(
    val width: Int,
    val height: Int,
    val rgba: ByteBuffer
)

data class GlbMaterialTextures(
    val name: String,
    val metallicFactor: Float,
    val roughnessFactor: Float,
    val albedo: GlbTextureBlob?,
    val normal: GlbTextureBlob?,
    val mra: GlbTextureBlob?,
    val baseColorFactor: Vec3 = Vec3(1f, 1f, 1f),
    val alphaFactor: Float = 1f,
    val emissiveFactor: Vec3 = Vec3.ZERO
)

data class GlbBoneNode(
    val boneIndex: Int,
    val nodeIndex: Int,
    val name: String,
    val parentBoneIndex: Int,
    val bindLocalTranslation: Vec3,
    val inverseBindMatrix: FloatArray, // 16 floats col-major
    val bindLocalRotation: Quat = Quat.IDENTITY
)

data class GlbAnimChannel(
    val boneIndex: Int,
    val path: GlbChannelPath,
    val times: FloatArray,
    val values: FloatArray // 3*N for translation, 4*N for rotation (x,y,z,w)
)

data class GlbAnimationClip(
    val name: String,
    val durationSec: Float,
    val channels: List<GlbAnimChannel>
)

data class GlbCharacterAsset(
    val assetPath: String,
    val primitives: List<GlbPrimitiveData>,
    val materials: List<GlbMaterialTextures>,
    val bones: List<GlbBoneNode>,
    val boneIndexByName: Map<String, Int>,
    val animations: Map<String, GlbAnimationClip>,
    val totalVertices: Int,
    val totalTriangles: Int,
    val validationErrors: List<String>
) {
    fun primitiveByName(name: String): GlbPrimitiveData? = primitives.firstOrNull { it.meshName == name }
}

data class GlbSocket(
    val name: String,
    val position: Vec3,
    val forward: Vec3 = Vec3(0f, 0f, -1f),
    val up: Vec3 = Vec3(0f, 1f, 0f)
)

data class GlbStaticPrimitive(
    val meshName: String,
    val materialIndex: Int,
    val meshLevel: MeshLevel,
    val positions: FloatArray,
    val normals: FloatArray,
    val uvs: FloatArray,
    val indices: ShortArray,
    val boundingRadius: Float
) {
    val vertexCount: Int get() = meshLevel.vertexCount
    val triangleCount: Int get() = meshLevel.indexCount / 3
}

data class GlbStaticAsset(
    val assetPath: String,
    val primitives: List<GlbStaticPrimitive>,
    val materials: List<GlbMaterialTextures>,
    val sockets: Map<String, GlbSocket>,
    val boundsMin: Vec3,
    val boundsMax: Vec3,
    val boundingRadius: Float,
    val totalVertices: Int,
    val totalTriangles: Int,
    val validationErrors: List<String>
) {
    val boundsCenter: Vec3
        get() = Vec3(
            (boundsMin.x + boundsMax.x) * 0.5f,
            (boundsMin.y + boundsMax.y) * 0.5f,
            (boundsMin.z + boundsMax.z) * 0.5f
        )

    val boundsSize: Vec3
        get() = Vec3(
            (boundsMax.x - boundsMin.x).coerceAtLeast(0.001f),
            (boundsMax.y - boundsMin.y).coerceAtLeast(0.001f),
            (boundsMax.z - boundsMin.z).coerceAtLeast(0.001f)
        )

    /** Combined MeshAsset across all primitives (preserves full silhouette for shadows / legacy fallback paths). */
    fun toCombinedMeshAsset(): MeshAsset {
        if (primitives.size == 1) {
            val lvl = primitives[0].meshLevel
            return MeshAsset(lvl, lvl, lvl, boundingRadius)
        }
        var totalFloats = 0
        var totalIndices = 0
        for (p in primitives) {
            totalFloats += p.meshLevel.vertices.size
            totalIndices += p.meshLevel.indices.size
        }
        val mergedVerts = FloatArray(totalFloats)
        val mergedIndices = ShortArray(totalIndices)
        var vOffsetFloats = 0
        var vOffsetVerts = 0
        var iOffset = 0
        for (p in primitives) {
            val v = p.meshLevel.vertices
            val idx = p.meshLevel.indices
            System.arraycopy(v, 0, mergedVerts, vOffsetFloats, v.size)
            for (k in idx.indices) {
                mergedIndices[iOffset + k] = ((idx[k].toInt() and 0xFFFF) + vOffsetVerts).toShort()
            }
            vOffsetFloats += v.size
            vOffsetVerts += p.vertexCount
            iOffset += idx.size
        }
        val lvl = MeshLevel(mergedVerts, mergedIndices)
        return MeshAsset(lvl, lvl, lvl, boundingRadius)
    }
}

object GlbLoader {

    private const val GLB_MAGIC = 0x46546C67 // "glTF"
    private const val CHUNK_JSON = 0x4E4F534A // "JSON"
    private const val CHUNK_BIN = 0x004E4942  // "BIN\0"

    fun loadFromAssets(context: Context, assetPath: String, decodeTextures: Boolean = true): GlbCharacterAsset {
        return context.assets.open(assetPath).use { stream ->
            parseStream(stream, assetPath, decodeTextures)
        }
    }

    fun loadStaticFromAssets(context: Context, assetPath: String, decodeTextures: Boolean = true): GlbStaticAsset {
        return context.assets.open(assetPath).use { stream ->
            parseStaticStream(stream, assetPath, decodeTextures)
        }
    }

    private data class GlbChunks(
        val raw: ByteArray,
        val rootJson: JSONObject,
        val binOffset: Int,
        val binLength: Int
    )

    private fun readChunks(input: InputStream, assetName: String): GlbChunks {
        val raw = input.readBytes()
        require(raw.size >= 20) { "GLB file '$assetName' is too small (${raw.size} bytes)" }

        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buf.getInt(0)
        val version = buf.getInt(4)
        val totalLen = buf.getInt(8)
        require(magic == GLB_MAGIC) { "Invalid GLB magic header in '$assetName': 0x${magic.toString(16)}" }
        require(version == 2) { "Unsupported glTF version $version in '$assetName'" }
        require(totalLen <= raw.size) { "Truncated GLB length $totalLen vs ${raw.size}" }

        var cursor = 12
        var jsonText: String? = null
        var binOffset = -1
        var binLength = 0

        while (cursor + 8 <= totalLen) {
            val chunkLen = buf.getInt(cursor)
            val chunkType = buf.getInt(cursor + 4)
            val dataStart = cursor + 8
            if (chunkType == CHUNK_JSON) {
                jsonText = String(raw, dataStart, chunkLen, Charsets.UTF_8)
            } else if (chunkType == CHUNK_BIN) {
                binOffset = dataStart
                binLength = chunkLen
            }
            cursor = dataStart + chunkLen
        }

        val rootJson = JSONObject(requireNotNull(jsonText) { "Missing JSON chunk in '$assetName'" })
        require(binOffset >= 0 && binLength > 0) { "Missing BIN chunk in '$assetName'" }
        return GlbChunks(raw, rootJson, binOffset, binLength)
    }

    private class AccessorReader(private val chunks: GlbChunks) {
        private val raw = chunks.raw
        private val binOffset = chunks.binOffset
        val bufferViews: JSONArray = chunks.rootJson.getJSONArray("bufferViews")
        val accessors: JSONArray = chunks.rootJson.getJSONArray("accessors")
        val texturesJson: JSONArray = chunks.rootJson.optJSONArray("textures") ?: JSONArray()
        val imagesJson: JSONArray = chunks.rootJson.optJSONArray("images") ?: JSONArray()

        private fun readAccessorMeta(accIdx: Int): Triple<Int, Int, Int> {
            val acc = accessors.getJSONObject(accIdx)
            val bvIdx = acc.getInt("bufferView")
            val bv = bufferViews.getJSONObject(bvIdx)
            return Triple(acc.getInt("componentType"), acc.getInt("count"), bv.optInt("byteStride", 0))
        }

        fun readAccessorBytes(accIdx: Int): Triple<Int, Int, ByteBuffer> {
            val acc = accessors.getJSONObject(accIdx)
            val bvIdx = acc.getInt("bufferView")
            val bv = bufferViews.getJSONObject(bvIdx)
            val compType = acc.getInt("componentType")
            val count = acc.getInt("count")
            val accByteOff = acc.optInt("byteOffset", 0)
            val bvByteOff = bv.optInt("byteOffset", 0)
            val available = (bv.getInt("byteLength") - accByteOff).coerceAtLeast(0)
            val slice = ByteBuffer.wrap(raw, binOffset + bvByteOff + accByteOff, available)
                .slice()
                .order(ByteOrder.LITTLE_ENDIAN)
            return Triple(compType, count, slice)
        }

        fun readFloatArray(accIdx: Int, components: Int): FloatArray {
            val (compType, count, slice) = readAccessorBytes(accIdx)
            val (_, _, byteStride) = readAccessorMeta(accIdx)
            require(compType == 5126) { "Expected FLOAT accessor ($accIdx), got $compType" }
            val out = FloatArray(count * components)
            val elemSize = components * 4
            if (byteStride == 0 || byteStride == elemSize) {
                slice.asFloatBuffer().get(out)
            } else {
                for (i in 0 until count) {
                    val rowBase = i * byteStride
                    for (c in 0 until components) {
                        out[i * components + c] = slice.getFloat(rowBase + c * 4)
                    }
                }
            }
            return out
        }

        fun readShortIndices(accIdx: Int): ShortArray {
            val (compType, count, slice) = readAccessorBytes(accIdx)
            require(compType == 5121 || compType == 5123 || compType == 5125) {
                "Expected index accessor ($accIdx), got $compType"
            }
            val out = ShortArray(count)
            when (compType) {
                5121 -> for (i in 0 until count) out[i] = (slice.get(i).toInt() and 0xFF).toShort()
                5123 -> slice.asShortBuffer().get(out)
                5125 -> {
                    val ib = slice.asIntBuffer()
                    for (i in 0 until count) out[i] = ib.get(i).toShort()
                }
            }
            return out
        }

        fun readJointsVec4(accIdx: Int): IntArray {
            val (compType, count, slice) = readAccessorBytes(accIdx)
            val (_, _, byteStride) = readAccessorMeta(accIdx)
            val out = IntArray(count * 4)
            when (compType) {
                5121 -> {
                    val stride = if (byteStride > 4) byteStride else 4
                    for (i in 0 until count) {
                        val base = i * stride
                        for (c in 0 until 4) out[i * 4 + c] = slice.get(base + c).toInt() and 0xFF
                    }
                }
                5123 -> {
                    val stride = if (byteStride > 8) byteStride else 8
                    for (i in 0 until count) {
                        val base = i * stride
                        for (c in 0 until 4) out[i * 4 + c] = slice.getShort(base + c * 2).toInt() and 0xFFFF
                    }
                }
                else -> error("Unsupported JOINTS_0 componentType $compType")
            }
            return out
        }

        fun decodeTextureIndex(texIdx: Int, decodeTextures: Boolean): GlbTextureBlob? {
            if (!decodeTextures || texIdx < 0 || texIdx >= texturesJson.length()) return null
            val sourceIdx = texturesJson.getJSONObject(texIdx).optInt("source", -1)
            if (sourceIdx < 0 || sourceIdx >= imagesJson.length()) return null
            val img = imagesJson.getJSONObject(sourceIdx)
            val bvIdx = img.optInt("bufferView", -1)
            if (bvIdx < 0) return null
            val bv = bufferViews.getJSONObject(bvIdx)
            val offset = binOffset + bv.optInt("byteOffset", 0)
            val len = bv.getInt("byteLength")
            val bmp = BitmapFactory.decodeByteArray(raw, offset, len) ?: return null
            val w = bmp.width
            val h = bmp.height
            val pixels = IntArray(w * h)
            bmp.getPixels(pixels, 0, w, 0, 0, w, h)
            bmp.recycle()
            val rgba = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
            for (px in pixels) {
                rgba.put(((px shr 16) and 0xFF).toByte())
                rgba.put(((px shr 8) and 0xFF).toByte())
                rgba.put((px and 0xFF).toByte())
                rgba.put(((px shr 24) and 0xFF).toByte())
            }
            rgba.position(0)
            return GlbTextureBlob(w, h, rgba)
        }

        fun decodeGltfMraTextureIndex(
            mraIdx: Int,
            occIdx: Int,
            metallicFactor: Float,
            roughnessFactor: Float,
            decodeTextures: Boolean
        ): GlbTextureBlob? {
            if (!decodeTextures || mraIdx < 0 || mraIdx >= texturesJson.length()) return null
            val sourceIdx = texturesJson.getJSONObject(mraIdx).optInt("source", -1)
            if (sourceIdx < 0 || sourceIdx >= imagesJson.length()) return null
            val img = imagesJson.getJSONObject(sourceIdx)
            val bvIdx = img.optInt("bufferView", -1)
            if (bvIdx < 0) return null
            val bv = bufferViews.getJSONObject(bvIdx)
            val offset = binOffset + bv.optInt("byteOffset", 0)
            val len = bv.getInt("byteLength")
            val bmp = BitmapFactory.decodeByteArray(raw, offset, len) ?: return null
            val w = bmp.width
            val h = bmp.height
            val pixels = IntArray(w * h)
            bmp.getPixels(pixels, 0, w, 0, 0, w, h)
            bmp.recycle()
            val hasSharedOcc = occIdx == mraIdx && occIdx >= 0
            val mScale = metallicFactor.coerceIn(0f, 1f)
            val rScale = roughnessFactor.coerceIn(0f, 1f)
            val rgba = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
            for (px in pixels) {
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                val met = (b * mScale).toInt().coerceIn(0, 255)
                val rgh = (g * rScale).toInt().coerceIn(0, 255)
                val ao = if (hasSharedOcc) r else 255
                rgba.put(met.toByte())
                rgba.put(rgh.toByte())
                rgba.put(ao.toByte())
                rgba.put(255.toByte())
            }
            rgba.position(0)
            return GlbTextureBlob(w, h, rgba)
        }
    }

    private fun parseMaterials(
        rootJson: JSONObject,
        reader: AccessorReader,
        decodeTextures: Boolean,
        remapGltfMra: Boolean = false
    ): List<GlbMaterialTextures> {
        val materialsJson = rootJson.optJSONArray("materials") ?: JSONArray()
        val materials = ArrayList<GlbMaterialTextures>(materialsJson.length())
        for (mi in 0 until materialsJson.length()) {
            val mObj = materialsJson.getJSONObject(mi)
            val name = mObj.optString("name", "mat_$mi")
            val pbr = mObj.optJSONObject("pbrMetallicRoughness")
            val metallic = pbr?.optDouble("metallicFactor", 0.0)?.toFloat() ?: 0f
            val roughness = pbr?.optDouble("roughnessFactor", 0.65)?.toFloat() ?: 0.65f
            val baseColArr = pbr?.optJSONArray("baseColorFactor")
            val baseCol = if (baseColArr != null && baseColArr.length() >= 3) {
                Vec3(
                    baseColArr.optDouble(0, 1.0).toFloat(),
                    baseColArr.optDouble(1, 1.0).toFloat(),
                    baseColArr.optDouble(2, 1.0).toFloat()
                )
            } else Vec3(1f, 1f, 1f)
            val alphaFactor = baseColArr?.optDouble(3, 1.0)?.toFloat() ?: 1f
            val emArr = mObj.optJSONArray("emissiveFactor")
            val emissive = if (emArr != null && emArr.length() >= 3) {
                Vec3(
                    emArr.optDouble(0, 0.0).toFloat(),
                    emArr.optDouble(1, 0.0).toFloat(),
                    emArr.optDouble(2, 0.0).toFloat()
                )
            } else Vec3.ZERO

            val albIdx = pbr?.optJSONObject("baseColorTexture")?.optInt("index", -1) ?: -1
            val mraIdx = pbr?.optJSONObject("metallicRoughnessTexture")?.optInt("index", -1) ?: -1
            val occIdx = mObj.optJSONObject("occlusionTexture")?.optInt("index", -1) ?: -1
            val nrmIdx = mObj.optJSONObject("normalTexture")?.optInt("index", -1) ?: -1
            val decodedMra = if (remapGltfMra) {
                reader.decodeGltfMraTextureIndex(mraIdx, occIdx, metallic, roughness, decodeTextures)
            } else {
                reader.decodeTextureIndex(mraIdx, decodeTextures)
            }
            materials.add(
                GlbMaterialTextures(
                    name = name,
                    metallicFactor = metallic,
                    roughnessFactor = roughness,
                    albedo = reader.decodeTextureIndex(albIdx, decodeTextures),
                    normal = reader.decodeTextureIndex(nrmIdx, decodeTextures),
                    mra = decodedMra,
                    baseColorFactor = baseCol,
                    alphaFactor = alphaFactor,
                    emissiveFactor = emissive
                )
            )
        }
        return materials
    }

    fun parseStream(input: InputStream, assetName: String = "character.glb", decodeTextures: Boolean = true): GlbCharacterAsset {
        val chunks = readChunks(input, assetName)
        val rootJson = chunks.rootJson
        val reader = AccessorReader(chunks)

        val nodesJson = rootJson.getJSONArray("nodes")
        val meshesJson = rootJson.getJSONArray("meshes")
        val skinsJson = rootJson.optJSONArray("skins")
        val animsJson = rootJson.optJSONArray("animations") ?: JSONArray()

        val errors = ArrayList<String>()
        val materials = parseMaterials(rootJson, reader, decodeTextures)

        // 2. Parse Skeleton Bones
        val bones = ArrayList<GlbBoneNode>()
        val boneIndexByName = HashMap<String, Int>()
        val nodeToBone = HashMap<Int, Int>()

        if (skinsJson != null && skinsJson.length() > 0) {
            val skin = skinsJson.getJSONObject(0)
            val jointsArr = skin.getJSONArray("joints")
            val ibmFloats = reader.readFloatArray(skin.getInt("inverseBindMatrices"), 16)

            val nodeParent = HashMap<Int, Int>()
            for (ni in 0 until nodesJson.length()) {
                val nObj = nodesJson.getJSONObject(ni)
                val ch = nObj.optJSONArray("children") ?: continue
                for (ci in 0 until ch.length()) {
                    nodeParent[ch.getInt(ci)] = ni
                }
            }
            for (bi in 0 until jointsArr.length()) {
                nodeToBone[jointsArr.getInt(bi)] = bi
            }
            for (bi in 0 until jointsArr.length()) {
                val ni = jointsArr.getInt(bi)
                val nObj = nodesJson.getJSONObject(ni)
                val bName = nObj.optString("name", "Bone_$bi")
                val parentNode = nodeParent[ni]
                val parentBone = if (parentNode != null) nodeToBone[parentNode] ?: -1 else -1
                val trArr = nObj.optJSONArray("translation")
                val localPos = if (trArr != null && trArr.length() >= 3) {
                    Vec3(trArr.getDouble(0).toFloat(), trArr.getDouble(1).toFloat(), trArr.getDouble(2).toFloat())
                } else Vec3.ZERO
                val rotArr = nObj.optJSONArray("rotation")
                val localRot = if (rotArr != null && rotArr.length() >= 4) {
                    Quat(
                        rotArr.getDouble(0).toFloat(),
                        rotArr.getDouble(1).toFloat(),
                        rotArr.getDouble(2).toFloat(),
                        rotArr.getDouble(3).toFloat()
                    ).normalized()
                } else Quat.IDENTITY
                val ibm = FloatArray(16)
                System.arraycopy(ibmFloats, bi * 16, ibm, 0, 16)
                bones.add(GlbBoneNode(bi, ni, bName, parentBone, localPos, ibm, localRot))
                boneIndexByName[bName] = bi
            }
        } else {
            errors.add("Missing skin / skeleton definition")
        }

        // 3. Parse Meshes & Primitives
        val primitives = ArrayList<GlbPrimitiveData>()
        var totalVerts = 0
        var totalTris = 0

        for (mi in 0 until meshesJson.length()) {
            val meshObj = meshesJson.getJSONObject(mi)
            val meshName = meshObj.optString("name", "Mesh_$mi")
            val extras = meshObj.optJSONObject("extras")
            val targetNamesArr = extras?.optJSONArray("targetNames")
            val primsArr = meshObj.getJSONArray("primitives")

            for (pi in 0 until primsArr.length()) {
                val pObj = primsArr.getJSONObject(pi)
                val attrs = pObj.getJSONObject("attributes")
                if (!attrs.has("POSITION") || !attrs.has("NORMAL") || !attrs.has("JOINTS_0") || !attrs.has("WEIGHTS_0")) {
                    errors.add("Mesh '$meshName' missing required vertex skinning attributes")
                    continue
                }
                val pos = reader.readFloatArray(attrs.getInt("POSITION"), 3)
                val nrm = reader.readFloatArray(attrs.getInt("NORMAL"), 3)
                val vCount = pos.size / 3
                val tan = if (attrs.has("TANGENT")) reader.readFloatArray(attrs.getInt("TANGENT"), 4) else FloatArray(vCount * 4) { idx -> if (idx % 4 == 0 || idx % 4 == 3) 1f else 0f }
                val uv = if (attrs.has("TEXCOORD_0")) reader.readFloatArray(attrs.getInt("TEXCOORD_0"), 2) else FloatArray(vCount * 2)
                val joints = reader.readJointsVec4(attrs.getInt("JOINTS_0"))
                val weights = reader.readFloatArray(attrs.getInt("WEIGHTS_0"), 4)
                val indices = reader.readShortIndices(pObj.getInt("indices"))
                val matIdx = pObj.optInt("material", 0)

                val morphList = ArrayList<GlbMorphTarget>()
                val targetsJson = pObj.optJSONArray("targets")
                if (targetsJson != null) {
                    for (ti in 0 until targetsJson.length()) {
                        val tObj = targetsJson.getJSONObject(ti)
                        if (tObj.has("POSITION")) {
                            val tName = targetNamesArr?.optString(ti, "target_$ti") ?: "target_$ti"
                            morphList.add(GlbMorphTarget(tName, reader.readFloatArray(tObj.getInt("POSITION"), 3)))
                        }
                    }
                }

                totalVerts += vCount
                totalTris += indices.size / 3
                primitives.add(
                    GlbPrimitiveData(
                        meshName = meshName,
                        materialIndex = matIdx,
                        positions = pos,
                        normals = nrm,
                        tangents = tan,
                        uvs = uv,
                        joints = joints,
                        weights = weights,
                        indices = indices,
                        morphTargets = morphList
                    )
                )
            }
        }

        // 4. Parse Animations
        val animClips = LinkedHashMap<String, GlbAnimationClip>()
        for (ai in 0 until animsJson.length()) {
            val aObj = animsJson.getJSONObject(ai)
            val clipName = aObj.optString("name", "Anim_$ai")
            val samplersArr = aObj.getJSONArray("samplers")
            val channelsArr = aObj.getJSONArray("channels")
            val channels = ArrayList<GlbAnimChannel>(channelsArr.length())
            var maxTime = 0.0f

            for (ci in 0 until channelsArr.length()) {
                val chObj = channelsArr.getJSONObject(ci)
                val target = chObj.getJSONObject("target")
                val nodeIdx = target.optInt("node", -1)
                val boneIdx = nodeToBone[nodeIdx] ?: continue
                val pathStr = target.optString("path", "")
                val path = when (pathStr) {
                    "rotation" -> GlbChannelPath.ROTATION
                    "translation" -> GlbChannelPath.TRANSLATION
                    else -> continue
                }
                val sampler = samplersArr.getJSONObject(chObj.getInt("sampler"))
                val times = reader.readFloatArray(sampler.getInt("input"), 1)
                if (times.isNotEmpty() && times.last() > maxTime) maxTime = times.last()
                val comps = if (path == GlbChannelPath.ROTATION) 4 else 3
                val vals = reader.readFloatArray(sampler.getInt("output"), comps)
                channels.add(GlbAnimChannel(boneIdx, path, times, vals))
            }
            if (channels.isEmpty()) {
                errors.add("Animation clip '$clipName' contains no valid bone channels")
            }
            animClips[clipName] = GlbAnimationClip(clipName, maxTime.coerceAtLeast(0.05f), channels)
        }

        return GlbCharacterAsset(
            assetPath = assetName,
            primitives = primitives,
            materials = materials,
            bones = bones,
            boneIndexByName = boneIndexByName,
            animations = animClips,
            totalVertices = totalVerts,
            totalTriangles = totalTris,
            validationErrors = errors
        )
    }

    fun parseStaticStream(input: InputStream, assetName: String = "model.glb", decodeTextures: Boolean = true): GlbStaticAsset {
        val chunks = readChunks(input, assetName)
        val rootJson = chunks.rootJson
        val reader = AccessorReader(chunks)
        val errors = ArrayList<String>()

        val materials = parseMaterials(rootJson, reader, decodeTextures, remapGltfMra = true)
        val nodesJson = rootJson.optJSONArray("nodes") ?: JSONArray()
        val meshesJson = rootJson.optJSONArray("meshes") ?: JSONArray()
        val scenesJson = rootJson.optJSONArray("scenes")

        val sockets = LinkedHashMap<String, GlbSocket>()
        val primitives = ArrayList<GlbStaticPrimitive>()

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        var totalVerts = 0
        var totalTris = 0

        fun buildLocalNodeMatrix(nodeObj: JSONObject): FloatArray {
            val mat = FloatArray(16)
            val mArr = nodeObj.optJSONArray("matrix")
            if (mArr != null && mArr.length() == 16) {
                for (i in 0 until 16) mat[i] = mArr.getDouble(i).toFloat()
                return mat
            }
            Matrix.setIdentityM(mat, 0)
            val tr = nodeObj.optJSONArray("translation")
            val tx = tr?.optDouble(0, 0.0)?.toFloat() ?: 0f
            val ty = tr?.optDouble(1, 0.0)?.toFloat() ?: 0f
            val tz = tr?.optDouble(2, 0.0)?.toFloat() ?: 0f
            val rotArr = nodeObj.optJSONArray("rotation")
            val q = if (rotArr != null && rotArr.length() >= 4) {
                Quat(
                    rotArr.optDouble(0, 0.0).toFloat(),
                    rotArr.optDouble(1, 0.0).toFloat(),
                    rotArr.optDouble(2, 0.0).toFloat(),
                    rotArr.optDouble(3, 1.0).toFloat()
                ).normalized()
            } else Quat.IDENTITY
            val sc = nodeObj.optJSONArray("scale")
            val sx = sc?.optDouble(0, 1.0)?.toFloat() ?: 1f
            val sy = sc?.optDouble(1, 1.0)?.toFloat() ?: 1f
            val sz = sc?.optDouble(2, 1.0)?.toFloat() ?: 1f
            q.writeModelMatrix(Vec3(tx, ty, tz), Vec3(sx, sy, sz), mat)
            return mat
        }

        fun extractSocketsFromExtras(extras: JSONObject?, worldMat: FloatArray) {
            if (extras == null) return
            val sockObj = extras.optJSONObject("sockets") ?: return
            val keys = sockObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = sockObj.optJSONArray(key)
                if (arr != null && arr.length() >= 3) {
                    val p = transformPoint(worldMat, arr.optDouble(0, 0.0).toFloat(), arr.optDouble(1, 0.0).toFloat(), arr.optDouble(2, 0.0).toFloat())
                    sockets[key] = GlbSocket(key, p)
                }
            }
        }

        fun visitNode(nodeIdx: Int, parentMat: FloatArray) {
            if (nodeIdx < 0 || nodeIdx >= nodesJson.length()) return
            val nodeObj = nodesJson.getJSONObject(nodeIdx)
            val nodeName = nodeObj.optString("name", "Node_$nodeIdx")
            val localMat = buildLocalNodeMatrix(nodeObj)
            val worldMat = FloatArray(16)
            Matrix.multiplyMM(worldMat, 0, parentMat, 0, localMat, 0)

            val extras = nodeObj.optJSONObject("extras")
            extractSocketsFromExtras(extras, worldMat)
            if (nodeName.startsWith("socket_", ignoreCase = true)) {
                val key = nodeName.removePrefix("socket_").lowercase()
                val pos = Vec3(worldMat[12], worldMat[13], worldMat[14])
                val fwd = transformDir(worldMat, 0f, 0f, -1f)
                val up = transformDir(worldMat, 0f, 1f, 0f)
                sockets[key] = GlbSocket(key, pos, fwd, up)
            }

            if (nodeObj.has("mesh")) {
                val meshIdx = nodeObj.getInt("mesh")
                if (meshIdx in 0 until meshesJson.length()) {
                    val meshObj = meshesJson.getJSONObject(meshIdx)
                    val meshName = meshObj.optString("name", nodeName)
                    val primsArr = meshObj.getJSONArray("primitives")
                    for (pi in 0 until primsArr.length()) {
                        val pObj = primsArr.getJSONObject(pi)
                        val attrs = pObj.getJSONObject("attributes")
                        if (!attrs.has("POSITION")) {
                            errors.add("Static mesh '$meshName' missing POSITION")
                            continue
                        }
                        val rawPos = reader.readFloatArray(attrs.getInt("POSITION"), 3)
                        val vCount = rawPos.size / 3
                        val rawNrm = if (attrs.has("NORMAL")) reader.readFloatArray(attrs.getInt("NORMAL"), 3) else null
                        val rawTan = if (attrs.has("TANGENT")) reader.readFloatArray(attrs.getInt("TANGENT"), 4) else null
                        val uvs = if (attrs.has("TEXCOORD_0")) reader.readFloatArray(attrs.getInt("TEXCOORD_0"), 2) else FloatArray(vCount * 2)
                        val indices = if (pObj.has("indices")) {
                            reader.readShortIndices(pObj.getInt("indices"))
                        } else {
                            ShortArray(vCount) { it.toShort() }
                        }
                        val matIdx = pObj.optInt("material", 0)

                        val bakedPos = FloatArray(vCount * 3)
                        val bakedNrm = FloatArray(vCount * 3)
                        val bakedTan = FloatArray(vCount * 3)
                        var maxR2 = 0f

                        for (vi in 0 until vCount) {
                            val px = rawPos[vi * 3]
                            val py = rawPos[vi * 3 + 1]
                            val pz = rawPos[vi * 3 + 2]
                            val wp = transformPoint(worldMat, px, py, pz)
                            bakedPos[vi * 3] = wp.x
                            bakedPos[vi * 3 + 1] = wp.y
                            bakedPos[vi * 3 + 2] = wp.z

                            minX = min(minX, wp.x); minY = min(minY, wp.y); minZ = min(minZ, wp.z)
                            maxX = max(maxX, wp.x); maxY = max(maxY, wp.y); maxZ = max(maxZ, wp.z)
                            val r2 = wp.x * wp.x + wp.y * wp.y + wp.z * wp.z
                            if (r2 > maxR2) maxR2 = r2

                            val wn = if (rawNrm != null) {
                                transformDir(worldMat, rawNrm[vi * 3], rawNrm[vi * 3 + 1], rawNrm[vi * 3 + 2])
                            } else Vec3.UP
                            bakedNrm[vi * 3] = wn.x
                            bakedNrm[vi * 3 + 1] = wn.y
                            bakedNrm[vi * 3 + 2] = wn.z

                            val wt = if (rawTan != null) {
                                transformDir(worldMat, rawTan[vi * 4], rawTan[vi * 4 + 1], rawTan[vi * 4 + 2])
                            } else {
                                val fallback = if (kotlin.math.abs(wn.y) < 0.9f) Vec3.UP.cross(wn) else Vec3.RIGHT.cross(wn)
                                fallback.normalized()
                            }
                            bakedTan[vi * 3] = wt.x
                            bakedTan[vi * 3 + 1] = wt.y
                            bakedTan[vi * 3 + 2] = wt.z
                        }

                        if (rawNrm == null) {
                            computeNormalsInPlace(bakedPos, indices, bakedNrm)
                        }

                        val interleaved = FloatArray(vCount * 11)
                        for (vi in 0 until vCount) {
                            val o = vi * 11
                            val v3 = vi * 3
                            val v2 = vi * 2
                            interleaved[o + 0] = bakedPos[v3]
                            interleaved[o + 1] = bakedPos[v3 + 1]
                            interleaved[o + 2] = bakedPos[v3 + 2]
                            interleaved[o + 3] = bakedNrm[v3]
                            interleaved[o + 4] = bakedNrm[v3 + 1]
                            interleaved[o + 5] = bakedNrm[v3 + 2]
                            interleaved[o + 6] = bakedTan[v3]
                            interleaved[o + 7] = bakedTan[v3 + 1]
                            interleaved[o + 8] = bakedTan[v3 + 2]
                            interleaved[o + 9] = uvs[v2]
                            interleaved[o + 10] = uvs[v2 + 1]
                        }

                        totalVerts += vCount
                        totalTris += indices.size / 3
                        primitives.add(
                            GlbStaticPrimitive(
                                meshName = if (primsArr.length() > 1) "${meshName}_$pi" else meshName,
                                materialIndex = matIdx,
                                meshLevel = MeshLevel(interleaved, indices),
                                positions = bakedPos,
                                normals = bakedNrm,
                                uvs = uvs,
                                indices = indices,
                                boundingRadius = sqrt(maxR2).coerceAtLeast(0.05f)
                            )
                        )
                    }
                }
            }

            val children = nodeObj.optJSONArray("children")
            if (children != null) {
                for (ci in 0 until children.length()) {
                    visitNode(children.getInt(ci), worldMat)
                }
            }
        }

        val identity = FloatArray(16)
        Matrix.setIdentityM(identity, 0)
        val rootNodes = scenesJson?.optJSONObject(rootJson.optInt("scene", 0))?.optJSONArray("nodes")
        if (rootNodes != null && rootNodes.length() > 0) {
            for (i in 0 until rootNodes.length()) {
                visitNode(rootNodes.getInt(i), identity)
            }
        } else if (nodesJson.length() > 0) {
            val childSet = HashSet<Int>()
            for (ni in 0 until nodesJson.length()) {
                val ch = nodesJson.getJSONObject(ni).optJSONArray("children") ?: continue
                for (ci in 0 until ch.length()) childSet.add(ch.getInt(ci))
            }
            for (ni in 0 until nodesJson.length()) {
                if (ni !in childSet) visitNode(ni, identity)
            }
        }

        if (primitives.isEmpty()) {
            errors.add("No drawable primitives found in '$assetName'")
            minX = -0.25f; minY = -0.25f; minZ = -0.25f
            maxX = 0.25f; maxY = 0.25f; maxZ = 0.25f
        }

        val bMin = Vec3(minX, minY, minZ)
        val bMax = Vec3(maxX, maxY, maxZ)
        val diag = (bMax - bMin).length() * 0.5f

        return GlbStaticAsset(
            assetPath = assetName,
            primitives = primitives,
            materials = materials,
            sockets = sockets,
            boundsMin = bMin,
            boundsMax = bMax,
            boundingRadius = diag.coerceAtLeast(0.1f),
            totalVertices = totalVerts,
            totalTriangles = totalTris,
            validationErrors = errors
        )
    }

    private fun transformPoint(m: FloatArray, x: Float, y: Float, z: Float): Vec3 {
        return Vec3(
            m[0] * x + m[4] * y + m[8] * z + m[12],
            m[1] * x + m[5] * y + m[9] * z + m[13],
            m[2] * x + m[6] * y + m[10] * z + m[14]
        )
    }

    private fun transformDir(m: FloatArray, x: Float, y: Float, z: Float): Vec3 {
        val vx = m[0] * x + m[4] * y + m[8] * z
        val vy = m[1] * x + m[5] * y + m[9] * z
        val vz = m[2] * x + m[6] * y + m[10] * z
        val len = sqrt(vx * vx + vy * vy + vz * vz)
        return if (len > 1e-6f) Vec3(vx / len, vy / len, vz / len) else Vec3.UP
    }

    private fun computeNormalsInPlace(pos: FloatArray, indices: ShortArray, outNrm: FloatArray) {
        outNrm.fill(0f)
        var k = 0
        while (k + 2 < indices.size) {
            val i0 = (indices[k].toInt() and 0xFFFF) * 3
            val i1 = (indices[k + 1].toInt() and 0xFFFF) * 3
            val i2 = (indices[k + 2].toInt() and 0xFFFF) * 3
            val ax = pos[i1] - pos[i0]; val ay = pos[i1 + 1] - pos[i0 + 1]; val az = pos[i1 + 2] - pos[i0 + 2]
            val bx = pos[i2] - pos[i0]; val by = pos[i2 + 1] - pos[i0 + 1]; val bz = pos[i2 + 2] - pos[i0 + 2]
            val nx = ay * bz - az * by
            val ny = az * bx - ax * bz
            val nz = ax * by - ay * bx
            for (idx in intArrayOf(i0, i1, i2)) {
                outNrm[idx] += nx
                outNrm[idx + 1] += ny
                outNrm[idx + 2] += nz
            }
            k += 3
        }
        var vi = 0
        while (vi < outNrm.size) {
            val len = sqrt(outNrm[vi] * outNrm[vi] + outNrm[vi + 1] * outNrm[vi + 1] + outNrm[vi + 2] * outNrm[vi + 2])
            if (len > 1e-6f) {
                outNrm[vi] /= len; outNrm[vi + 1] /= len; outNrm[vi + 2] /= len
            } else {
                outNrm[vi] = 0f; outNrm[vi + 1] = 1f; outNrm[vi + 2] = 0f
            }
            vi += 3
        }
    }
}
