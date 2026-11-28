package com.example.render

import com.example.math.Vec3
import com.example.physics.MeshId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ProceduralMeshGenerator {

    fun buildAllMeshes(): Map<MeshId, MeshAsset> {
        val map = HashMap<MeshId, MeshAsset>(MeshId.entries.size)
        for (id in MeshId.entries) {
            map[id] = buildAssetFor(id)
        }
        return map
    }

    private fun buildAssetFor(id: MeshId): MeshAsset {
        return when (id) {
            MeshId.REINFORCED_CRATE -> makeCrate(0.55f, 0.55f, 0.55f)
            MeshId.WOOD_PALLET_CRATE -> makeWoodCrate(0.52f, 0.52f, 0.52f)
            MeshId.TREADED_TIRE -> makeTire()
            MeshId.GEODESIC_SPHERE -> makeMachinedSphere()
            MeshId.GYRO_FLYWHEEL -> makeGyroFlywheel()
            MeshId.STEEL_IBEAM -> makeIBeam()
            MeshId.GLASS_CONTAINER -> makeGlassContainer()
            MeshId.CONCRETE_MASONRY -> makeMasonryBlock()
            MeshId.DOMINO_MONOLITH -> makeDomino()
            MeshId.RAMP_WEDGE -> makeRamp()
            MeshId.CENTRIFUGE_POD -> makeCentrifuge()
            MeshId.SPRING_PISTON -> makeSpringPiston()
            MeshId.LAB_CONSOLE -> makeLabConsole()
            MeshId.SHELF_RACK -> makeShelfRack()
            MeshId.CONVEYOR_UNIT -> makeConveyor()
            MeshId.SHIPPING_CONTAINER -> makeShippingContainer()
            MeshId.SCAFFOLD_FRAME -> makeScaffold()
            MeshId.BARREL_DRUM -> makeBarrel()
            MeshId.FLOOR_SLAB -> makeArchitecturalBox(1f, 1f, 1f, bevel = 0.015f)
            MeshId.WALL_PANEL -> makeArchitecturalBox(1f, 1f, 1f, bevel = 0.02f)
            MeshId.PILLAR_COLUMN -> makeArchitecturalBox(1f, 1f, 1f, bevel = 0.04f)
            MeshId.GLASS_PARTITION -> makeGlassPartition()
            MeshId.CRANE_BOOM -> makeCraneBoom()
            MeshId.BATTERY_UNIT -> makeBatteryUnit()
            MeshId.SWITCH_BOX -> makeSwitchBox()
            MeshId.LIGHT_BULB_LAMP -> makeLightBulb()
            MeshId.LED_INDICATOR -> makeLedIndicator()
            MeshId.DC_MOTOR -> makeDcMotor()
            MeshId.KINETIC_GENERATOR -> makeKineticGenerator()
            MeshId.FUSE_BLOCK -> makeFuseBlock()
            MeshId.CIRCUIT_CONNECTOR -> makeCircuitConnector()
            MeshId.ELECTRICAL_PANEL -> makeElectricalPanel()
            MeshId.LAB_WORKBENCH -> makeLabWorkbench()
            MeshId.OFFICE_CHAIR -> makeOfficeChair()
            MeshId.METAL_CABINET -> makeMetalCabinet()
            MeshId.GEAR_WHEEL -> makeGearWheel()
            MeshId.AXLE_ROD -> makeAxleRod()
            MeshId.SUSPENSION_BOGIE -> makeSuspensionBogie()
            MeshId.SOFT_FOAM_BLOCK -> makeSoftFoamBlock()
            MeshId.BUOYANCY_FLOAT -> makeBuoyancyFloat()
            MeshId.CEILING_LIGHT -> makeCeilingLight()
            MeshId.DOOR_FRAME -> makeDoorFrame()
            MeshId.WINDOW_FRAME -> makeWindowFrame()
            MeshId.WALL_CABINET -> makeWallCabinet()
            MeshId.LAB_TABLE -> makeLabTable()
            MeshId.CNC_MACHINE -> makeCncMachine()
            MeshId.HYDRAULIC_PRESS -> makeHydraulicPress()
            MeshId.ROOF_TRUSS -> makeRoofTruss()
            MeshId.STAIR_FLIGHT -> makeStairFlight()
            MeshId.PIPE_CONDUIT -> makePipeConduit()
            MeshId.SAFETY_RAIL -> makeSafetyRail()
            MeshId.WARNING_STRIPE_CURB -> makeWarningCurb()
            MeshId.CEILING_VENT -> makeCeilingVent()
            MeshId.AMMO_BOX_9MM_AUTO,
            MeshId.AMMO_BOX_45_SERVICE,
            MeshId.AMMO_BOX_SMG_9MM,
            MeshId.AMMO_BOX_556_CARBINE,
            MeshId.AMMO_BOX_12G_BUCKSHOT,
            MeshId.AMMO_BOX_762_MATCH,
            MeshId.AMMO_BOX_762_AK47 -> makeAmmoBoxFallback()
            MeshId.GRENADE_FRAG,
            MeshId.GRENADE_SMOKE -> makeGrenadeFallback()
        }
    }

    private fun makeCrate(hx: Float, hy: Float, hz: Float): MeshAsset {
        fun build(detail: Int): MeshLevel {
            val b = MeshBuilder()
            // Recessed core cube
            b.addBox(0f, 0f, 0f, hx * 0.90f, hy * 0.90f, hz * 0.90f)
            // 12 reinforced corner frame rails
            val t = hx * 0.14f
            for (sx in listOf(-1f, 1f)) for (sy in listOf(-1f, 1f)) {
                b.addChamferedBox(sx * (hx - t * 0.7f), sy * (hy - t * 0.7f), 0f, t, t, hz, 0.02f)
            }
            for (sy in listOf(-1f, 1f)) for (sz in listOf(-1f, 1f)) {
                b.addChamferedBox(0f, sy * (hy - t * 0.7f), sz * (hz - t * 0.7f), hx, t, t, 0.02f)
            }
            for (sx in listOf(-1f, 1f)) for (sz in listOf(-1f, 1f)) {
                b.addChamferedBox(sx * (hx - t * 0.7f), 0f, sz * (hz - t * 0.7f), t, hy, t, 0.02f)
            }
            if (detail >= 1) {
                b.addBox(0f, 0f, hz * 0.91f, hx * 0.65f, t * 0.5f, 0.025f)
                b.addBox(0f, 0f, -hz * 0.91f, hx * 0.65f, t * 0.5f, 0.025f)
            }
            return b.build()
        }
        return MeshAsset(build(2), build(1), build(0), 1.1f)
    }

    private fun makeWoodCrate(hx: Float, hy: Float, hz: Float): MeshAsset {
        fun build(detail: Int): MeshLevel {
            val b = MeshBuilder()
            b.addBox(0f, 0f, 0f, hx * 0.88f, hy * 0.88f, hz * 0.88f)
            val rail = 0.07f
            for (sx in listOf(-1f, 1f)) for (sz in listOf(-1f, 1f)) {
                b.addBox(sx * (hx - rail), 0f, sz * (hz - rail), rail, hy, rail)
            }
            val slats = if (detail >= 1) listOf(-0.34f, 0f, 0.34f) else listOf(-0.34f, 0.34f)
            for (y in slats) {
                b.addBox(0f, y, hz - 0.03f, hx, 0.06f, 0.03f)
                b.addBox(0f, y, -hz + 0.03f, hx, 0.06f, 0.03f)
                b.addBox(hx - 0.03f, y, 0f, 0.03f, 0.06f, hz)
                b.addBox(-hx + 0.03f, y, 0f, 0.03f, 0.06f, hz)
            }
            return b.build()
        }
        return MeshAsset(build(2), build(1), build(0), 1.0f)
    }

    private fun makeTire(): MeshAsset {
        fun build(maj: Int, min: Int, treads: Int): MeshLevel {
            val b = MeshBuilder()
            b.addTorusXZ(0f, 0f, 0f, 0.36f, 0.16f, maj, min, rotateToVerticalX = false)
            b.addCylinderY(0f, 0f, 0f, 0.22f, 0.22f, 0.22f, maj / 2)
            for (i in 0 until treads) {
                val a = (i.toFloat() / treads) * 2f * PI.toFloat()
                b.addBox(cos(a) * 0.51f, 0f, sin(a) * 0.51f, 0.025f, 0.11f, 0.025f)
            }
            return b.build()
        }
        return MeshAsset(build(24, 12, 16), build(16, 8, 8), build(10, 6, 0), 0.65f)
    }

    private fun makeMachinedSphere(): MeshAsset {
        fun build(rings: Int, sectors: Int, collar: Boolean): MeshLevel {
            val b = MeshBuilder()
            b.addUvSphere(0f, 0f, 0f, 0.44f, rings, sectors)
            if (collar) {
                b.addTorusXZ(0f, 0f, 0f, 0.43f, 0.025f, sectors, 6)
                b.addCylinderY(0f, 0.44f, 0f, 0.08f, 0.06f, 0.08f, 8)
            }
            return b.build()
        }
        return MeshAsset(build(16, 24, true), build(10, 14, true), build(6, 10, false), 0.52f)
    }

    private fun makeGyroFlywheel(): MeshAsset {
        fun build(segs: Int): MeshLevel {
            val b = MeshBuilder()
            b.addTorusXZ(0f, 0f, 0f, 0.48f, 0.045f, segs, 8, rotateToVerticalX = true)
            b.addTorusXZ(0f, 0f, 0f, 0.38f, 0.038f, segs, 8, rotateToVerticalX = false)
            b.addCylinderY(0f, 0f, 0f, 0.04f, 0.04f, 0.88f, 10)
            b.addCylinderY(0f, 0f, 0f, 0.24f, 0.24f, 0.12f, segs)
            return b.build()
        }
        return MeshAsset(build(22), build(14), build(8), 0.72f)
    }

    private fun makeIBeam(): MeshAsset {
        fun build(detail: Int): MeshLevel {
            val b = MeshBuilder()
            b.addChamferedBox(0f, 0.17f, 0f, 1.50f, 0.05f, 0.24f, 0.015f)
            b.addChamferedBox(0f, -0.17f, 0f, 1.50f, 0.05f, 0.24f, 0.015f)
            b.addBox(0f, 0f, 0f, 1.48f, 0.14f, 0.055f)
            if (detail >= 1) {
                for (x in listOf(-1.45f, 0f, 1.45f)) {
                    b.addBox(x, 0f, 0f, 0.035f, 0.14f, 0.22f)
                }
            }
            return b.build()
        }
        return MeshAsset(build(2), build(1), build(0), 1.6f)
    }

    private fun makeGlassContainer(): MeshAsset {
        fun build(): MeshLevel {
            val b = MeshBuilder()
            b.addChamferedBox(0f, 0f, 0f, 0.50f, 0.50f, 0.50f, 0.045f)
            return b.build()
        }
        val m = build()
        return MeshAsset(m, m, m, 0.90f)
    }

    private fun makeMasonryBlock(): MeshAsset {
        fun build(detail: Int): MeshLevel {
            val b = MeshBuilder()
            b.addChamferedBox(0f, 0f, 0f, 0.60f, 0.35f, 0.35f, 0.035f)
            if (detail >= 1) {
                b.addBox(-0.26f, 0.36f, 0f, 0.10f, 0.02f, 0.16f)
                b.addBox(0.26f, 0.36f, 0f, 0.10f, 0.02f, 0.16f)
            }
            return b.build()
        }
        return MeshAsset(build(1), build(1), build(0), 0.8f)
    }

    private fun makeDomino(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, 0f, 0.14f, 0.90f, 0.45f, 0.025f)
        b.addBox(0f, 0f, 0f, 0.155f, 0.03f, 0.40f)
        val m = b.build()
        return MeshAsset(m, m, m, 1.05f)
    }

    private fun makeRamp(): MeshAsset {
        // Engineered steel ramp deck spanning [-1..1] with side safety curbs & underside ribs
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0.10f, 0f, 0.94f, 0.90f, 1.0f, 0.04f)
        b.addBox(-0.95f, 0.35f, 0f, 0.05f, 0.65f, 1.0f)
        b.addBox(0.95f, 0.35f, 0f, 0.05f, 0.65f, 1.0f)
        for (z in listOf(-0.65f, 0f, 0.65f)) {
            b.addBox(0f, -0.86f, z, 0.92f, 0.14f, 0.08f)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeCentrifuge(): MeshAsset {
        fun build(segs: Int, fins: Boolean): MeshLevel {
            val b = MeshBuilder()
            b.addCylinderY(0f, -0.45f, 0f, 0.44f, 0.42f, 0.22f, segs)
            b.addCylinderY(0f, 0.0f, 0f, 0.38f, 0.34f, 0.72f, segs)
            b.addCylinderY(0f, 0.46f, 0f, 0.36f, 0.26f, 0.22f, segs)
            if (fins) {
                for (i in 0 until 6) {
                    val a = (i.toFloat() / 6f) * 2f * PI.toFloat()
                    b.addBox(cos(a) * 0.39f, 0f, sin(a) * 0.39f, 0.035f, 0.30f, 0.035f)
                }
            }
            return b.build()
        }
        return MeshAsset(build(18, true), build(12, true), build(8, false), 0.90f)
    }

    private fun makeSpringPiston(): MeshAsset {
        fun build(steps: Int): MeshLevel {
            val b = MeshBuilder()
            b.addCylinderY(0f, -0.56f, 0f, 0.34f, 0.34f, 0.14f, 14)
            b.addCylinderY(0f, 0.56f, 0f, 0.34f, 0.34f, 0.14f, 14)
            b.addHelicalCoilY(0f, 0f, 0f, 0.22f, 0.045f, 0.98f, 5.5f, steps)
            return b.build()
        }
        return MeshAsset(build(48), build(28), build(16), 0.85f)
    }

    private fun makeLabConsole(): MeshAsset {
        // Strictly spans y=[-1..+1]: base cabinet, flanged drawer modules, recessed worktop, monitor bezel & dials
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.10f, 0f, 0.98f, 0.88f, 0.96f, 0.03f)
        b.addChamferedBox(0f, 0.90f, 0f, 1.0f, 0.10f, 1.0f, 0.02f)
        // Twin front instrumentation drawers & brushed pulls
        for (sx in listOf(-0.46f, 0.46f)) {
            b.addChamferedBox(sx, 0.34f, -0.965f, 0.38f, 0.22f, 0.025f, 0.01f)
            b.addBox(sx, 0.36f, -0.992f, 0.18f, 0.03f, 0.008f)
            b.addChamferedBox(sx, -0.32f, -0.965f, 0.38f, 0.34f, 0.025f, 0.01f)
            b.addBox(sx, -0.22f, -0.992f, 0.18f, 0.03f, 0.008f)
        }
        // Rear diagnostic riser & indicator bezel
        b.addBox(0f, 0.45f, 0.97f, 0.88f, 0.34f, 0.03f)
        b.addChamferedBox(-0.44f, 0.50f, 0.99f, 0.28f, 0.20f, 0.01f, 0.005f)
        b.addCylinderY(0.45f, 0.48f, 0.99f, 0.08f, 0.08f, 0.02f, 10)
        for (sx in listOf(-0.88f, 0.88f)) for (sz in listOf(-0.88f, 0.88f)) {
            b.addBox(sx, -0.94f, sz, 0.10f, 0.06f, 0.10f)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeShelfRack(): MeshAsset {
        val b = MeshBuilder()
        for (sx in listOf(-0.93f, 0.93f)) for (sz in listOf(-0.93f, 0.93f)) {
            b.addBox(sx, 0f, sz, 0.07f, 1.0f, 0.07f)
        }
        for (y in listOf(-0.88f, -0.18f, 0.48f, 0.95f)) {
            b.addBox(0f, y, 0f, 0.98f, 0.05f, 0.98f)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeConveyor(): MeshAsset {
        // Belt top surface strictly at y = +1.0, support legs strictly reach y = -1.0
        val b = MeshBuilder()
        b.addBox(0f, 0.72f, 0.93f, 1.0f, 0.24f, 0.07f)
        b.addBox(0f, 0.72f, -0.93f, 1.0f, 0.24f, 0.07f)
        b.addBox(0f, 0.80f, 0f, 0.98f, 0.20f, 0.86f)
        // Rollers & structural steel legs down to y = -1.0
        for (x in listOf(-0.86f, -0.43f, 0f, 0.43f, 0.86f)) {
            b.addBox(x, -0.16f, 0.88f, 0.045f, 0.84f, 0.06f)
            b.addBox(x, -0.16f, -0.88f, 0.045f, 0.84f, 0.06f)
            b.addBox(x, -0.55f, 0f, 0.035f, 0.07f, 0.86f)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeShippingContainer(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, 0f, 0.97f, 1.0f, 0.98f, 0.03f)
        for (i in -4..4) {
            val t = i * 0.21f
            b.addBox(0.985f, 0f, t, 0.015f, 0.92f, 0.04f)
            b.addBox(-0.985f, 0f, t, 0.015f, 0.92f, 0.04f)
        }
        // Corner ISO castings
        for (sx in listOf(-0.94f, 0.94f)) for (sy in listOf(-0.94f, 0.94f)) for (sz in listOf(-0.94f, 0.94f)) {
            b.addBox(sx, sy, sz, 0.06f, 0.06f, 0.06f)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeScaffold(): MeshAsset {
        val b = MeshBuilder()
        for (sx in listOf(-0.95f, 0.95f)) for (sz in listOf(-0.95f, 0.95f)) {
            b.addBox(sx, 0f, sz, 0.05f, 1.0f, 0.05f)
        }
        for (y in listOf(-0.65f, 0.05f, 0.65f, 0.96f)) {
            b.addBox(0f, y, 0.95f, 0.95f, 0.035f, 0.035f)
            b.addBox(0f, y, -0.95f, 0.95f, 0.035f, 0.035f)
            b.addBox(0.95f, y, 0f, 0.035f, 0.035f, 0.95f)
            b.addBox(-0.95f, y, 0f, 0.035f, 0.035f, 0.95f)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeBarrel(): MeshAsset {
        fun build(segs: Int): MeshLevel {
            val b = MeshBuilder()
            b.addCylinderY(0f, 0f, 0f, 0.37f, 0.37f, 1.14f, segs)
            b.addTorusXZ(0f, 0.28f, 0f, 0.365f, 0.022f, segs, 6)
            b.addTorusXZ(0f, -0.28f, 0f, 0.365f, 0.022f, segs, 6)
            return b.build()
        }
        return MeshAsset(build(18), build(12), build(8), 0.78f)
    }

    private fun makeArchitecturalBox(hx: Float, hy: Float, hz: Float, bevel: Float): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, 0f, hx, hy, hz, bevel)
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeGlassPartition(): MeshAsset {
        val b = MeshBuilder()
        b.addBox(0f, 0f, 0f, 0.98f, 0.94f, 0.70f)
        b.addBox(0f, 0.97f, 0f, 1.0f, 0.03f, 1.0f)
        b.addBox(0f, -0.97f, 0f, 1.0f, 0.03f, 1.0f)
        b.addBox(-0.97f, 0f, 0f, 0.03f, 1.0f, 1.0f)
        b.addBox(0.97f, 0f, 0f, 0.03f, 1.0f, 1.0f)
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeCraneBoom(): MeshAsset {
        val b = MeshBuilder()
        b.addBox(0f, 0.85f, 0.85f, 1.0f, 0.12f, 0.12f)
        b.addBox(0f, 0.85f, -0.85f, 1.0f, 0.12f, 0.12f)
        b.addBox(0f, -0.85f, 0f, 1.0f, 0.14f, 0.16f)
        for (i in -4..4) {
            b.addBox(i * 0.22f, 0f, 0f, 0.03f, 0.85f, 0.85f)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeBatteryUnit(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.02f, 0f, 0.32f, 0.24f, 0.24f, 0.025f)
        b.addCylinderY(-0.16f, 0.24f, 0f, 0.042f, 0.038f, 0.08f, 10)
        b.addCylinderY(0.16f, 0.24f, 0f, 0.042f, 0.038f, 0.08f, 10)
        b.addBox(0f, 0.22f, 0f, 0.24f, 0.02f, 0.04f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.55f)
    }

    private fun makeSwitchBox(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.04f, 0f, 0.24f, 0.11f, 0.24f, 0.02f)
        b.addCylinderY(0f, 0.08f, 0f, 0.055f, 0.055f, 0.04f, 10)
        b.addBox(0f, 0.13f, 0.04f, 0.022f, 0.07f, 0.022f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.40f)
    }

    private fun makeLightBulb(): MeshAsset {
        val b = MeshBuilder()
        b.addCylinderY(0f, -0.20f, 0f, 0.10f, 0.11f, 0.18f, 12)
        b.addUvSphere(0f, 0.05f, 0f, 0.21f, 12, 16)
        val m = b.build()
        return MeshAsset(m, m, m, 0.45f)
    }

    private fun makeLedIndicator(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.14f, 0f, 0.16f, 0.06f, 0.16f, 0.015f)
        b.addCylinderY(0f, 0.02f, 0f, 0.10f, 0.09f, 0.20f, 12)
        b.addUvSphere(0f, 0.12f, 0f, 0.095f, 8, 12)
        val m = b.build()
        return MeshAsset(m, m, m, 0.36f)
    }

    private fun makeDcMotor(): MeshAsset {
        val b = MeshBuilder()
        b.addBox(0f, -0.26f, 0f, 0.34f, 0.05f, 0.32f)
        b.addCylinderY(0f, 0.02f, 0f, 0.26f, 0.26f, 0.50f, 16)
        b.addCylinderY(0f, 0.32f, 0f, 0.055f, 0.055f, 0.16f, 10)
        b.addBox(0.28f, 0.06f, 0f, 0.08f, 0.12f, 0.14f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.62f)
    }

    private fun makeKineticGenerator(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.28f, 0f, 0.40f, 0.06f, 0.40f, 0.02f)
        b.addCylinderY(0f, 0.02f, 0f, 0.30f, 0.30f, 0.48f, 16)
        for (i in 0 until 6) {
            val a = (i.toFloat() / 6f) * 2f * PI.toFloat()
            b.addBox(cos(a) * 0.33f, 0.02f, sin(a) * 0.33f, 0.03f, 0.22f, 0.03f)
        }
        b.addTorusXZ(0f, 0.30f, 0f, 0.24f, 0.04f, 14, 6)
        val m = b.build()
        return MeshAsset(m, m, m, 0.72f)
    }

    private fun makeFuseBlock(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.06f, 0f, 0.26f, 0.06f, 0.18f, 0.015f)
        b.addBox(-0.18f, 0.04f, 0f, 0.04f, 0.08f, 0.08f)
        b.addBox(0.18f, 0.04f, 0f, 0.04f, 0.08f, 0.08f)
        b.addCylinderY(0f, 0.05f, 0f, 0.055f, 0.055f, 0.18f, 10)
        val m = b.build()
        return MeshAsset(m, m, m, 0.38f)
    }

    private fun makeCircuitConnector(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.04f, 0f, 0.20f, 0.08f, 0.20f, 0.015f)
        b.addCylinderY(-0.08f, 0.08f, 0f, 0.038f, 0.038f, 0.10f, 8)
        b.addCylinderY(0.08f, 0.08f, 0f, 0.038f, 0.038f, 0.10f, 8)
        val m = b.build()
        return MeshAsset(m, m, m, 0.34f)
    }

    private fun makeElectricalPanel(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, 0f, 0.46f, 0.60f, 0.18f, 0.025f)
        // Breaker switch bank, analog gauge bezels, busbar conduit collar & warning faceplate
        b.addCylinderY(-0.20f, 0.28f, 0.19f, 0.10f, 0.10f, 0.03f, 12)
        b.addCylinderY(0.20f, 0.28f, 0.19f, 0.10f, 0.10f, 0.03f, 12)
        b.addBox(0f, 0.02f, 0.19f, 0.34f, 0.09f, 0.022f)
        for (bx in listOf(-0.22f, -0.07f, 0.08f, 0.23f)) {
            b.addBox(bx, 0.02f, 0.21f, 0.035f, 0.045f, 0.015f)
        }
        b.addBox(0f, -0.26f, 0.19f, 0.34f, 0.16f, 0.02f)
        b.addCylinderY(-0.24f, 0.61f, 0f, 0.055f, 0.055f, 0.06f, 8)
        b.addCylinderY(0.24f, 0.61f, 0f, 0.055f, 0.055f, 0.06f, 8)
        val m = b.build()
        return MeshAsset(m, m, m, 0.85f)
    }

    private fun makeLabWorkbench(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0.40f, 0f, 1.10f, 0.08f, 0.55f, 0.02f)
        for (sx in listOf(-0.98f, 0.98f)) for (sz in listOf(-0.45f, 0.45f)) {
            b.addBox(sx, -0.04f, sz, 0.06f, 0.44f, 0.06f)
        }
        b.addBox(0f, -0.22f, 0f, 0.96f, 0.04f, 0.42f)
        val m = b.build()
        return MeshAsset(m, m, m, 1.35f)
    }

    private fun makeOfficeChair(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.04f, 0f, 0.32f, 0.06f, 0.32f, 0.02f)
        b.addChamferedBox(0f, 0.28f, -0.28f, 0.28f, 0.26f, 0.05f, 0.02f)
        b.addCylinderY(0f, -0.26f, 0f, 0.045f, 0.045f, 0.38f, 10)
        b.addBox(0f, -0.48f, 0f, 0.32f, 0.04f, 0.06f)
        b.addBox(0f, -0.48f, 0f, 0.06f, 0.04f, 0.32f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.78f)
    }

    private fun makeMetalCabinet(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, 0f, 0.54f, 0.86f, 0.35f, 0.025f)
        b.addBox(-0.26f, 0f, 0.36f, 0.24f, 0.78f, 0.018f)
        b.addBox(0.26f, 0f, 0.36f, 0.24f, 0.78f, 0.018f)
        val m = b.build()
        return MeshAsset(m, m, m, 1.12f)
    }

    private fun makeGearWheel(): MeshAsset {
        fun build(teeth: Int): MeshLevel {
            val b = MeshBuilder()
            b.addCylinderY(0f, 0f, 0f, 0.38f, 0.38f, 0.18f, teeth)
            b.addCylinderY(0f, 0f, 0f, 0.14f, 0.14f, 0.26f, 10)
            for (i in 0 until teeth) {
                val a = (i.toFloat() / teeth) * 2f * PI.toFloat()
                b.addBox(cos(a) * 0.43f, 0f, sin(a) * 0.43f, 0.055f, 0.08f, 0.055f)
            }
            return b.build()
        }
        return MeshAsset(build(14), build(10), build(8), 0.52f)
    }

    private fun makeAxleRod(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, 0f, 0.94f, 0.09f, 0.09f, 0.02f)
        b.addBox(-0.84f, 0f, 0f, 0.08f, 0.13f, 0.13f)
        b.addBox(0.84f, 0f, 0f, 0.08f, 0.13f, 0.13f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.98f)
    }

    private fun makeSuspensionBogie(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0.08f, 0f, 0.72f, 0.12f, 0.34f, 0.02f)
        for (sx in listOf(-0.48f, 0.48f)) for (sz in listOf(-0.40f, 0.40f)) {
            b.addCylinderY(sx, -0.14f, sz, 0.18f, 0.18f, 0.14f, 12)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 0.98f)
    }

    private fun makeSoftFoamBlock(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, 0f, 0.50f, 0.42f, 0.50f, 0.09f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.85f)
    }

    private fun makeBuoyancyFloat(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, 0f, 0.45f, 0.45f, 0.45f, 0.05f)
        b.addTorusXZ(0f, 0.26f, 0f, 0.34f, 0.03f, 14, 6)
        val m = b.build()
        return MeshAsset(m, m, m, 0.80f)
    }

    // --- Phase 3 Architectural & Heavy Equipment Compound Meshes (strict [-1..+1] bounds) ---

    private fun makeCeilingLight(): MeshAsset {
        // Recessed / suspended linear LED troffer fixture: mounting canopy at y=+1, housing & diffuser down to y=-1
        val b = MeshBuilder()
        // Top ceiling canopy & mounting brackets flush to y=+1.0
        b.addBox(0f, 0.86f, 0f, 0.92f, 0.14f, 0.85f)
        b.addBox(-0.65f, 0.48f, 0f, 0.08f, 0.38f, 0.45f)
        b.addBox(0.65f, 0.48f, 0f, 0.08f, 0.38f, 0.45f)
        // Aluminum reflector frame & louver fins
        b.addChamferedBox(0f, -0.15f, 0f, 1.0f, 0.38f, 1.0f, 0.05f)
        // Bottom luminous lens panel at y=-0.75..-1.0
        b.addBox(0f, -0.78f, 0f, 0.92f, 0.22f, 0.86f)
        for (x in listOf(-0.55f, 0f, 0.55f)) {
            b.addBox(x, -0.94f, 0f, 0.03f, 0.06f, 0.88f)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeDoorFrame(): MeshAsset {
        // Hollow architectural doorway frame spanning [-1..+1]: left jamb, right jamb, top lintel + threshold sill
        val b = MeshBuilder()
        b.addChamferedBox(-0.91f, 0f, 0f, 0.09f, 1.0f, 1.0f, 0.02f)
        b.addChamferedBox(0.91f, 0f, 0f, 0.09f, 1.0f, 1.0f, 0.02f)
        b.addChamferedBox(0f, 0.92f, 0f, 1.0f, 0.08f, 1.0f, 0.02f)
        b.addBox(0f, -0.97f, 0f, 0.82f, 0.03f, 0.92f)
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeWindowFrame(): MeshAsset {
        // Architectural steel window frame with perimeter sash & central vertical mullion, spanning [-1..+1]
        val b = MeshBuilder()
        b.addChamferedBox(-0.93f, 0f, 0f, 0.07f, 1.0f, 1.0f, 0.02f)
        b.addChamferedBox(0.93f, 0f, 0f, 0.07f, 1.0f, 1.0f, 0.02f)
        b.addChamferedBox(0f, 0.92f, 0f, 1.0f, 0.08f, 1.0f, 0.02f)
        b.addChamferedBox(0f, -0.92f, 0f, 1.0f, 0.08f, 1.0f, 0.02f)
        b.addBox(0f, 0f, 0f, 0.045f, 0.86f, 0.72f)
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeWallCabinet(): MeshAsset {
        // Wall-mounted equipment cabinet with recessed doors, handles & top vent louver, spanning [-1..+1]
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, -0.08f, 1.0f, 1.0f, 0.92f, 0.04f)
        // Twin front door leaves
        b.addChamferedBox(-0.48f, -0.02f, 0.90f, 0.45f, 0.90f, 0.07f, 0.02f)
        b.addChamferedBox(0.48f, -0.02f, 0.90f, 0.45f, 0.90f, 0.07f, 0.02f)
        // Door handles
        b.addBox(-0.14f, -0.05f, 0.985f, 0.03f, 0.24f, 0.015f)
        b.addBox(0.14f, -0.05f, 0.985f, 0.03f, 0.24f, 0.015f)
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeLabTable(): MeshAsset {
        // Open-frame 4-legged laboratory worktable with steel frame, lower stretcher bar, and solid phenolic worktop at y=+1.0
        val b = MeshBuilder()
        // Top work surface slab [y: 0.84..1.0]
        b.addChamferedBox(0f, 0.92f, 0f, 1.0f, 0.08f, 1.0f, 0.02f)
        // Under-top apron frame [y: 0.70..0.84]
        b.addBox(0f, 0.77f, 0.86f, 0.92f, 0.07f, 0.05f)
        b.addBox(0f, 0.77f, -0.86f, 0.92f, 0.07f, 0.05f)
        b.addBox(-0.86f, 0.77f, 0f, 0.05f, 0.07f, 0.82f)
        b.addBox(0.86f, 0.77f, 0f, 0.05f, 0.07f, 0.82f)
        // 4 sturdy steel legs reaching strictly down to y=-1.0
        for (sx in listOf(-0.88f, 0.88f)) for (sz in listOf(-0.86f, 0.86f)) {
            b.addBox(sx, -0.08f, sz, 0.06f, 0.92f, 0.06f)
        }
        // Lower crossbar stretcher
        b.addBox(0f, -0.62f, -0.84f, 0.86f, 0.045f, 0.045f)
        b.addBox(-0.86f, -0.62f, 0f, 0.045f, 0.045f, 0.84f)
        b.addBox(0.86f, -0.62f, 0f, 0.045f, 0.045f, 0.84f)
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeCncMachine(): MeshAsset {
        // Industrial 3-axis CNC machining center enclosure: heavy base, upper column, spindle housing & side control console
        val b = MeshBuilder()
        // Heavy cast base [-1.0..-0.25]
        b.addChamferedBox(0f, -0.62f, 0f, 1.0f, 0.38f, 0.96f, 0.04f)
        // Rear & side enclosure walls [-0.25..0.85]
        b.addChamferedBox(0f, 0.30f, -0.58f, 0.96f, 0.58f, 0.38f, 0.04f)
        b.addBox(-0.88f, 0.25f, 0.08f, 0.10f, 0.54f, 0.64f)
        b.addBox(0.88f, 0.25f, 0.08f, 0.10f, 0.54f, 0.64f)
        // Top spindle bridge & cap up to y=+1.0
        b.addChamferedBox(0f, 0.90f, -0.10f, 0.98f, 0.10f, 0.86f, 0.03f)
        // Spindle head cylinder inside bay
        b.addCylinderY(0f, 0.48f, 0.15f, 0.18f, 0.22f, 0.62f, 12)
        // Front worktable & vice inside enclosure
        b.addBox(0f, -0.12f, 0.20f, 0.62f, 0.12f, 0.42f)
        // Right side CNC control panel arm
        b.addBox(0.84f, 0.22f, 0.86f, 0.16f, 0.38f, 0.14f)
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeHydraulicPress(): MeshAsset {
        // Heavy industrial H-frame hydraulic press spanning [-1..+1]
        val b = MeshBuilder()
        // Floor base feet & platen bed
        b.addChamferedBox(0f, -0.88f, 0f, 1.0f, 0.12f, 0.96f, 0.03f)
        // Twin H-frame vertical columns
        b.addChamferedBox(-0.82f, 0f, 0f, 0.16f, 1.0f, 0.52f, 0.03f)
        b.addChamferedBox(0.82f, 0f, 0f, 0.16f, 1.0f, 0.52f, 0.03f)
        // Upper crown beam & hydraulic cylinder top [y: +0.55..+1.0]
        b.addChamferedBox(0f, 0.72f, 0f, 0.98f, 0.18f, 0.56f, 0.03f)
        b.addCylinderY(0f, 0.65f, 0f, 0.24f, 0.24f, 0.70f, 14)
        // Ram platen & bolster anvil
        b.addBox(0f, 0.16f, 0f, 0.46f, 0.10f, 0.44f)
        b.addBox(0f, -0.32f, 0f, 0.68f, 0.14f, 0.50f)
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeRoofTruss(): MeshAsset {
        // Steel Warren roof truss along X spanning [-1..+1]: top chord, bottom chord & web diagonals/verticals
        val b = MeshBuilder()
        b.addBox(0f, 0.90f, 0f, 1.0f, 0.10f, 0.95f)
        b.addBox(0f, -0.90f, 0f, 1.0f, 0.10f, 0.95f)
        for (i in -4..4) {
            val x = i * 0.23f
            b.addBox(x, 0f, 0f, 0.035f, 0.86f, 0.70f)
        }
        // Gusset plates at panel nodes
        for (i in -2..2) {
            b.addBox(i * 0.46f, 0f, 0f, 0.06f, 0.86f, 0.88f)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeStairFlight(): MeshAsset {
        // Ascending stair flight from bottom (z=+1, y=-1) up to top (z=-1, y=+1) inside [-1..+1]
        val b = MeshBuilder()
        val steps = 8
        val stepDepth = 2.0f / steps
        val stepRise = 2.0f / steps
        for (i in 0 until steps) {
            val yTop = -1.0f + (i + 1) * stepRise
            val zCenter = 1.0f - (i + 0.5f) * stepDepth
            // Solid step block down to y=-1 or stepped soffit for clean underside
            val soffitBottom = (-1.0f + (i - 1).coerceAtLeast(0) * stepRise)
            val cy = (yTop + soffitBottom) * 0.5f
            val hy = (yTop - soffitBottom) * 0.5f
            b.addBox(0f, cy, zCenter, 0.88f, hy.coerceAtLeast(0.06f), stepDepth * 0.52f)
        }
        // Left & right stair stringers & handrail posts
        for (sx in listOf(-0.94f, 0.94f)) {
            b.addBox(sx, -0.52f, 0.88f, 0.055f, 0.48f, 0.12f)
            b.addBox(sx, 0.0f, 0.0f, 0.055f, 0.48f, 0.12f)
            b.addBox(sx, 0.52f, -0.88f, 0.055f, 0.48f, 0.12f)
            // Angled handrail segments approximated by stepped top rail bars
            for (k in 0 until 4) {
                val zMid = 0.75f - k * 0.5f
                val yRail = (-0.25f + k * 0.40f).coerceAtMost(0.95f)
                b.addBox(sx, yRail, zMid, 0.045f, 0.045f, 0.28f)
            }
        }
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makePipeConduit(): MeshAsset {
        // Horizontal multi-flange industrial utility pipe run spanning [-1..+1] along X
        val b = MeshBuilder()
        for (i in 0 until 8) {
            val x = -0.875f + i * 0.25f
            b.addBox(x, 0f, 0f, 0.125f, 0.65f, 0.65f)
        }
        for (fx in listOf(-0.92f, -0.32f, 0.32f, 0.92f)) {
            b.addBox(fx, 0f, 0f, 0.06f, 0.98f, 0.98f)
        }
        // Valve wheel collar top
        b.addCylinderY(0f, 0.78f, 0f, 0.22f, 0.22f, 0.20f, 10)
        b.addTorusXZ(0f, 0.92f, 0f, 0.32f, 0.06f, 12, 6)
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeSafetyRail(): MeshAsset {
        // Industrial tubular guardrail spanning X [-1..+1], height [-1..+1]
        val b = MeshBuilder()
        for (px in listOf(-0.92f, 0f, 0.92f)) {
            b.addBox(px, 0f, 0f, 0.06f, 1.0f, 0.45f)
            b.addBox(px, -0.94f, 0f, 0.12f, 0.06f, 0.85f) // floor flange plate
        }
        b.addBox(0f, 0.92f, 0f, 1.0f, 0.08f, 0.48f) // top rail
        b.addBox(0f, 0.08f, 0f, 1.0f, 0.06f, 0.38f) // mid knee rail
        b.addBox(0f, -0.78f, 0f, 1.0f, 0.12f, 0.25f) // bottom toe kickplate
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeWarningCurb(): MeshAsset {
        // Low-profile beveled parking / dock safety wheel-stop & curb
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.25f, 0f, 1.0f, 0.75f, 0.98f, 0.18f)
        for (sx in listOf(-0.65f, 0f, 0.65f)) {
            b.addBox(sx, 0.56f, 0f, 0.14f, 0.40f, 0.55f)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeCeilingVent(): MeshAsset {
        // HVAC duct & louvered exhaust diffuser mounted flush to ceiling at y=+1.0
        val b = MeshBuilder()
        b.addBox(0f, 0.72f, 0f, 0.94f, 0.28f, 0.94f)
        b.addChamferedBox(0f, 0.10f, 0f, 1.0f, 0.45f, 1.0f, 0.06f)
        for (i in -3..3) {
            b.addBox(i * 0.26f, -0.58f, 0f, 0.045f, 0.24f, 0.88f)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 1.8f)
    }

    private fun makeAmmoBoxFallback(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.02f, 0f, 0.22f, 0.11f, 0.14f, 0.015f)
        b.addChamferedBox(0f, 0.11f, 0f, 0.23f, 0.025f, 0.15f, 0.01f)
        b.addBox(0f, 0.145f, 0f, 0.08f, 0.015f, 0.02f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.32f)
    }

    private fun makeGrenadeFallback(): MeshAsset {
        val b = MeshBuilder()
        b.addCylinderY(0f, -0.02f, 0f, 0.075f, 0.085f, 0.19f, 12)
        b.addCylinderY(0f, 0.10f, 0f, 0.036f, 0.036f, 0.055f, 10)
        val m = b.build()
        return MeshAsset(m, m, m, 0.16f)
    }
}

