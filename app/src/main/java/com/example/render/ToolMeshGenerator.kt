package com.example.render

import com.example.physics.ToolMeshStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ToolMeshGenerator {

    fun buildAllToolMeshes(): Map<ToolMeshStyle, MeshAsset> {
        val map = HashMap<ToolMeshStyle, MeshAsset>(ToolMeshStyle.entries.size)
        for (style in ToolMeshStyle.entries) {
            map[style] = buildToolAsset(style)
        }
        return map
    }

    private fun buildToolAsset(style: ToolMeshStyle): MeshAsset {
        return when (style) {
            ToolMeshStyle.HAMMER_CLAW -> makeHammer()
            ToolMeshStyle.MALLET_HEAD -> makeMallet()
            ToolMeshStyle.WRENCH_OPEN -> makeWrench()
            ToolMeshStyle.RATCHET_SOCKET -> makeRatchet()
            ToolMeshStyle.SCREWDRIVER_DRIVER -> makeScrewdriver()
            ToolMeshStyle.PLIERS_JAWS -> makePliers()
            ToolMeshStyle.HAND_SAW_BLADE -> makeHandSaw()
            ToolMeshStyle.POWER_DRILL -> makePowerDrill()
            ToolMeshStyle.ANGLE_GRINDER -> makeAngleGrinder()
            ToolMeshStyle.CIRCULAR_SAW -> makeCircularSaw()
            ToolMeshStyle.HEAT_GUN_NOZZLE -> makeHeatGun()
            ToolMeshStyle.WELDING_UNIT -> makeWeldingUnit()
            ToolMeshStyle.WELDING_TORCH -> makeWeldingTorch()
            ToolMeshStyle.CLAMP_C_FRAME -> makeCClamp()
            ToolMeshStyle.HYDRAULIC_JACK -> makeHydraulicJack()
            ToolMeshStyle.HOIST_WINCH -> makeHoistWinch()
            ToolMeshStyle.COMPRESSOR_TANK -> makeCompressor()
            ToolMeshStyle.FAN_SHROUD -> makeIndustrialFan()
            ToolMeshStyle.GENERATOR_FRAME -> makeGeneratorFrame()
            ToolMeshStyle.DIGITAL_METER -> makeDigitalMeter()
            ToolMeshStyle.THERMOMETER_PROBE -> makeThermometer()
            ToolMeshStyle.SCALE_PLATFORM -> makeDigitalScale()
            ToolMeshStyle.CALIPER_SLIDE -> makeCaliper()
            ToolMeshStyle.PRESSURE_DIAL -> makePressureDial()
            ToolMeshStyle.OPTICAL_MICROSCOPE -> makeMicroscope()
            ToolMeshStyle.LAB_BEAKER -> makeBeaker()
            ToolMeshStyle.EXTINGUISHER_CYL -> makeExtinguisher()
            ToolMeshStyle.FLASHLIGHT_TORCH -> makeFlashlight()
            ToolMeshStyle.WORKLIGHT_TRIPOD -> makeWorklight()
            ToolMeshStyle.DETECTOR_WAND -> makeMetalDetector()
            ToolMeshStyle.MAGNET_HORSESHOE -> makeMagnet()
            ToolMeshStyle.SUCTION_CUP_HANDLE -> makeSuctionCup()
            ToolMeshStyle.PULLEY_BLOCK -> makePulleyBlock()
            ToolMeshStyle.SPIRIT_LEVEL -> makeSpiritLevel()
            ToolMeshStyle.FIREARM_GLOCK18,
            ToolMeshStyle.FIREARM_SERVICE_PISTOL -> makePistolFallback()
            ToolMeshStyle.FIREARM_MP5,
            ToolMeshStyle.FIREARM_M4A1,
            ToolMeshStyle.FIREARM_SHOTGUN,
            ToolMeshStyle.FIREARM_SNIPER,
            ToolMeshStyle.FIREARM_AK47 -> makeLongGunFallback()
        }
    }

    private fun makeHammer(): MeshAsset {
        val b = MeshBuilder()
        // Ergonomic handle + steel head + claw + striking face
        b.addCylinderY(0f, -0.05f, 0f, 0.032f, 0.038f, 0.56f, 10)
        b.addChamferedBox(0f, 0.24f, 0.02f, 0.048f, 0.045f, 0.17f, 0.01f)
        b.addCylinderY(0f, 0.24f, 0.19f, 0.044f, 0.044f, 0.04f, 10)
        b.addBox(-0.02f, 0.23f, -0.17f, 0.015f, 0.025f, 0.07f)
        b.addBox(0.02f, 0.23f, -0.17f, 0.015f, 0.025f, 0.07f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.48f)
    }

    private fun makeMallet(): MeshAsset {
        val b = MeshBuilder()
        b.addCylinderY(0f, -0.06f, 0f, 0.030f, 0.036f, 0.54f, 10)
        b.addChamferedBox(0f, 0.22f, 0f, 0.075f, 0.075f, 0.14f, 0.02f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.45f)
    }

    private fun makeWrench(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, 0f, 0.042f, 0.24f, 0.018f, 0.006f)
        b.addChamferedBox(-0.04f, 0.24f, 0f, 0.032f, 0.06f, 0.022f, 0.006f)
        b.addChamferedBox(0.04f, 0.24f, 0f, 0.032f, 0.06f, 0.022f, 0.006f)
        b.addTorusXZ(0f, -0.24f, 0f, 0.055f, 0.018f, 12, 6, rotateToVerticalX = true)
        val m = b.build()
        return MeshAsset(m, m, m, 0.40f)
    }

    private fun makeRatchet(): MeshAsset {
        val b = MeshBuilder()
        b.addCylinderY(0f, -0.06f, 0f, 0.028f, 0.026f, 0.44f, 10)
        b.addCylinderY(0f, 0.20f, 0f, 0.068f, 0.068f, 0.055f, 12)
        b.addCylinderY(0f, 0.24f, 0.05f, 0.036f, 0.036f, 0.08f, 10)
        val m = b.build()
        return MeshAsset(m, m, m, 0.38f)
    }

    private fun makeScrewdriver(): MeshAsset {
        val b = MeshBuilder()
        b.addCylinderY(0f, -0.14f, 0f, 0.046f, 0.042f, 0.24f, 10)
        b.addCylinderY(0f, 0.14f, 0f, 0.014f, 0.012f, 0.34f, 8)
        b.addBox(0f, 0.32f, 0f, 0.020f, 0.03f, 0.006f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.36f)
    }

    private fun makePliers(): MeshAsset {
        val b = MeshBuilder()
        b.addBox(-0.035f, -0.12f, 0f, 0.018f, 0.16f, 0.020f)
        b.addBox(0.035f, -0.12f, 0f, 0.018f, 0.16f, 0.020f)
        b.addCylinderY(0f, 0.05f, 0f, 0.046f, 0.046f, 0.038f, 10)
        b.addBox(-0.018f, 0.16f, 0f, 0.016f, 0.10f, 0.018f)
        b.addBox(0.018f, 0.16f, 0f, 0.016f, 0.10f, 0.018f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.36f)
    }

    private fun makeHandSaw(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.18f, 0f, 0.08f, 0.06f, 0.035f, 0.01f)
        b.addBox(0f, 0.08f, 0f, 0.09f, 0.26f, 0.008f)
        for (i in 0 until 6) {
            b.addBox(0.09f, -0.10f + i * 0.075f, 0f, 0.022f, 0.018f, 0.008f)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 0.45f)
    }

    private fun makePowerDrill(): MeshAsset {
        val b = MeshBuilder()
        // Ergonomic grip ribs + trigger guard + battery pack + brushed motor vents + keyless chuck + fluted bit
        b.addChamferedBox(0f, -0.10f, -0.04f, 0.044f, 0.12f, 0.052f, 0.01f)
        b.addBox(0f, -0.02f, 0.04f, 0.018f, 0.05f, 0.042f) // trigger & guard
        b.addChamferedBox(0f, -0.22f, -0.04f, 0.064f, 0.042f, 0.078f, 0.01f)
        b.addChamferedBox(0f, 0.07f, 0.02f, 0.056f, 0.068f, 0.17f, 0.015f)
        b.addBox(0f, 0.07f, -0.155f, 0.048f, 0.052f, 0.012f) // rear cooling grille
        b.addCylinderY(0f, 0.07f, 0.21f, 0.042f, 0.038f, 0.05f, 12) // torque collar
        b.addCylinderY(0f, 0.07f, 0.26f, 0.032f, 0.024f, 0.05f, 10) // chuck nose
        b.addBox(0f, 0.07f, 0.33f, 0.010f, 0.010f, 0.09f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.44f)
    }

    private fun makeAngleGrinder(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, -0.08f, 0.052f, 0.052f, 0.20f, 0.015f)
        b.addBox(0.085f, -0.01f, 0.08f, 0.065f, 0.018f, 0.018f) // side aux handle
        b.addCylinderY(0f, 0.02f, 0.14f, 0.065f, 0.065f, 0.08f, 12)
        b.addCylinderY(0f, 0.07f, 0.14f, 0.024f, 0.024f, 0.02f, 8) // spindle lock cap
        b.addCylinderY(0f, -0.04f, 0.16f, 0.16f, 0.16f, 0.016f, 16)
        b.addBox(0f, -0.01f, 0.10f, 0.17f, 0.028f, 0.09f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.44f)
    }

    private fun makeCircularSaw(): MeshAsset {
        val b = MeshBuilder()
        b.addBox(0f, -0.08f, 0f, 0.14f, 0.018f, 0.22f)
        b.addChamferedBox(-0.04f, 0.06f, 0f, 0.08f, 0.09f, 0.14f, 0.02f)
        b.addChamferedBox(-0.04f, 0.16f, -0.04f, 0.032f, 0.04f, 0.08f, 0.01f) // top loop handle
        b.addCylinderY(0.06f, -0.02f, 0f, 0.16f, 0.16f, 0.016f, 16)
        b.addBox(0.06f, 0.08f, 0f, 0.022f, 0.08f, 0.16f) // upper blade guard
        val m = b.build()
        return MeshAsset(m, m, m, 0.44f)
    }

    private fun makeHeatGun(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.10f, -0.04f, 0.04f, 0.11f, 0.046f, 0.01f)
        b.addChamferedBox(0f, 0.06f, 0f, 0.054f, 0.056f, 0.15f, 0.015f)
        b.addCylinderY(0f, 0.06f, 0.17f, 0.046f, 0.040f, 0.04f, 10) // heat shield flange
        b.addCylinderY(0f, 0.06f, 0.24f, 0.032f, 0.022f, 0.11f, 10)
        val m = b.build()
        return MeshAsset(m, m, m, 0.42f)
    }

    private fun makeWeldingUnit(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, 0f, 0.20f, 0.24f, 0.28f, 0.025f)
        b.addBox(0f, 0.26f, 0f, 0.03f, 0.03f, 0.16f) // carry handle
        b.addBox(0.21f, 0.08f, 0.12f, 0.02f, 0.08f, 0.08f) // dial panel
        val m = b.build()
        return MeshAsset(m, m, m, 0.46f)
    }

    private fun makeWeldingTorch(): MeshAsset {
        val b = MeshBuilder()
        b.addCylinderY(0f, -0.06f, 0f, 0.032f, 0.032f, 0.26f, 10)
        b.addCylinderY(0f, 0.14f, 0.05f, 0.018f, 0.012f, 0.20f, 8)
        b.addCylinderY(0f, 0.24f, 0.09f, 0.024f, 0.018f, 0.05f, 8)
        val m = b.build()
        return MeshAsset(m, m, m, 0.36f)
    }

    private fun makeCClamp(): MeshAsset {
        val b = MeshBuilder()
        b.addBox(-0.10f, 0f, 0f, 0.03f, 0.20f, 0.03f)
        b.addBox(0f, 0.18f, 0f, 0.12f, 0.03f, 0.03f)
        b.addBox(0f, -0.18f, 0f, 0.12f, 0.03f, 0.03f)
        b.addCylinderY(0.10f, -0.06f, 0f, 0.016f, 0.016f, 0.26f, 8)
        val m = b.build()
        return MeshAsset(m, m, m, 0.35f)
    }

    private fun makeHydraulicJack(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.18f, 0f, 0.14f, 0.04f, 0.14f, 0.01f)
        b.addCylinderY(0f, -0.02f, 0f, 0.08f, 0.08f, 0.26f, 12)
        b.addCylinderY(0f, 0.16f, 0f, 0.045f, 0.045f, 0.14f, 10)
        b.addBox(0.16f, -0.05f, 0f, 0.12f, 0.015f, 0.015f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.40f)
    }

    private fun makeHoistWinch(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, 0f, 0.16f, 0.14f, 0.16f, 0.02f)
        b.addCylinderY(0f, 0f, 0f, 0.10f, 0.10f, 0.22f, 12)
        b.addBox(0.20f, 0.06f, 0f, 0.02f, 0.12f, 0.02f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.38f)
    }

    private fun makeCompressor(): MeshAsset {
        val b = MeshBuilder()
        b.addCylinderY(0f, -0.02f, 0f, 0.16f, 0.16f, 0.34f, 14)
        b.addChamferedBox(0f, 0.18f, 0f, 0.10f, 0.06f, 0.12f, 0.015f)
        b.addBox(0f, -0.20f, 0f, 0.18f, 0.02f, 0.16f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.42f)
    }

    private fun makeIndustrialFan(): MeshAsset {
        val b = MeshBuilder()
        b.addTorusXZ(0f, 0f, 0f, 0.22f, 0.032f, 16, 6, rotateToVerticalX = true)
        b.addCylinderY(0f, 0f, 0f, 0.06f, 0.06f, 0.10f, 10)
        for (i in 0 until 4) {
            val a = i * (PI.toFloat() / 2f)
            b.addBox(0f, cos(a) * 0.11f, sin(a) * 0.11f, 0.015f, 0.08f, 0.035f)
        }
        val m = b.build()
        return MeshAsset(m, m, m, 0.40f)
    }

    private fun makeGeneratorFrame(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.02f, 0f, 0.22f, 0.18f, 0.28f, 0.02f)
        b.addCylinderY(0f, 0.02f, 0.28f, 0.09f, 0.09f, 0.08f, 12)
        b.addBox(0f, 0.20f, 0f, 0.24f, 0.025f, 0.30f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.48f)
    }

    private fun makeDigitalMeter(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, 0f, 0.10f, 0.18f, 0.038f, 0.012f)
        b.addBox(0f, 0.08f, 0.040f, 0.07f, 0.055f, 0.006f) // screen
        b.addCylinderY(0f, -0.04f, 0.040f, 0.042f, 0.042f, 0.018f, 10) // dial
        val m = b.build()
        return MeshAsset(m, m, m, 0.30f)
    }

    private fun makeThermometer(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.06f, 0f, 0.04f, 0.10f, 0.04f, 0.01f)
        b.addChamferedBox(0f, 0.08f, 0.05f, 0.044f, 0.05f, 0.11f, 0.01f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.30f)
    }

    private fun makeDigitalScale(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.04f, 0f, 0.22f, 0.04f, 0.22f, 0.012f)
        b.addChamferedBox(0f, 0.02f, 0f, 0.18f, 0.02f, 0.18f, 0.01f)
        b.addBox(0f, 0.02f, 0.24f, 0.10f, 0.03f, 0.03f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.38f)
    }

    private fun makeCaliper(): MeshAsset {
        val b = MeshBuilder()
        b.addBox(0f, 0f, 0f, 0.02f, 0.24f, 0.012f)
        b.addBox(0.08f, 0.18f, 0f, 0.08f, 0.024f, 0.014f)
        b.addBox(0.08f, 0.02f, 0f, 0.08f, 0.024f, 0.014f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.36f)
    }

    private fun makePressureDial(): MeshAsset {
        val b = MeshBuilder()
        b.addCylinderY(0f, 0.04f, 0f, 0.12f, 0.12f, 0.05f, 14)
        b.addCylinderY(0f, -0.10f, 0f, 0.03f, 0.03f, 0.16f, 8)
        val m = b.build()
        return MeshAsset(m, m, m, 0.32f)
    }

    private fun makeMicroscope(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, -0.20f, 0f, 0.14f, 0.035f, 0.18f, 0.01f)
        b.addBox(0f, -0.02f, -0.10f, 0.035f, 0.18f, 0.04f)
        b.addBox(0f, -0.06f, 0.02f, 0.10f, 0.015f, 0.10f)
        b.addCylinderY(0f, 0.02f, 0.03f, 0.052f, 0.052f, 0.03f, 10) // objective turret
        b.addCylinderY(0f, 0.14f, 0.03f, 0.032f, 0.026f, 0.20f, 10)
        b.addCylinderY(0.05f, -0.14f, -0.09f, 0.024f, 0.024f, 0.02f, 8) // focus knob
        val m = b.build()
        return MeshAsset(m, m, m, 0.38f)
    }

    private fun makeBeaker(): MeshAsset {
        val b = MeshBuilder()
        b.addCylinderY(0f, 0f, 0f, 0.12f, 0.13f, 0.28f, 14)
        b.addTorusXZ(0f, 0.14f, 0f, 0.13f, 0.014f, 14, 6)
        b.addCylinderY(0f, -0.135f, 0f, 0.124f, 0.124f, 0.02f, 14) // reinforced glass base
        val m = b.build()
        return MeshAsset(m, m, m, 0.32f)
    }

    private fun makeExtinguisher(): MeshAsset {
        val b = MeshBuilder()
        b.addCylinderY(0f, -0.02f, 0f, 0.10f, 0.10f, 0.36f, 14)
        b.addCylinderY(0f, 0.20f, 0f, 0.032f, 0.032f, 0.08f, 8)
        b.addCylinderY(0f, 0.24f, 0.03f, 0.022f, 0.022f, 0.02f, 8) // pressure gauge dial
        b.addBox(0.08f, 0.18f, 0f, 0.08f, 0.02f, 0.02f)
        b.addCylinderY(-0.10f, 0.02f, 0.02f, 0.018f, 0.034f, 0.18f, 8) // discharge horn
        val m = b.build()
        return MeshAsset(m, m, m, 0.38f)
    }

    private fun makeFlashlight(): MeshAsset {
        val b = MeshBuilder()
        b.addCylinderY(0f, -0.04f, 0f, 0.036f, 0.036f, 0.28f, 12)
        b.addTorusXZ(0f, 0.06f, 0f, 0.040f, 0.008f, 12, 6) // knurled switch collar
        b.addCylinderY(0f, 0.14f, 0f, 0.042f, 0.065f, 0.10f, 12)
        b.addTorusXZ(0f, 0.19f, 0f, 0.065f, 0.010f, 12, 6) // crenelated bezel
        val m = b.build()
        return MeshAsset(m, m, m, 0.34f)
    }

    private fun makeWorklight(): MeshAsset {
        val b = MeshBuilder()
        b.addBox(0f, -0.14f, 0f, 0.16f, 0.02f, 0.14f)
        b.addCylinderY(0f, -0.02f, 0f, 0.025f, 0.025f, 0.22f, 8)
        b.addChamferedBox(0f, 0.14f, 0f, 0.15f, 0.11f, 0.06f, 0.015f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.38f)
    }

    private fun makeMetalDetector(): MeshAsset {
        val b = MeshBuilder()
        b.addCylinderY(0f, 0.06f, 0f, 0.022f, 0.022f, 0.42f, 8)
        b.addChamferedBox(0f, 0.22f, 0f, 0.055f, 0.045f, 0.07f, 0.01f)
        b.addTorusXZ(0f, -0.18f, 0f, 0.14f, 0.022f, 14, 6)
        val m = b.build()
        return MeshAsset(m, m, m, 0.42f)
    }

    private fun makeMagnet(): MeshAsset {
        val b = MeshBuilder()
        b.addBox(-0.10f, 0.04f, 0f, 0.038f, 0.14f, 0.038f)
        b.addBox(0.10f, 0.04f, 0f, 0.038f, 0.14f, 0.038f)
        b.addBox(0f, -0.10f, 0f, 0.138f, 0.042f, 0.038f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.35f)
    }

    private fun makeSuctionCup(): MeshAsset {
        val b = MeshBuilder()
        b.addCylinderY(0f, -0.06f, 0f, 0.14f, 0.05f, 0.08f, 14)
        b.addBox(0f, 0.04f, 0f, 0.035f, 0.08f, 0.035f)
        b.addBox(0f, 0.11f, 0f, 0.11f, 0.028f, 0.032f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.32f)
    }

    private fun makePulleyBlock(): MeshAsset {
        val b = MeshBuilder()
        b.addBox(-0.055f, 0f, 0f, 0.015f, 0.16f, 0.08f)
        b.addBox(0.055f, 0f, 0f, 0.015f, 0.16f, 0.08f)
        b.addTorusXZ(0f, 0f, 0f, 0.10f, 0.025f, 14, 6, rotateToVerticalX = true)
        b.addCylinderY(0f, 0.18f, 0f, 0.02f, 0.02f, 0.08f, 8)
        val m = b.build()
        return MeshAsset(m, m, m, 0.34f)
    }

    private fun makeSpiritLevel(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0f, 0f, 0.32f, 0.045f, 0.026f, 0.008f)
        b.addCylinderY(0f, 0.046f, 0f, 0.022f, 0.022f, 0.015f, 10)
        val m = b.build()
        return MeshAsset(m, m, m, 0.38f)
    }

    private fun makePistolFallback(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0.02f, -0.05f, 0.022f, 0.026f, 0.13f, 0.005f)
        b.addChamferedBox(0f, -0.06f, 0.03f, 0.020f, 0.075f, 0.028f, 0.005f)
        val m = b.build()
        return MeshAsset(m, m, m, 0.22f)
    }

    private fun makeLongGunFallback(): MeshAsset {
        val b = MeshBuilder()
        b.addChamferedBox(0f, 0.02f, -0.10f, 0.028f, 0.038f, 0.32f, 0.006f)
        b.addChamferedBox(0f, -0.07f, 0.06f, 0.022f, 0.075f, 0.028f, 0.005f)
        b.addCylinderY(0f, 0.025f, -0.44f, 0.012f, 0.012f, 0.16f, 8)
        val m = b.build()
        return MeshAsset(m, m, m, 0.56f)
    }
}
