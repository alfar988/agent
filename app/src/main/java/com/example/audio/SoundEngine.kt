package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.HandlerThread
import com.example.math.Vec3
import com.example.physics.AcousticClass
import com.example.physics.CollisionAudioEvent
import com.example.physics.MapId
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine(private val context: Context) {

    private val audioThread = HandlerThread("KineticLab-AudioThread").apply { start() }
    private val handler = Handler(audioThread.looper)

    private var soundPool: SoundPool? = null
    private val sampleIds = HashMap<String, Int>()
    private var activeAmbientStreamId: Int = 0
    @Volatile private var initialized = false

    @Volatile var masterVolume: Float = 0.85f
    @Volatile var sfxVolume: Float = 0.85f
    @Volatile var uiVolume: Float = 0.85f
    @Volatile var ambientVolume: Float = 0.40f
        set(value) {
            field = value
            updateAmbientVolume()
        }

    @Volatile private var lastImpactMs = 0L
    @Volatile var listenerProvider: (() -> Pair<Vec3, Float>)? = null

    fun initialize() {
        handler.post {
            if (initialized) return@post
            try {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val pool = SoundPool.Builder()
                    .setMaxStreams(16)
                    .setAudioAttributes(attrs)
                    .build()

                val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
                loadSynthWav(pool, dir, "impact_metal") { i, sr -> synthMetalClang(i, sr) }
                loadSynthWav(pool, dir, "impact_rubber") { i, sr -> synthRubberThump(i, sr) }
                loadSynthWav(pool, dir, "impact_wood") { i, sr -> synthWoodKnock(i, sr) }
                loadSynthWav(pool, dir, "impact_glass") { i, sr -> synthGlassClink(i, sr) }
                loadSynthWav(pool, dir, "impact_concrete") { i, sr -> synthConcreteThud(i, sr) }

                loadSynthWav(pool, dir, "tool_grab", 0.16f) { i, sr -> synthChirp(i, sr, 320f, 540f) }
                loadSynthWav(pool, dir, "tool_impulse", 0.24f) { i, sr -> synthBlast(i, sr) }
                loadSynthWav(pool, dir, "tool_attach", 0.14f) { i, sr -> synthChirp(i, sr, 560f, 880f) }
                loadSynthWav(pool, dir, "tool_detach", 0.14f) { i, sr -> synthChirp(i, sr, 720f, 360f) }
                loadSynthWav(pool, dir, "tool_spawn", 0.15f) { i, sr -> synthChirp(i, sr, 420f, 680f) }
                loadSynthWav(pool, dir, "tool_freeze", 0.18f) { i, sr -> synthChirp(i, sr, 960f, 480f) }
                loadSynthWav(pool, dir, "tool_shatter", 0.30f) { i, sr -> synthShatter(i, sr) }
                loadSynthWav(pool, dir, "tool_jump", 0.15f) { i, sr -> synthChirp(i, sr, 180f, 310f) }
                loadSynthWav(pool, dir, "tool_weld", 0.28f) { i, sr -> synthWeldArc(i, sr) }
                loadSynthWav(pool, dir, "tool_drill", 0.24f) { i, sr -> synthDrillMotor(i, sr) }
                loadSynthWav(pool, dir, "tool_grinder", 0.26f) { i, sr -> synthGrinderRasp(i, sr) }
                loadSynthWav(pool, dir, "tool_hammer", 0.20f) { i, sr -> synthHammerStrike(i, sr) }
                loadSynthWav(pool, dir, "tool_switch", 0.08f) { i, sr -> synthRelaySwitch(i, sr) }
                loadSynthWav(pool, dir, "tool_spark", 0.22f) { i, sr -> synthOverloadZap(i, sr) }
                loadSynthWav(pool, dir, "tool_land", 0.16f) { i, sr -> synthLandingThud(i, sr) }
                loadSynthWav(pool, dir, "step_concrete", 0.11f) { i, sr -> synthStepConcrete(i, sr) }
                loadSynthWav(pool, dir, "step_metal", 0.12f) { i, sr -> synthStepMetal(i, sr) }
                loadSynthWav(pool, dir, "step_wood", 0.11f) { i, sr -> synthStepWood(i, sr) }
                loadSynthWav(pool, dir, "step_gravel", 0.13f) { i, sr -> synthStepGravel(i, sr) }
                loadSynthWav(pool, dir, "gun_pistol", 0.18f) { i, sr -> synthGunshot(i, sr, punchFreq = 260f, decay = 22f, bass = 0.45f) }
                loadSynthWav(pool, dir, "gun_smg", 0.14f) { i, sr -> synthGunshot(i, sr, punchFreq = 320f, decay = 28f, bass = 0.35f) }
                loadSynthWav(pool, dir, "gun_rifle", 0.22f) { i, sr -> synthGunshot(i, sr, punchFreq = 210f, decay = 18f, bass = 0.55f) }
                loadSynthWav(pool, dir, "gun_shotgun", 0.32f) { i, sr -> synthGunshot(i, sr, punchFreq = 125f, decay = 11f, bass = 0.72f) }
                loadSynthWav(pool, dir, "gun_sniper", 0.36f) { i, sr -> synthGunshot(i, sr, punchFreq = 145f, decay = 9.5f, bass = 0.68f) }
                loadSynthWav(pool, dir, "gun_reload", 0.26f) { i, sr -> synthGunReload(i, sr) }
                loadSynthWav(pool, dir, "gun_dryfire", 0.06f) { i, sr -> synthDryFire(i, sr) }
                loadSynthWav(pool, dir, "ammo_pickup", 0.18f) { i, sr -> synthAmmoPickup(i, sr) }
                loadSynthWav(pool, dir, "explosion", 0.65f) { i, sr -> synthExplosion(i, sr) }
                loadSynthWav(pool, dir, "ui_click", 0.06f) { i, sr -> synthClick(i, sr) }
                loadSynthWav(pool, dir, "ambient_hum", 1.8f) { i, sr -> synthAmbientHum(i, sr) }

                soundPool = pool
                initialized = true
            } catch (_: Throwable) {
                // Safe fallback in unit test / Robolectric environments
            }
        }
    }

    fun startAmbient(mapId: MapId) {
        handler.postDelayed({
            val pool = soundPool ?: return@postDelayed
            val id = sampleIds["ambient_hum"] ?: return@postDelayed
            if (activeAmbientStreamId != 0) {
                pool.stop(activeAmbientStreamId)
            }
            val vol = (masterVolume * ambientVolume * 0.35f).coerceIn(0f, 1f)
            val rate = when (mapId) {
                MapId.LAB -> 1.05f
                MapId.FACTORY -> 0.82f
                MapId.WAREHOUSE -> 0.72f
                MapId.CONSTRUCTION -> 1.15f
                MapId.FACILITY -> 0.96f
                MapId.UNDERGROUND_MAINTENANCE -> 0.68f
                MapId.OUTDOOR_TESTING_YARD -> 1.12f
            }
            activeAmbientStreamId = pool.play(id, vol, vol, 0, -1, rate)
        }, 250L)
    }

    fun stopAmbient() {
        handler.post {
            if (activeAmbientStreamId != 0) {
                soundPool?.stop(activeAmbientStreamId)
                activeAmbientStreamId = 0
            }
        }
    }

    private fun updateAmbientVolume() {
        handler.post {
            if (activeAmbientStreamId != 0) {
                val vol = (masterVolume * ambientVolume * 0.35f).coerceIn(0f, 1f)
                soundPool?.setVolume(activeAmbientStreamId, vol, vol)
            }
        }
    }

    fun playCollision(event: CollisionAudioEvent) {
        val now = System.currentTimeMillis()
        if (now - lastImpactMs < 38L) return
        lastImpactMs = now

        handler.post {
            val pool = soundPool ?: return@post
            val key = when {
                event.materialA == AcousticClass.GLASS || event.materialB == AcousticClass.GLASS -> "impact_glass"
                event.materialA == AcousticClass.METAL || event.materialB == AcousticClass.METAL -> "impact_metal"
                event.materialA == AcousticClass.RUBBER || event.materialB == AcousticClass.RUBBER -> "impact_rubber"
                event.materialA == AcousticClass.WOOD || event.materialB == AcousticClass.WOOD -> "impact_wood"
                else -> "impact_concrete"
            }
            val sampleId = sampleIds[key] ?: return@post
            val speedGain = ((event.impactSpeed - 1.4f) / 14f).coerceIn(0.08f, 1.0f)
            val (left, right) = computeSpatialPan(event.worldPos, speedGain * masterVolume * sfxVolume)
            val pitch = (0.88f + Random.nextFloat() * 0.24f)
            pool.play(sampleId, left, right, 1, 0, pitch)
        }
    }

    fun playToolSound(tag: String, worldPos: Vec3) {
        handler.post {
            val pool = soundPool ?: return@post
            val key = when (tag) {
                "grab" -> "tool_grab"
                "impulse", "throw" -> "tool_impulse"
                "attach" -> "tool_attach"
                "detach", "delete" -> "tool_detach"
                "spawn" -> "tool_spawn"
                "freeze" -> "tool_freeze"
                "shatter" -> "tool_shatter"
                "jump" -> "tool_jump"
                "weld" -> "tool_weld"
                "drill" -> "tool_drill"
                "grinder" -> "tool_grinder"
                "hammer" -> "tool_hammer"
                "switch" -> "tool_switch"
                "overload", "spark" -> "tool_spark"
                "land" -> "tool_land"
                "step_concrete" -> "step_concrete"
                "step_metal" -> "step_metal"
                "step_wood" -> "step_wood"
                "step_gravel" -> "step_gravel"
                "gun_pistol" -> "gun_pistol"
                "gun_smg" -> "gun_smg"
                "gun_rifle" -> "gun_rifle"
                "gun_shotgun" -> "gun_shotgun"
                "gun_sniper" -> "gun_sniper"
                "gun_reload" -> "gun_reload"
                "gun_dryfire" -> "gun_dryfire"
                "ammo_pickup" -> "ammo_pickup"
                "explosion" -> "explosion"
                else -> "ui_click"
            }
            val sampleId = sampleIds[key] ?: return@post
            val isStep = key.startsWith("step_")
            val isGun = key.startsWith("gun_")
            val volMult = when {
                key == "explosion" -> 1.0f
                isStep -> 0.48f
                isGun -> 0.88f
                else -> 0.75f
            }
            val pitch = if (isStep || key == "tool_land" || isGun) (0.94f + Random.nextFloat() * 0.12f) else 1.0f
            val (left, right) = computeSpatialPan(worldPos, masterVolume * sfxVolume * volMult)
            pool.play(sampleId, left, right, 2, 0, pitch)
        }
    }

    fun playFootstep(acoustic: AcousticClass, worldPos: Vec3) {
        val tag = when (acoustic) {
            AcousticClass.METAL -> "step_metal"
            AcousticClass.WOOD -> "step_wood"
            AcousticClass.RUBBER -> "step_gravel"
            else -> "step_concrete"
        }
        playToolSound(tag, worldPos)
    }

    fun playUiClick() {
        handler.post {
            val pool = soundPool ?: return@post
            val sampleId = sampleIds["ui_click"] ?: return@post
            val v = (masterVolume * uiVolume * 0.55f).coerceIn(0f, 1f)
            pool.play(sampleId, v, v, 2, 0, 1.0f)
        }
    }

    private fun computeSpatialPan(pos: Vec3, baseVol: Float): Pair<Float, Float> {
        val listener = listenerProvider?.invoke() ?: return Pair(baseVol, baseVol)
        val delta = pos - listener.first
        val dist = delta.length().coerceAtLeast(1.0f)
        val atten = (1.0f / (1.0f + 0.08f * dist)).coerceIn(0.06f, 1.0f)
        val yaw = listener.second
        val rightDir = Vec3(cos(yaw), 0f, sin(yaw))
        val pan = if (dist > 0.05f) (delta / dist).dot(rightDir).coerceIn(-1f, 1f) else 0f
        val left = (baseVol * atten * (1f - pan * 0.65f)).coerceIn(0f, 1f)
        val right = (baseVol * atten * (1f + pan * 0.65f)).coerceIn(0f, 1f)
        return Pair(left, right)
    }

    fun shutdown() {
        handler.post {
            soundPool?.release()
            soundPool = null
            audioThread.quitSafely()
        }
    }

    private fun loadSynthWav(
        pool: SoundPool,
        dir: File,
        name: String,
        durationSec: Float = 0.28f,
        gen: (Int, Int) -> Float
    ) {
        val sampleRate = 22050
        val numSamples = (sampleRate * durationSec).toInt()
        val pcm = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            pcm[i] = (gen(i, sampleRate).coerceIn(-1f, 1f) * 28000f).toInt().toShort()
        }
        val file = File(dir, "$name.wav")
        writeWav(file, pcm, sampleRate)
        sampleIds[name] = pool.load(file.absolutePath, 1)
    }

    private fun writeWav(file: File, pcm: ShortArray, sampleRate: Int) {
        val dataBytes = pcm.size * 2
        val buf = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray())
        buf.putInt(36 + dataBytes)
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16)
        buf.putShort(1) // PCM
        buf.putShort(1) // Mono
        buf.putInt(sampleRate)
        buf.putInt(sampleRate * 2)
        buf.putShort(2)
        buf.putShort(16)
        buf.put("data".toByteArray())
        buf.putInt(dataBytes)
        for (s in pcm) buf.putShort(s)
        FileOutputStream(file).use { it.write(buf.array()) }
    }

    private fun synthMetalClang(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 14f)
        return env * (0.45f * sin(2f * PI.toFloat() * 540f * t) +
            0.35f * sin(2f * PI.toFloat() * 1185f * t) +
            0.20f * sin(2f * PI.toFloat() * 2140f * t))
    }

    private fun synthRubberThump(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 18f)
        val freq = 140f * exp(-t * 9f) + 65f
        return env * sin(2f * PI.toFloat() * freq * t)
    }

    private fun synthWoodKnock(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 28f)
        return env * (0.65f * sin(2f * PI.toFloat() * 340f * t) +
            0.35f * sin(2f * PI.toFloat() * 760f * t))
    }

    private fun synthGlassClink(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 16f)
        return env * (0.5f * sin(2f * PI.toFloat() * 2450f * t) +
            0.3f * sin(2f * PI.toFloat() * 3890f * t) +
            0.2f * sin(2f * PI.toFloat() * 5120f * t))
    }

    private fun synthConcreteThud(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 22f)
        val noise = ((i * 1103515245 + 12345) and 0x7fff) / 16384f - 1f
        return env * (0.6f * sin(2f * PI.toFloat() * 115f * t) + 0.4f * noise)
    }

    private fun synthChirp(i: Int, sr: Int, f0: Float, f1: Float): Float {
        val t = i.toFloat() / sr
        val env = sin((i.toFloat() / (sr * 0.16f)).coerceIn(0f, 1f) * PI.toFloat())
        val f = f0 + (f1 - f0) * (t / 0.16f).coerceIn(0f, 1f)
        return env * 0.55f * sin(2f * PI.toFloat() * f * t)
    }

    private fun synthBlast(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 14f)
        val noise = ((i * 1664525 + 1013904223) and 0x7fff) / 16384f - 1f
        return env * (0.6f * sin(2f * PI.toFloat() * 90f * exp(-t * 6f) * t) + 0.4f * noise)
    }

    private fun synthShatter(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 10f)
        val noise = ((i * 1664525 + 1013904223) and 0x7fff) / 16384f - 1f
        return env * (0.45f * noise + 0.35f * sin(2f * PI.toFloat() * 3200f * t) + 0.2f * sin(2f * PI.toFloat() * 4800f * t))
    }

    private fun synthClick(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 65f)
        return env * 0.6f * sin(2f * PI.toFloat() * 1100f * t)
    }

    private fun synthAmbientHum(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        // Seamless loop at 1.8s: frequencies must be multiples of 1/1.8Hz (e.g. 55Hz -> 99 cycles, 110Hz -> 198 cycles)
        val f1 = 55f
        val f2 = 110f
        return 0.18f * sin(2f * PI.toFloat() * f1 * t) + 0.08f * sin(2f * PI.toFloat() * f2 * t)
    }

    private fun synthWeldArc(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 4.5f)
        val noise = ((i * 1103515245 + 12345) and 0x7fff) / 16384f - 1f
        val crackle = if (i % 7 < 2) noise * 0.9f else noise * 0.35f
        return env * (0.45f * sin(2f * PI.toFloat() * 120f * t) + 0.55f * crackle)
    }

    private fun synthDrillMotor(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = sin((t / 0.24f).coerceIn(0f, 1f) * PI.toFloat())
        val freq = 220f + 160f * sin(t * 24f)
        return env * (0.55f * sin(2f * PI.toFloat() * freq * t) + 0.25f * sin(2f * PI.toFloat() * freq * 2.1f * t))
    }

    private fun synthGrinderRasp(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 5f)
        val noise = ((i * 1664525 + 1013904223) and 0x7fff) / 16384f - 1f
        return env * (0.55f * noise + 0.35f * sin(2f * PI.toFloat() * 1450f * t))
    }

    private fun synthHammerStrike(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 20f)
        return env * (0.65f * sin(2f * PI.toFloat() * 185f * t) + 0.35f * sin(2f * PI.toFloat() * 680f * t))
    }

    private fun synthRelaySwitch(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 48f)
        return env * (0.7f * sin(2f * PI.toFloat() * 1650f * t) + 0.3f * sin(2f * PI.toFloat() * 2900f * t))
    }

    private fun synthOverloadZap(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 9f)
        val noise = ((i * 1103515245 + 12345) and 0x7fff) / 16384f - 1f
        return env * (0.5f * noise + 0.5f * sin(2f * PI.toFloat() * (980f - t * 2200f) * t))
    }

    private fun synthLandingThud(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 24f)
        val noise = ((i * 1664525 + 1013904223) and 0x7fff) / 16384f - 1f
        val freq = 95f * exp(-t * 10f) + 48f
        return env * (0.70f * sin(2f * PI.toFloat() * freq * t) + 0.30f * noise)
    }

    private fun synthStepConcrete(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 42f)
        val noise = ((i * 1103515245 + 12345) and 0x7fff) / 16384f - 1f
        return env * (0.55f * sin(2f * PI.toFloat() * 145f * t) + 0.45f * noise)
    }

    private fun synthStepMetal(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 28f)
        return env * (0.48f * sin(2f * PI.toFloat() * 410f * t) +
            0.32f * sin(2f * PI.toFloat() * 880f * t) +
            0.20f * sin(2f * PI.toFloat() * 1520f * t))
    }

    private fun synthStepWood(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 36f)
        return env * (0.65f * sin(2f * PI.toFloat() * 230f * t) + 0.35f * sin(2f * PI.toFloat() * 510f * t))
    }

    private fun synthStepGravel(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 26f)
        val noise = ((i * 1664525 + 1013904223) and 0x7fff) / 16384f - 1f
        val crunch = if (i % 9 < 3) noise else noise * 0.45f
        return env * (0.75f * crunch + 0.25f * sin(2f * PI.toFloat() * 180f * t))
    }

    private fun synthGunshot(i: Int, sr: Int, punchFreq: Float, decay: Float, bass: Float): Float {
        val t = i.toFloat() / sr
        val crackEnv = exp(-t * decay * 1.9f)
        val tailEnv = exp(-t * decay)
        val n1 = (((i * 1103515245 + 12345) and 0x7fff) / 16384f - 1f)
        val n2 = (((i * 1664525 + 1013904223) and 0x7fff) / 16384f - 1f)
        val bodySweep = sin(2f * PI.toFloat() * (punchFreq * exp(-t * 14f) + 52f) * t)
        val sample = crackEnv * 0.58f * n1 + tailEnv * (bass * bodySweep + (1f - bass) * 0.45f * n2)
        return sample.coerceIn(-1f, 1f)
    }

    private fun synthGunReload(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val click1 = exp(-((t - 0.04f) * 85f).let { it * it }) * sin(2f * PI.toFloat() * 1350f * t)
        val click2 = exp(-((t - 0.16f) * 75f).let { it * it }) * sin(2f * PI.toFloat() * 1880f * t)
        val slide = exp(-t * 14f) * ((((i * 1103515245) and 0x7fff) / 16384f - 1f) * 0.18f)
        return (0.55f * click1 + 0.65f * click2 + slide).coerceIn(-1f, 1f)
    }

    private fun synthDryFire(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 85f)
        return env * (0.7f * sin(2f * PI.toFloat() * 2400f * t) + 0.3f * sin(2f * PI.toFloat() * 3900f * t))
    }

    private fun synthAmmoPickup(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val env = exp(-t * 18f)
        val c1 = sin(2f * PI.toFloat() * 680f * t)
        val c2 = if (t > 0.055f) sin(2f * PI.toFloat() * 1020f * t) else 0f
        return env * (0.45f * c1 + 0.55f * c2)
    }

    private fun synthExplosion(i: Int, sr: Int): Float {
        val t = i.toFloat() / sr
        val shockEnv = exp(-t * 14f)
        val rumbleEnv = exp(-t * 4.2f)
        val n1 = ((i * 1103515245 + 12345) and 0x7fff) / 16384f - 1f
        val n2 = ((i * 1664525 + 1013904223) and 0x7fff) / 16384f - 1f
        val sub = sin(2f * PI.toFloat() * (95f * exp(-t * 5f) + 32f) * t)
        val crackle = if (i % 5 == 0) n1 * 0.8f else n2 * 0.45f
        return (shockEnv * 0.50f * crackle + rumbleEnv * (0.62f * sub + 0.35f * n2)).coerceIn(-1f, 1f)
    }
}
