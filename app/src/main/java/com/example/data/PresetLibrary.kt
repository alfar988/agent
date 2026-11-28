package com.example.data

import com.example.electrical.WireConnection
import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.ConstraintType
import com.example.physics.MapId
import com.example.physics.ObjectType
import com.example.physics.PhysicsConstraint
import com.example.physics.PhysicsMaterial
import com.example.physics.SavedBodyData
import com.example.physics.SessionExportData

data class BuiltInPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val recommendedMap: MapId,
    val build: () -> SessionExportData
)

object PresetLibrary {

    val all: List<BuiltInPreset> = listOf(
        BuiltInPreset(
            id = "NEWTON_PENDULUM",
            title = "Newton Pendulum Array",
            subtitle = "Five suspended chrome spheres demonstrating momentum transfer",
            recommendedMap = MapId.LAB,
            build = ::buildNewtonPendulum
        ),
        BuiltInPreset(
            id = "DC_MOTOR_FUSE_LAB",
            title = "DC Motor & Fuse Lab",
            subtitle = "24V panel, toggle switch, 3A fuse, drive motor, and indicator LEDs",
            recommendedMap = MapId.FACILITY,
            build = ::buildDcMotorFuseLab
        ),
        BuiltInPreset(
            id = "CONVEYOR_SORTATION",
            title = "Conveyor Sortation Line",
            subtitle = "Mixed density crates & tires feeding onto factory conveyors",
            recommendedMap = MapId.FACTORY,
            build = ::buildConveyorSortation
        ),
        BuiltInPreset(
            id = "SUSPENSION_ROVER",
            title = "Suspension Rover Chassis",
            subtitle = "Bogie platform linked via spring dampers and four rubber tires",
            recommendedMap = MapId.FACILITY,
            build = ::buildSuspensionRover
        ),
        BuiltInPreset(
            id = "DOMINO_CHAIN",
            title = "Domino Chain Reaction",
            subtitle = "Curved marble monolith cascade triggered by a steel pendulum",
            recommendedMap = MapId.WAREHOUSE,
            build = ::buildDominoChain
        ),
        BuiltInPreset(
            id = "BUOYANCY_TANK",
            title = "Hydrostatic Buoyancy Comparison",
            subtitle = "Cork, oak, rubber, ice, aluminum, and tungsten comparison blocks",
            recommendedMap = MapId.LAB,
            build = ::buildBuoyancyComparison
        )
    )

    private fun body(
        id: Int,
        type: ObjectType,
        pos: Vec3,
        matId: String = type.defaultMaterialId,
        frozen: Boolean = false,
        vel: Vec3 = Vec3.ZERO,
        scale: Float = 1f,
        label: String = type.displayName
    ) = SavedBodyData(
        id = id,
        type = type,
        material = type.defaultMaterial,
        position = pos,
        orientation = Quat.IDENTITY,
        linearVelocity = vel,
        angularVelocity = Vec3.ZERO,
        scale = scale,
        isFrozen = frozen,
        materialId = matId,
        customLabel = label
    )

    private fun buildNewtonPendulum(): SessionExportData {
        val bodies = ArrayList<SavedBodyData>()
        val constraints = ArrayList<PhysicsConstraint>()
        var nextId = 500
        for (i in 0 until 5) {
            val x = (i - 2) * 0.94f
            val bid = nextId++
            val pos = if (i == 0) Vec3(x - 2.4f, 3.6f, 0f) else Vec3(x, 2.2f, 0f)
            bodies.add(body(bid, ObjectType.STEEL_SPHERE, pos, matId = "CHROME", label = "Chrome Sphere ${i + 1}"))
            constraints.add(
                PhysicsConstraint(
                    id = nextId++,
                    type = ConstraintType.ROPE,
                    bodyAId = bid,
                    bodyBId = -1,
                    localAnchorA = Vec3(0f, 0.45f, 0f),
                    localAnchorB = Vec3(x, 5.2f, 0f),
                    restLength = 2.6f
                )
            )
        }
        return SessionExportData(MapId.LAB, Vec3(0f, 0.2f, 7.5f), 0f, -0.05f, bodies, constraints)
    }

    private fun buildDcMotorFuseLab(): SessionExportData {
        val bodies = listOf(
            body(601, ObjectType.ELECTRICAL_PANEL, Vec3(-2.4f, 0.7f, 0f), frozen = true),
            body(602, ObjectType.SWITCH_BOX, Vec3(-1.2f, 0.3f, 0f)),
            body(603, ObjectType.FUSE_BLOCK, Vec3(0.0f, 0.3f, 0f)),
            body(604, ObjectType.DC_MOTOR, Vec3(1.2f, 0.42f, 0f)),
            body(605, ObjectType.LIGHT_BULB_LAMP, Vec3(2.3f, 0.40f, 0f)),
            body(606, ObjectType.LED_INDICATOR, Vec3(3.1f, 0.30f, 0f))
        )
        val wires = listOf(
            WireConnection(650, 601, 602),
            WireConnection(651, 602, 603),
            WireConnection(652, 603, 604),
            WireConnection(653, 603, 605),
            WireConnection(654, 605, 606)
        )
        return SessionExportData(MapId.FACILITY, Vec3(0.4f, 0.2f, 5.5f), 0f, -0.10f, bodies, emptyList(), wires)
    }

