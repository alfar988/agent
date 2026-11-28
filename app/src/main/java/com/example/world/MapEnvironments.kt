package com.example.world

import com.example.math.Vec3
import com.example.physics.ConstraintType
import com.example.physics.MeshId
import com.example.physics.MountType
import com.example.physics.ObjectType
import com.example.physics.PhysicsMaterial
import kotlin.math.PI

object MapEnvironments {

    // MAP 01 — MODERN SCIENCE LABORATORY
    fun buildLab(): BuiltMap {
        val c = MapConstructionContext()
        val floorColor = Vec3(0.42f, 0.45f, 0.49f)
        val wallColor = Vec3(0.54f, 0.57f, 0.61f)
        val trimColor = Vec3(0.20f, 0.23f, 0.28f)
        val benchColor = Vec3(0.20f, 0.24f, 0.30f)

        c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(0f, -0.5f, 0f), Vec3(19f, 0.5f, 19f),
            PhysicsMaterial.CONCRETE, colorOverride = floorColor, mountType = MountType.STRUCTURAL
        )
        c.addPerimeterWalls(19f, 7.2f, PhysicsMaterial.CONCRETE, wallColor)

        // Overhead structural trusses, HVAC diffusers & recessed linear ceiling lighting
        for (z in listOf(-12.5f, -5.0f, 3.0f, 12.0f)) {
            c.addStatic(
                MeshId.ROOF_TRUSS, Vec3(0f, 6.85f, z), Vec3(18.4f, 0.35f, 0.35f),
                PhysicsMaterial.STEEL, colorOverride = Vec3(0.26f, 0.29f, 0.34f), mountType = MountType.STRUCTURAL
            )
            c.addStatic(
                MeshId.CEILING_VENT, Vec3(0f, 6.32f, z), Vec3(0.95f, 0.18f, 0.70f),
                PhysicsMaterial.STEEL, isCollider = false, colorOverride = Vec3(0.32f, 0.35f, 0.40f), mountType = MountType.CEILING
            )
            for (lx in listOf(-13.5f, -4.0f, 4.0f, 13.5f)) {
                c.placeCeilingLight(x = lx, z = z, ceilingBottomY = 6.50f, emissiveIntensity = 0.42f)
            }
        }

        // Reception partition wall (z = 9.0) with central doorway into main chamber
        c.addWallWithDoorway(
            isAlongX = true, centerX = 0f, centerZ = 9.0f, halfLength = 8.4f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 6.5f, doorCenterOffset = 0f, doorHalfWidth = 1.65f, doorHeight = 2.8f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = trimColor
        )
        val receptionDesk = c.placeStaticOnFloor(
            MeshId.LAB_CONSOLE, -4.6f, 13.2f, Vec3(1.55f, 0.55f, 0.65f),
            PhysicsMaterial.STEEL, colorOverride = benchColor, label = "Reception Desk"
        )
        c.placeDynamicOnFloor(ObjectType.OFFICE_CHAIR, -4.6f, 15.0f)
        c.placeDynamicOnSupport(ObjectType.LED_INDICATOR, -4.1f, 13.2f, receptionDesk)

        // West Wing wall (x = -8.5): Prep Room (north) & Storage Room (south)
        c.addWallWithWindow(
            isAlongX = false, centerX = -8.5f, centerZ = -13.0f, halfLength = 5.4f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 6.5f, winCenterOffset = 0f, winHalfWidth = 2.1f,
            sillHeight = 1.0f, winTopHeight = 2.8f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = trimColor
        )
        c.addWallWithDoorway(
            isAlongX = false, centerX = -8.5f, centerZ = -3.8f, halfLength = 3.8f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 6.5f, doorCenterOffset = -1.2f, doorHalfWidth = 1.3f, doorHeight = 2.8f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = trimColor
        )
        c.addWallWithDoorway(
            isAlongX = false, centerX = -8.5f, centerZ = 9.2f, halfLength = 9.2f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 6.5f, doorCenterOffset = -3.0f, doorHalfWidth = 1.35f, doorHeight = 2.8f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = trimColor
        )
        // Divider between Prep & Storage (z = 0.0, x in -18.4..-8.74)
        c.addWallWithDoorway(
            isAlongX = true, centerX = -13.6f, centerZ = 0.0f, halfLength = 4.85f, halfThickness = 0.22f,
            baseY = 0f, wallHeight = 6.5f, doorCenterOffset = 0f, doorHalfWidth = 1.3f, doorHeight = 2.8f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = trimColor
        )

        // Prep Room Equipment
        val prepTable = c.placeStaticOnFloor(
            MeshId.LAB_TABLE, -13.6f, -11.5f, Vec3(1.85f, 0.48f, 0.85f),
            PhysicsMaterial.STEEL, colorOverride = benchColor, label = "Prep Lab Table"
        )
        c.placeDynamicOnSupport(ObjectType.CENTRIFUGE_POD, -14.4f, -11.5f, prepTable)
        c.placeDynamicOnSupport(ObjectType.GLASS_CUBE, -12.7f, -11.5f, prepTable)
        c.placeWallMounted(
            MeshId.WALL_CABINET, wallCenterX = -19.0f, wallCenterZ = -11.5f, wallHalfThickness = 0.6f,
            mountBottomY = 1.45f, inwardDirX = 1f, inwardDirZ = 0f,
            halfExtents = Vec3(1.35f, 0.75f, 0.32f), material = PhysicsMaterial.STEEL,
            colorOverride = Vec3(0.74f, 0.78f, 0.82f), label = "Prep Wall Cabinet"
        )

        // Storage Room Shelving & Containers
        c.placeStaticOnFloor(
            MeshId.SHELF_RACK, -16.8f, 5.5f, Vec3(0.62f, 1.45f, 2.2f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.36f, 0.40f, 0.45f), label = "Lab Shelf A"
        )
        c.placeStaticOnFloor(
            MeshId.SHELF_RACK, -16.8f, 11.8f, Vec3(0.62f, 1.45f, 2.2f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.36f, 0.40f, 0.45f), label = "Lab Shelf B"
        )
        c.placeDynamicOnFloor(ObjectType.STEEL_CRATE, -12.2f, 13.0f)
        c.placeDynamicOnFloor(ObjectType.SOFT_FOAM_BLOCK, -10.8f, 13.0f)

        // East Wing wall (x = 8.5): Observation Room (north) & Electrical Control Room (south)
        c.addWallWithWindow(
            isAlongX = false, centerX = 8.5f, centerZ = -12.2f, halfLength = 6.2f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 6.5f, winCenterOffset = 0f, winHalfWidth = 3.2f,
            sillHeight = 0.95f, winTopHeight = 2.95f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = trimColor
        )
        c.addWallWithDoorway(
            isAlongX = false, centerX = 8.5f, centerZ = -4.0f, halfLength = 2.0f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 6.5f, doorCenterOffset = 0f, doorHalfWidth = 1.25f, doorHeight = 2.8f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = trimColor
        )
        c.addWallWithDoorway(
            isAlongX = false, centerX = 8.5f, centerZ = 8.2f, halfLength = 10.2f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 6.5f, doorCenterOffset = -2.2f, doorHalfWidth = 1.35f, doorHeight = 2.8f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = trimColor
        )
        // Divider between Observation & Electrical Control Room (z = -2.0)
        c.addWallWithDoorway(
            isAlongX = true, centerX = 13.6f, centerZ = -2.0f, halfLength = 4.85f, halfThickness = 0.22f,
            baseY = 0f, wallHeight = 6.5f, doorCenterOffset = 0f, doorHalfWidth = 1.3f, doorHeight = 2.8f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = trimColor
        )

        // Observation Room Consoles
        c.placeStaticOnFloor(
            MeshId.LAB_CONSOLE, 10.6f, -12.0f, Vec3(0.65f, 0.58f, 2.1f),
            PhysicsMaterial.STEEL,
            colorOverride = benchColor, label = "Observation Console"
        )
        c.placeDynamicOnFloor(ObjectType.OFFICE_CHAIR, 12.6f, -12.0f)

