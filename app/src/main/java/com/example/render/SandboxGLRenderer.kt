package com.example.render

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.character.AnimationWorker
import com.example.character.BootStyle
import com.example.character.CharacterAppearance
import com.example.character.CharacterPoseSnapshot
import com.example.character.CharacterProfilePreset
import com.example.character.ClothingColorPreset
import com.example.character.GloveStyle
import com.example.character.HairStyle
import com.example.character.JacketStyle
import com.example.character.SkeletonPose
import com.example.character.ToolAttachmentRegistry
import com.example.character.TrouserStyle
import com.example.math.Frustum
import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.CameraMode
import com.example.physics.ConstraintType
import com.example.physics.MaterialLibrary
import com.example.physics.MaterialSpec
import com.example.physics.MeshId
import com.example.physics.ParticleSystem
import com.example.physics.PhysicsMaterial
import com.example.physics.PhysicsWorld
import com.example.physics.StaticProp
import com.example.physics.TexturePattern
import com.example.physics.ToolMeshStyle
import com.example.physics.ToolType
import com.example.physics.WorldRenderSnapshot
import com.example.render.gltf.GlbCharacterAsset
import com.example.render.gltf.GlbLoader
import com.example.render.gltf.GlbMaterialTextures
import com.example.render.gltf.GlbModelRegistry
import com.example.render.gltf.GlbMorphTarget
import com.example.render.gltf.GlbPrimitiveData
import com.example.render.gltf.GlbStaticAsset
import com.example.weapons.FirearmCatalog
import com.example.weapons.WeaponActionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class CharacterCameraFocus { FULL, HEAD, HANDS, BOOTS }

class CharacterPreviewState {
    @Volatile var enabled: Boolean = false
    @Volatile var orbitYawDeg: Float = 165f
    @Volatile var orbitPitchDeg: Float = -8f
    @Volatile var zoom: Float = 1.0f
    @Volatile var focusPreset: CharacterCameraFocus = CharacterCameraFocus.FULL
}

class AssetPreviewState {
    @Volatile var enabled: Boolean = false
    @Volatile var meshId: MeshId? = null
    @Volatile var toolStyle: ToolMeshStyle? = null
    @Volatile var materialSpec: MaterialSpec? = null
    @Volatile var orbitYawDeg: Float = 38f
    @Volatile var orbitPitchDeg: Float = -18f
    @Volatile var zoom: Float = 1.0f
}

private class GpuMeshLevel(
    val vao: Int,
    val vbo: Int,
    val ibo: Int,
    val indexCount: Int
)

private class GpuMeshAsset(
    val lod0: GpuMeshLevel,
    val lod1: GpuMeshLevel,
    val lod2: GpuMeshLevel,
    val boundingRadius: Float
) {
    fun selectLod(distSq: Float, qualityLevel: Int): GpuMeshLevel = when {
        qualityLevel == 0 -> if (distSq < 100f) lod1 else lod2
        distSq < 16f * 16f -> lod0
        distSq < 36f * 36f -> lod1
        else -> lod2
    }
}

private data class GpuMaterialTextures(
    val albedoId: Int,
    val normalId: Int,
    val mraId: Int
)

private class GpuSkinnedSubmesh(
    val meshName: String,
    val materialIndex: Int,
    val vao: Int,
    val vbo: Int,
    val ibo: Int,
    val indexCount: Int,
    val basePositions: FloatArray,
    val normals: FloatArray,
    val tangents: FloatArray,
    val uvs: FloatArray,
    val joints: IntArray,
    val weights: FloatArray,
    val morphTargets: List<GlbMorphTarget>,
    val packedScratch: FloatArray,
    val uploadBuffer: FloatBuffer?
) {
    var lastBlinkL = -1f
    var lastBlinkR = -1f
    var lastBrow = -1f
    var lastMouth = -1f
}

private class GpuCharacterLod(
    val submeshesByName: Map<String, GpuSkinnedSubmesh>,
    val materials: List<GpuMaterialTextures>,
    val metallicFactors: FloatArray,
    val roughnessFactors: FloatArray
)

private data class TransparentDrawItem(
    val distSq: Float,
    val modelMatrix: FloatArray,
    val mesh: GpuMeshLevel,
    val glbAsset: GpuStaticGlbAsset? = null,
    val spec: MaterialSpec,
    val colorTint: Vec3,
    val alpha: Float,
    val crackLevel: Float,
    val emissiveColor: Vec3,
    val emissiveIntensity: Float,
    val isHighlighted: Boolean,
    val isFrozen: Boolean
)

private class GpuGlbPrimitive(
    val mesh: GpuMeshLevel,
    val material: GlbMaterialTextures,
    val meshName: String,
    val textures: GpuMaterialTextures?,
    val hasNativeAlbedo: Boolean,
    val hasNativeNormal: Boolean,
    val hasNativeMra: Boolean
)

private class GpuStaticGlbAsset(
    val primitives: List<GpuGlbPrimitive>,
    val boundsCenter: Vec3,
    val halfExtents: Vec3,
    val muzzleSocket: Vec3?,
    val boundingRadius: Float
)

