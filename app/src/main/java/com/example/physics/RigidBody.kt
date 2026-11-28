package com.example.physics

import com.example.electrical.ElectricalRole
import com.example.math.Mat3
import com.example.math.Quat
import com.example.math.Vec3

enum class CollisionMode(val displayName: String) {
    SOLID("Solid Rigid"),
    TRIGGER("Non-Colliding Trigger"),
    NO_SLEEP("Always Active")
}

class RigidBody(
    val id: Int,
    val type: ObjectType,
    material: PhysicsMaterial = type.defaultMaterial,
    var position: Vec3 = Vec3.ZERO,
    var orientation: Quat = Quat.IDENTITY,
    var scale: Float = 1.0f,
    var isFrozen: Boolean = false,
    var customLabel: String = type.displayName,
    materialSpec: MaterialSpec? = null
) {
    var materialSpec: MaterialSpec = materialSpec
        ?: if (material == type.defaultMaterial) MaterialLibrary.get(type.defaultMaterialId)
        else MaterialLibrary.fromLegacy(material)
        private set

    var material: PhysicsMaterial = MaterialLibrary.toLegacyFallback(this.materialSpec).let {
        if (materialSpec == null && material != type.defaultMaterial) material else it
    }
        set(value) {
            field = value
            this.materialSpec = MaterialLibrary.fromLegacy(value)
        }

    fun applyMaterialSpec(spec: MaterialSpec) {
        this.materialSpec = spec
        this.material = MaterialLibrary.toLegacyFallback(spec)
        recomputeMassProperties()
        wake()
    }

    var prevPosition: Vec3 = position
    var prevOrientation: Quat = orientation

    var linearVelocity: Vec3 = Vec3.ZERO
    var angularVelocity: Vec3 = Vec3.ZERO

    var forceAccum: Vec3 = Vec3.ZERO
    var torqueAccum: Vec3 = Vec3.ZERO

    var mass: Float = 1f
        private set
    var invMass: Float = 1f
        private set
    var localInvInertiaDiag: Vec3 = Vec3.ONE
        private set
    var invInertiaWorld: Mat3 = Mat3.IDENTITY
        private set

    var isSleeping: Boolean = false
    var sleepTimer: Float = 0f
    var health: Float = if (type.destructible || this.materialSpec.destructionProfile != DestructionProfile.NONE) 100f else Float.POSITIVE_INFINITY
    var isDestroyed: Boolean = false

    // Phase 2 configurable physical, thermal, destruction, grouping & electrical properties
    var gravityScale: Float = 1.0f
    var collisionMode: CollisionMode = CollisionMode.SOLID
    var massOverride: Float? = null
    var frictionOverride: Float? = null
    var restitutionOverride: Float? = null
    var temperatureC: Float = 22.0f
    var crackLevel: Float = 0.0f
    var squashAmount: Float = 0.0f
    var groupId: Int? = null
    var electricalRole: ElectricalRole = type.electricalRole
    var toolId: String? = null
    var ammoVariantId: String? = type.defaultAmmoVariantId()
    var ammoAmount: Int = com.example.weapons.AmmoBoxVariant.fromId(ammoVariantId)?.ammoAmount ?: 0
    var emissiveIntensity: Float = 0.0f
    var emissiveColor: Vec3 = Vec3(1.0f, 0.85f, 0.45f)
    var isDebris: Boolean = false
    var isArmed: Boolean = false
    var fuseTimerSec: Float = 0f
    var smokeActiveTimerSec: Float = 0f

    val isGrenade: Boolean
        get() = type == ObjectType.GRENADE_FRAG || type == ObjectType.GRENADE_SMOKE

    fun armGrenade(fuseSec: Float = 2.6f) {
        if (!isGrenade || isDestroyed) return
        if (!isArmed && smokeActiveTimerSec <= 0f) {
            isArmed = true
            fuseTimerSec = fuseSec.coerceAtLeast(0.05f)
            collisionMode = CollisionMode.NO_SLEEP
            wake()
        } else if (isArmed && fuseSec < fuseTimerSec) {
            fuseTimerSec = fuseSec.coerceAtLeast(0.05f)
        }
    }

    val effectiveStaticFriction: Float
        get() = frictionOverride ?: materialSpec.staticFriction

    val effectiveDynamicFriction: Float
        get() = frictionOverride ?: materialSpec.dynamicFriction

    val effectiveRestitution: Float
        get() = restitutionOverride ?: materialSpec.restitution

    val effectiveHalfExtents: Vec3
        get() {
            val base = type.halfExtents * scale
            return if (squashAmount > 0.005f) {
                val sy = (1f - squashAmount).coerceAtLeast(0.45f)
                val sxz = 1f + squashAmount * 0.35f
                Vec3(base.x * sxz, base.y * sy, base.z * sxz)
            } else base
        }

    val effectiveRadius: Float
        get() = type.radius * scale

    init {
        recomputeMassProperties()
        updateWorldInertia()
    }

    fun recomputeMassProperties() {
        val he = effectiveHalfExtents
        val volume = if (type.colliderType == ColliderShapeType.SPHERE) {
            val r = effectiveRadius
            (4f / 3f) * Math.PI.toFloat() * r * r * r
        } else {
            (he.x * 2f) * (he.y * 2f) * (he.z * 2f)
        }
        val computedMass = (volume * materialSpec.densityKgM3 * 0.02f).coerceIn(0.45f, 1200f)
        mass = massOverride?.coerceIn(0.2f, 2500f) ?: computedMass

        if (isFrozen) {
            invMass = 0f
            localInvInertiaDiag = Vec3.ZERO
        } else {
            invMass = 1f / mass
            localInvInertiaDiag = if (type.colliderType == ColliderShapeType.SPHERE) {
                val r = effectiveRadius
                val inertia = (2f / 5f) * mass * r * r
                val invI = 1f / inertia.coerceAtLeast(0.01f)
                Vec3(invI, invI, invI)
            } else {
                val sx = he.x * 2f
                val sy = he.y * 2f
                val sz = he.z * 2f
                val ix = (1f / 12f) * mass * (sy * sy + sz * sz)
                val iy = (1f / 12f) * mass * (sx * sx + sz * sz)
                val iz = (1f / 12f) * mass * (sx * sx + sy * sy)
                Vec3(
                    1f / ix.coerceAtLeast(0.01f),
                    1f / iy.coerceAtLeast(0.01f),
                    1f / iz.coerceAtLeast(0.01f)
                )
            }
        }
        updateWorldInertia()
    }

    fun updateWorldInertia() {
        if (isFrozen) {
            invInertiaWorld = Mat3(FloatArray(9))
            return
        }
        val rotMat = orientation.toMat3()
        invInertiaWorld = Mat3.computeWorldInvInertia(rotMat, localInvInertiaDiag)
    }

    fun setFrozenState(frozen: Boolean) {
        isFrozen = frozen
        if (frozen) {
            linearVelocity = Vec3.ZERO
            angularVelocity = Vec3.ZERO
            forceAccum = Vec3.ZERO
            torqueAccum = Vec3.ZERO
        }
        wake()
        recomputeMassProperties()
    }

    fun wake() {
        isSleeping = false
        sleepTimer = 0f
    }

    fun applyImpulseAtPoint(impulse: Vec3, worldPoint: Vec3) {
        if (isFrozen || invMass == 0f) return
        wake()
        linearVelocity += impulse * invMass
        val r = worldPoint - position
        angularVelocity += invInertiaWorld.multiplyVec(r.cross(impulse))
    }

    fun applyForce(force: Vec3) {
        if (isFrozen || invMass == 0f) return
        wake()
        forceAccum += force
    }
}