        // Electrical Control Room: Wall Cabinet + Wired Lab Circuit
        c.placeWallMounted(
            MeshId.WALL_CABINET, wallCenterX = 19.0f, wallCenterZ = 6.0f, wallHalfThickness = 0.6f,
            mountBottomY = 1.45f, inwardDirX = -1f, inwardDirZ = 0f,
            halfExtents = Vec3(1.35f, 0.75f, 0.32f), material = PhysicsMaterial.STEEL,
            colorOverride = Vec3(0.32f, 0.36f, 0.42f), label = "Electrical Panel Cabinet"
        )
        val elecTable = c.placeStaticOnFloor(
            MeshId.LAB_TABLE, 13.6f, 6.0f, Vec3(1.9f, 0.48f, 0.85f),
            PhysicsMaterial.STEEL, colorOverride = benchColor, label = "Electrical Test Bench"
        )
        val battery = c.placeDynamicOnSupport(ObjectType.BATTERY_UNIT, 12.2f, 6.0f, elecTable)
        val sw = c.placeDynamicOnSupport(ObjectType.SWITCH_BOX, 13.1f, 6.0f, elecTable)
        val fuse = c.placeDynamicOnSupport(ObjectType.FUSE_BLOCK, 14.0f, 6.0f, elecTable)
        val bulb = c.placeDynamicOnSupport(ObjectType.LIGHT_BULB_LAMP, 14.9f, 6.0f, elecTable)
        c.addWire(battery, sw)
        c.addWire(sw, fuse)
        c.addWire(fuse, bulb)

        // Central Testing Chamber: Apparatus Gantry & Material Bench
        c.placeStaticOnFloor(MeshId.PILLAR_COLUMN, -3.4f, -5.0f, Vec3(0.22f, 3.1f, 0.22f), PhysicsMaterial.STEEL, mountType = MountType.STRUCTURAL)
        c.placeStaticOnFloor(MeshId.PILLAR_COLUMN, 3.4f, -5.0f, Vec3(0.22f, 3.1f, 0.22f), PhysicsMaterial.STEEL, mountType = MountType.STRUCTURAL)
        c.addStatic(
            MeshId.STEEL_IBEAM, Vec3(0f, 6.2f, -5.0f), Vec3(3.8f, 0.24f, 0.26f),
            PhysicsMaterial.STEEL, mountType = MountType.STRUCTURAL
        )
        val pendulumBall = c.addDynamic(ObjectType.STEEL_SPHERE, Vec3(-2.1f, 3.8f, -5.0f), scale = 1.15f)
        c.addConstraint(ConstraintType.ROPE, pendulumBall, null, Vec3(0f, 0.45f, 0f), Vec3(-0.6f, 5.95f, -5.0f), restLength = 3.1f)
        val springGyro = c.addDynamic(ObjectType.COPPER_GYRO, Vec3(1.6f, 3.4f, -5.0f))
        c.addConstraint(ConstraintType.SPRING, springGyro, null, Vec3(0f, 0.5f, 0f), Vec3(1.6f, 5.95f, -5.0f), restLength = 2.0f, stiffness = 170f)

        val centerTable = c.placeStaticOnFloor(
            MeshId.LAB_TABLE, 0f, -12.5f, Vec3(1.8f, 0.48f, 0.8f),
            PhysicsMaterial.STEEL, colorOverride = benchColor, label = "Central Apparatus Bench"
        )
        c.placeDynamicOnSupport(ObjectType.BUOYANCY_FLOAT, -0.7f, -12.5f, centerTable)
        c.placeDynamicOnSupport(ObjectType.SPRING_PISTON, 0.7f, -12.5f, centerTable)

        val crateBase = c.placeDynamicOnFloor(ObjectType.STEEL_CRATE, -2.5f, 2.0f)
        c.placeDynamicOnBody(ObjectType.STEEL_CRATE, -2.5f, 2.0f, crateBase)
        c.placeDynamicOnFloor(ObjectType.BOUNCY_SPHERE, -4.5f, 2.4f)
        c.placeDynamicOnFloor(ObjectType.DOMINO_SLAB, 3.2f, 2.0f)
        c.placeDynamicOnFloor(ObjectType.DOMINO_SLAB, 4.1f, 2.0f)
        c.placeDynamicOnFloor(ObjectType.DOMINO_SLAB, 5.0f, 2.0f)

