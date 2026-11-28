package com.example.world

import com.example.electrical.WireConnection
import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.ConstraintType
import com.example.physics.MapId
import com.example.physics.MeshId
import com.example.physics.MountType
import com.example.physics.ObjectType
import com.example.physics.PhysicsConstraint
import com.example.physics.PhysicsMaterial
import com.example.physics.RigidBody
import com.example.physics.StaticProp

data class BuiltMap(
    val statics: List<StaticProp>,
    val dynamics: List<RigidBody>,
    val constraints: List<PhysicsConstraint>,
    val spawnPos: Vec3,
    val spawnYaw: Float,
    val nextId: Int,
    val wires: List<WireConnection> = emptyList()
)

class MapConstructionContext {
    val statics = ArrayList<StaticProp>(128)
    val dynamics = ArrayList<RigidBody>(96)
    val constraints = ArrayList<PhysicsConstraint>(32)
    val wires = ArrayList<WireConnection>(16)
    var nextId = 100

    fun addStatic(
        meshId: MeshId,
        pos: Vec3,
        halfExtents: Vec3,
        material: PhysicsMaterial,
        rot: Quat = Quat.IDENTITY,
        isCollider: Boolean = true,
        conveyorVel: Vec3 = Vec3.ZERO,
        colorOverride: Vec3? = null,
        alphaOverride: Float? = null,
        label: String = "",
        mountType: MountType = MountType.FLOOR,
        emissiveColor: Vec3 = Vec3.ZERO,
        emissiveIntensity: Float = 0f,
        isVisible: Boolean = true
    ): Int {
        val id = nextId++
        statics.add(
            StaticProp(
                id = id,
                meshId = meshId,
                position = pos,
                orientation = rot,
                halfExtents = halfExtents,
                material = material,
                isCollider = isCollider,
                conveyorVelocity = conveyorVel,
                colorOverride = colorOverride,
                alphaOverride = alphaOverride,
                label = label,
                mountType = mountType,
                emissiveColor = emissiveColor,
                emissiveIntensity = emissiveIntensity,
                isVisible = isVisible
            )
        )
        return id
    }

    fun addDynamic(
        type: ObjectType,
        pos: Vec3,
        rot: Quat = Quat.IDENTITY,
        material: PhysicsMaterial = type.defaultMaterial,
        scale: Float = 1.0f,
        frozen: Boolean = false
    ): RigidBody {
        val rb = RigidBody(
            id = nextId++,
            type = type,
            material = material,
            position = pos,
            orientation = rot,
            scale = scale,
            isFrozen = frozen
        )
        dynamics.add(rb)
        return rb
    }

    fun addConstraint(
        type: ConstraintType,
        bodyA: RigidBody,
        bodyB: RigidBody?,
        localAnchorA: Vec3,
        localAnchorBWorldOrLocal: Vec3,
        restLength: Float = 2.0f,
        stiffness: Float = 220f,
        damping: Float = 14f,
        hingeAxis: Vec3 = Vec3.UP
    ) {
        constraints.add(
            PhysicsConstraint(
                id = nextId++,
                type = type,
                bodyAId = bodyA.id,
                bodyBId = bodyB?.id ?: -1,
                localAnchorA = localAnchorA,
                localAnchorB = localAnchorBWorldOrLocal,
                restLength = restLength,
                stiffness = stiffness,
                damping = damping,
                hingeAxisWorld = hingeAxis
            )
        )
    }

    fun addWire(bodyA: RigidBody, bodyB: RigidBody) {
        wires.add(WireConnection(nextId++, bodyA.id, bodyB.id))
    }

    fun addPerimeterWalls(halfSize: Float, height: Float, material: PhysicsMaterial, wallColor: Vec3? = null) {
        val t = 0.6f
        val hy = height * 0.5f
        addStatic(MeshId.WALL_PANEL, Vec3(0f, hy, -halfSize), Vec3(halfSize, hy, t), material, colorOverride = wallColor, mountType = MountType.STRUCTURAL)
        addStatic(MeshId.WALL_PANEL, Vec3(0f, hy, halfSize), Vec3(halfSize, hy, t), material, colorOverride = wallColor, mountType = MountType.STRUCTURAL)
        addStatic(MeshId.WALL_PANEL, Vec3(-halfSize, hy, 0f), Vec3(t, hy, halfSize), material, colorOverride = wallColor, mountType = MountType.STRUCTURAL)
        addStatic(MeshId.WALL_PANEL, Vec3(halfSize, hy, 0f), Vec3(t, hy, halfSize), material, colorOverride = wallColor, mountType = MountType.STRUCTURAL)
    }
}

object MapBuilder {