enum class MountType {
    FLOOR,
    SURFACE,
    WALL,
    CEILING,
    STRUCTURAL
}

/** Static architectural collider & visual piece in a map. */
data class StaticProp(
    val id: Int,
    val meshId: MeshId,
    val position: Vec3,
    val orientation: Quat = Quat.IDENTITY,
    val halfExtents: Vec3,
    val material: PhysicsMaterial,
    val isCollider: Boolean = true,
    val conveyorVelocity: Vec3 = Vec3.ZERO,
    val colorOverride: Vec3? = null,
    val alphaOverride: Float? = null,
    val label: String = "",
    val mountType: MountType = MountType.FLOOR,
    val emissiveColor: Vec3 = Vec3.ZERO,
    val emissiveIntensity: Float = 0f,
    val isVisible: Boolean = true
)

enum class ConstraintType {
    WELD,
    SPRING,
    ROPE,
    HINGE
}

data class PhysicsConstraint(
    val id: Int,
    val type: ConstraintType,
    val bodyAId: Int,
    val bodyBId: Int, // -1 means attached to static world anchor
    val localAnchorA: Vec3,
    val localAnchorB: Vec3,
    val restLength: Float = 1.5f,
    val stiffness: Float = 180f,
    val damping: Float = 12f,
    val hingeAxisWorld: Vec3 = Vec3.UP,
    val relativeOrientation: Quat = Quat.IDENTITY
)