        return BuiltMap(c.statics, c.dynamics, c.constraints, Vec3(0f, 0.1f, 14.5f), 0f, c.nextId, c.wires)
    }

    // MAP 02 — INDUSTRIAL FACTORY
    fun buildFactory(): BuiltMap {
        val c = MapConstructionContext()
        val floorColor = Vec3(0.30f, 0.32f, 0.35f)
        val wallColor = Vec3(0.28f, 0.31f, 0.35f)
        val machineColor = Vec3(0.26f, 0.32f, 0.36f)
        val trimColor = Vec3(0.20f, 0.22f, 0.26f)

        c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(0f, -0.5f, 0f), Vec3(21f, 0.5f, 21f),
            PhysicsMaterial.CONCRETE, colorOverride = floorColor, mountType = MountType.STRUCTURAL
        )
        c.addPerimeterWalls(21f, 9.6f, PhysicsMaterial.STEEL, wallColor)

        // Structural columns & roof trusses with high-bay ceiling lights
        for (z in listOf(-12f, 0f, 12f)) {
            for (x in listOf(-9.5f, 9.5f)) {
                c.placeStaticOnFloor(
                    MeshId.PILLAR_COLUMN, x, z, Vec3(0.42f, 4.2f, 0.42f),
                    PhysicsMaterial.STEEL, colorOverride = Vec3(0.34f, 0.37f, 0.42f), mountType = MountType.STRUCTURAL
                )
            }
            c.addStatic(
                MeshId.ROOF_TRUSS, Vec3(0f, 8.85f, z), Vec3(20.4f, 0.45f, 0.45f),
                PhysicsMaterial.STEEL, colorOverride = Vec3(0.34f, 0.37f, 0.42f), mountType = MountType.STRUCTURAL
            )
            for (lx in listOf(-14f, -4f, 4f, 14f)) {
                c.placeCeilingLight(x = lx, z = z, ceilingBottomY = 8.40f, halfX = 1.25f, halfZ = 0.32f, emissiveColor = Vec3(1.0f, 0.93f, 0.80f), emissiveIntensity = 0.50f)
            }
        }

        // West Machinery Section: CNC Machining Centers & Hydraulic Press
        c.placeStaticOnFloor(
            MeshId.CNC_MACHINE, -16.2f, -12.5f, Vec3(1.8f, 1.45f, 1.4f),
            PhysicsMaterial.STEEL, rot = EnvironmentPlacer.yawQuat(PI.toFloat() * 0.5f),
            colorOverride = machineColor, label = "CNC Center 01"
        )
        c.placeStaticOnFloor(
            MeshId.CNC_MACHINE, -16.2f, -5.5f, Vec3(1.8f, 1.45f, 1.4f),
            PhysicsMaterial.STEEL, rot = EnvironmentPlacer.yawQuat(PI.toFloat() * 0.5f),
            colorOverride = machineColor, label = "CNC Center 02"
        )
        c.placeStaticOnFloor(
            MeshId.HYDRAULIC_PRESS, -16.2f, 1.5f, Vec3(1.45f, 1.65f, 1.15f),
            PhysicsMaterial.STEEL, rot = EnvironmentPlacer.yawQuat(PI.toFloat() * 0.5f),
            colorOverride = Vec3(0.56f, 0.34f, 0.16f), label = "Hydraulic Press"
        )

        // Dual Connected Motorized Conveyors in Main Production Hall
        val convA = c.placeStaticOnFloor(
            MeshId.CONVEYOR_UNIT, -2.0f, -4.5f, Vec3(6.2f, 0.55f, 1.25f),
            PhysicsMaterial.RUBBER, conveyorVel = Vec3(2.8f, 0f, 0f),
            colorOverride = Vec3(0.18f, 0.20f, 0.23f), label = "Conveyor Line A"
        )
        val convB = c.placeStaticOnFloor(
            MeshId.CONVEYOR_UNIT, 5.75f, 1.6f, Vec3(1.25f, 0.55f, 6.0f),
            PhysicsMaterial.RUBBER, conveyorVel = Vec3(0f, 0f, 2.6f),
            colorOverride = Vec3(0.18f, 0.20f, 0.23f), label = "Conveyor Line B"
        )
        c.placeDynamicOnSupport(ObjectType.STEEL_CRATE, -6.5f, -4.5f, convA)
        c.placeDynamicOnSupport(ObjectType.INDUSTRIAL_BARREL, -3.2f, -4.5f, convA)
        c.placeDynamicOnSupport(ObjectType.WOOD_CRATE, 0.4f, -4.5f, convA)
        c.placeDynamicOnSupport(ObjectType.RUBBER_TIRE, 5.75f, -2.5f, convB)

        // SW Maintenance Bay
        val maintTable = c.placeStaticOnFloor(
            MeshId.LAB_TABLE, -15.2f, 9.5f, Vec3(2.0f, 0.48f, 0.90f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.28f, 0.26f, 0.24f), label = "Maintenance Bench"
        )
        c.placeDynamicOnSupport(ObjectType.CENTRIFUGE_POD, -16.0f, 9.5f, maintTable)
        c.placeDynamicOnSupport(ObjectType.GEAR_WHEEL, -14.3f, 9.5f, maintTable)
        c.placeWallMounted(
            MeshId.WALL_CABINET, wallCenterX = -21.0f, wallCenterZ = 9.5f, wallHalfThickness = 0.6f,
            mountBottomY = 1.5f, inwardDirX = 1f, inwardDirZ = 0f,
            halfExtents = Vec3(1.4f, 0.75f, 0.34f), material = PhysicsMaterial.STEEL,
            colorOverride = Vec3(0.36f, 0.38f, 0.42f), label = "Tool Storage Cabinet"
        )
        val bench = c.placeDynamicOnFloor(ObjectType.LAB_WORKBENCH, -13.5f, 14.5f, frozen = true)
        val panel = c.placeDynamicOnBody(ObjectType.ELECTRICAL_PANEL, -14.1f, 14.5f, bench)
        val motor = c.placeDynamicOnBody(ObjectType.DC_MOTOR, -12.9f, 14.5f, bench)
        c.addWire(panel, motor)

        // SE Storage Area
        c.placeStaticOnFloor(
            MeshId.SHIPPING_CONTAINER, 15.2f, 13.8f, Vec3(2.8f, 1.55f, 1.55f),
            PhysicsMaterial.RUSTED_IRON, colorOverride = Vec3(0.24f, 0.32f, 0.36f), label = "Parts Container"
        )
        c.placeStaticOnFloor(
            MeshId.SHELF_RACK, 18.6f, 6.2f, Vec3(0.75f, 1.65f, 2.8f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.32f, 0.35f, 0.39f), label = "Factory Storage Rack"
        )
        c.placeDynamicOnFloor(ObjectType.STEEL_IBEAM, -2.0f, 6.5f)
        c.placeDynamicOnFloor(ObjectType.STEEL_SPHERE, 1.5f, 6.5f, scale = 1.25f)

        // NE Elevated Control Room Mezzanine (x in 11..19.5, z in -19.5..-9.5) + Staircase
        c.placeStaticOnFloor(MeshId.PILLAR_COLUMN, 11.4f, -18.8f, Vec3(0.28f, 1.7f, 0.28f), PhysicsMaterial.STEEL, mountType = MountType.STRUCTURAL)
        c.placeStaticOnFloor(MeshId.PILLAR_COLUMN, 19.0f, -18.8f, Vec3(0.28f, 1.7f, 0.28f), PhysicsMaterial.STEEL, mountType = MountType.STRUCTURAL)
        c.placeStaticOnFloor(MeshId.PILLAR_COLUMN, 11.4f, -10.2f, Vec3(0.28f, 1.7f, 0.28f), PhysicsMaterial.STEEL, mountType = MountType.STRUCTURAL)
        c.placeStaticOnFloor(MeshId.PILLAR_COLUMN, 19.0f, -10.2f, Vec3(0.28f, 1.7f, 0.28f), PhysicsMaterial.STEEL, mountType = MountType.STRUCTURAL)
        val deckId = c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(15.2f, 3.65f, -14.5f), Vec3(4.3f, 0.25f, 4.9f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.32f, 0.35f, 0.39f), mountType = MountType.STRUCTURAL
        )
        // Safety glass rail on West & South edges of deck
        c.placeStaticOnSupport(
            MeshId.GLASS_PARTITION, 11.0f, -14.5f, deckId, Vec3(0.08f, 0.65f, 4.6f),
            PhysicsMaterial.GLASS, alphaOverride = 0.42f, colorOverride = Vec3(0.76f, 0.88f, 0.94f)
        )
        c.placeStaticOnSupport(
            MeshId.LAB_CONSOLE, 15.2f, -15.5f, deckId, Vec3(1.8f, 0.55f, 0.7f),
            PhysicsMaterial.STEEL, colorOverride = trimColor, label = "Factory Control Console"
        )
        c.addStaircaseToDeck(
            centerX = 14.5f, centerZ = -6.0f, floorY = 0f, topY = 3.90f,
            halfWidth = 1.15f, halfRun = 3.6f, ascendYaw = 0f
        )

        return BuiltMap(c.statics, c.dynamics, c.constraints, Vec3(0f, 0.1f, 15.0f), 0f, c.nextId, c.wires)
    }

    // MAP 03 — ABANDONED WAREHOUSE
    fun buildWarehouse(): BuiltMap {
        val c = MapConstructionContext()
        val floorColor = Vec3(0.36f, 0.35f, 0.33f)
        val wallColor = Vec3(0.38f, 0.36f, 0.34f)
        val trimColor = Vec3(0.25f, 0.23f, 0.22f)

        c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(0f, -0.5f, 0f), Vec3(21f, 0.5f, 21f),
            PhysicsMaterial.CONCRETE, colorOverride = floorColor, mountType = MountType.STRUCTURAL
        )
        c.addPerimeterWalls(21f, 8.8f, PhysicsMaterial.CONCRETE, wallColor)

        for (z in listOf(-10f, 0f, 10f)) {
            for (x in listOf(-8.5f, 8.5f)) {
                c.placeStaticOnFloor(
                    MeshId.PILLAR_COLUMN, x, z, Vec3(0.36f, 4.0f, 0.36f),
                    PhysicsMaterial.RUSTED_IRON, mountType = MountType.STRUCTURAL
                )
            }
            c.addStatic(
                MeshId.ROOF_TRUSS, Vec3(0f, 8.15f, z), Vec3(20.4f, 0.40f, 0.40f),
                PhysicsMaterial.RUSTED_IRON, mountType = MountType.STRUCTURAL
            )
            c.placeCeilingLight(x = -4.5f, z = z, ceilingBottomY = 7.75f, emissiveColor = Vec3(0.95f, 0.82f, 0.64f), emissiveIntensity = 0.35f)
            c.placeCeilingLight(x = 4.5f, z = z, ceilingBottomY = 7.75f, emissiveColor = Vec3(0.95f, 0.82f, 0.64f), emissiveIntensity = 0.35f)
        }

        // NW Corner Office Section (x in -20.4..-9.2, z in -20.4..-8.2)
        c.addWallWithDoorway(
            isAlongX = false, centerX = -9.2f, centerZ = -14.3f, halfLength = 6.1f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 5.4f, doorCenterOffset = 2.0f, doorHalfWidth = 1.35f, doorHeight = 2.7f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = trimColor
        )
        c.addWallWithWindow(
            isAlongX = true, centerX = -14.8f, centerZ = -8.2f, halfLength = 5.6f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 5.4f, winCenterOffset = 0f, winHalfWidth = 2.2f,
            sillHeight = 1.0f, winTopHeight = 2.8f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = trimColor
        )
        val officeDesk = c.placeStaticOnFloor(
            MeshId.LAB_TABLE, -15.5f, -15.0f, Vec3(1.65f, 0.46f, 0.78f),
            PhysicsMaterial.WOOD, colorOverride = Vec3(0.36f, 0.28f, 0.22f), label = "Warehouse Office Desk"
        )
        c.placeDynamicOnFloor(ObjectType.OFFICE_CHAIR, -15.5f, -13.2f)
        c.placeDynamicOnSupport(ObjectType.GLASS_CUBE, -15.0f, -15.0f, officeDesk)
        c.placeWallMounted(
            MeshId.WALL_CABINET, wallCenterX = -21.0f, wallCenterZ = -15.0f, wallHalfThickness = 0.6f,
            mountBottomY = 1.45f, inwardDirX = 1f, inwardDirZ = 0f,
            halfExtents = Vec3(1.3f, 0.75f, 0.30f), material = PhysicsMaterial.RUSTED_IRON,
            colorOverride = Vec3(0.34f, 0.32f, 0.30f), label = "Office Filing Cabinet"
        )

        // Heavy Storage Racks in North Aisle
        val rackA = c.placeStaticOnFloor(
            MeshId.SHELF_RACK, 0.0f, -15.5f, Vec3(3.2f, 1.55f, 0.72f),
            PhysicsMaterial.RUSTED_IRON, label = "Warehouse Rack North"
        )
        c.placeDynamicOnSupport(ObjectType.CENTRIFUGE_POD, 0.0f, -15.5f, rackA, material = PhysicsMaterial.RUSTED_IRON)
        c.placeStaticOnFloor(
            MeshId.SHELF_RACK, 12.5f, -15.5f, Vec3(3.0f, 1.55f, 0.72f),
            PhysicsMaterial.RUSTED_IRON, label = "Warehouse Rack NE"
        )

        // Shipping Containers & Ramp Access
        c.placeStaticOnFloor(
            MeshId.SHIPPING_CONTAINER, -14.8f, 4.0f, Vec3(1.65f, 1.55f, 3.8f),
            PhysicsMaterial.RUSTED_IRON, colorOverride = Vec3(0.42f, 0.22f, 0.18f), label = "Red Container"
        )
        c.addSlopedRampWithPiers(
            centerX = -9.6f, centerZ = 4.0f, lowY = 0f, highY = 3.10f,
            halfWidth = 1.35f, halfRun = 3.4f, ascendYaw = PI.toFloat() * 0.5f,
            material = PhysicsMaterial.WOOD, colorOverride = Vec3(0.46f, 0.35f, 0.24f)
        )

        val eastBottom = c.placeStaticOnFloor(
            MeshId.SHIPPING_CONTAINER, 15.2f, -3.5f, Vec3(1.65f, 1.55f, 3.8f),
            PhysicsMaterial.RUSTED_IRON, colorOverride = Vec3(0.24f, 0.34f, 0.36f), label = "Teal Container"
        )
        c.placeStaticOnSupport(
            MeshId.SHIPPING_CONTAINER, 15.2f, -3.5f, eastBottom, Vec3(1.65f, 1.55f, 3.8f),
            PhysicsMaterial.RUSTED_IRON, colorOverride = Vec3(0.38f, 0.34f, 0.22f), label = "Stacked Container"
        )

        // Floor-Settled Cargo Preserving Clear Walkways
        for (i in 0 until 4) {
            val base = c.placeDynamicOnFloor(ObjectType.WOOD_CRATE, -3.0f + i * 1.35f, -4.5f)
            if (i % 2 == 0) c.placeDynamicOnBody(ObjectType.WOOD_CRATE, -3.0f + i * 1.35f, -4.5f, base)
        }
        c.placeDynamicOnFloor(ObjectType.INDUSTRIAL_BARREL, 4.5f, 1.5f)
        c.placeDynamicOnFloor(ObjectType.INDUSTRIAL_BARREL, 5.6f, 2.2f)
        c.placeDynamicOnFloor(ObjectType.RUBBER_TIRE, -4.5f, 6.0f)
        c.placeDynamicOnFloor(ObjectType.RUBBER_TIRE, -3.1f, 6.3f)
        c.placeDynamicOnFloor(ObjectType.CONCRETE_BLOCK, 2.0f, 6.2f)

        return BuiltMap(c.statics, c.dynamics, c.constraints, Vec3(0f, 0.1f, 14.5f), 0f, c.nextId)
    }

    // MAP 04 — CONSTRUCTION SITE
    fun buildConstruction(): BuiltMap {
        val c = MapConstructionContext()
        // Open-air concrete & earth yard
        c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(0f, -0.5f, 0f), Vec3(24f, 0.5f, 24f),
            PhysicsMaterial.CONCRETE, colorOverride = Vec3(0.45f, 0.42f, 0.37f), mountType = MountType.STRUCTURAL
        )
        c.addPerimeterWalls(24f, 2.2f, PhysicsMaterial.CONCRETE, Vec3(0.48f, 0.46f, 0.43f))

        // Poured concrete building foundation slab (flush on ground 0..0.3m)
        val foundation = c.placeStaticOnFloor(
            MeshId.FLOOR_SLAB, -8.5f, -8.5f, Vec3(5.6f, 0.15f, 5.6f),
            PhysicsMaterial.CONCRETE, colorOverride = Vec3(0.56f, 0.55f, 0.53f), mountType = MountType.STRUCTURAL
        )
        // 4 aligned structural concrete columns supporting 2nd-story slab
        for (px in listOf(-12.8f, -4.2f)) for (pz in listOf(-12.8f, -4.2f)) {
            c.placeStaticOnSupport(
                MeshId.PILLAR_COLUMN, px, pz, foundation, Vec3(0.45f, 1.80f, 0.45f),
                PhysicsMaterial.CONCRETE, colorOverride = Vec3(0.60f, 0.59f, 0.56f)
            )
        }
        // Upper 2nd-story slab sitting cleanly atop column tops (0.3 + 3.6 = 3.90m bottom, 4.40m top)
        val upperDeck = c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(-8.5f, 4.15f, -8.5f), Vec3(5.2f, 0.25f, 5.2f),
            PhysicsMaterial.CONCRETE, colorOverride = Vec3(0.58f, 0.57f, 0.54f), mountType = MountType.STRUCTURAL
        )
        c.placeDynamicOnSupport(ObjectType.STEEL_IBEAM, -8.5f, -8.5f, upperDeck)

        // Connected staircase from ground (z = 2.4, y = 0) flush up to upper floor edge (z = -3.3, y = 4.40)
        c.addStaircaseToDeck(
            centerX = -8.5f, centerZ = -0.45f, floorY = 0f, topY = 4.40f,
            halfWidth = 1.25f, halfRun = 2.85f, ascendYaw = 0f
        )

        // Multi-tier scaffolding tower (East side)
        val scaffoldBase = c.placeStaticOnFloor(
            MeshId.SCAFFOLD_FRAME, 12.0f, -5.0f, Vec3(2.4f, 1.42f, 1.4f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.44f, 0.48f, 0.52f), label = "Scaffolding Tower"
        )
        val scaffoldDeck = c.placeStaticOnSupport(
            MeshId.FLOOR_SLAB, 12.0f, -5.0f, scaffoldBase, Vec3(2.3f, 0.12f, 1.3f),
            PhysicsMaterial.WOOD, colorOverride = Vec3(0.52f, 0.39f, 0.25f), label = "Scaffold Platform"
        )
        c.placeDynamicOnSupport(ObjectType.WOOD_CRATE, 12.0f, -5.0f, scaffoldDeck)

        // Tower Crane & Wrecking Ball aligned with Masonry Test Wall
        c.placeStaticOnFloor(
            MeshId.PILLAR_COLUMN, 8.5f, 7.5f, Vec3(0.55f, 5.2f, 0.55f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.84f, 0.62f, 0.16f), mountType = MountType.STRUCTURAL
        )
        c.addStatic(
            MeshId.CRANE_BOOM, Vec3(4.2f, 10.8f, 3.2f), Vec3(6.2f, 0.40f, 0.45f),
            PhysicsMaterial.STEEL, rot = EnvironmentPlacer.yawQuat(0.78f),
            colorOverride = Vec3(0.84f, 0.62f, 0.16f), mountType = MountType.STRUCTURAL
        )
        val wreckingBall = c.addDynamic(ObjectType.STEEL_SPHERE, Vec3(1.0f, 4.6f, 0.2f), scale = 1.6f)
        c.addConstraint(ConstraintType.ROPE, wreckingBall, null, Vec3(0f, 0.7f, 0f), Vec3(4.2f, 10.35f, 3.2f), restLength = 6.4f)

        // Masonry Test Wall
        var wallRows = ArrayList<com.example.physics.RigidBody>()
        for (row in 0 until 4) {
            val nextRow = ArrayList<com.example.physics.RigidBody>()
            for (col in 0 until 3) {
                val x = 3.6f + col * 1.28f
                val b = if (row == 0) {
                    c.placeDynamicOnFloor(ObjectType.CONCRETE_BLOCK, x, -2.0f)
                } else {
                    c.placeDynamicOnBody(ObjectType.CONCRETE_BLOCK, x, -2.0f, wallRows[col])
                }
                nextRow.add(b)
            }
            wallRows = nextRow
        }

        // SW Material Staging Zone
        c.placeDynamicOnFloor(ObjectType.STEEL_IBEAM, -12.0f, 8.5f)
        c.placeDynamicOnFloor(ObjectType.STEEL_IBEAM, -12.0f, 10.2f)
        c.placeDynamicOnFloor(ObjectType.RUBBER_TIRE, -6.5f, 9.5f)
        c.placeDynamicOnFloor(ObjectType.WOOD_CRATE, -8.5f, 12.0f)

        return BuiltMap(c.statics, c.dynamics, c.constraints, Vec3(0f, 0.1f, 16.5f), 0f, c.nextId)
    }

    // MAP 05 — PHYSICS TESTING FACILITY
    fun buildFacility(): BuiltMap {
        val c = MapConstructionContext()
        val floorColor = Vec3(0.38f, 0.42f, 0.46f)
        val wallColor = Vec3(0.49f, 0.52f, 0.57f)
        val frameColor = Vec3(0.22f, 0.26f, 0.32f)

        c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(0f, -0.5f, 0f), Vec3(23f, 0.5f, 23f),
            PhysicsMaterial.CONCRETE, colorOverride = floorColor, mountType = MountType.STRUCTURAL
        )
        c.addPerimeterWalls(23f, 10.5f, PhysicsMaterial.CONCRETE, wallColor)

        // High-span roof trusses & recessed architectural lighting
        for (z in listOf(-14f, -4f, 6f, 15f)) {
            c.addStatic(
                MeshId.ROOF_TRUSS, Vec3(0f, 9.8f, z), Vec3(22.4f, 0.45f, 0.45f),
                PhysicsMaterial.STEEL, colorOverride = Vec3(0.28f, 0.31f, 0.37f), mountType = MountType.STRUCTURAL
            )
            for (lx in listOf(-14f, -5f, 5f, 14f)) {
                c.placeCeilingLight(x = lx, z = z, ceilingBottomY = 9.35f, halfX = 1.3f, halfZ = 0.32f, emissiveIntensity = 0.42f)
            }
        }

        // Elevated Observation Gallery (NE Mezzanine + Columns + Glass Safety Rail + Staircase)
        for (px in listOf(8.5f, 19.5f)) for (pz in listOf(-19.5f, -6.8f)) {
            c.placeStaticOnFloor(
                MeshId.PILLAR_COLUMN, px, pz, Vec3(0.30f, 1.70f, 0.30f),
                PhysicsMaterial.STEEL, colorOverride = frameColor, mountType = MountType.STRUCTURAL
            )
        }
        val galleryDeck = c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(14.0f, 3.65f, -13.2f), Vec3(6.0f, 0.25f, 6.8f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.34f, 0.38f, 0.43f), mountType = MountType.STRUCTURAL
        )
        c.placeStaticOnSupport(
            MeshId.GLASS_PARTITION, 8.15f, -13.2f, galleryDeck, Vec3(0.08f, 0.65f, 6.4f),
            PhysicsMaterial.GLASS, alphaOverride = 0.42f, colorOverride = Vec3(0.76f, 0.88f, 0.94f)
        )
        c.placeStaticOnSupport(
            MeshId.LAB_CONSOLE, 14.0f, -14.5f, galleryDeck, Vec3(2.2f, 0.55f, 0.75f),
            PhysicsMaterial.STEEL, colorOverride = frameColor, label = "Gallery Telemetry Console"
        )
        c.placeDynamicOnSupport(ObjectType.GLASS_CUBE, 17.5f, -12.0f, galleryDeck)
        c.addStaircaseToDeck(
            centerX = 13.8f, centerZ = -2.8f, floorY = 0f, topY = 3.90f,
            halfWidth = 1.25f, halfRun = 3.6f, ascendYaw = 0f
        )

        // Calibration Sloped Ramp Raceway (NW) with Support Piers
        c.addSlopedRampWithPiers(
            centerX = -11.5f, centerZ = -9.5f, lowY = 0f, highY = 3.20f,
            halfWidth = 2.2f, halfRun = 5.4f, ascendYaw = 0f,
            material = PhysicsMaterial.STEEL, colorOverride = Vec3(0.42f, 0.46f, 0.52f)
        )
        c.placeDynamicOnFloor(ObjectType.STEEL_SPHERE, -12.3f, -2.5f)
        c.placeDynamicOnFloor(ObjectType.BOUNCY_SPHERE, -10.7f, -2.5f)

        // Hinged Seesaw Lever in Central Testing Bay
        c.placeStaticOnFloor(
            MeshId.CONCRETE_MASONRY, 0f, -5.5f, Vec3(0.6f, 0.55f, 0.8f),
            PhysicsMaterial.CONCRETE, mountType = MountType.STRUCTURAL
        )
        val seesawBeam = c.addDynamic(ObjectType.STEEL_IBEAM, Vec3(0f, 1.48f, -5.5f), scale = 1.5f)
        c.addConstraint(ConstraintType.HINGE, seesawBeam, null, Vec3.ZERO, Vec3(0f, 1.48f, -5.5f), hingeAxis = Vec3(0f, 0f, 1f))

        // Destructible Comparison Towers (Center-East)
        var cPrev: com.example.physics.RigidBody? = null
        var wPrev: com.example.physics.RigidBody? = null
        for (layer in 0 until 5) {
            cPrev = if (cPrev == null) {
                c.placeDynamicOnFloor(ObjectType.CONCRETE_BLOCK, 5.2f, 5.2f)
            } else {
                c.placeDynamicOnBody(ObjectType.CONCRETE_BLOCK, 5.2f, 5.2f, cPrev)
            }
            wPrev = if (wPrev == null) {
                c.placeDynamicOnFloor(ObjectType.WOOD_CRATE, 7.0f, 5.2f)
            } else {
                c.placeDynamicOnBody(ObjectType.WOOD_CRATE, 7.0f, 5.2f, wPrev)
            }
        }

        // Equipment Storage Bay (SE)
        c.placeStaticOnFloor(
            MeshId.SHELF_RACK, 19.2f, 8.5f, Vec3(0.70f, 1.50f, 2.8f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.34f, 0.38f, 0.43f), label = "Testing Storage Rack"
        )
        val storTable = c.placeStaticOnFloor(
            MeshId.LAB_TABLE, 14.5f, 10.5f, Vec3(1.8f, 0.48f, 0.85f),
            PhysicsMaterial.STEEL, colorOverride = frameColor, label = "Instrumentation Table"
        )
        c.placeDynamicOnSupport(ObjectType.CENTRIFUGE_POD, 14.5f, 10.5f, storTable)
        c.placeWallMounted(
            MeshId.WALL_CABINET, wallCenterX = 23.0f, wallCenterZ = 14.0f, wallHalfThickness = 0.6f,
            mountBottomY = 1.45f, inwardDirX = -1f, inwardDirZ = 0f,
            halfExtents = Vec3(1.4f, 0.75f, 0.32f), material = PhysicsMaterial.STEEL,
            colorOverride = Vec3(0.32f, 0.36f, 0.41f), label = "Calibration Cabinet"
        )

        // DC Motor & Buoyancy Apparatus Table (Center-West)
        val motorBench = c.placeStaticOnFloor(
            MeshId.LAB_TABLE, -5.2f, 5.2f, Vec3(2.4f, 0.48f, 0.85f),
            PhysicsMaterial.STEEL, colorOverride = frameColor, label = "Electromechanical Bench"
        )
        val panel = c.placeDynamicOnSupport(ObjectType.ELECTRICAL_PANEL, -6.8f, 5.2f, motorBench)
        val sw = c.placeDynamicOnSupport(ObjectType.SWITCH_BOX, -5.7f, 5.2f, motorBench)
        val motor = c.placeDynamicOnSupport(ObjectType.DC_MOTOR, -4.6f, 5.2f, motorBench)
        val led = c.placeDynamicOnSupport(ObjectType.LED_INDICATOR, -3.5f, 5.2f, motorBench)
        c.addWire(panel, sw)
        c.addWire(sw, motor)
        c.addWire(sw, led)
        c.placeDynamicOnFloor(ObjectType.BUOYANCY_FLOAT, -9.2f, 5.2f)
        c.placeDynamicOnFloor(ObjectType.SOFT_FOAM_BLOCK, -10.8f, 5.2f)

        return BuiltMap(c.statics, c.dynamics, c.constraints, Vec3(0f, 0.1f, 15.5f), 0f, c.nextId, c.wires)
    }

    // MAP 06 — UNDERGROUND MAINTENANCE (8 Zones)
    fun buildUndergroundMaintenance(): BuiltMap {
        val c = MapConstructionContext()
        val floorColor = Vec3(0.28f, 0.30f, 0.32f)
        val wallColor = Vec3(0.36f, 0.38f, 0.41f)
        val steelColor = Vec3(0.22f, 0.25f, 0.29f)
        val pipeColor = Vec3(0.44f, 0.35f, 0.20f)

        c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(0f, -0.5f, 0f), Vec3(22f, 0.5f, 22f),
            PhysicsMaterial.CONCRETE, colorOverride = floorColor, mountType = MountType.STRUCTURAL
        )
        // Enclosed subterranean roof slab & concrete perimeter walls
        c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(0f, 6.7f, 0f), Vec3(22f, 0.45f, 22f),
            PhysicsMaterial.CONCRETE, colorOverride = Vec3(0.25f, 0.27f, 0.30f), mountType = MountType.STRUCTURAL
        )
        c.addPerimeterWalls(22f, 6.4f, PhysicsMaterial.CONCRETE, wallColor)

        // Subterranean girders, ventilation ducts & overhead utility pipe runs
        for (z in listOf(-11.5f, 0f, 11.5f)) {
            c.addStatic(
                MeshId.ROOF_TRUSS, Vec3(0f, 5.95f, z), Vec3(21.2f, 0.30f, 0.32f),
                PhysicsMaterial.STEEL, colorOverride = steelColor, mountType = MountType.STRUCTURAL
            )
            c.placeCeilingLight(x = -4.5f, z = z, ceilingBottomY = 6.25f, halfX = 1.2f, halfZ = 0.28f, emissiveColor = Vec3(1.0f, 0.90f, 0.72f), emissiveIntensity = 0.48f)
            c.placeCeilingLight(x = 4.5f, z = z, ceilingBottomY = 6.25f, halfX = 1.2f, halfZ = 0.28f, emissiveColor = Vec3(1.0f, 0.90f, 0.72f), emissiveIntensity = 0.48f)
        }
        // Overhead industrial utility pipes along East corridor and West service run
        for (px in listOf(-7.8f, 14.2f)) {
            c.addStatic(
                MeshId.PIPE_CONDUIT, Vec3(px, 5.25f, 0f), Vec3(16.0f, 0.26f, 0.26f),
                PhysicsMaterial.COPPER, rot = EnvironmentPlacer.yawQuat(PI.toFloat() * 0.5f),
                isCollider = false, colorOverride = pipeColor, mountType = MountType.STRUCTURAL
            )
        }
        c.addStatic(
            MeshId.CEILING_VENT, Vec3(0f, 6.08f, -5.5f), Vec3(1.1f, 0.17f, 0.8f),
            PhysicsMaterial.STEEL, isCollider = false, colorOverride = Vec3(0.30f, 0.33f, 0.37f), mountType = MountType.CEILING
        )

        // ZONE 2: Equipment Storage Bay (NW: x in -21.4..-9.5, z in -21.4..-6.5)
        c.addWallWithDoorway(
            isAlongX = false, centerX = -9.5f, centerZ = -14.0f, halfLength = 7.4f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 6.25f, doorCenterOffset = 2.5f, doorHalfWidth = 1.55f, doorHeight = 2.9f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = steelColor
        )
        c.addWallWithDoorway(
            isAlongX = true, centerX = -15.4f, centerZ = -6.6f, halfLength = 5.9f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 6.25f, doorCenterOffset = 0.5f, doorHalfWidth = 1.45f, doorHeight = 2.9f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = steelColor
        )
        c.placeStaticOnFloor(MeshId.SHELF_RACK, -19.2f, -17.0f, Vec3(0.75f, 1.45f, 2.2f), PhysicsMaterial.STEEL, colorOverride = steelColor, label = "Underground Rack A")
        c.placeStaticOnFloor(MeshId.SHELF_RACK, -19.2f, -11.0f, Vec3(0.75f, 1.45f, 2.2f), PhysicsMaterial.STEEL, colorOverride = steelColor, label = "Underground Rack B")
        c.placeDynamicOnFloor(ObjectType.INDUSTRIAL_BARREL, -13.2f, -17.5f)
        c.placeDynamicOnFloor(ObjectType.STEEL_CRATE, -13.2f, -15.6f)

        // ZONE 3: Electrical Control Room (SW: x in -21.4..-9.5, z in 4.0..21.4)
        c.addWallWithDoorway(
            isAlongX = false, centerX = -9.5f, centerZ = 12.7f, halfLength = 8.7f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 6.25f, doorCenterOffset = -2.2f, doorHalfWidth = 1.50f, doorHeight = 2.9f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = steelColor
        )
        c.addWallWithDoorway(
            isAlongX = true, centerX = -15.4f, centerZ = 4.0f, halfLength = 5.9f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 6.25f, doorCenterOffset = 0f, doorHalfWidth = 1.45f, doorHeight = 2.9f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = steelColor
        )
        c.placeWallMounted(
            MeshId.WALL_CABINET, wallCenterX = -22.0f, wallCenterZ = 13.5f, wallHalfThickness = 0.6f,
            mountBottomY = 1.45f, inwardDirX = 1f, inwardDirZ = 0f,
            halfExtents = Vec3(1.4f, 0.78f, 0.34f), material = PhysicsMaterial.STEEL,
            colorOverride = Vec3(0.28f, 0.33f, 0.38f), label = "Main Breaker Cabinet"
        )
        val bunkerElecTable = c.placeStaticOnFloor(
            MeshId.LAB_TABLE, -15.5f, 15.0f, Vec3(2.5f, 0.48f, 0.85f),
            PhysicsMaterial.STEEL, colorOverride = steelColor, label = "Substation Control Bench"
        )
        val bPanel = c.placeDynamicOnSupport(ObjectType.ELECTRICAL_PANEL, -17.3f, 15.0f, bunkerElecTable)
        val bSwitch = c.placeDynamicOnSupport(ObjectType.SWITCH_BOX, -16.4f, 15.0f, bunkerElecTable)
        val bFuse = c.placeDynamicOnSupport(ObjectType.FUSE_BLOCK, -15.5f, 15.0f, bunkerElecTable)
        val bLamp = c.placeDynamicOnSupport(ObjectType.LIGHT_BULB_LAMP, -14.6f, 15.0f, bunkerElecTable)
        val bMotor = c.placeDynamicOnSupport(ObjectType.DC_MOTOR, -13.7f, 15.0f, bunkerElecTable)
        c.addWire(bPanel, bSwitch); c.addWire(bSwitch, bFuse); c.addWire(bFuse, bLamp); c.addWire(bFuse, bMotor)

        // ZONE 4 & 5: NE Mechanical Workshop & East Utility Corridor
        c.addWallWithDoorway(
            isAlongX = false, centerX = 9.5f, centerZ = -13.5f, halfLength = 7.9f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 6.25f, doorCenterOffset = 1.0f, doorHalfWidth = 1.65f, doorHeight = 3.0f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = steelColor
        )
        c.placeStaticOnFloor(
            MeshId.CNC_MACHINE, 18.6f, -17.5f, Vec3(1.65f, 1.40f, 1.35f),
            PhysicsMaterial.STEEL, rot = EnvironmentPlacer.yawQuat(-PI.toFloat() * 0.5f),
            colorOverride = Vec3(0.24f, 0.30f, 0.35f), label = "Subterranean CNC Lathe"
        )
        c.placeStaticOnFloor(
            MeshId.HYDRAULIC_PRESS, 18.6f, -11.5f, Vec3(1.35f, 1.55f, 1.05f),
            PhysicsMaterial.STEEL, rot = EnvironmentPlacer.yawQuat(-PI.toFloat() * 0.5f),
            colorOverride = Vec3(0.52f, 0.32f, 0.16f), label = "Bunker Hydraulic Press"
        )
        val mechBench = c.placeStaticOnFloor(
            MeshId.LAB_TABLE, 13.8f, -17.5f, Vec3(1.8f, 0.48f, 0.80f),
            PhysicsMaterial.STEEL, colorOverride = steelColor, label = "Machining Workbench"
        )
        c.placeDynamicOnSupport(ObjectType.GEAR_WHEEL, 13.0f, -17.5f, mechBench)
        c.placeDynamicOnSupport(ObjectType.AXLE_ROD, 14.5f, -17.5f, mechBench)

        // East Utility Corridor pipe valve manifold along East wall (Zone 5)
        c.placeStaticOnFloor(
            MeshId.PIPE_CONDUIT, 19.6f, 0.0f, Vec3(4.5f, 0.72f, 0.55f),
            PhysicsMaterial.COPPER, rot = EnvironmentPlacer.yawQuat(PI.toFloat() * 0.5f),
            colorOverride = pipeColor, label = "Main Utility Pipe Header"
        )

        // ZONE 6: Pump & Filtration Room (SE: x in 9.5..21.4, z in 5.5..21.4)
        c.addWallWithDoorway(
            isAlongX = false, centerX = 9.5f, centerZ = 13.4f, halfLength = 7.9f, halfThickness = 0.24f,
            baseY = 0f, wallHeight = 6.25f, doorCenterOffset = -0.9f, doorHalfWidth = 1.65f, doorHeight = 3.0f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = steelColor
        )
        val pumpConsole = c.placeStaticOnFloor(
            MeshId.LAB_CONSOLE, 15.8f, 17.5f, Vec3(2.2f, 0.58f, 0.82f),
            PhysicsMaterial.STEEL, colorOverride = steelColor, label = "Filtration Pump Station"
        )
        c.placeDynamicOnSupport(ObjectType.KINETIC_GENERATOR, 14.6f, 17.5f, pumpConsole)
        c.placeDynamicOnSupport(ObjectType.BUOYANCY_FLOAT, 16.9f, 17.5f, pumpConsole)
        c.placeDynamicOnFloor(ObjectType.INDUSTRIAL_BARREL, 19.0f, 11.2f)
        c.placeDynamicOnFloor(ObjectType.INDUSTRIAL_BARREL, 19.0f, 12.6f)

        // ZONE 7: Service Elevator Bay (North-West-Center: x in -7.8..-1.2, z in -21.4..-14.5)
        val elevatorDeck = c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(-4.5f, 0.14f, -18.0f), Vec3(3.1f, 0.14f, 3.0f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.32f, 0.35f, 0.38f),
            mountType = MountType.STRUCTURAL, label = "Service Elevator Deck"
        )
        c.placeStaticOnSupport(
            MeshId.SAFETY_RAIL, -7.45f, -18.0f, elevatorDeck, Vec3(0.08f, 0.55f, 2.8f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.76f, 0.56f, 0.16f), label = "Elevator Safety Rail West"
        )
        c.placeStaticOnSupport(
            MeshId.SAFETY_RAIL, -1.55f, -18.0f, elevatorDeck, Vec3(0.08f, 0.55f, 2.8f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.76f, 0.56f, 0.16f), label = "Elevator Safety Rail East"
        )
        c.placeDynamicOnSupport(ObjectType.STEEL_CRATE, -4.5f, -18.2f, elevatorDeck)

        // ZONE 8: Observation Section (North-East-Center: x in 1.2..9.2, z in -21.4..-13.0)
        c.addWallWithWindow(
            isAlongX = true, centerX = 5.2f, centerZ = -13.0f, halfLength = 4.0f, halfThickness = 0.22f,
            baseY = 0f, wallHeight = 6.25f, winCenterOffset = 0f, winHalfWidth = 2.4f,
            sillHeight = 1.0f, winTopHeight = 2.8f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = steelColor
        )
        c.addWallWithDoorway(
            isAlongX = false, centerX = 1.2f, centerZ = -17.2f, halfLength = 4.2f, halfThickness = 0.22f,
            baseY = 0f, wallHeight = 6.25f, doorCenterOffset = 0.5f, doorHalfWidth = 1.25f, doorHeight = 2.8f,
            material = PhysicsMaterial.CONCRETE, wallColor = wallColor, frameColor = steelColor
        )
        val obsConsole = c.placeStaticOnFloor(
            MeshId.LAB_CONSOLE, 5.6f, -17.8f, Vec3(1.8f, 0.55f, 0.72f),
            PhysicsMaterial.STEEL, colorOverride = steelColor, label = "Bunker Observation Console"
        )
        c.placeDynamicOnFloor(ObjectType.OFFICE_CHAIR, 5.6f, -15.8f)
        c.placeDynamicOnSupport(ObjectType.LED_INDICATOR, 5.6f, -17.8f, obsConsole)

        // ZONE 1 Central Hall Experiment Objects
        c.placeStaticOnFloor(MeshId.PILLAR_COLUMN, -3.5f, 1.5f, Vec3(0.30f, 2.75f, 0.30f), PhysicsMaterial.STEEL, colorOverride = steelColor, mountType = MountType.STRUCTURAL)
        c.placeStaticOnFloor(MeshId.PILLAR_COLUMN, 3.5f, 1.5f, Vec3(0.30f, 2.75f, 0.30f), PhysicsMaterial.STEEL, colorOverride = steelColor, mountType = MountType.STRUCTURAL)
        c.addStatic(MeshId.STEEL_IBEAM, Vec3(0f, 5.50f, 1.5f), Vec3(4.0f, 0.24f, 0.28f), PhysicsMaterial.STEEL, mountType = MountType.STRUCTURAL)
        val cranePendulum = c.addDynamic(ObjectType.STEEL_SPHERE, Vec3(-1.4f, 3.2f, 1.5f), scale = 1.2f)
        c.addConstraint(ConstraintType.ROPE, cranePendulum, null, Vec3(0f, 0.48f, 0f), Vec3(0f, 5.25f, 1.5f), restLength = 2.8f)
        c.placeDynamicOnFloor(ObjectType.CONCRETE_BLOCK, 2.8f, 4.0f)
        c.placeDynamicOnFloor(ObjectType.WOOD_CRATE, -2.8f, 4.5f)
        c.placeDynamicOnFloor(ObjectType.RUBBER_TIRE, 2.4f, -3.5f)

        return BuiltMap(c.statics, c.dynamics, c.constraints, Vec3(0f, 0.1f, 12.0f), 0f, c.nextId, c.wires)
    }

    // MAP 07 — OUTDOOR TESTING YARD (7 Zones)
    fun buildOutdoorTestingYard(): BuiltMap {
        val c = MapConstructionContext()
        val asphaltColor = Vec3(0.24f, 0.25f, 0.27f)
        val concretePadColor = Vec3(0.46f, 0.46f, 0.44f)
        val perimeterColor = Vec3(0.42f, 0.43f, 0.41f)
        val steelFrameColor = Vec3(0.28f, 0.32f, 0.36f)

        // ZONE 1: Open Asphalt Testing Ground (25m half-size) + low concrete perimeter barrier
        c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(0f, -0.5f, 0f), Vec3(25f, 0.5f, 25f),
            PhysicsMaterial.CONCRETE, colorOverride = asphaltColor, mountType = MountType.STRUCTURAL
        )
        c.addPerimeterWalls(25f, 1.8f, PhysicsMaterial.CONCRETE, perimeterColor)

        // Central Heavy Gantry & Wrecking Drop Rig
        c.placeStaticOnFloor(MeshId.PILLAR_COLUMN, -4.2f, -4.5f, Vec3(0.32f, 3.8f, 0.32f), PhysicsMaterial.STEEL, colorOverride = Vec3(0.76f, 0.56f, 0.16f), mountType = MountType.STRUCTURAL)
        c.placeStaticOnFloor(MeshId.PILLAR_COLUMN, 4.2f, -4.5f, Vec3(0.32f, 3.8f, 0.32f), PhysicsMaterial.STEEL, colorOverride = Vec3(0.76f, 0.56f, 0.16f), mountType = MountType.STRUCTURAL)
        c.addStatic(
            MeshId.CRANE_BOOM, Vec3(0f, 7.85f, -4.5f), Vec3(4.8f, 0.35f, 0.40f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.76f, 0.56f, 0.16f), mountType = MountType.STRUCTURAL
        )
        val dropSphere = c.addDynamic(ObjectType.STEEL_SPHERE, Vec3(-1.8f, 4.4f, -4.5f), scale = 1.45f)
        c.addConstraint(ConstraintType.ROPE, dropSphere, null, Vec3(0f, 0.58f, 0f), Vec3(0f, 7.50f, -4.5f), restLength = 3.8f)

        // Destructible concrete target wall & domino array in center yard
        var targetPrev: com.example.physics.RigidBody? = null
        for (layer in 0 until 3) {
            targetPrev = if (targetPrev == null) {
                c.placeDynamicOnFloor(ObjectType.CONCRETE_BLOCK, 2.2f, -4.5f)
            } else {
                c.placeDynamicOnBody(ObjectType.CONCRETE_BLOCK, 2.2f, -4.5f, targetPrev)
            }
        }
        for (i in 0 until 3) {
            c.placeDynamicOnFloor(ObjectType.DOMINO_SLAB, -2.4f + i * 1.15f, 2.2f)
        }

        // ZONE 2: Covered Workshop Canopy (NW: x in -20..-8, z in -20..-8)
        val workshopPad = c.placeStaticOnFloor(
            MeshId.FLOOR_SLAB, -14.0f, -14.0f, Vec3(5.8f, 0.08f, 5.8f),
            PhysicsMaterial.CONCRETE, colorOverride = concretePadColor, mountType = MountType.STRUCTURAL
        )
        for (px in listOf(-18.8f, -9.2f)) for (pz in listOf(-18.8f, -9.2f)) {
            c.placeStaticOnSupport(
                MeshId.PILLAR_COLUMN, px, pz, workshopPad, Vec3(0.28f, 2.2f, 0.28f),
                PhysicsMaterial.STEEL, colorOverride = steelFrameColor
            )
        }
        // Canopy roof deck at Y = 0.16 + 4.4 = 4.56..4.96
        c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(-14.0f, 4.76f, -14.0f), Vec3(6.0f, 0.20f, 6.0f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.32f, 0.36f, 0.40f), mountType = MountType.STRUCTURAL
        )
        c.placeCeilingLight(x = -14.0f, z = -14.0f, ceilingBottomY = 4.56f, halfX = 1.6f, halfZ = 0.35f, emissiveIntensity = 0.50f)
        c.placeStaticOnSupport(
            MeshId.HYDRAULIC_PRESS, -16.8f, -16.0f, workshopPad, Vec3(1.35f, 1.50f, 1.05f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.56f, 0.34f, 0.16f), label = "Yard Hydraulic Press"
        )
        val yardBench = c.placeStaticOnSupport(
            MeshId.LAB_TABLE, -11.5f, -16.2f, workshopPad, Vec3(2.0f, 0.48f, 0.80f),
            PhysicsMaterial.STEEL, colorOverride = steelFrameColor, label = "Outdoor Canopy Bench"
        )
        val yBat = c.placeDynamicOnSupport(ObjectType.BATTERY_UNIT, -12.8f, -16.2f, yardBench)
        val ySw = c.placeDynamicOnSupport(ObjectType.SWITCH_BOX, -11.8f, -16.2f, yardBench)
        val yMot = c.placeDynamicOnSupport(ObjectType.DC_MOTOR, -10.8f, -16.2f, yardBench)
        val yLed = c.placeDynamicOnSupport(ObjectType.LED_INDICATOR, -9.8f, -16.2f, yardBench)
        c.addWire(yBat, ySw); c.addWire(ySw, yMot); c.addWire(ySw, yLed)

        // ZONE 3: Storage Yard (NE: Shipping Containers & Heavy Racks)
        c.placeStaticOnFloor(
            MeshId.SHIPPING_CONTAINER, 17.5f, -17.5f, Vec3(3.4f, 1.55f, 1.65f),
            PhysicsMaterial.RUSTED_IRON, colorOverride = Vec3(0.42f, 0.22f, 0.18f), label = "Yard Container Red"
        )
        c.placeStaticOnFloor(
            MeshId.SHIPPING_CONTAINER, 17.5f, -12.5f, Vec3(3.4f, 1.55f, 1.65f),
            PhysicsMaterial.RUSTED_IRON, colorOverride = Vec3(0.22f, 0.34f, 0.36f), label = "Yard Container Teal"
        )
        c.placeStaticOnFloor(
            MeshId.SHELF_RACK, 10.5f, -17.5f, Vec3(2.2f, 1.45f, 0.72f),
            PhysicsMaterial.STEEL, colorOverride = steelFrameColor, label = "Yard Storage Rack"
        )
        c.placeDynamicOnFloor(ObjectType.WOOD_CRATE, 10.5f, -13.2f)
        c.placeDynamicOnFloor(ObjectType.STEEL_CRATE, 12.2f, -13.2f)

        // ZONE 4: Mechanical Test Rig & Calibration Ramp (West)
        c.addSlopedRampWithPiers(
            centerX = -14.2f, centerZ = 1.5f, lowY = 0f, highY = 2.80f,
            halfWidth = 2.0f, halfRun = 4.6f, ascendYaw = 0f,
            material = PhysicsMaterial.STEEL, colorOverride = Vec3(0.40f, 0.44f, 0.48f)
        )
        c.placeDynamicOnFloor(ObjectType.RUBBER_TIRE, -14.2f, 7.6f)
        c.placeDynamicOnFloor(ObjectType.COPPER_GYRO, -11.4f, 6.2f)
        c.placeDynamicOnFloor(ObjectType.SPRING_PISTON, -11.4f, 7.8f)

        // ZONE 5: Loading Bay Dock (South-East)
        val dockSlab = c.placeStaticOnFloor(
            MeshId.FLOOR_SLAB, 12.0f, 18.0f, Vec3(5.6f, 0.45f, 2.8f),
            PhysicsMaterial.CONCRETE, colorOverride = concretePadColor, mountType = MountType.STRUCTURAL
        )
        c.addSlopedRampWithPiers(
            centerX = 4.8f, centerZ = 18.0f, lowY = 0f, highY = 0.90f,
            halfWidth = 1.6f, halfRun = 2.2f, ascendYaw = -PI.toFloat() * 0.5f,
            material = PhysicsMaterial.CONCRETE, colorOverride = concretePadColor
        )
        c.placeDynamicOnSupport(ObjectType.WOOD_CRATE, 11.0f, 17.7f, dockSlab)
        c.placeDynamicOnSupport(ObjectType.INDUSTRIAL_BARREL, 13.5f, 17.7f, dockSlab)
        c.placeStaticOnFloor(
            MeshId.WARNING_STRIPE_CURB, 11.5f, 14.8f, Vec3(3.2f, 0.14f, 0.25f),
            PhysicsMaterial.CONCRETE, colorOverride = Vec3(0.76f, 0.56f, 0.16f), label = "Loading Bay Bumper"
        )

        // ZONE 6: Elevated Watch Platform + Staircase (East)
        for (px in listOf(11.6f, 20.0f)) for (pz in listOf(-5.0f, 3.6f)) {
            c.placeStaticOnFloor(
                MeshId.PILLAR_COLUMN, px, pz, Vec3(0.28f, 1.70f, 0.28f),
                PhysicsMaterial.STEEL, colorOverride = steelFrameColor, mountType = MountType.STRUCTURAL
            )
        }
        val watchDeck = c.addStatic(
            MeshId.FLOOR_SLAB, Vec3(15.8f, 3.65f, -0.7f), Vec3(4.5f, 0.25f, 4.6f),
            PhysicsMaterial.STEEL, colorOverride = Vec3(0.32f, 0.36f, 0.41f), mountType = MountType.STRUCTURAL
        )
        c.placeStaticOnSupport(
            MeshId.GLASS_PARTITION, 11.45f, -0.7f, watchDeck, Vec3(0.08f, 0.62f, 4.3f),
            PhysicsMaterial.GLASS, alphaOverride = 0.42f, colorOverride = Vec3(0.76f, 0.88f, 0.94f)
        )
        c.placeStaticOnSupport(
            MeshId.LAB_CONSOLE, 15.8f, -1.5f, watchDeck, Vec3(1.8f, 0.55f, 0.7f),
            PhysicsMaterial.STEEL, colorOverride = steelFrameColor, label = "Range Watch Console"
        )
        c.addStaircaseToDeck(
            centerX = 15.5f, centerZ = 7.4f, floorY = 0f, topY = 3.90f,
            halfWidth = 1.20f, halfRun = 3.5f, ascendYaw = 0f
        )

        // ZONE 7: Equipment Parking Zone (SW)
        for (cx in listOf(-16.0f, -10.5f)) {
            c.placeStaticOnFloor(
                MeshId.WARNING_STRIPE_CURB, cx, 19.5f, Vec3(1.8f, 0.15f, 0.25f),
                PhysicsMaterial.CONCRETE, colorOverride = Vec3(0.76f, 0.56f, 0.16f), label = "Equipment Bay Stop"
            )
        }
        c.placeDynamicOnFloor(ObjectType.SUSPENSION_BOGIE, -16.0f, 16.5f)
        c.placeDynamicOnFloor(ObjectType.AXLE_ROD, -10.5f, 16.5f)
        c.placeDynamicOnFloor(ObjectType.STEEL_IBEAM, -13.2f, 13.5f)

        return BuiltMap(c.statics, c.dynamics, c.constraints, Vec3(0f, 0.1f, 15.5f), 0f, c.nextId, c.wires)
    }
}