    private fun buildConveyorSortation(): SessionExportData {
        val bodies = listOf(
            body(701, ObjectType.WOOD_CRATE, Vec3(-8.5f, 2.0f, -3.5f), matId = "PINE"),
            body(702, ObjectType.SOFT_FOAM_BLOCK, Vec3(-6.2f, 2.0f, -3.5f), matId = "FOAM"),
            body(703, ObjectType.STEEL_CRATE, Vec3(-4.0f, 2.0f, -3.5f), matId = "TUNGSTEN"),
            body(704, ObjectType.GLASS_CUBE, Vec3(-1.8f, 2.0f, -3.5f), matId = "TEMPERED_GLASS"),
            body(705, ObjectType.RUBBER_TIRE, Vec3(7.2f, 2.0f, -2.5f), matId = "NATURAL_RUBBER")
        )
        return SessionExportData(MapId.FACTORY, Vec3(0f, 0.2f, 7.5f), 0f, -0.08f, bodies, emptyList())
    }

    private fun buildSuspensionRover(): SessionExportData {
        val chassis = body(801, ObjectType.SUSPENSION_BOGIE, Vec3(0f, 1.5f, 0f), matId = "ALUMINUM", label = "Rover Chassis")
        val w1 = body(802, ObjectType.RUBBER_TIRE, Vec3(-1.0f, 0.7f, -0.8f), matId = "NEOPRENE")
        val w2 = body(803, ObjectType.RUBBER_TIRE, Vec3(1.0f, 0.7f, -0.8f), matId = "NEOPRENE")
        val w3 = body(804, ObjectType.RUBBER_TIRE, Vec3(-1.0f, 0.7f, 0.8f), matId = "NEOPRENE")
        val w4 = body(805, ObjectType.RUBBER_TIRE, Vec3(1.0f, 0.7f, 0.8f), matId = "NEOPRENE")
        val constraints = listOf(
            PhysicsConstraint(820, ConstraintType.SPRING, 801, 802, Vec3(-0.7f, -0.2f, -0.5f), Vec3.ZERO, restLength = 0.8f, stiffness = 190f),
            PhysicsConstraint(821, ConstraintType.SPRING, 801, 803, Vec3(0.7f, -0.2f, -0.5f), Vec3.ZERO, restLength = 0.8f, stiffness = 190f),
            PhysicsConstraint(822, ConstraintType.SPRING, 801, 804, Vec3(-0.7f, -0.2f, 0.5f), Vec3.ZERO, restLength = 0.8f, stiffness = 190f),
            PhysicsConstraint(823, ConstraintType.SPRING, 801, 805, Vec3(0.7f, -0.2f, 0.5f), Vec3.ZERO, restLength = 0.8f, stiffness = 190f)
        )
        return SessionExportData(MapId.FACILITY, Vec3(0f, 0.2f, 6.0f), 0f, -0.10f, listOf(chassis, w1, w2, w3, w4), constraints)
    }

    private fun buildDominoChain(): SessionExportData {
        val bodies = ArrayList<SavedBodyData>()
        var nextId = 900
        for (i in 0 until 10) {
            bodies.add(body(nextId++, ObjectType.DOMINO_SLAB, Vec3(-4.0f + i * 0.92f, 0.92f, 0f), matId = "MARBLE", label = "Domino ${i + 1}"))
        }
        bodies.add(body(nextId, ObjectType.STEEL_SPHERE, Vec3(-5.6f, 1.3f, 0f), matId = "GOLD", vel = Vec3(6.2f, 0f, 0f), label = "Gold Striker"))
        return SessionExportData(MapId.WAREHOUSE, Vec3(0f, 0.2f, 7.5f), 0f, -0.06f, bodies, emptyList())
    }

    private fun buildBuoyancyComparison(): SessionExportData {
        val mats = listOf("CORK", "OAK", "FOAM", "ICE", "ALUMINUM", "TUNGSTEN")
        val bodies = mats.mapIndexed { idx, matId ->
            body(
                id = 1000 + idx,
                type = if (matId == "CORK") ObjectType.BUOYANCY_FLOAT else if (matId == "FOAM") ObjectType.SOFT_FOAM_BLOCK else ObjectType.STEEL_CRATE,
                pos = Vec3(-3.5f + idx * 1.4f, 1.8f, 0f),
                matId = matId,
                label = "$matId Test Cube"
            )
        }
        return SessionExportData(MapId.LAB, Vec3(0f, 0.4f, 7.2f), 0f, -0.12f, bodies, emptyList())
    }
}