class SandboxGLRenderer(
    private val context: Context,
    private val physicsWorld: PhysicsWorld,
    private val animationWorker: AnimationWorker
) : GLSurfaceView.Renderer {

    private val assetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var cpuMeshes: Map<MeshId, MeshAsset>? = null
    @Volatile private var cpuToolMeshes: Map<ToolMeshStyle, MeshAsset>? = null
    @Volatile private var cpuMeshGlbs: Map<MeshId, GlbStaticAsset>? = null
    @Volatile private var cpuToolGlbs: Map<ToolMeshStyle, GlbStaticAsset>? = null
    @Volatile private var cpuPatternTextures: Map<TexturePattern, MaterialTextureData>? = null
    @Volatile private var cpuCharLods: List<GlbCharacterAsset>? = null
    @Volatile private var cpuCompanionChar: GlbCharacterAsset? = null
    @Volatile private var assetsUploaded = false
    @Volatile private var staticGlbsUploaded = false
    @Volatile private var charUploaded = false

    // Graphics configuration
    @Volatile var shadowsEnabled: Boolean = true
    @Volatile var graphicsQuality: Int = 2 // 0 Low, 1 Med, 2 High, 3 Ultra
    @Volatile var shadowQuality: Int = 2 // 0 Off, 1 512, 2 1024, 3 1536
    @Volatile var textureQuality: Int = 2 // 0 Low, 1 Med, 2 High
    @Volatile var aoQuality: Int = 2 // 0 Low, 1 Med, 2 High
    @Volatile var reflectionQuality: Int = 2 // 0 Low, 1 Med, 2 High
    @Volatile var resolutionScale: Float = 1.0f // 0.5..1.0
    @Volatile var renderDistance: Float = 140f // 60..220
    @Volatile var fpsLimit: Int = 60 // 0 = unlimited, 30, 60, 90, 120
    @Volatile var fovDegrees: Float = 70f
    private var currentRenderFovDeg: Float = 70f

    @Volatile var characterAppearance: CharacterAppearance = CharacterProfilePreset.VANCE.defaultAppearance
    @Volatile var useCompanionRig: Boolean = false
    val characterPreviewState = CharacterPreviewState()
    val assetPreviewState = AssetPreviewState()

    val fpsRef = AtomicReference(60)
    val culledCountRef = AtomicReference(0)
    val particleCountRef = AtomicReference(0)

    private val gpuMeshes = HashMap<MeshId, GpuMeshAsset>()
    private val gpuToolMeshes = HashMap<ToolMeshStyle, GpuMeshAsset>()
    private val gpuMeshGlbs = HashMap<MeshId, GpuStaticGlbAsset>()
    private val gpuToolGlbs = HashMap<ToolMeshStyle, GpuStaticGlbAsset>()
    private val gpuPatternTextures = HashMap<TexturePattern, GpuMaterialTextures>()
    private val uploadedGlbTexIds = ArrayList<Int>()
    private var neutralGlbTextures: GpuMaterialTextures? = null
    private val gpuCharLods = ArrayList<GpuCharacterLod>(3)
    private var gpuCompanionLod: GpuCharacterLod? = null
    private var companionPose: SkeletonPose? = null
    private var companionRightHandBoneIdx: Int = -1
    private var companionMotionPhase: Float = 0f

    private lateinit var pbrShader: ShaderProgram
    private lateinit var skinnedPbrShader: ShaderProgram
    private lateinit var shadowShader: ShaderProgram
    private lateinit var skinnedShadowShader: ShaderProgram
    private lateinit var lineShader: ShaderProgram
    private lateinit var skyShader: ShaderProgram
    private lateinit var particleShader: ShaderProgram

    private var boneUboId = 0
    private val boneUboBuffer: FloatBuffer = ByteBuffer.allocateDirect(72 * 16 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val boneUploadTmp = FloatArray(72 * 16)

    private var skyVao = 0
    private var lineVao = 0
    private var lineVbo = 0
    private val lineBuffer: FloatBuffer = ByteBuffer.allocateDirect(8192 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    private var particleVao = 0
    private var particleVbo = 0
    private val particleBuffer: FloatBuffer = ByteBuffer.allocateDirect(ParticleSystem.HARD_POOL_LIMIT * 8 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    private var shadowFbo = 0
    private var shadowTex = 0
    private var activeShadowMapSize = 1024

    // Optional offscreen resolution-scale FBO
    private var scaleFbo = 0
    private var scaleColorTex = 0
    private var scaleDepthRb = 0
    private var scaleW = 0
    private var scaleH = 0

    private var viewWidth = 1280
    private var viewHeight = 720

    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val handWorldMatrix = FloatArray(16)
    private val toolLocalMatrix = FloatArray(16)
    private val lightViewMatrix = FloatArray(16)
    private val lightProjMatrix = FloatArray(16)
    private val lightSpaceMatrix = FloatArray(16)
    private val lightMvpMatrix = FloatArray(16)
    private val frustum = Frustum()

    private var frameCount = 0
    private var lastFpsNs = System.nanoTime()
    private var lastFrameStartNs = System.nanoTime()
    private var lastAppliedTexQuality = -1

    @Volatile
    private var pendingScreenshotCallback: ((Bitmap?) -> Unit)? = null

    init {
        assetScope.launch {
            val meshes = ProceduralMeshGenerator.buildAllMeshes()
            val toolMeshes = ToolMeshGenerator.buildAllToolMeshes()
            val patterns = ProceduralTextureGenerator.generateAllPatterns(size = 256)
            cpuMeshes = meshes
            cpuToolMeshes = toolMeshes
            cpuPatternTextures = patterns
        }
        assetScope.launch {
            val high = runCatching { GlbLoader.loadFromAssets(context, "characters/operator_high.glb", true) }.getOrNull()
            val med = runCatching { GlbLoader.loadFromAssets(context, "characters/operator_med.glb", true) }.getOrNull()
            val low = runCatching { GlbLoader.loadFromAssets(context, "characters/operator_low.glb", true) }.getOrNull()
            val companion = runCatching { GlbLoader.loadFromAssets(context, GlbModelRegistry.KARAKTER_COMPANION_PATH, true) }.getOrNull()
            cpuCompanionChar = companion
            if (high != null) {
                animationWorker.setCharacterRigData(high.bones, high.animations)
                animationWorker.start()
                cpuCharLods = listOfNotNull(high, med ?: high, low ?: med ?: high)
            }
        }
        assetScope.launch {
            val meshMap = HashMap<MeshId, GlbStaticAsset>(MeshId.entries.size)
            for (id in MeshId.entries) {
                GlbModelRegistry.loadOrGet(context, GlbModelRegistry.assetPathFor(id), decodeTextures = true)?.let {
                    meshMap[id] = it
                }
            }
            val toolMap = HashMap<ToolMeshStyle, GlbStaticAsset>(ToolMeshStyle.entries.size)
            for (style in ToolMeshStyle.entries) {
                GlbModelRegistry.loadOrGet(context, GlbModelRegistry.assetPathFor(style), decodeTextures = true)?.let {
                    toolMap[style] = it
                }
            }
            cpuMeshGlbs = meshMap
            cpuToolGlbs = toolMap
        }
    }

    fun requestScreenshot(callback: (Bitmap?) -> Unit) {
        pendingScreenshotCallback = callback
    }

    fun shutdown() {
        assetScope.cancel()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        pbrShader = ShaderProgram(Shaders.PBR_VERTEX, Shaders.PBR_FRAGMENT)
        skinnedPbrShader = ShaderProgram(Shaders.SKINNED_PBR_VERTEX, Shaders.PBR_FRAGMENT)
        shadowShader = ShaderProgram(Shaders.SHADOW_VERTEX, Shaders.SHADOW_FRAGMENT)
        skinnedShadowShader = ShaderProgram(Shaders.SKINNED_SHADOW_VERTEX, Shaders.SHADOW_FRAGMENT)
        lineShader = ShaderProgram(Shaders.LINE_VERTEX, Shaders.LINE_FRAGMENT)
        skyShader = ShaderProgram(Shaders.SKY_VERTEX, Shaders.SKY_FRAGMENT)
        particleShader = ShaderProgram(Shaders.PARTICLE_VERTEX, Shaders.PARTICLE_FRAGMENT)

        initBoneUbo()
        initSkyQuad()
        initLineBuffers()
        initParticleBuffers()
        initShadowFbo(targetShadowSize())

        assetsUploaded = false
        staticGlbsUploaded = false
        charUploaded = false
        gpuMeshes.clear()
        gpuToolMeshes.clear()
        gpuMeshGlbs.clear()
        gpuToolGlbs.clear()
        gpuPatternTextures.clear()
        uploadedGlbTexIds.clear()
        neutralGlbTextures = null
        gpuCharLods.clear()
        gpuCompanionLod = null
        lastAppliedTexQuality = -1
        tryUploadCpuAssets()
        tryUploadStaticGlbs()
        tryUploadCharacterAssets()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width.coerceAtLeast(1)
        viewHeight = height.coerceAtLeast(1)
        GLES30.glViewport(0, 0, viewWidth, viewHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Frame pacing for FPS limit
        val limit = fpsLimit
        if (limit > 0) {
            val targetNs = 1_000_000_000L / limit
            val elapsed = System.nanoTime() - lastFrameStartNs
            val waitNs = targetNs - elapsed
            if (waitNs > 1_000_000L) {
                try {
                    Thread.sleep(waitNs / 1_000_000L, (waitNs % 1_000_000L).toInt())
                } catch (_: InterruptedException) {
                }
            }
        }
        lastFrameStartNs = System.nanoTime()

        if (!assetsUploaded) {
            tryUploadCpuAssets()
        }
        if (!staticGlbsUploaded) {
            tryUploadStaticGlbs()
        }
        if (!charUploaded) {
            tryUploadCharacterAssets()
        }

        val snapshot = physicsWorld.snapshotRef.get() ?: return
        if (!assetsUploaded) {
            GLES30.glClearColor(0.08f, 0.11f, 0.15f, 1f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
            return
        }

        animationWorker.previewModeActive = characterPreviewState.enabled || assetPreviewState.enabled
        animationWorker.submitWorldSnapshot(snapshot)

        // Dynamic shadow map resolution resize if setting changed
        val desiredShadowSize = targetShadowSize()
        if (desiredShadowSize != activeShadowMapSize) {
            rebuildShadowFbo(desiredShadowSize)
        }
        if (textureQuality != lastAppliedTexQuality) {
            applyTextureFilterQuality(textureQuality)
        }

        if (characterPreviewState.enabled) {
            renderCharacterStudioPreview(snapshot)
            updateFpsCounter()
            return
        }
        if (assetPreviewState.enabled) {
            renderAssetStudioPreview(snapshot)
            updateFpsCounter()
            return
        }

        val elapsedSec = ((System.nanoTime() - snapshot.timestampNs) / 1_000_000_000f)
        val alpha = (elapsedSec / PhysicsWorld.FIXED_DT).coerceIn(0f, 1f)

        val env = computeLightingForTimeOfDay(snapshot)
        updateCameraMatrices(snapshot, env.sunDir)

        if (shadowsEnabled && graphicsQuality > 0 && shadowQuality > 0) {
            renderShadowPass(snapshot, alpha)
        }

        val useScaledFbo = resolutionScale < 0.98f
        val renderW = if (useScaledFbo) (viewWidth * resolutionScale.coerceIn(0.5f, 1f)).toInt().coerceAtLeast(240) else viewWidth
        val renderH = if (useScaledFbo) (viewHeight * resolutionScale.coerceIn(0.5f, 1f)).toInt().coerceAtLeast(160) else viewHeight

        if (useScaledFbo) {
            ensureScaleFbo(renderW, renderH)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, scaleFbo)
        } else {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        }

        GLES30.glViewport(0, 0, renderW, renderH)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        renderSkyPass(snapshot, env)
        renderMainPbrPass(snapshot, alpha, env)
        renderConstraintAndWireLines(snapshot)
        renderParticlesPass(snapshot, renderH)
        if (!snapshot.isMenuDemo && snapshot.cameraMode == CameraMode.FIRST_PERSON) {
            renderFirstPersonArmsAndTool(snapshot, env)
        }

        if (useScaledFbo) {
            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, scaleFbo)
            GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0)
            GLES30.glBlitFramebuffer(
                0, 0, renderW, renderH,
                0, 0, viewWidth, viewHeight,
                GLES30.GL_COLOR_BUFFER_BIT, GLES30.GL_LINEAR
            )
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        }

        pendingScreenshotCallback?.let { cb ->
            pendingScreenshotCallback = null
            cb(captureFramebufferBitmap())
        }

        updateFpsCounter()
    }

    private fun targetShadowSize(): Int = when (shadowQuality) {
        0 -> 512
        1 -> 512
        2 -> 1024
        else -> 1536
    }

    private data class EnvLighting(
        val sunDir: Vec3,
        val sunColor: Vec3,
        val skyColor: Vec3,
        val groundAmbient: Vec3,
        val fogColor: Vec3
    )

    private fun computeLightingForTimeOfDay(snap: WorldRenderSnapshot): EnvLighting {
        val map = snap.mapId
        if (!map.isOutdoor && snap.timeOfDayHours in 8f..18f) {
            return EnvLighting(map.sunDir, map.sunColor, map.skyColor, map.groundAmbient, map.fogColor)
        }
        // Map 0..24h -> solar angle (-PI/2 midnight, 0 sunrise 6h, PI/2 noon 12h, PI sunset 18h)
        val hourNorm = ((snap.timeOfDayHours - 6f) / 24f) * 2f * PI.toFloat()
        val elev = sin(hourNorm)
        val sunDir = Vec3(cos(hourNorm) * 0.65f, max(0.18f, elev), 0.35f).normalized()
        val dayFactor = ((elev + 0.25f) / 1.25f).coerceIn(0.14f, 1.0f)
        val duskFactor = (1f - kotlin.math.abs(elev) * 2.4f).coerceIn(0f, 1f)
        val sunCol = Vec3(
            (map.sunColor.x * dayFactor + duskFactor * 0.35f).coerceIn(0.15f, 1.0f),
            (map.sunColor.y * dayFactor * (1f - duskFactor * 0.25f)).coerceIn(0.14f, 1.0f),
            (map.sunColor.z * dayFactor * (1f - duskFactor * 0.50f)).coerceIn(0.18f, 1.0f)
        )
        val nightBase = Vec3(0.08f, 0.11f, 0.17f)
        val skyCol = nightBase.lerp(map.skyColor, dayFactor)
        val ambCol = Vec3(0.06f, 0.08f, 0.11f).lerp(map.groundAmbient, dayFactor)
        val fogCol = Vec3(0.07f, 0.09f, 0.14f).lerp(map.fogColor, dayFactor)
        return EnvLighting(sunDir, sunCol, skyCol, ambCol, fogCol)
    }

    private fun tryUploadCpuAssets() {
        val m = cpuMeshes ?: return
        val tm = cpuToolMeshes ?: return
        val pt = cpuPatternTextures ?: return
        for ((id, asset) in m) {
            gpuMeshes[id] = GpuMeshAsset(
                lod0 = uploadMeshLevel(asset.lod0),
                lod1 = uploadMeshLevel(asset.lod1),
                lod2 = uploadMeshLevel(asset.lod2),
                boundingRadius = asset.boundingRadius
            )
        }
        for ((style, asset) in tm) {
            gpuToolMeshes[style] = GpuMeshAsset(
                lod0 = uploadMeshLevel(asset.lod0),
                lod1 = uploadMeshLevel(asset.lod1),
                lod2 = uploadMeshLevel(asset.lod2),
                boundingRadius = asset.boundingRadius
            )
        }
        for ((pat, tex) in pt) {
            gpuPatternTextures[pat] = GpuMaterialTextures(
                albedoId = uploadTexture2D(tex.size, tex.size, tex.albedoRgba),
                normalId = uploadTexture2D(tex.size, tex.size, tex.normalRgba),
                mraId = uploadTexture2D(tex.size, tex.size, tex.mraRgba)
            )
        }
        applyTextureFilterQuality(textureQuality)
        assetsUploaded = true
    }

    private fun tryUploadStaticGlbs() {
        val mGlb = cpuMeshGlbs ?: return
        val tGlb = cpuToolGlbs ?: return
        if (neutralGlbTextures == null) {
            neutralGlbTextures = createNeutralGlbTextures()
        }
        for ((id, asset) in mGlb) {
            gpuMeshGlbs[id] = uploadStaticGlbAsset(asset)
        }
        for ((style, asset) in tGlb) {
            gpuToolGlbs[style] = uploadStaticGlbAsset(asset)
        }
        applyTextureFilterQuality(textureQuality)
        staticGlbsUploaded = gpuMeshGlbs.isNotEmpty()
    }

    private fun createNeutralGlbTextures(): GpuMaterialTextures {
        val sz = 4
        val albBuf = ByteBuffer.allocateDirect(sz * sz * 4).order(ByteOrder.nativeOrder())
        val nrmBuf = ByteBuffer.allocateDirect(sz * sz * 4).order(ByteOrder.nativeOrder())
        val mraBuf = ByteBuffer.allocateDirect(sz * sz * 4).order(ByteOrder.nativeOrder())
        repeat(sz * sz) {
            // Albedo 153 = 0.60 so (0.76 + 0.40 * 0.60) == 1.000 exact uColorTint preservation
            albBuf.put(153.toByte()); albBuf.put(153.toByte()); albBuf.put(153.toByte()); albBuf.put(255.toByte())
            nrmBuf.put(128.toByte()); nrmBuf.put(128.toByte()); nrmBuf.put(255.toByte()); nrmBuf.put(255.toByte())
            // MRA g=128 (0.50) so (0.85 + 0.30 * 0.50) == 1.000 exact uRoughnessOverride preservation
            mraBuf.put(0.toByte()); mraBuf.put(128.toByte()); mraBuf.put(255.toByte()); mraBuf.put(255.toByte())
        }
        return GpuMaterialTextures(
            albedoId = uploadTexture2D(sz, sz, albBuf),
            normalId = uploadTexture2D(sz, sz, nrmBuf),
            mraId = uploadTexture2D(sz, sz, mraBuf)
        )
    }

    private fun uploadStaticGlbAsset(asset: GlbStaticAsset): GpuStaticGlbAsset {
        val neutralTex = neutralGlbTextures ?: createNeutralGlbTextures().also { neutralGlbTextures = it }
        val uploadedMatCache = HashMap<Int, GpuMaterialTextures>()
        val prims = asset.primitives.map { p ->
            val mat = asset.materials.getOrElse(p.materialIndex) {
                GlbMaterialTextures("default", 0.1f, 0.5f, null, null, null)
            }
            val gpuMatTex = uploadedMatCache.getOrPut(p.materialIndex) {
                val albId = mat.albedo?.let { blob ->
                    uploadTexture2D(blob.width, blob.height, blob.rgba).also { uploadedGlbTexIds.add(it) }
                } ?: neutralTex.albedoId
                val nrmId = mat.normal?.let { blob ->
                    uploadTexture2D(blob.width, blob.height, blob.rgba).also { uploadedGlbTexIds.add(it) }
                } ?: neutralTex.normalId
                val mraId = mat.mra?.let { blob ->
                    uploadTexture2D(blob.width, blob.height, blob.rgba).also { uploadedGlbTexIds.add(it) }
                } ?: neutralTex.mraId
                GpuMaterialTextures(albId, nrmId, mraId)
            }
            GpuGlbPrimitive(
                mesh = uploadMeshLevel(p.meshLevel),
                material = mat,
                meshName = p.meshName,
                textures = gpuMatTex,
                hasNativeAlbedo = mat.albedo != null,
                hasNativeNormal = mat.normal != null,
                hasNativeMra = mat.mra != null
            )
        }
        val half = asset.boundsSize * 0.5f
        return GpuStaticGlbAsset(
            primitives = prims,
            boundsCenter = asset.boundsCenter,
            halfExtents = Vec3(half.x.coerceAtLeast(0.05f), half.y.coerceAtLeast(0.05f), half.z.coerceAtLeast(0.05f)),
            muzzleSocket = asset.sockets["muzzle"]?.position,
            boundingRadius = asset.boundingRadius
        )
    }

    private fun uploadCharacterLod(asset: GlbCharacterAsset): GpuCharacterLod {
        val matTex = ArrayList<GpuMaterialTextures>(asset.materials.size)
        val mets = FloatArray(asset.materials.size)
        val rghs = FloatArray(asset.materials.size)
        val fallbackTex = gpuPatternTextures[TexturePattern.GLASS_SMOOTH] ?: neutralGlbTextures
        asset.materials.forEachIndexed { idx, mat ->
            val alb = mat.albedo?.let {
                uploadTexture2D(it.width, it.height, it.rgba).also { id -> uploadedGlbTexIds.add(id) }
            } ?: fallbackTex?.albedoId ?: 0
            val nrm = mat.normal?.let {
                uploadTexture2D(it.width, it.height, it.rgba).also { id -> uploadedGlbTexIds.add(id) }
            } ?: fallbackTex?.normalId ?: 0
            val mra = mat.mra?.let {
                uploadTexture2D(it.width, it.height, it.rgba).also { id -> uploadedGlbTexIds.add(id) }
            } ?: fallbackTex?.mraId ?: 0
            matTex.add(GpuMaterialTextures(alb, nrm, mra))
            mets[idx] = mat.metallicFactor
            rghs[idx] = mat.roughnessFactor
        }
        val submeshes = HashMap<String, GpuSkinnedSubmesh>(asset.primitives.size)
        for (prim in asset.primitives) {
            submeshes[prim.meshName] = uploadSkinnedSubmesh(prim)
        }
        return GpuCharacterLod(submeshes, matTex, mets, rghs)
    }

    private fun tryUploadCharacterAssets() {
        val lods = cpuCharLods ?: return
        gpuCharLods.clear()
        for (asset in lods) {
            gpuCharLods.add(uploadCharacterLod(asset))
        }
        val companion = cpuCompanionChar
        if (companion != null && gpuCompanionLod == null) {
            gpuCompanionLod = uploadCharacterLod(companion)
            companionPose = SkeletonPose(companion.bones.size).apply { resetToBind(companion.bones) }
            companionRightHandBoneIdx = companion.boneIndexByName["mixamorig_RightHand"] ?: -1
        }
        charUploaded = gpuCharLods.isNotEmpty()
    }

    private fun initBoneUbo() {
        val pbrIdx = GLES30.glGetUniformBlockIndex(skinnedPbrShader.programId, "BoneBlock")
        if (pbrIdx >= 0) GLES30.glUniformBlockBinding(skinnedPbrShader.programId, pbrIdx, 0)
        val shdIdx = GLES30.glGetUniformBlockIndex(skinnedShadowShader.programId, "BoneBlock")
        if (shdIdx >= 0) GLES30.glUniformBlockBinding(skinnedShadowShader.programId, shdIdx, 0)

        val uboArr = IntArray(1)
        GLES30.glGenBuffers(1, uboArr, 0)
        boneUboId = uboArr[0]
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, boneUboId)
        GLES30.glBufferData(GLES30.GL_UNIFORM_BUFFER, 72 * 16 * 4, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBindBufferBase(GLES30.GL_UNIFORM_BUFFER, 0, boneUboId)
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, 0)
    }

    private fun uploadBonePalette(skinMatrices: FloatArray) {
        if (boneUboId == 0) return
        val copyLen = minOf(skinMatrices.size, boneUploadTmp.size)
        System.arraycopy(skinMatrices, 0, boneUploadTmp, 0, copyLen)
        for (b in (copyLen / 16) until 72) {
            val base = b * 16
            boneUploadTmp[base] = 1f; boneUploadTmp[base + 1] = 0f; boneUploadTmp[base + 2] = 0f; boneUploadTmp[base + 3] = 0f
            boneUploadTmp[base + 4] = 0f; boneUploadTmp[base + 5] = 1f; boneUploadTmp[base + 6] = 0f; boneUploadTmp[base + 7] = 0f
            boneUploadTmp[base + 8] = 0f; boneUploadTmp[base + 9] = 0f; boneUploadTmp[base + 10] = 1f; boneUploadTmp[base + 11] = 0f
            boneUploadTmp[base + 12] = 0f; boneUploadTmp[base + 13] = 0f; boneUploadTmp[base + 14] = 0f; boneUploadTmp[base + 15] = 1f
        }
        boneUboBuffer.clear()
        boneUboBuffer.put(boneUploadTmp)
        boneUboBuffer.flip()
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, boneUboId)
        GLES30.glBufferSubData(GLES30.GL_UNIFORM_BUFFER, 0, boneUploadTmp.size * 4, boneUboBuffer)
        GLES30.glBindBufferBase(GLES30.GL_UNIFORM_BUFFER, 0, boneUboId)
    }

    private fun uploadSkinnedSubmesh(prim: GlbPrimitiveData): GpuSkinnedSubmesh {
        val vCount = prim.vertexCount
        val strideFloats = 19
        val packed = FloatArray(vCount * strideFloats)
        packSkinnedVertices(prim.positions, prim.normals, prim.tangents, prim.uvs, prim.joints, prim.weights, packed)

        val vaoArr = IntArray(1)
        val vboArr = IntArray(1)
        val iboArr = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoArr, 0)
        GLES30.glGenBuffers(1, vboArr, 0)
        GLES30.glGenBuffers(1, iboArr, 0)

        GLES30.glBindVertexArray(vaoArr[0])

        val vb = ByteBuffer.allocateDirect(packed.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(packed); position(0)
        }
        val usage = if (prim.morphTargets.isNotEmpty()) GLES30.GL_DYNAMIC_DRAW else GLES30.GL_STATIC_DRAW
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboArr[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, packed.size * 4, vb, usage)

        val ib = ByteBuffer.allocateDirect(prim.indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply {
            put(prim.indices); position(0)
        }
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, iboArr[0])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, prim.indices.size * 2, ib, GLES30.GL_STATIC_DRAW)

        val strideBytes = strideFloats * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, strideBytes, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, strideBytes, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, strideBytes, 6 * 4)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 2, GLES30.GL_FLOAT, false, strideBytes, 9 * 4)
        GLES30.glEnableVertexAttribArray(4)
        GLES30.glVertexAttribPointer(4, 4, GLES30.GL_FLOAT, false, strideBytes, 11 * 4)
        GLES30.glEnableVertexAttribArray(5)
        GLES30.glVertexAttribPointer(5, 4, GLES30.GL_FLOAT, false, strideBytes, 15 * 4)
        GLES30.glBindVertexArray(0)

        return GpuSkinnedSubmesh(
            meshName = prim.meshName,
            materialIndex = prim.materialIndex,
            vao = vaoArr[0],
            vbo = vboArr[0],
            ibo = iboArr[0],
            indexCount = prim.indices.size,
            basePositions = prim.positions,
            normals = prim.normals,
            tangents = prim.tangents,
            uvs = prim.uvs,
            joints = prim.joints,
            weights = prim.weights,
            morphTargets = prim.morphTargets,
            packedScratch = packed,
            uploadBuffer = if (prim.morphTargets.isNotEmpty()) vb else null
        )
    }

    private fun packSkinnedVertices(
        pos: FloatArray,
        nrm: FloatArray,
        tan: FloatArray,
        uv: FloatArray,
        joints: IntArray,
        weights: FloatArray,
        out: FloatArray
    ) {
        val vCount = pos.size / 3
        for (i in 0 until vCount) {
            val o = i * 19
            val p3 = i * 3
            val t4 = i * 4
            val u2 = i * 2
            out[o] = pos[p3]; out[o + 1] = pos[p3 + 1]; out[o + 2] = pos[p3 + 2]
            out[o + 3] = nrm[p3]; out[o + 4] = nrm[p3 + 1]; out[o + 5] = nrm[p3 + 2]
            out[o + 6] = tan[t4]; out[o + 7] = tan[t4 + 1]; out[o + 8] = tan[t4 + 2]
            out[o + 9] = uv[u2]; out[o + 10] = uv[u2 + 1]
            out[o + 11] = joints[t4].toFloat()
            out[o + 12] = joints[t4 + 1].toFloat()
            out[o + 13] = joints[t4 + 2].toFloat()
            out[o + 14] = joints[t4 + 3].toFloat()
            out[o + 15] = weights[t4]
            out[o + 16] = weights[t4 + 1]
            out[o + 17] = weights[t4 + 2]
            out[o + 18] = weights[t4 + 3]
        }
    }

    private fun applyTextureFilterQuality(quality: Int) {
        lastAppliedTexQuality = quality
        val minFilter = when (quality) {
            0 -> GLES30.GL_NEAREST_MIPMAP_NEAREST
            1 -> GLES30.GL_LINEAR_MIPMAP_NEAREST
            else -> GLES30.GL_LINEAR_MIPMAP_LINEAR
        }
        for (tex in gpuPatternTextures.values) {
            for (id in listOf(tex.albedoId, tex.normalId, tex.mraId)) {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, minFilter)
            }
        }
        for (id in uploadedGlbTexIds) {
            if (id != 0) {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, minFilter)
            }
        }
    }

    private fun uploadMeshLevel(level: MeshLevel): GpuMeshLevel {
        val vaoArr = IntArray(1)
        val vboArr = IntArray(1)
        val iboArr = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoArr, 0)
        GLES30.glGenBuffers(1, vboArr, 0)
        GLES30.glGenBuffers(1, iboArr, 0)

        GLES30.glBindVertexArray(vaoArr[0])

        val vb: FloatBuffer = ByteBuffer.allocateDirect(level.vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(level.vertices); position(0)
            }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboArr[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, level.vertices.size * 4, vb, GLES30.GL_STATIC_DRAW)

        val ib: ShortBuffer = ByteBuffer.allocateDirect(level.indices.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer().apply {
                put(level.indices); position(0)
            }
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, iboArr[0])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, level.indices.size * 2, ib, GLES30.GL_STATIC_DRAW)

        val stride = 11 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 6 * 4)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 2, GLES30.GL_FLOAT, false, stride, 9 * 4)

        GLES30.glBindVertexArray(0)
        return GpuMeshLevel(vaoArr[0], vboArr[0], iboArr[0], level.indexCount)
    }

    private fun uploadTexture2D(width: Int, height: Int, data: ByteBuffer): Int {
        val texArr = IntArray(1)
        GLES30.glGenTextures(1, texArr, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texArr[0])
        data.position(0)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8,
            width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, data
        )
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
        return texArr[0]
    }

    private fun initSkyQuad() {
        val quad = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        val vao = IntArray(1); val vbo = IntArray(1)
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(1, vbo, 0)
        GLES30.glBindVertexArray(vao[0])
        val buf = ByteBuffer.allocateDirect(quad.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(quad); position(0)
        }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quad.size * 4, buf, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0)
        GLES30.glBindVertexArray(0)
        skyVao = vao[0]
    }

    private fun initLineBuffers() {
        val vao = IntArray(1); val vbo = IntArray(1)
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(1, vbo, 0)
        GLES30.glBindVertexArray(vao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 8192 * 4, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 12, 0)
        GLES30.glBindVertexArray(0)
        lineVao = vao[0]
        lineVbo = vbo[0]
    }

    private fun initParticleBuffers() {
        val vao = IntArray(1); val vbo = IntArray(1)
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(1, vbo, 0)
        GLES30.glBindVertexArray(vao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, ParticleSystem.HARD_POOL_LIMIT * 8 * 4, null, GLES30.GL_DYNAMIC_DRAW)
        val stride = 8 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 4, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, stride, 4 * 4)
        GLES30.glBindVertexArray(0)
        particleVao = vao[0]
        particleVbo = vbo[0]
    }

    private fun initShadowFbo(size: Int) {
        activeShadowMapSize = size
        val fbo = IntArray(1); val tex = IntArray(1)
        GLES30.glGenFramebuffers(1, fbo, 0)
        GLES30.glGenTextures(1, tex, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0])
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT24,
            size, size, 0, GLES30.GL_DEPTH_COMPONENT, GLES30.GL_UNSIGNED_INT, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo[0])
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_TEXTURE_2D, tex[0], 0)
        GLES30.glDrawBuffers(1, intArrayOf(GLES30.GL_NONE), 0)
        GLES30.glReadBuffer(GLES30.GL_NONE)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        shadowFbo = fbo[0]
        shadowTex = tex[0]
    }

    private fun rebuildShadowFbo(newSize: Int) {
        if (shadowFbo != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(shadowFbo), 0)
        if (shadowTex != 0) GLES30.glDeleteTextures(1, intArrayOf(shadowTex), 0)
        initShadowFbo(newSize)
    }

    private fun ensureScaleFbo(w: Int, h: Int) {
        if (scaleFbo != 0 && scaleW == w && scaleH == h) return
        if (scaleFbo != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(scaleFbo), 0)
            GLES30.glDeleteTextures(1, intArrayOf(scaleColorTex), 0)
            GLES30.glDeleteRenderbuffers(1, intArrayOf(scaleDepthRb), 0)
        }
        scaleW = w; scaleH = h
        val fbo = IntArray(1); val tex = IntArray(1); val rb = IntArray(1)
        GLES30.glGenFramebuffers(1, fbo, 0)
        GLES30.glGenTextures(1, tex, 0)
        GLES30.glGenRenderbuffers(1, rb, 0)

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0])
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, w, h, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, rb[0])
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT24, w, h)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo[0])
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, tex[0], 0)
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_RENDERBUFFER, rb[0])
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        scaleFbo = fbo[0]; scaleColorTex = tex[0]; scaleDepthRb = rb[0]
    }

    private fun updateCameraMatrices(snap: WorldRenderSnapshot, sunDir: Vec3) {
        val aspect = viewWidth.toFloat() / viewHeight.toFloat()
        val targetFov = if (!snap.isMenuDemo && snap.adsFovDegrees > 1f && snap.adsBlend > 0.01f) {
            fovDegrees + (snap.adsFovDegrees - fovDegrees) * snap.adsBlend.coerceIn(0f, 1f)
        } else {
            fovDegrees
        }
        currentRenderFovDeg += (targetFov - currentRenderFovDeg) * 0.35f
        Matrix.perspectiveM(projMatrix, 0, currentRenderFovDeg.coerceIn(24f, 95f), aspect, 0.08f, renderDistance.coerceIn(50f, 220f))

        val cp = cos(snap.cameraPitch)
        val dir = Vec3(sin(snap.cameraYaw) * cp, sin(snap.cameraPitch), -cos(snap.cameraYaw) * cp)
        val lookAt = snap.cameraPos + dir
        Matrix.setLookAtM(
            viewMatrix, 0,
            snap.cameraPos.x, snap.cameraPos.y, snap.cameraPos.z,
            lookAt.x, lookAt.y, lookAt.z,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
        frustum.updateFromViewProj(vpMatrix)

        val center = snap.playerPos
        val lightPos = center + sunDir * 36f
        Matrix.setLookAtM(
            lightViewMatrix, 0,
            lightPos.x, lightPos.y, lightPos.z,
            center.x, center.y, center.z,
            0f, 1f, 0f
        )
        Matrix.orthoM(lightProjMatrix, 0, -34f, 34f, -34f, 34f, 2f, 85f)
        Matrix.multiplyMM(lightSpaceMatrix, 0, lightProjMatrix, 0, lightViewMatrix, 0)
    }

    private fun renderShadowPass(snap: WorldRenderSnapshot, alpha: Float) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, shadowFbo)
        GLES30.glViewport(0, 0, activeShadowMapSize, activeShadowMapSize)
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glCullFace(GLES30.GL_FRONT)

        shadowShader.use()
        val uLightMvp = shadowShader.uniformLocation("uLightMVP")

        for (prop in snap.staticProps) {
            if (!prop.isVisible || prop.material == PhysicsMaterial.GLASS) continue
            val glb = gpuMeshGlbs[prop.meshId]
            if (glb != null) {
                val drawHalf = Vec3(
                    prop.halfExtents.x / glb.halfExtents.x,
                    prop.halfExtents.y / glb.halfExtents.y,
                    prop.halfExtents.z / glb.halfExtents.z
                )
                prop.orientation.writeModelMatrix(prop.position, drawHalf, modelMatrix)
                Matrix.multiplyMM(lightMvpMatrix, 0, lightSpaceMatrix, 0, modelMatrix, 0)
                GLES30.glUniformMatrix4fv(uLightMvp, 1, false, lightMvpMatrix, 0)
                for (p in glb.primitives) drawMesh(p.mesh)
            } else {
                val mesh = gpuMeshes[prop.meshId]?.lod1 ?: continue
                prop.orientation.writeModelMatrix(prop.position, prop.halfExtents, modelMatrix)
                Matrix.multiplyMM(lightMvpMatrix, 0, lightSpaceMatrix, 0, modelMatrix, 0)
                GLES30.glUniformMatrix4fv(uLightMvp, 1, false, lightMvpMatrix, 0)
                drawMesh(mesh)
            }
        }

        for (b in snap.dynamicBodies) {
            if (b.materialSpec.alpha < 0.85f) continue
            val pos = b.prevPosition.lerp(b.position, alpha)
            val rot = b.prevOrientation.slerp(b.orientation, alpha)
            rot.writeModelMatrix(pos, b.scale, modelMatrix)
            Matrix.multiplyMM(lightMvpMatrix, 0, lightSpaceMatrix, 0, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(uLightMvp, 1, false, lightMvpMatrix, 0)
            val glb = if (b.toolMeshStyle != null) gpuToolGlbs[b.toolMeshStyle] else gpuMeshGlbs[b.meshId]
            if (glb != null) {
                for (p in glb.primitives) drawMesh(p.mesh)
            } else {
                val asset = if (b.toolMeshStyle != null) gpuToolMeshes[b.toolMeshStyle] else gpuMeshes[b.meshId]
                val mesh = asset?.lod1 ?: continue
                drawMesh(mesh)
            }
        }

        if (!snap.isMenuDemo && snap.cameraMode == CameraMode.THIRD_PERSON && charUploaded) {
            val poseSnap = animationWorker.latestPoseRef.get()
            if (useCompanionRig && gpuCompanionLod != null && cpuCompanionChar != null) {
                val compLod = gpuCompanionLod!!
                val compAsset = cpuCompanionChar!!
                val compSkin = evaluateCompanionSkinMatrices(compAsset, snap, poseSnap)
                uploadBonePalette(compSkin)
                skinnedShadowShader.use()
                val uCharLightMvp = skinnedShadowShader.uniformLocation("uLightMVP")
                Quat.fromAxisAngle(Vec3.UP, -snap.cameraYaw).writeModelMatrix(snap.playerPos, Vec3.ONE, modelMatrix)
                Matrix.multiplyMM(lightMvpMatrix, 0, lightSpaceMatrix, 0, modelMatrix, 0)
                GLES30.glUniformMatrix4fv(uCharLightMvp, 1, false, lightMvpMatrix, 0)
                for (sub in compLod.submeshesByName.values) {
                    GLES30.glBindVertexArray(sub.vao)
                    GLES30.glDrawElements(GLES30.GL_TRIANGLES, sub.indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
                }
                GLES30.glBindVertexArray(0)
            } else {
                val charLod = gpuCharLods.getOrNull(1) ?: gpuCharLods.firstOrNull()
                if (poseSnap != null && charLod != null) {
                    uploadBonePalette(poseSnap.boneSkinMatrices)
                    skinnedShadowShader.use()
                    val uCharLightMvp = skinnedShadowShader.uniformLocation("uLightMVP")
                    Quat.fromAxisAngle(Vec3.UP, -snap.cameraYaw).writeModelMatrix(snap.playerPos, Vec3.ONE, modelMatrix)
                    Matrix.multiplyMM(lightMvpMatrix, 0, lightSpaceMatrix, 0, modelMatrix, 0)
                    GLES30.glUniformMatrix4fv(uCharLightMvp, 1, false, lightMvpMatrix, 0)
                    for (name in visibleThirdPersonMeshNames(characterAppearance)) {
                        val sub = charLod.submeshesByName[name] ?: continue
                        GLES30.glBindVertexArray(sub.vao)
                        GLES30.glDrawElements(GLES30.GL_TRIANGLES, sub.indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
                    }
                    GLES30.glBindVertexArray(0)
                }
            }
        }

        GLES30.glCullFace(GLES30.GL_BACK)
    }

    private fun renderSkyPass(snap: WorldRenderSnapshot, env: EnvLighting) {
        GLES30.glDepthMask(false)
        skyShader.use()
        GLES30.glUniform3f(skyShader.uniformLocation("uSkyTop"), env.skyColor.x * 0.72f, env.skyColor.y * 0.80f, env.skyColor.z * 0.96f)
        GLES30.glUniform3f(skyShader.uniformLocation("uSkyHorizon"), env.fogColor.x, env.fogColor.y, env.fogColor.z)
        GLES30.glUniform1f(skyShader.uniformLocation("uPitch"), snap.cameraPitch)
        GLES30.glBindVertexArray(skyVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
        GLES30.glDepthMask(true)
    }

    private fun renderMainPbrPass(snap: WorldRenderSnapshot, alpha: Float, env: EnvLighting) {
        pbrShader.use()
        bindSceneLightingUniforms(pbrShader, snap, env)
        GLES30.glUniform1f(pbrShader.uniformLocation("uIsSkin"), 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uIsHair"), 0f)
        GLES30.glUniform3f(pbrShader.uniformLocation("uSssColor"), 0f, 0f, 0f)

        val uModel = pbrShader.uniformLocation("uModel")
        val uMvp = pbrShader.uniformLocation("uMVP")
        val uColorTint = pbrShader.uniformLocation("uColorTint")
        val uAlpha = pbrShader.uniformLocation("uAlpha")
        val uUseOverride = pbrShader.uniformLocation("uUseOverrideTint")
        val uMetOver = pbrShader.uniformLocation("uMetallicOverride")
        val uRghOver = pbrShader.uniformLocation("uRoughnessOverride")
        val uUseMatOver = pbrShader.uniformLocation("uUseMaterialOverrides")
        val uCrack = pbrShader.uniformLocation("uCrackLevel")
        val uEmisCol = pbrShader.uniformLocation("uEmissiveColor")
        val uEmisInt = pbrShader.uniformLocation("uEmissiveIntensity")
        val uHighlight = pbrShader.uniformLocation("uHighlight")
        val uFrozen = pbrShader.uniformLocation("uFrozen")

        val maxDistSq = renderDistance * renderDistance
        val transparentQueue = ArrayList<TransparentDrawItem>(24)
        var culled = 0

        // 1. Static props
        for (prop in snap.staticProps) {
            if (!prop.isVisible) continue
            val distSq = (prop.position - snap.cameraPos).lengthSq()
            val radius = prop.halfExtents.length()
            if (distSq > maxDistSq || !frustum.isSphereVisible(prop.position, radius)) {
                culled++
                continue
            }
            val glb = gpuMeshGlbs[prop.meshId]
            val fallbackAsset = gpuMeshes[prop.meshId]
            if (glb == null && fallbackAsset == null) continue
            val spec = MaterialLibrary.fromLegacy(prop.material)
            val alphaVal = prop.alphaOverride ?: spec.alpha
            val tint = prop.colorOverride ?: Vec3(spec.tintR, spec.tintG, spec.tintB)
            val drawScale = if (glb != null) {
                Vec3(
                    prop.halfExtents.x / glb.halfExtents.x,
                    prop.halfExtents.y / glb.halfExtents.y,
                    prop.halfExtents.z / glb.halfExtents.z
                )
            } else {
                prop.halfExtents
            }
            if (alphaVal < 0.99f) {
                val mMat = FloatArray(16)
                prop.orientation.writeModelMatrix(prop.position, drawScale, mMat)
                val fallbackMesh = fallbackAsset?.selectLod(distSq, graphicsQuality) ?: glb!!.primitives.first().mesh
                transparentQueue.add(
                    TransparentDrawItem(
                        distSq, mMat, fallbackMesh, glb, spec, tint, alphaVal, 0f,
                        prop.emissiveColor, prop.emissiveIntensity, false, false
                    )
                )
                continue
            }

            prop.orientation.writeModelMatrix(prop.position, drawScale, modelMatrix)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
            GLES30.glUniform1f(uCrack, 0f)
            GLES30.glUniform1f(uAlpha, 1f)
            GLES30.glUniform1f(uHighlight, 0f)
            GLES30.glUniform1f(uFrozen, 0f)

            if (glb != null) {
                val useArchitecturePattern = prop.meshId == MeshId.FLOOR_SLAB ||
                    prop.meshId == MeshId.WALL_PANEL ||
                    prop.meshId == MeshId.PILLAR_COLUMN ||
                    prop.meshId == MeshId.RAMP_WEDGE ||
                    prop.meshId == MeshId.CONCRETE_MASONRY
                drawStaticGlbAsset(
                    glb = glb,
                    patternSpec = if (useArchitecturePattern) spec else null,
                    primaryTintOverride = if (useArchitecturePattern || prop.colorOverride != null) tint else null,
                    emissiveColor = prop.emissiveColor,
                    emissiveIntensity = prop.emissiveIntensity
                )
            } else {
                val mesh = fallbackAsset!!.selectLod(distSq, graphicsQuality)
                bindPattern(spec.texturePattern)
                GLES30.glUniform1f(uUseOverride, 1f)
                GLES30.glUniform3f(uColorTint, tint.x, tint.y, tint.z)
                GLES30.glUniform1f(uUseMatOver, 1f)
                GLES30.glUniform1f(uMetOver, spec.metallic)
                GLES30.glUniform1f(uRghOver, spec.roughness)
                GLES30.glUniform3f(uEmisCol, prop.emissiveColor.x, prop.emissiveColor.y, prop.emissiveColor.z)
                GLES30.glUniform1f(uEmisInt, prop.emissiveIntensity)
                drawMesh(mesh)
            }
        }

        // 2. Dynamic rigid bodies (105 materials + 99 GLB multi-material props/tools/firearms/ammo)
        for (b in snap.dynamicBodies) {
            val pos = b.prevPosition.lerp(b.position, alpha)
            val distSq = (pos - snap.cameraPos).lengthSq()
            if (distSq > maxDistSq || !frustum.isSphereVisible(pos, b.boundingRadius)) {
                culled++
                continue
            }
            val glb = if (b.toolMeshStyle != null) gpuToolGlbs[b.toolMeshStyle] else gpuMeshGlbs[b.meshId]
            val asset = if (b.toolMeshStyle != null) gpuToolMeshes[b.toolMeshStyle] else gpuMeshes[b.meshId]
            if (glb == null && asset == null) continue
            val rot = b.prevOrientation.slerp(b.orientation, alpha)
            val spec = b.materialSpec
            val visualScale = if (b.squashAmount > 0.005f) {
                val sy = (1f - b.squashAmount).coerceAtLeast(0.45f)
                val sxz = 1f + b.squashAmount * 0.35f
                Vec3(b.scale.x * sxz, b.scale.y * sy, b.scale.z * sxz)
            } else b.scale

            if (spec.alpha < 0.99f) {
                val mMat = FloatArray(16)
                rot.writeModelMatrix(pos, visualScale, mMat)
                val mesh = asset?.selectLod(distSq, graphicsQuality) ?: glb!!.primitives.first().mesh
                transparentQueue.add(
                    TransparentDrawItem(
                        distSq, mMat, mesh, glb, spec,
                        Vec3(spec.tintR, spec.tintG, spec.tintB),
                        spec.alpha, b.crackLevel, b.emissiveColor, b.emissiveIntensity,
                        b.isHighlighted, b.isFrozen
                    )
                )
                continue
            }

            rot.writeModelMatrix(pos, visualScale, modelMatrix)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
            GLES30.glUniform1f(uCrack, b.crackLevel)
            GLES30.glUniform1f(uAlpha, 1f)
            GLES30.glUniform1f(uHighlight, if (b.isHighlighted) 1f else 0f)
            GLES30.glUniform1f(uFrozen, if (b.isFrozen) 1f else 0f)

            if (glb != null) {
                val isPreservedMultiMat = b.toolMeshStyle != null ||
                    b.meshId.name.startsWith("AMMO_BOX_") ||
                    b.meshId == MeshId.ELECTRICAL_PANEL ||
                    b.meshId == MeshId.BATTERY_UNIT ||
                    b.meshId == MeshId.DC_MOTOR ||
                    b.meshId == MeshId.KINETIC_GENERATOR
                drawStaticGlbAsset(
                    glb = glb,
                    patternSpec = if (isPreservedMultiMat) null else spec,
                    primaryTintOverride = if (isPreservedMultiMat) null else Vec3(spec.tintR, spec.tintG, spec.tintB),
                    emissiveColor = b.emissiveColor,
                    emissiveIntensity = b.emissiveIntensity
                )
            } else {
                val mesh = asset!!.selectLod(distSq, graphicsQuality)
                bindPattern(spec.texturePattern)
                GLES30.glUniform1f(uUseOverride, 1f)
                GLES30.glUniform3f(uColorTint, spec.tintR, spec.tintG, spec.tintB)
                GLES30.glUniform1f(uUseMatOver, 1f)
                GLES30.glUniform1f(uMetOver, spec.metallic)
                GLES30.glUniform1f(uRghOver, spec.roughness)
                GLES30.glUniform3f(uEmisCol, b.emissiveColor.x, b.emissiveColor.y, b.emissiveColor.z)
                GLES30.glUniform1f(uEmisInt, b.emissiveIntensity)
                drawMesh(mesh)
            }
        }

        // 3. Third-person rigged 65-bone GLB player character + right-hand tool attachment
        if (!snap.isMenuDemo && snap.cameraMode == CameraMode.THIRD_PERSON && charUploaded) {
            val poseSnap = animationWorker.latestPoseRef.get()
            if (poseSnap != null) {
                val distSq = (snap.playerPos - snap.cameraPos).lengthSq()
                val lodIdx = when {
                    graphicsQuality == 0 -> 2
                    distSq < 9f * 9f -> 0
                    distSq < 22f * 22f -> 1
                    else -> 2
                }
                Quat.fromAxisAngle(Vec3.UP, -snap.cameraYaw).writeModelMatrix(snap.playerPos, Vec3.ONE, modelMatrix)
                val usingCompanion = useCompanionRig && gpuCompanionLod != null && cpuCompanionChar != null
                if (usingCompanion) {
                    renderSkinnedCompanionModel(modelMatrix, snap, poseSnap, env)
                } else {
                    renderSkinnedCharacterModel(
                        lodIdx = lodIdx,
                        poseSnap = poseSnap,
                        charModelMatrix = modelMatrix,
                        submeshNames = visibleThirdPersonMeshNames(characterAppearance),
                        snap = snap,
                        env = env
                    )
                }
                val firearmHolstered = FirearmCatalog.isFirearmTool(snap.activeCatalogToolId) &&
                    (snap.weaponActionState == WeaponActionState.HOLSTERED || snap.weaponActionState == WeaponActionState.HOLSTERING)
                if (snap.selectedTool == ToolType.CATALOG_TOOL && snap.equippedToolStyle != null && !firearmHolstered) {
                    renderAttachedHandTool(snap, poseSnap, modelMatrix, env, useCompanionHand = usingCompanion)
                }
                pbrShader.use()
                bindSceneLightingUniforms(pbrShader, snap, env)
                GLES30.glUniform1f(pbrShader.uniformLocation("uIsSkin"), 0f)
                GLES30.glUniform1f(pbrShader.uniformLocation("uIsHair"), 0f)
            }
        }

        // 4. Optional Fluid Buoyancy Tank Surface Plane
        if (!snap.isMenuDemo && snap.fluidBasinEnabled) {
            val waterMesh = gpuMeshes[MeshId.FLOOR_SLAB]?.lod2
            if (waterMesh != null) {
                val waterMat = MaterialLibrary.get("CLEAR_GLASS")
                val mMat = FloatArray(16)
                Quat.IDENTITY.writeModelMatrix(
                    Vec3(0f, snap.fluidLevelY - 0.06f, 0f),
                    Vec3(18.5f, 0.06f, 18.5f),
                    mMat
                )
                transparentQueue.add(
                    TransparentDrawItem(
                        250f, mMat, waterMesh, null, waterMat,
                        Vec3(0.16f, 0.56f, 0.78f),
                        0.40f, 0f, Vec3(0.06f, 0.25f, 0.42f), 0.18f, false, false
                    )
                )
            }
        }

        // 5. Translucent pass sorted back-to-front
        if (transparentQueue.isNotEmpty()) {
            transparentQueue.sortByDescending { it.distSq }
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            GLES30.glDepthMask(false)

            for (item in transparentQueue) {
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, item.modelMatrix, 0)
                GLES30.glUniformMatrix4fv(uModel, 1, false, item.modelMatrix, 0)
                GLES30.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
                GLES30.glUniform1f(uCrack, item.crackLevel)
                GLES30.glUniform1f(uAlpha, item.alpha)
                GLES30.glUniform1f(uHighlight, if (item.isHighlighted) 1f else 0f)
                GLES30.glUniform1f(uFrozen, if (item.isFrozen) 1f else 0f)
                if (item.glbAsset != null) {
                    drawStaticGlbAsset(
                        glb = item.glbAsset,
                        patternSpec = item.spec,
                        primaryTintOverride = item.colorTint,
                        emissiveColor = item.emissiveColor,
                        emissiveIntensity = item.emissiveIntensity
                    )
                } else {
                    bindPattern(item.spec.texturePattern)
                    GLES30.glUniform1f(uUseOverride, 1f)
                    GLES30.glUniform3f(uColorTint, item.colorTint.x, item.colorTint.y, item.colorTint.z)
                    GLES30.glUniform1f(uUseMatOver, 1f)
                    GLES30.glUniform1f(uMetOver, item.spec.metallic)
                    GLES30.glUniform1f(uRghOver, item.spec.roughness)
                    GLES30.glUniform3f(uEmisCol, item.emissiveColor.x, item.emissiveColor.y, item.emissiveColor.z)
                    GLES30.glUniform1f(uEmisInt, item.emissiveIntensity)
                    drawMesh(item.mesh)
                }
            }

            GLES30.glDepthMask(true)
            GLES30.glDisable(GLES30.GL_BLEND)
        }

        culledCountRef.set(culled)
    }

    private fun drawStaticGlbAsset(
        glb: GpuStaticGlbAsset,
        patternSpec: MaterialSpec?,
        primaryTintOverride: Vec3?,
        emissiveColor: Vec3,
        emissiveIntensity: Float
    ) {
        val neutralTex = neutralGlbTextures
        val uColorTint = pbrShader.uniformLocation("uColorTint")
        val uUseOverride = pbrShader.uniformLocation("uUseOverrideTint")
        val uMetOver = pbrShader.uniformLocation("uMetallicOverride")
        val uRghOver = pbrShader.uniformLocation("uRoughnessOverride")
        val uUseMatOver = pbrShader.uniformLocation("uUseMaterialOverrides")
        val uEmisCol = pbrShader.uniformLocation("uEmissiveColor")
        val uEmisInt = pbrShader.uniformLocation("uEmissiveIntensity")

        for (idx in glb.primitives.indices) {
            val prim = glb.primitives[idx]
            val mat = prim.material
            val isPrimary = idx == 0
            val useProcPattern = isPrimary && patternSpec != null
            if (useProcPattern) {
                bindPattern(patternSpec!!.texturePattern)
            } else {
                val tex = prim.textures ?: neutralTex
                if (tex != null) {
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.albedoId)
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.normalId)
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.mraId)
                }
            }

            val hasNonWhiteBaseFactor = (mat.baseColorFactor - Vec3(1f, 1f, 1f)).lengthSq() > 0.005f
            val useOverrideTint = when {
                useProcPattern -> 1f
                isPrimary && primaryTintOverride != null -> 1f
                !prim.hasNativeAlbedo -> 1f
                hasNonWhiteBaseFactor -> 1f
                else -> 0f
            }
            val tint = if (isPrimary && primaryTintOverride != null) primaryTintOverride else mat.baseColorFactor

            val useMatOverrides = when {
                useProcPattern -> 1f
                prim.hasNativeMra -> 0f
                else -> 1f
            }
            val met = if (useProcPattern) patternSpec!!.metallic else mat.metallicFactor
            val rgh = if (useProcPattern) patternSpec!!.roughness else mat.roughnessFactor
            val emisInt = max(emissiveIntensity, if (mat.emissiveFactor.lengthSq() > 0.01f) 1.0f else 0f)
            val emisCol = if (emissiveIntensity > 0.01f) emissiveColor else mat.emissiveFactor

            GLES30.glUniform1f(uUseOverride, useOverrideTint)
            GLES30.glUniform3f(uColorTint, tint.x, tint.y, tint.z)
            GLES30.glUniform1f(uUseMatOver, useMatOverrides)
            GLES30.glUniform1f(uMetOver, met)
            GLES30.glUniform1f(uRghOver, rgh)
            GLES30.glUniform3f(uEmisCol, emisCol.x, emisCol.y, emisCol.z)
            GLES30.glUniform1f(uEmisInt, emisInt)
            drawMesh(prim.mesh)
        }
    }

    private fun bindSceneLightingUniforms(
        shader: ShaderProgram,
        snap: WorldRenderSnapshot,
        env: EnvLighting,
        fogOverride: Float? = null
    ) {
        val aoStrength = when (aoQuality) {
            0 -> 0.35f
            1 -> 0.72f
            else -> 1.05f
        }
        val reflStrength = when (reflectionQuality) {
            0 -> 0.22f
            1 -> 0.65f
            else -> 1.05f
        }
        GLES30.glUniformMatrix4fv(shader.uniformLocation("uLightSpaceMatrix"), 1, false, lightSpaceMatrix, 0)
        GLES30.glUniform3f(shader.uniformLocation("uCameraPos"), snap.cameraPos.x, snap.cameraPos.y, snap.cameraPos.z)
        GLES30.glUniform3f(shader.uniformLocation("uSunDir"), env.sunDir.x, env.sunDir.y, env.sunDir.z)
        GLES30.glUniform3f(shader.uniformLocation("uSunColor"), env.sunColor.x, env.sunColor.y, env.sunColor.z)
        GLES30.glUniform3f(shader.uniformLocation("uSkyColor"), env.skyColor.x, env.skyColor.y, env.skyColor.z)
        GLES30.glUniform3f(shader.uniformLocation("uGroundAmbient"), env.groundAmbient.x, env.groundAmbient.y, env.groundAmbient.z)
        GLES30.glUniform3f(shader.uniformLocation("uFogColor"), env.fogColor.x, env.fogColor.y, env.fogColor.z)
        GLES30.glUniform1f(shader.uniformLocation("uFogDensity"), fogOverride ?: snap.mapId.fogDensity)
        GLES30.glUniform1f(shader.uniformLocation("uShadowsEnabled"), if (shadowsEnabled && graphicsQuality > 0 && shadowQuality > 0) 1f else 0f)
        GLES30.glUniform1f(shader.uniformLocation("uAoStrength"), aoStrength)
        GLES30.glUniform1f(shader.uniformLocation("uReflectionStrength"), reflStrength)

        GLES30.glUniform3f(shader.uniformLocation("uPointLightPos"), 0f, 6.8f, 0f)
        GLES30.glUniform3f(shader.uniformLocation("uPointLightColor"), env.sunColor.x, env.sunColor.y, env.sunColor.z)
        GLES30.glUniform3f(shader.uniformLocation("uFillLightPos"), -9f, 5.5f, 9f)
        GLES30.glUniform3f(shader.uniformLocation("uFillLightColor"), env.skyColor.x * 0.65f, env.skyColor.y * 0.70f, env.skyColor.z * 0.78f)

        val cp = cos(snap.cameraPitch)
        val camDir = Vec3(sin(snap.cameraYaw) * cp, sin(snap.cameraPitch), -cos(snap.cameraYaw) * cp)
        GLES30.glUniform3f(shader.uniformLocation("uFlashlightPos"), snap.cameraPos.x, snap.cameraPos.y, snap.cameraPos.z)
        GLES30.glUniform3f(shader.uniformLocation("uFlashlightDir"), camDir.x, camDir.y, camDir.z)
        GLES30.glUniform1f(shader.uniformLocation("uFlashlightActive"), if (snap.flashlightActive) 1f else 0f)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE3)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowTex)
        GLES30.glUniform1i(shader.uniformLocation("uShadowMap"), 3)
        GLES30.glUniform1i(shader.uniformLocation("uAlbedoMap"), 0)
        GLES30.glUniform1i(shader.uniformLocation("uNormalMap"), 1)
        GLES30.glUniform1i(shader.uniformLocation("uMraMap"), 2)
    }

    private fun visibleThirdPersonMeshNames(app: CharacterAppearance): List<String> = listOf(
        "Body_HeadFaceSkin",
        "Face_Eyes",
        "Face_Eyebrows",
        app.hairStyle.meshNodeName,
        app.jacketStyle.meshNodeName,
        app.trouserStyle.meshNodeName,
        "Outfit_Belt",
        app.gloveStyle.meshNodeName,
        app.bootStyle.meshNodeName
    )

    private fun renderSkinnedCharacterModel(
        lodIdx: Int,
        poseSnap: CharacterPoseSnapshot,
        charModelMatrix: FloatArray,
        submeshNames: List<String>,
        snap: WorldRenderSnapshot,
        env: EnvLighting,
        fogOverride: Float? = null
    ) {
        val lod = gpuCharLods.getOrNull(lodIdx) ?: gpuCharLods.firstOrNull() ?: return
        uploadBonePalette(poseSnap.boneSkinMatrices)

        skinnedPbrShader.use()
        bindSceneLightingUniforms(skinnedPbrShader, snap, env, fogOverride)

        val app = characterAppearance
        val uModel = skinnedPbrShader.uniformLocation("uModel")
        val uMvp = skinnedPbrShader.uniformLocation("uMVP")
        val uColorTint = skinnedPbrShader.uniformLocation("uColorTint")
        val uAlpha = skinnedPbrShader.uniformLocation("uAlpha")
        val uUseOverride = skinnedPbrShader.uniformLocation("uUseOverrideTint")
        val uMetOver = skinnedPbrShader.uniformLocation("uMetallicOverride")
        val uRghOver = skinnedPbrShader.uniformLocation("uRoughnessOverride")
        val uUseMatOver = skinnedPbrShader.uniformLocation("uUseMaterialOverrides")
        val uCrack = skinnedPbrShader.uniformLocation("uCrackLevel")
        val uEmisInt = skinnedPbrShader.uniformLocation("uEmissiveIntensity")
        val uHighlight = skinnedPbrShader.uniformLocation("uHighlight")
        val uFrozen = skinnedPbrShader.uniformLocation("uFrozen")
        val uIsSkin = skinnedPbrShader.uniformLocation("uIsSkin")
        val uIsHair = skinnedPbrShader.uniformLocation("uIsHair")
        val uSssColor = skinnedPbrShader.uniformLocation("uSssColor")

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, charModelMatrix, 0)
        GLES30.glUniformMatrix4fv(uModel, 1, false, charModelMatrix, 0)
        GLES30.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(uCrack, 0f)
        GLES30.glUniform1f(uEmisInt, 0f)
        GLES30.glUniform1f(uAlpha, 1f)
        GLES30.glUniform1f(uHighlight, 0f)
        GLES30.glUniform1f(uFrozen, 0f)
        GLES30.glUniform1f(uUseOverride, 1f)
        GLES30.glUniform1f(uUseMatOver, 1f)

        for (name in submeshNames) {
            val sub = lod.submeshesByName[name] ?: continue
            if (sub.morphTargets.isNotEmpty()) {
                applyMorphTargetsToGpuSubmesh(sub, poseSnap)
            }
            val tex = lod.materials.getOrNull(sub.materialIndex)
            if (tex != null) {
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.albedoId)
                GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.normalId)
                GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.mraId)
            }

            var isSkin = 0f
            var isHair = 0f
            var tint = Vec3(0.8f, 0.8f, 0.8f)
            var met = lod.metallicFactors.getOrElse(sub.materialIndex) { 0f }
            var rgh = lod.roughnessFactors.getOrElse(sub.materialIndex) { 0.65f }

            when {
                name == "Body_HeadFaceSkin" || name == "Body_HandsSkin" -> {
                    isSkin = 1f
                    tint = app.skinTone.rgb
                    rgh = 0.54f
                }
                name == "Face_Eyes" -> {
                    tint = app.eyeColor.rgb
                    met = 0.05f
                    rgh = 0.12f
                }
                name == "Face_Eyebrows" || name.startsWith("Hair_") -> {
                    isHair = 1f
                    tint = app.hairColor.rgb
                    rgh = 0.42f
                }
                name.startsWith("Outfit_Jacket") -> {
                    tint = app.jacketColor.rgb
                    met = (met + app.jacketColor.metallicBias).coerceIn(0f, 1f)
                    rgh = (0.72f + app.jacketColor.roughnessBias).coerceIn(0.2f, 0.95f)
                }
                name.startsWith("Outfit_Trousers") -> {
                    tint = app.trouserColor.rgb
                    met = (met + app.trouserColor.metallicBias).coerceIn(0f, 1f)
                    rgh = (0.80f + app.trouserColor.roughnessBias).coerceIn(0.25f, 0.96f)
                }
                name == "Outfit_Gloves_Tactical" -> {
                    tint = app.gloveColor.rgb
                    met = (met + app.gloveColor.metallicBias).coerceIn(0f, 1f)
                    rgh = (0.56f + app.gloveColor.roughnessBias).coerceIn(0.2f, 0.95f)
                }
                name.startsWith("Outfit_Boots") -> {
                    tint = app.bootColor.rgb
                    met = (met + app.bootColor.metallicBias).coerceIn(0f, 1f)
                    rgh = (0.52f + app.bootColor.roughnessBias).coerceIn(0.18f, 0.95f)
                }
                name == "Outfit_Belt" -> {
                    tint = ClothingColorPreset.CHARCOAL.rgb
                    met = 0.16f
                    rgh = 0.46f
                }
            }

            GLES30.glUniform1f(uIsSkin, isSkin)
            GLES30.glUniform1f(uIsHair, isHair)
            GLES30.glUniform3f(uSssColor, app.skinTone.sssColor.x, app.skinTone.sssColor.y, app.skinTone.sssColor.z)
            GLES30.glUniform3f(uColorTint, tint.x, tint.y, tint.z)
            GLES30.glUniform1f(uMetOver, met)
            GLES30.glUniform1f(uRghOver, rgh)

            GLES30.glBindVertexArray(sub.vao)
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, sub.indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        }
        GLES30.glBindVertexArray(0)
    }

    private fun applyMorphTargetsToGpuSubmesh(sub: GpuSkinnedSubmesh, poseSnap: CharacterPoseSnapshot) {
        val uploadBuf = sub.uploadBuffer ?: return
        if (abs(poseSnap.blinkLeft - sub.lastBlinkL) < 0.01f &&
            abs(poseSnap.blinkRight - sub.lastBlinkR) < 0.01f &&
            abs(poseSnap.browFocus - sub.lastBrow) < 0.01f &&
            abs(poseSnap.mouthOpen - sub.lastMouth) < 0.01f
        ) {
            return
        }
        sub.lastBlinkL = poseSnap.blinkLeft
        sub.lastBlinkR = poseSnap.blinkRight
        sub.lastBrow = poseSnap.browFocus
        sub.lastMouth = poseSnap.mouthOpen

        val vCount = sub.basePositions.size / 3
        for (i in 0 until vCount) {
            val o = i * 19
            val p3 = i * 3
            sub.packedScratch[o] = sub.basePositions[p3]
            sub.packedScratch[o + 1] = sub.basePositions[p3 + 1]
            sub.packedScratch[o + 2] = sub.basePositions[p3 + 2]
        }
        for (mt in sub.morphTargets) {
            val w = when {
                mt.name.contains("blink_l", ignoreCase = true) -> poseSnap.blinkLeft
                mt.name.contains("blink_r", ignoreCase = true) -> poseSnap.blinkRight
                mt.name.contains("brow", ignoreCase = true) -> poseSnap.browFocus
                mt.name.contains("mouth", ignoreCase = true) -> poseSnap.mouthOpen
                else -> 0f
            }
            if (w <= 0.002f) continue
            val deltas = mt.positionDeltas
            val limit = minOf(vCount, deltas.size / 3)
            for (i in 0 until limit) {
                val o = i * 19
                val p3 = i * 3
                sub.packedScratch[o] += deltas[p3] * w
                sub.packedScratch[o + 1] += deltas[p3 + 1] * w
                sub.packedScratch[o + 2] += deltas[p3 + 2] * w
            }
        }
        uploadBuf.clear()
        uploadBuf.put(sub.packedScratch)
        uploadBuf.flip()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, sub.vbo)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, sub.packedScratch.size * 4, uploadBuf)
    }

    private fun evaluateCompanionSkinMatrices(
        compAsset: GlbCharacterAsset,
        snap: WorldRenderSnapshot,
        poseSnap: CharacterPoseSnapshot?
    ): FloatArray {
        val cPose = companionPose ?: SkeletonPose(compAsset.bones.size).also { companionPose = it }
        val bones = compAsset.bones
        val speed = kotlin.math.hypot(snap.playerVel.x, snap.playerVel.z)
        val previewing = characterPreviewState.enabled
        val gaitScale = if (previewing) 0f else (speed / 3.8f).coerceIn(0f, 1.35f)
        companionMotionPhase = (companionMotionPhase + (0.016f * (1.2f + gaitScale * 6.5f))) % (2f * PI.toFloat())
        val stride = sin(companionMotionPhase) * 0.42f * gaitScale
        val armSwing = sin(companionMotionPhase) * 0.34f * gaitScale
        val breathe = sin(companionMotionPhase * 0.8f) * 0.018f

        val localMat = FloatArray(16)
        val skinTmp = FloatArray(16)
        for (i in bones.indices) {
            val bone = bones[i]
            val deltaRot = when (bone.name) {
                "mixamorig_Spine" -> Quat.fromAxisAngle(Vec3.RIGHT, breathe + 0.04f * gaitScale)
                "mixamorig_Head" -> {
                    val lookPitch = if (previewing) 0f else (snap.cameraPitch * 0.35f).coerceIn(-0.35f, 0.35f)
                    Quat.fromAxisAngle(Vec3.RIGHT, -lookPitch)
                }
                "mixamorig_LeftArm" -> (Quat.fromAxisAngle(Vec3.RIGHT, 1.18f) *
                    Quat.fromAxisAngle(Vec3.UP, -armSwing)).normalized()
                "mixamorig_RightArm" -> (Quat.fromAxisAngle(Vec3.RIGHT, 1.18f) *
                    Quat.fromAxisAngle(Vec3.UP, armSwing + if (poseSnap != null && !previewing) poseSnap.toolActionPulse * 0.35f else 0f)).normalized()
                "mixamorig_LeftForeArm" -> Quat.fromAxisAngle(Vec3(0f, 0f, 1f), -0.22f - max(0f, -armSwing) * 0.25f)
                "mixamorig_RightForeArm" -> Quat.fromAxisAngle(Vec3(0f, 0f, 1f), 0.22f + max(0f, armSwing) * 0.25f)
                "mixamorig_LeftUpLeg" -> Quat.fromAxisAngle(Vec3.RIGHT, stride)
                "mixamorig_RightUpLeg" -> Quat.fromAxisAngle(Vec3.RIGHT, -stride)
                "mixamorig_LeftLeg" -> Quat.fromAxisAngle(Vec3.RIGHT, max(0f, -stride) * 0.85f)
                "mixamorig_RightLeg" -> Quat.fromAxisAngle(Vec3.RIGHT, max(0f, stride) * 0.85f)
                else -> Quat.IDENTITY
            }
            val combinedRot = (bone.bindLocalRotation * deltaRot).normalized()
            combinedRot.writeModelMatrix(bone.bindLocalTranslation, Vec3.ONE, localMat)
            if (bone.parentBoneIndex >= 0 && bone.parentBoneIndex < i) {
                SkeletonPose.mulMat4(cPose.worldMatrices[bone.parentBoneIndex], localMat, cPose.worldMatrices[i])
            } else {
                System.arraycopy(localMat, 0, cPose.worldMatrices[i], 0, 16)
            }
            SkeletonPose.mulMat4(cPose.worldMatrices[i], bone.inverseBindMatrix, skinTmp)
            System.arraycopy(skinTmp, 0, cPose.skinMatrices, i * 16, 16)
        }
        return cPose.skinMatrices
    }

    private fun renderSkinnedCompanionModel(
        charModelMatrix: FloatArray,
        snap: WorldRenderSnapshot,
        poseSnap: CharacterPoseSnapshot?,
        env: EnvLighting,
        fogOverride: Float? = null
    ) {
        val compLod = gpuCompanionLod ?: return
        val compAsset = cpuCompanionChar ?: return
        val skinMatrices = evaluateCompanionSkinMatrices(compAsset, snap, poseSnap)
        uploadBonePalette(skinMatrices)

        skinnedPbrShader.use()
        bindSceneLightingUniforms(skinnedPbrShader, snap, env, fogOverride)

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, charModelMatrix, 0)
        GLES30.glUniformMatrix4fv(skinnedPbrShader.uniformLocation("uModel"), 1, false, charModelMatrix, 0)
        GLES30.glUniformMatrix4fv(skinnedPbrShader.uniformLocation("uMVP"), 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(skinnedPbrShader.uniformLocation("uCrackLevel"), 0f)
        GLES30.glUniform1f(skinnedPbrShader.uniformLocation("uEmisInt"), 0f)
        GLES30.glUniform1f(skinnedPbrShader.uniformLocation("uAlpha"), 1f)
        GLES30.glUniform1f(skinnedPbrShader.uniformLocation("uHighlight"), 0f)
        GLES30.glUniform1f(skinnedPbrShader.uniformLocation("uFrozen"), 0f)
        GLES30.glUniform1f(skinnedPbrShader.uniformLocation("uIsSkin"), 0f)
        GLES30.glUniform1f(skinnedPbrShader.uniformLocation("uIsHair"), 0f)
        GLES30.glUniform1f(skinnedPbrShader.uniformLocation("uUseOverrideTint"), 0f)
        GLES30.glUniform3f(skinnedPbrShader.uniformLocation("uColorTint"), 1f, 1f, 1f)
        GLES30.glUniform1f(skinnedPbrShader.uniformLocation("uUseMaterialOverrides"), 1f)

        for (sub in compLod.submeshesByName.values) {
            val tex = compLod.materials.getOrNull(sub.materialIndex)
            if (tex != null) {
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.albedoId)
                GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.normalId)
                GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.mraId)
            }
            val met = compLod.metallicFactors.getOrElse(sub.materialIndex) { 0f }
            val rgh = compLod.roughnessFactors.getOrElse(sub.materialIndex) { 0.85f }
            GLES30.glUniform1f(skinnedPbrShader.uniformLocation("uMetallicOverride"), met)
            GLES30.glUniform1f(skinnedPbrShader.uniformLocation("uRoughnessOverride"), rgh)
            GLES30.glBindVertexArray(sub.vao)
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, sub.indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        }
        GLES30.glBindVertexArray(0)
    }

    private fun renderAttachedHandTool(
        snap: WorldRenderSnapshot,
        poseSnap: CharacterPoseSnapshot,
        charModelMatrix: FloatArray,
        env: EnvLighting,
        fogOverride: Float? = null,
        useCompanionHand: Boolean = false
    ) {
        val style = snap.equippedToolStyle ?: return
        val glbTool = gpuToolGlbs[style]
        val mesh = gpuToolMeshes[style]?.lod0
        if (glbTool == null && mesh == null) return
        val spec = snap.equippedToolMaterialSpec ?: MaterialLibrary.get("STEEL")
        val grip = ToolAttachmentRegistry.getSpec(snap.activeCatalogToolId)

        val handMat = if (useCompanionHand && companionPose != null && companionRightHandBoneIdx >= 0) {
            companionPose!!.worldMatrices[companionRightHandBoneIdx]
        } else {
            poseSnap.rightHandWorldMatrix
        }
        Matrix.multiplyMM(handWorldMatrix, 0, charModelMatrix, 0, handMat, 0)
        val spinQ = Quat.fromAxisAngle(Vec3.UP, snap.equippedToolSpinAngle * 0.25f)
        val localQ = (ToolAttachmentRegistry.gripLocalQuat(grip) * spinQ).normalized()
        localQ.writeModelMatrix(
            grip.primaryOffset,
            Vec3(grip.equippedScale, grip.equippedScale, grip.equippedScale),
            toolLocalMatrix
        )
        Matrix.multiplyMM(modelMatrix, 0, handWorldMatrix, 0, toolLocalMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        pbrShader.use()
        bindSceneLightingUniforms(pbrShader, snap, env, fogOverride)
        GLES30.glUniform1f(pbrShader.uniformLocation("uIsSkin"), 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uIsHair"), 0f)
        GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uModel"), 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uMVP"), 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(pbrShader.uniformLocation("uCrackLevel"), 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uAlpha"), 1f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uHighlight"), 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uFrozen"), 0f)

        if (glbTool != null) {
            drawStaticGlbAsset(
                glb = glbTool,
                patternSpec = null,
                primaryTintOverride = null,
                emissiveColor = Vec3(0.25f, 0.78f, 1.0f),
                emissiveIntensity = snap.equippedToolActionPulse * 0.45f
            )
            if (snap.muzzleFlashIntensity > 0.05f && glbTool.muzzleSocket != null) {
                renderMuzzleFlashAtSocket(glbTool.muzzleSocket, modelMatrix, snap.muzzleFlashIntensity)
            }
        } else {
            bindPattern(spec.texturePattern)
            GLES30.glUniform1f(pbrShader.uniformLocation("uUseOverrideTint"), 1f)
            GLES30.glUniform3f(pbrShader.uniformLocation("uColorTint"), spec.tintR, spec.tintG, spec.tintB)
            GLES30.glUniform1f(pbrShader.uniformLocation("uUseMaterialOverrides"), 1f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uMetallicOverride"), spec.metallic)
            GLES30.glUniform1f(pbrShader.uniformLocation("uRoughnessOverride"), spec.roughness)
            GLES30.glUniform3f(pbrShader.uniformLocation("uEmissiveColor"), 0.25f, 0.78f, 1.0f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uEmissiveIntensity"), snap.equippedToolActionPulse * 0.65f)
            drawMesh(mesh!!)
        }
    }

    private fun renderMuzzleFlashAtSocket(localSocket: Vec3, weaponWorldMat: FloatArray, intensity: Float) {
        val flashMesh = gpuMeshes[MeshId.GEODESIC_SPHERE]?.lod2 ?: return
        val neutralTex = neutralGlbTextures
        if (neutralTex != null) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, neutralTex.albedoId)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, neutralTex.normalId)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, neutralTex.mraId)
        }
        val flashRadius = (0.045f + intensity * 0.055f)
        Quat.IDENTITY.writeModelMatrix(localSocket + Vec3(0f, 0f, -0.04f), Vec3(flashRadius, flashRadius, flashRadius * 2.2f), toolLocalMatrix)
        Matrix.multiplyMM(handWorldMatrix, 0, weaponWorldMat, 0, toolLocalMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, handWorldMatrix, 0)
        GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uModel"), 1, false, handWorldMatrix, 0)
        GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uMVP"), 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(pbrShader.uniformLocation("uUseOverrideTint"), 1f)
        GLES30.glUniform3f(pbrShader.uniformLocation("uColorTint"), 1.0f, 0.86f, 0.42f)
        GLES30.glUniform3f(pbrShader.uniformLocation("uEmissiveColor"), 1.0f, 0.72f, 0.24f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uEmissiveIntensity"), 3.6f * intensity)
        drawMesh(flashMesh)
    }

    private fun renderFirstPersonArmsAndTool(snap: WorldRenderSnapshot, env: EnvLighting) {
        val isFirearm = FirearmCatalog.isFirearmTool(snap.activeCatalogToolId)
        if (isFirearm && (snap.weaponActionState == WeaponActionState.HOLSTERED || snap.weaponActionState == WeaponActionState.HOLSTERING)) {
            return
        }
        // In full sniper optic zoom, hide 3D gun/arms behind scope bezel so reticle stays unobstructed
        if (snap.equippedToolStyle == ToolMeshStyle.FIREARM_SNIPER && snap.isAiming && snap.adsBlend > 0.72f) {
            return
        }

        val poseSnap = animationWorker.latestPoseRef.get()
        val wantsHands = snap.selectedTool == ToolType.CATALOG_TOOL || snap.isGrabbing || snap.interactPulse > 0.12f
        if ((!charUploaded || poseSnap == null || !wantsHands) && snap.equippedToolStyle == null) return

        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)

        if (isFirearm && snap.selectedTool == ToolType.CATALOG_TOOL && snap.equippedToolStyle != null) {
            renderFirstPersonFirearmViewmodel(snap, env)
            return
        }

        if (charUploaded && poseSnap != null && wantsHands) {
            Quat.fromAxisAngle(Vec3.UP, -snap.cameraYaw).writeModelMatrix(snap.playerPos, Vec3.ONE, modelMatrix)
            renderSkinnedCharacterModel(
                lodIdx = 0,
                poseSnap = poseSnap,
                charModelMatrix = modelMatrix,
                submeshNames = listOf(characterAppearance.gloveStyle.meshNodeName),
                snap = snap,
                env = env,
                fogOverride = 0f
            )
            if (snap.selectedTool == ToolType.CATALOG_TOOL && snap.equippedToolStyle != null) {
                renderAttachedHandTool(snap, poseSnap, modelMatrix, env, fogOverride = 0f)
            }
            return
        }

        // Fallback tool viewmodel during initial ~100ms GLB asset decode
        val style = snap.equippedToolStyle ?: return
        val glbTool = gpuToolGlbs[style]
        val mesh = gpuToolMeshes[style]?.lod0
        if (glbTool == null && mesh == null) return
        val spec = snap.equippedToolMaterialSpec ?: MaterialLibrary.get("STEEL")

        pbrShader.use()
        bindSceneLightingUniforms(pbrShader, snap, env, fogOverride = 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uIsSkin"), 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uIsHair"), 0f)

        val cp = cos(snap.cameraPitch)
        val forward = Vec3(sin(snap.cameraYaw) * cp, sin(snap.cameraPitch), -cos(snap.cameraYaw) * cp).normalized()
        val right = forward.cross(Vec3.UP).normalized()
        val up = right.cross(forward).normalized()

        val swingOffset = snap.equippedToolActionPulse * 0.14f
        val toolPos = snap.cameraPos + forward * (0.62f + swingOffset) + right * 0.32f - up * (0.24f - swingOffset * 0.4f)
        val baseRot = Quat.fromAxisAngle(Vec3.UP, snap.cameraYaw + 0.35f) *
            Quat.fromAxisAngle(Vec3.RIGHT, snap.cameraPitch - snap.equippedToolActionPulse * 0.45f) *
            Quat.fromAxisAngle(Vec3(0f, 0f, 1f), snap.equippedToolSpinAngle)
        baseRot.writeModelMatrix(toolPos, Vec3(0.65f, 0.65f, 0.65f), modelMatrix)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uModel"), 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uMVP"), 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(pbrShader.uniformLocation("uCrackLevel"), 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uAlpha"), 1f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uHighlight"), 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uFrozen"), 0f)

        if (glbTool != null) {
            drawStaticGlbAsset(
                glb = glbTool,
                patternSpec = null,
                primaryTintOverride = null,
                emissiveColor = Vec3(0.25f, 0.78f, 1.0f),
                emissiveIntensity = snap.equippedToolActionPulse * 0.4f
            )
        } else {
            bindPattern(spec.texturePattern)
            GLES30.glUniform1f(pbrShader.uniformLocation("uUseOverrideTint"), 1f)
            GLES30.glUniform3f(pbrShader.uniformLocation("uColorTint"), spec.tintR, spec.tintG, spec.tintB)
            GLES30.glUniform1f(pbrShader.uniformLocation("uUseMaterialOverrides"), 1f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uMetallicOverride"), spec.metallic)
            GLES30.glUniform1f(pbrShader.uniformLocation("uRoughnessOverride"), spec.roughness)
            GLES30.glUniform3f(pbrShader.uniformLocation("uEmissiveColor"), 0.25f, 0.78f, 1.0f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uEmissiveIntensity"), snap.equippedToolActionPulse * 0.65f)
            drawMesh(mesh!!)
        }
    }

    private fun renderFirstPersonFirearmViewmodel(snap: WorldRenderSnapshot, env: EnvLighting) {
        val style = snap.equippedToolStyle ?: return
        val glbTool = gpuToolGlbs[style]
        val mesh = gpuToolMeshes[style]?.lod0
        if (glbTool == null && mesh == null) return

        pbrShader.use()
        bindSceneLightingUniforms(pbrShader, snap, env, fogOverride = 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uIsSkin"), 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uIsHair"), 0f)

        val cp = cos(snap.cameraPitch)
        val forward = Vec3(sin(snap.cameraYaw) * cp, sin(snap.cameraPitch), -cos(snap.cameraYaw) * cp).normalized()
        val right = forward.cross(Vec3.UP).normalized()
        val up = right.cross(forward).normalized()

        val ads = snap.adsBlend.coerceIn(0f, 1f)
        val recoil = snap.recoilVisualOffset.coerceIn(0f, 2f)
        val reloadDip = if (snap.weaponActionState == WeaponActionState.RELOADING) 0.14f else 0f
        val inspectRot = if (snap.weaponActionState == WeaponActionState.INSPECTING) 0.42f else 0f

        val rightOff = 0.19f * (1f - ads)
        val upOff = -0.155f + 0.068f * ads - reloadDip + recoil * 0.015f
        val fwdOff = 0.38f - 0.06f * ads - recoil * 0.055f

        val weaponPos = snap.cameraPos + forward * fwdOff + right * rightOff + up * upOff
        val yawRad = -snap.cameraYaw + (0.04f * (1f - ads)) + inspectRot
        val pitchRad = snap.cameraPitch + recoil * 0.12f - reloadDip * 2.2f
        val rollRad = if (snap.weaponActionState == WeaponActionState.RELOADING) -0.35f else 0.25f * inspectRot

        val weaponRot = (Quat.fromAxisAngle(Vec3.UP, yawRad) *
            Quat.fromAxisAngle(Vec3.RIGHT, pitchRad) *
            Quat.fromAxisAngle(Vec3(0f, 0f, 1f), rollRad)).normalized()
        weaponRot.writeModelMatrix(weaponPos, Vec3(0.92f, 0.92f, 0.92f), modelMatrix)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uModel"), 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uMVP"), 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(pbrShader.uniformLocation("uCrackLevel"), 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uAlpha"), 1f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uHighlight"), 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uFrozen"), 0f)

        if (glbTool != null) {
            drawStaticGlbAsset(
                glb = glbTool,
                patternSpec = null,
                primaryTintOverride = null,
                emissiveColor = Vec3.ZERO,
                emissiveIntensity = 0f
            )
            if (snap.muzzleFlashIntensity > 0.05f && glbTool.muzzleSocket != null) {
                renderMuzzleFlashAtSocket(glbTool.muzzleSocket, modelMatrix, snap.muzzleFlashIntensity)
            }
        } else {
            val spec = snap.equippedToolMaterialSpec ?: MaterialLibrary.get("STEEL")
            bindPattern(spec.texturePattern)
            GLES30.glUniform1f(pbrShader.uniformLocation("uUseOverrideTint"), 1f)
            GLES30.glUniform3f(pbrShader.uniformLocation("uColorTint"), spec.tintR, spec.tintG, spec.tintB)
            GLES30.glUniform1f(pbrShader.uniformLocation("uUseMaterialOverrides"), 1f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uMetallicOverride"), spec.metallic)
            GLES30.glUniform1f(pbrShader.uniformLocation("uRoughnessOverride"), spec.roughness)
            GLES30.glUniform1f(pbrShader.uniformLocation("uEmissiveIntensity"), 0f)
            drawMesh(mesh!!)
        }
    }

    private fun renderCharacterStudioPreview(baseSnap: WorldRenderSnapshot) {
        val ps = characterPreviewState
        val (target, baseDist) = when (ps.focusPreset) {
            CharacterCameraFocus.FULL -> Vec3(0f, 0.92f, 0f) to 2.75f
            CharacterCameraFocus.HEAD -> Vec3(0f, 1.62f, 0f) to 0.85f
            CharacterCameraFocus.HANDS -> Vec3(0.30f, 1.12f, -0.05f) to 1.05f
            CharacterCameraFocus.BOOTS -> Vec3(0f, 0.24f, 0f) to 1.15f
        }
        val dist = (baseDist / ps.zoom.coerceIn(0.6f, 2.4f))
        val yawRad = ps.orbitYawDeg * (PI.toFloat() / 180f)
        val pitchRad = ps.orbitPitchDeg.coerceIn(-45f, 55f) * (PI.toFloat() / 180f)
        val cp = cos(pitchRad)
        val camPos = target + Vec3(sin(yawRad) * cp * dist, -sin(pitchRad) * dist, cos(yawRad) * cp * dist)

        val aspect = viewWidth.toFloat() / viewHeight.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 42f, aspect, 0.08f, 40f)
        Matrix.setLookAtM(
            viewMatrix, 0,
            camPos.x, camPos.y, camPos.z,
            target.x, target.y, target.z,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        val studioEnv = EnvLighting(
            sunDir = Vec3(0.42f, 0.72f, -0.55f).normalized(),
            sunColor = Vec3(1.0f, 0.96f, 0.90f),
            skyColor = Vec3(0.28f, 0.34f, 0.44f),
            groundAmbient = Vec3(0.14f, 0.16f, 0.20f),
            fogColor = Vec3(0.08f, 0.10f, 0.14f)
        )
        val studioSnap = baseSnap.copy(
            cameraPos = camPos,
            cameraYaw = yawRad,
            cameraPitch = pitchRad,
            playerPos = Vec3.ZERO,
            flashlightActive = false
        )

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, viewWidth, viewHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glDepthMask(false)
        skyShader.use()
        GLES30.glUniform3f(skyShader.uniformLocation("uSkyTop"), 0.06f, 0.08f, 0.12f)
        GLES30.glUniform3f(skyShader.uniformLocation("uSkyHorizon"), 0.13f, 0.16f, 0.21f)
        GLES30.glUniform1f(skyShader.uniformLocation("uPitch"), 0f)
        GLES30.glBindVertexArray(skyVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
        GLES30.glDepthMask(true)

        // Studio turntable pedestal
        val plinthMesh = gpuMeshes[MeshId.BARREL_DRUM]?.lod0
        if (plinthMesh != null) {
            val steel = MaterialLibrary.get("GRAPHITE")
            pbrShader.use()
            bindSceneLightingUniforms(pbrShader, studioSnap, studioEnv, fogOverride = 0f)
            bindPattern(steel.texturePattern)
            Quat.IDENTITY.writeModelMatrix(Vec3(0f, -0.045f, 0f), Vec3(0.85f, 0.045f, 0.85f), modelMatrix)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            GLES30.glUniform1f(pbrShader.uniformLocation("uIsSkin"), 0f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uIsHair"), 0f)
            GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uModel"), 1, false, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uMVP"), 1, false, mvpMatrix, 0)
            GLES30.glUniform1f(pbrShader.uniformLocation("uUseOverrideTint"), 1f)
            GLES30.glUniform3f(pbrShader.uniformLocation("uColorTint"), 0.16f, 0.19f, 0.23f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uUseMaterialOverrides"), 1f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uMetallicOverride"), 0.45f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uRoughnessOverride"), 0.35f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uCrackLevel"), 0f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uEmissiveIntensity"), 0f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uAlpha"), 1f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uHighlight"), 0f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uFrozen"), 0f)
            drawMesh(plinthMesh)
        }

        if (charUploaded) {
            val poseSnap = animationWorker.latestPoseRef.get()
            if (useCompanionRig && gpuCompanionLod != null && cpuCompanionChar != null) {
                Quat.IDENTITY.writeModelMatrix(Vec3.ZERO, Vec3.ONE, modelMatrix)
                renderSkinnedCompanionModel(
                    charModelMatrix = modelMatrix,
                    snap = studioSnap,
                    poseSnap = poseSnap,
                    env = studioEnv,
                    fogOverride = 0f
                )
            } else if (poseSnap != null) {
                Quat.IDENTITY.writeModelMatrix(Vec3.ZERO, Vec3.ONE, modelMatrix)
                renderSkinnedCharacterModel(
                    lodIdx = 0,
                    poseSnap = poseSnap,
                    charModelMatrix = modelMatrix,
                    submeshNames = visibleThirdPersonMeshNames(characterAppearance),
                    snap = studioSnap,
                    env = studioEnv,
                    fogOverride = 0f
                )
            }
        }
    }

    private fun renderAssetStudioPreview(baseSnap: WorldRenderSnapshot) {
        val ps = assetPreviewState
        val glb = when {
            ps.toolStyle != null -> gpuToolGlbs[ps.toolStyle]
            ps.meshId != null -> gpuMeshGlbs[ps.meshId]
            else -> null
        }
        val fallbackMesh = when {
            ps.toolStyle != null -> gpuToolMeshes[ps.toolStyle]?.lod0
            ps.meshId != null -> gpuMeshes[ps.meshId]?.lod0
            else -> null
        }

        val previewRadius = (glb?.boundingRadius ?: fallbackMesh?.let { 0.7f } ?: 0.7f).coerceAtLeast(0.14f)
        val target = glb?.boundsCenter ?: Vec3.ZERO
        val dist = (previewRadius * 2.65f / ps.zoom.coerceIn(0.55f, 3.0f)).coerceIn(0.32f, 14f)
        val yawRad = ps.orbitYawDeg * (PI.toFloat() / 180f)
        val pitchRad = ps.orbitPitchDeg.coerceIn(-65f, 65f) * (PI.toFloat() / 180f)
        val cp = cos(pitchRad)
        val camPos = target + Vec3(sin(yawRad) * cp * dist, -sin(pitchRad) * dist, cos(yawRad) * cp * dist)

        val aspect = viewWidth.toFloat() / viewHeight.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 40f, aspect, 0.04f, 60f)
        Matrix.setLookAtM(
            viewMatrix, 0,
            camPos.x, camPos.y, camPos.z,
            target.x, target.y, target.z,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        val studioEnv = EnvLighting(
            sunDir = Vec3(0.45f, 0.74f, -0.50f).normalized(),
            sunColor = Vec3(1.0f, 0.97f, 0.92f),
            skyColor = Vec3(0.30f, 0.36f, 0.46f),
            groundAmbient = Vec3(0.15f, 0.17f, 0.21f),
            fogColor = Vec3(0.08f, 0.10f, 0.14f)
        )
        val studioSnap = baseSnap.copy(
            cameraPos = camPos,
            cameraYaw = yawRad,
            cameraPitch = pitchRad,
            playerPos = target,
            flashlightActive = false
        )

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, viewWidth, viewHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glDepthMask(false)
        skyShader.use()
        GLES30.glUniform3f(skyShader.uniformLocation("uSkyTop"), 0.05f, 0.07f, 0.11f)
        GLES30.glUniform3f(skyShader.uniformLocation("uSkyHorizon"), 0.12f, 0.15f, 0.20f)
        GLES30.glUniform1f(skyShader.uniformLocation("uPitch"), 0f)
        GLES30.glBindVertexArray(skyVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
        GLES30.glDepthMask(true)

        pbrShader.use()
        bindSceneLightingUniforms(pbrShader, studioSnap, studioEnv, fogOverride = 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uIsSkin"), 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uIsHair"), 0f)
        GLES30.glUniform3f(pbrShader.uniformLocation("uSssColor"), 0f, 0f, 0f)

        // Subtle turntable base below object bounds
        val plinthMesh = gpuMeshes[MeshId.BARREL_DRUM]?.lod0
        val bottomY = target.y - (glb?.halfExtents?.y ?: 0.35f)
        if (plinthMesh != null) {
            val steel = MaterialLibrary.get("GRAPHITE")
            bindPattern(steel.texturePattern)
            val discR = (previewRadius * 1.15f).coerceIn(0.22f, 3.2f)
            val discH = (previewRadius * 0.055f).coerceIn(0.01f, 0.09f)
            Quat.IDENTITY.writeModelMatrix(Vec3(target.x, bottomY - discH * 1.1f, target.z), Vec3(discR, discH, discR), modelMatrix)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uModel"), 1, false, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uMVP"), 1, false, mvpMatrix, 0)
            GLES30.glUniform1f(pbrShader.uniformLocation("uUseOverrideTint"), 1f)
            GLES30.glUniform3f(pbrShader.uniformLocation("uColorTint"), 0.15f, 0.18f, 0.22f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uUseMaterialOverrides"), 1f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uMetallicOverride"), 0.45f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uRoughnessOverride"), 0.35f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uCrackLevel"), 0f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uEmissiveIntensity"), 0f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uAlpha"), 1f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uHighlight"), 0f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uFrozen"), 0f)
            drawMesh(plinthMesh)
        }

        Quat.IDENTITY.writeModelMatrix(Vec3.ZERO, Vec3.ONE, modelMatrix)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uModel"), 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(pbrShader.uniformLocation("uMVP"), 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(pbrShader.uniformLocation("uCrackLevel"), 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uAlpha"), 1f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uHighlight"), 0f)
        GLES30.glUniform1f(pbrShader.uniformLocation("uFrozen"), 0f)

        if (glb != null) {
            drawStaticGlbAsset(
                glb = glb,
                patternSpec = null,
                primaryTintOverride = null,
                emissiveColor = Vec3.ZERO,
                emissiveIntensity = 0f
            )
        } else if (fallbackMesh != null) {
            val spec = ps.materialSpec ?: MaterialLibrary.get("STEEL")
            bindPattern(spec.texturePattern)
            GLES30.glUniform1f(pbrShader.uniformLocation("uUseOverrideTint"), 1f)
            GLES30.glUniform3f(pbrShader.uniformLocation("uColorTint"), spec.tintR, spec.tintG, spec.tintB)
            GLES30.glUniform1f(pbrShader.uniformLocation("uUseMaterialOverrides"), 1f)
            GLES30.glUniform1f(pbrShader.uniformLocation("uMetallicOverride"), spec.metallic)
            GLES30.glUniform1f(pbrShader.uniformLocation("uRoughnessOverride"), spec.roughness)
            GLES30.glUniform1f(pbrShader.uniformLocation("uEmissiveIntensity"), 0f)
            drawMesh(fallbackMesh)
        }
    }

    private fun renderConstraintAndWireLines(snap: WorldRenderSnapshot) {
        val hasLines = snap.constraints.isNotEmpty() || snap.wires.isNotEmpty() ||
            snap.grabbedBeamStart != null || snap.attachPreviewStart != null
        if (!hasLines) return

        lineShader.use()
        GLES30.glUniformMatrix4fv(lineShader.uniformLocation("uVP"), 1, false, vpMatrix, 0)
        val uColor = lineShader.uniformLocation("uColor")
        GLES30.glLineWidth(4.5f)
        GLES30.glBindVertexArray(lineVao)

        for (c in snap.constraints) {
            when (c.type) {
                ConstraintType.ROPE -> {
                    GLES30.glUniform4f(uColor, 0.85f, 0.72f, 0.44f, 1f)
                    drawSegments(listOf(c.startWorld, c.endWorld))
                }
                ConstraintType.SPRING -> {
                    GLES30.glUniform4f(uColor, 0.90f, 0.52f, 0.16f, 1f)
                    drawSpringZigZag(c.startWorld, c.endWorld)
                }
                ConstraintType.HINGE -> {
                    GLES30.glUniform4f(uColor, 0.25f, 0.78f, 0.96f, 1f)
                    drawSegments(listOf(c.startWorld, c.endWorld))
                }
                ConstraintType.WELD -> {
                    GLES30.glUniform4f(uColor, 0.92f, 0.94f, 0.98f, 1f)
                    drawSegments(listOf(c.startWorld, c.endWorld))
                }
            }
        }

        for (w in snap.wires) {
            when {
                w.overloaded -> GLES30.glUniform4f(uColor, 0.98f, 0.24f, 0.20f, 1f)
                w.energized -> GLES30.glUniform4f(uColor, 0.20f, 0.92f, 0.98f, 1f)
                else -> GLES30.glUniform4f(uColor, 0.34f, 0.44f, 0.52f, 1f)
            }
            drawSegments(listOf(w.startWorld, w.endWorld))
        }

        val gs = snap.grabbedBeamStart
        val ge = snap.grabbedBeamEnd
        if (gs != null && ge != null) {
            GLES30.glUniform4f(uColor, 0.22f, 0.75f, 0.97f, 1f)
            drawSegments(listOf(gs, ge))
        }

        val as1 = snap.attachPreviewStart
        val ae1 = snap.attachPreviewEnd
        if (as1 != null && ae1 != null) {
            GLES30.glUniform4f(uColor, 0.96f, 0.62f, 0.14f, 1f)
            drawSegments(listOf(as1, ae1))
        }

        GLES30.glBindVertexArray(0)
    }

    private fun renderParticlesPass(snap: WorldRenderSnapshot, renderH: Int) {
        val parts = snap.particles
        particleCountRef.set(parts.size)
        if (parts.isEmpty()) return

        particleBuffer.clear()
        var count = 0
        for (p in parts) {
            if (particleBuffer.remaining() < 8) break
            particleBuffer.put(p.position.x)
            particleBuffer.put(p.position.y)
            particleBuffer.put(p.position.z)
            particleBuffer.put(p.size)
            particleBuffer.put(p.colorR)
            particleBuffer.put(p.colorG)
            particleBuffer.put(p.colorB)
            particleBuffer.put(p.alpha)
            count++
        }
        if (count == 0) return
        particleBuffer.flip()

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(false)

        particleShader.use()
        GLES30.glUniformMatrix4fv(particleShader.uniformLocation("uVP"), 1, false, vpMatrix, 0)
        GLES30.glUniform1f(particleShader.uniformLocation("uPointScale"), renderH * 1.1f)

        GLES30.glBindVertexArray(particleVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, particleVbo)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, count * 8 * 4, particleBuffer)
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, count)
        GLES30.glBindVertexArray(0)

        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    private fun captureFramebufferBitmap(): Bitmap? {
        return try {
            val w = viewWidth
            val h = viewHeight
            val buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
            GLES30.glReadPixels(0, 0, w, h, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buf)
            val pixels = IntArray(w * h)
            val src = IntArray(w * h)
            buf.asIntBuffer().get(src)
            for (y in 0 until h) {
                val srcRow = (h - 1 - y) * w
                val dstRow = y * w
                for (x in 0 until w) {
                    val abgr = src[srcRow + x]
                    val r = abgr and 0xFF
                    val g = (abgr shr 8) and 0xFF
                    val b = (abgr shr 16) and 0xFF
                    val a = (abgr shr 24) and 0xFF
                    pixels[dstRow + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
        } catch (_: Throwable) {
            null
        }
    }

    private fun drawSpringZigZag(a: Vec3, b: Vec3) {
        val pts = ArrayList<Vec3>(20)
        val segs = 10
        val dir = (b - a).normalized()
        val side = dir.cross(Vec3.UP).let { if (it.lengthSq() < 0.01f) Vec3.RIGHT else it.normalized() } * 0.12f
        pts.add(a)
        for (i in 1 until segs) {
            val t = i.toFloat() / segs
            val p = a.lerp(b, t) + (if (i % 2 == 0) side else -side)
            pts.add(p)
            pts.add(p)
        }
        pts.add(b)
        drawSegments(pts)
    }

    private fun drawSegments(pts: List<Vec3>) {
        if (pts.size < 2) return
        lineBuffer.clear()
        for (p in pts) {
            if (lineBuffer.remaining() < 3) break
            lineBuffer.put(p.x); lineBuffer.put(p.y); lineBuffer.put(p.z)
        }
        lineBuffer.flip()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, lineVbo)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, lineBuffer.limit() * 4, lineBuffer)
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, lineBuffer.limit() / 3)
    }

    private fun bindPattern(pattern: TexturePattern) {
        val tex = gpuPatternTextures[pattern] ?: return
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.albedoId)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.normalId)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.mraId)
    }

    private fun drawMesh(mesh: GpuMeshLevel) {
        GLES30.glBindVertexArray(mesh.vao)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, mesh.indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun updateFpsCounter() {
        frameCount++
        val now = System.nanoTime()
        if (now - lastFpsNs >= 500_000_000L) {
            val fps = ((frameCount * 1_000_000_000.0) / (now - lastFpsNs)).toInt().coerceIn(1, 144)
            fpsRef.set(fps)
            frameCount = 0
            lastFpsNs = now
        }
    }
}