    fun buildMenuDemoScene(): BuiltMap {
        val c = MapConstructionContext()
        // Architectural testing bay backdrop for Main Menu
        c.addStatic(
            MeshId.FLOOR_SLAB,
            Vec3(0f, -0.5f, 0f),
            Vec3(18f, 0.5f, 18f),
            PhysicsMaterial.CONCRETE,
            colorOverride = Vec3(0.22f, 0.25f, 0.29f),
            mountType = MountType.STRUCTURAL
        )
        // Rear architectural wall & acoustic bay framing
        c.addStatic(
            MeshId.WALL_PANEL,
            Vec3(0f, 4.5f, -7.2f),
            Vec3(16f, 4.5f, 0.45f),
            PhysicsMaterial.CONCRETE,
            colorOverride = Vec3(0.27f, 0.30f, 0.35f),
            mountType = MountType.STRUCTURAL
        )
        c.addStatic(
            MeshId.GLASS_PARTITION,
            Vec3(-6.2f, 2.4f, -4.8f),
            Vec3(2.8f, 2.4f, 0.12f),
            PhysicsMaterial.GLASS,
            alphaOverride = 0.36f,
            mountType = MountType.STRUCTURAL
        )
        c.addStatic(
            MeshId.ROOF_TRUSS,
            Vec3(0f, 6.8f, -1.2f),
            Vec3(9.5f, 0.45f, 0.45f),
            PhysicsMaterial.STEEL,
            colorOverride = Vec3(0.32f, 0.36f, 0.41f),
            mountType = MountType.STRUCTURAL
        )
        c.placeCeilingLight(x = -3.2f, z = -1.2f, ceilingBottomY = 6.35f, halfX = 1.1f, halfZ = 0.25f)
        c.placeCeilingLight(x = 3.2f, z = -1.2f, ceilingBottomY = 6.35f, halfX = 1.1f, halfZ = 0.25f)

        // Precision test gantry
        c.placeStaticOnFloor(MeshId.PILLAR_COLUMN, -4.2f, -1.2f, Vec3(0.24f, 3.15f, 0.24f), PhysicsMaterial.STEEL, colorOverride = Vec3(0.30f, 0.34f, 0.39f))
        c.placeStaticOnFloor(MeshId.PILLAR_COLUMN, 4.2f, -1.2f, Vec3(0.24f, 3.15f, 0.24f), PhysicsMaterial.STEEL, colorOverride = Vec3(0.30f, 0.34f, 0.39f))
        c.addStatic(
            MeshId.STEEL_IBEAM,
            Vec3(0f, 6.55f, -1.2f),
            Vec3(4.5f, 0.25f, 0.28f),
            PhysicsMaterial.STEEL,
            colorOverride = Vec3(0.34f, 0.38f, 0.43f),
            mountType = MountType.STRUCTURAL
        )

        // Offset workbench & instrumentation to keep center-left UI rail legible
        val tableId = c.placeStaticOnFloor(
            MeshId.LAB_TABLE, 5.4f, -3.2f,
            Vec3(1.5f, 0.48f, 0.75f),
            PhysicsMaterial.STEEL,
            colorOverride = Vec3(0.26f, 0.29f, 0.34f)
        )
        c.placeDynamicOnSupport(ObjectType.CENTRIFUGE_POD, 5.4f, -3.2f, tableId)

        // Swinging precision pendulum & suspended gyro
        val ball1 = c.addDynamic(ObjectType.STEEL_SPHERE, Vec3(-1.4f, 3.8f, -1.2f), scale = 1.2f)
        ball1.linearVelocity = Vec3(3.4f, 0f, 0.25f)
        c.addConstraint(ConstraintType.ROPE, ball1, null, Vec3(0f, 0.45f, 0f), Vec3(0.6f, 6.28f, -1.2f), restLength = 3.5f)

        val gyro = c.addDynamic(ObjectType.COPPER_GYRO, Vec3(2.8f, 3.1f, -1.2f), scale = 1.15f)
        c.addConstraint(ConstraintType.SPRING, gyro, null, Vec3(0f, 0.48f, 0f), Vec3(2.8f, 6.28f, -1.2f), restLength = 2.4f, stiffness = 150f)

        // Grounded physics calibration blocks on right stage
        val baseY = 0f
        c.placeDynamicOnFloor(ObjectType.STEEL_CRATE, 1.4f, 0.8f, floorY = baseY)
        c.placeDynamicOnFloor(ObjectType.STEEL_CRATE, 2.65f, 0.8f, floorY = baseY)
        c.addDynamic(ObjectType.GLASS_CUBE, Vec3(2.02f, 1.60f, 0.8f))
        c.placeDynamicOnFloor(ObjectType.CONCRETE_BLOCK, 3.9f, 1.1f, floorY = baseY)
        c.placeDynamicOnFloor(ObjectType.BOUNCY_SPHERE, 0.2f, 1.8f, floorY = baseY)

        return BuiltMap(c.statics, c.dynamics, c.constraints, Vec3(0f, 1.5f, 9f), 0f, c.nextId)
    }

    fun buildMap(mapId: MapId): BuiltMap {
        return when (mapId) {
            MapId.LAB -> MapEnvironments.buildLab()
            MapId.FACTORY -> MapEnvironments.buildFactory()
            MapId.WAREHOUSE -> MapEnvironments.buildWarehouse()
            MapId.CONSTRUCTION -> MapEnvironments.buildConstruction()
            MapId.FACILITY -> MapEnvironments.buildFacility()
            MapId.UNDERGROUND_MAINTENANCE -> MapEnvironments.buildUndergroundMaintenance()
            MapId.OUTDOOR_TESTING_YARD -> MapEnvironments.buildOutdoorTestingYard()
        }
    }
}
