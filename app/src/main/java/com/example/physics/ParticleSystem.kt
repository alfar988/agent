package com.example.physics

import com.example.math.Quat
import com.example.math.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ParticleType {
    SPARK,
    GLASS_SHARD,
    WOOD_SPLINTER,
    CONCRETE_DUST,
    WELD_ARC,
    FOAM_SPRAY,
    RAIN,
    SNOW,
    MAGNET_PULSE
}

data class ParticleRenderData(
    val position: Vec3,
    val colorR: Float,
    val colorG: Float,
    val colorB: Float,
    val alpha: Float,
    val size: Float
)

private class PooledParticle {
    var active: Boolean = false
    var type: ParticleType = ParticleType.SPARK
    var pos: Vec3 = Vec3.ZERO
    var vel: Vec3 = Vec3.ZERO
    var gravityFactor: Float = 1f
    var life: Float = 0f
    var maxLife: Float = 1f
    var size: Float = 0.06f
    var r: Float = 1f
    var g: Float = 0.7f
    var b: Float = 0.2f
}

class ParticleSystem {
    companion object {
        const val HARD_POOL_LIMIT = 900
    }

    private val pool = Array(HARD_POOL_LIMIT) { PooledParticle() }
    private var cursor = 0

    @Volatile
    var maxActiveBudget: Int = 550

    fun clear() {
        for (p in pool) p.active = false
    }

    fun emitBurst(
        origin: Vec3,
        type: ParticleType,
        count: Int,
        speed: Float = 4.5f,
        directionHint: Vec3 = Vec3.UP,
        tintOverride: Vec3? = null
    ) {
        val budget = maxActiveBudget.coerceIn(40, HARD_POOL_LIMIT)
        val clampedCount = count.coerceAtMost(budget / 3)
        for (i in 0 until clampedCount) {
            val slot = acquireSlot(budget) ?: break
            val theta = Random.nextFloat() * 2f * PI.toFloat()
            val phi = (Random.nextFloat() - 0.35f) * PI.toFloat()
            val randDir = Vec3(
                cos(theta) * cos(phi),
                sin(phi),
                sin(theta) * cos(phi)
            )
            val dir = (directionHint * 0.55f + randDir * 0.85f).normalized()
            val spd = speed * (0.45f + Random.nextFloat() * 0.85f)

            slot.active = true
            slot.type = type
            slot.pos = origin + randDir * 0.08f
            slot.vel = dir * spd

            when (type) {
                ParticleType.SPARK -> {
                    slot.gravityFactor = 0.85f
                    slot.maxLife = 0.28f + Random.nextFloat() * 0.25f
                    slot.size = 0.045f + Random.nextFloat() * 0.03f
                    slot.r = 1.0f; slot.g = 0.72f + Random.nextFloat() * 0.25f; slot.b = 0.18f
                }
                ParticleType.WELD_ARC -> {
                    slot.gravityFactor = 0.3f
                    slot.maxLife = 0.18f + Random.nextFloat() * 0.14f
                    slot.size = 0.07f + Random.nextFloat() * 0.04f
                    slot.r = 0.65f; slot.g = 0.90f; slot.b = 1.0f
                }
                ParticleType.GLASS_SHARD -> {
                    slot.gravityFactor = 1.0f
                    slot.maxLife = 0.65f + Random.nextFloat() * 0.35f
                    slot.size = 0.06f + Random.nextFloat() * 0.04f
                    slot.r = tintOverride?.x ?: 0.78f
                    slot.g = tintOverride?.y ?: 0.92f
                    slot.b = tintOverride?.z ?: 0.98f
                }
                ParticleType.WOOD_SPLINTER -> {
                    slot.gravityFactor = 0.95f
                    slot.maxLife = 0.60f + Random.nextFloat() * 0.35f
                    slot.size = 0.065f + Random.nextFloat() * 0.04f
                    slot.r = tintOverride?.x ?: 0.68f
                    slot.g = tintOverride?.y ?: 0.48f
                    slot.b = tintOverride?.z ?: 0.26f
                }
                ParticleType.CONCRETE_DUST -> {
                    slot.gravityFactor = 0.15f
                    slot.maxLife = 0.75f + Random.nextFloat() * 0.50f
                    slot.size = 0.11f + Random.nextFloat() * 0.08f
                    slot.r = tintOverride?.x ?: 0.68f
                    slot.g = tintOverride?.y ?: 0.69f
                    slot.b = tintOverride?.z ?: 0.70f
                }
                ParticleType.FOAM_SPRAY -> {
                    slot.gravityFactor = 0.20f
                    slot.maxLife = 0.55f + Random.nextFloat() * 0.35f
                    slot.size = 0.10f + Random.nextFloat() * 0.07f
                    slot.r = 0.92f; slot.g = 0.96f; slot.b = 0.99f
                }
                ParticleType.MAGNET_PULSE -> {
                    slot.gravityFactor = 0.0f
                    slot.maxLife = 0.35f + Random.nextFloat() * 0.20f
                    slot.size = 0.065f
                    slot.r = 0.22f; slot.g = 0.80f; slot.b = 1.0f
                }
                ParticleType.RAIN -> {
                    slot.gravityFactor = 1.4f
                    slot.maxLife = 0.8f
                    slot.size = 0.03f
                    slot.r = 0.65f; slot.g = 0.78f; slot.b = 0.95f
                }
                ParticleType.SNOW -> {
                    slot.gravityFactor = 0.12f
                    slot.maxLife = 1.8f
                    slot.size = 0.055f
                    slot.r = 0.95f; slot.g = 0.97f; slot.b = 1.0f
                }
            }
            slot.life = slot.maxLife
        }
    }

    fun emitWeatherStep(cameraPos: Vec3, weather: WeatherType, windStrength: Float) {
        if (weather != WeatherType.RAIN && weather != WeatherType.SNOW) return
        val budget = maxActiveBudget.coerceIn(40, HARD_POOL_LIMIT)
        val perTick = if (weather == WeatherType.RAIN) 4 else 2
        val type = if (weather == WeatherType.RAIN) ParticleType.RAIN else ParticleType.SNOW
        for (i in 0 until perTick) {
            val slot = acquireSlot(budget) ?: break
            slot.active = true
            slot.type = type
            slot.pos = Vec3(
                cameraPos.x + (Random.nextFloat() - 0.5f) * 22f,
                cameraPos.y + 8f + Random.nextFloat() * 4f,
                cameraPos.z + (Random.nextFloat() - 0.5f) * 22f
            )
            if (weather == WeatherType.RAIN) {
                slot.vel = Vec3(windStrength * 1.4f, -13.5f, windStrength * 0.4f)
                slot.gravityFactor = 0.4f
                slot.maxLife = 0.95f
                slot.size = 0.032f
                slot.r = 0.68f; slot.g = 0.80f; slot.b = 0.95f
            } else {
                slot.vel = Vec3(windStrength * 0.9f + (Random.nextFloat() - 0.5f) * 0.8f, -2.1f, (Random.nextFloat() - 0.5f) * 0.8f)
                slot.gravityFactor = 0.02f
                slot.maxLife = 2.4f
                slot.size = 0.058f
                slot.r = 0.95f; slot.g = 0.97f; slot.b = 1.0f
            }
            slot.life = slot.maxLife
        }
    }

    fun step(dt: Float, gravityY: Float) {
        val budget = maxActiveBudget.coerceIn(40, HARD_POOL_LIMIT)
        for (i in 0 until budget) {
            val p = pool[i]
            if (!p.active) continue
            p.life -= dt
            if (p.life <= 0f || p.pos.y < -15f) {
                p.active = false
                continue
            }
            p.vel = Vec3(p.vel.x, p.vel.y + gravityY * p.gravityFactor * dt, p.vel.z)
            p.pos += p.vel * dt
        }
    }

    fun buildRenderList(): List<ParticleRenderData> {
        val budget = maxActiveBudget.coerceIn(40, HARD_POOL_LIMIT)
        val out = ArrayList<ParticleRenderData>(budget / 2)
        for (i in 0 until budget) {
            val p = pool[i]
            if (!p.active) continue
            val alpha = (p.life / p.maxLife).coerceIn(0.05f, 1.0f)
            out.add(ParticleRenderData(p.pos, p.r, p.g, p.b, alpha, p.size))
        }
        return out
    }

    private fun acquireSlot(budget: Int): PooledParticle? {
        for (attempt in 0 until budget) {
            val idx = (cursor + attempt) % budget
            if (!pool[idx].active) {
                cursor = (idx + 1) % budget
                return pool[idx]
            }
        }
        val overwrite = pool[cursor % budget]
        cursor = (cursor + 1) % budget
        return overwrite
    }
}

object DestructionHelper {

    fun spawnDebrisForBody(
        body: RigidBody,
        nextIdProvider: () -> Int,
        currentBodyCount: Int,
        particles: ParticleSystem
    ): List<RigidBody> {
        val spec = body.materialSpec
        val tint = Vec3(spec.tintR, spec.tintG, spec.tintB)
        val particleType = when (spec.destructionProfile) {
            DestructionProfile.SHATTER_GLASS -> ParticleType.GLASS_SHARD
            DestructionProfile.SPLINTER_WOOD -> ParticleType.WOOD_SPLINTER
            DestructionProfile.FRACTURE_CONCRETE -> ParticleType.CONCRETE_DUST
            DestructionProfile.DEFORM_METAL -> ParticleType.SPARK
            DestructionProfile.SNAP_PLASTIC -> ParticleType.SPARK
            DestructionProfile.TEAR_SOFT -> ParticleType.FOAM_SPRAY
            DestructionProfile.MELT_WAX -> ParticleType.FOAM_SPRAY
            DestructionProfile.NONE -> ParticleType.SPARK
        }
        particles.emitBurst(body.position, particleType, count = 22, speed = 5.6f, tintOverride = tint)

        // Cap physical debris spawning when scene already has many bodies for stability
        if (currentBodyCount > 110 || body.isDebris) return emptyList()

        val fragCount = when (spec.destructionProfile) {
            DestructionProfile.SHATTER_GLASS -> 4
            DestructionProfile.SPLINTER_WOOD -> 3
            DestructionProfile.FRACTURE_CONCRETE -> 3
            DestructionProfile.SNAP_PLASTIC -> 2
            else -> 0
        }
        if (fragCount == 0) return emptyList()

        val out = ArrayList<RigidBody>(fragCount)
        val fragScale = (body.scale * 0.36f).coerceIn(0.22f, 0.55f)
        for (i in 0 until fragCount) {
            val ang = (i.toFloat() / fragCount) * 2f * PI.toFloat() + Random.nextFloat() * 0.5f
            val offset = Vec3(cos(ang) * 0.24f, 0.1f + (i % 2) * 0.15f, sin(ang) * 0.24f)
            val frag = RigidBody(
                id = nextIdProvider(),
                type = ObjectType.CONCRETE_BLOCK,
                materialSpec = spec,
                position = body.position + offset,
                orientation = Quat.fromEuler(Random.nextFloat() * 3f, Random.nextFloat() * 3f, Random.nextFloat() * 3f),
                scale = fragScale,
                isFrozen = false,
                customLabel = "${spec.displayName} Fragment"
            )
            frag.isDebris = true
            frag.health = 30f
            frag.linearVelocity = body.linearVelocity * 0.6f + offset.normalized() * (2.5f + Random.nextFloat() * 2.5f)
            frag.angularVelocity = Vec3(
                (Random.nextFloat() - 0.5f) * 8f,
                (Random.nextFloat() - 0.5f) * 8f,
                (Random.nextFloat() - 0.5f) * 8f
            )
            out.add(frag)
        }
        return out
    }
}
