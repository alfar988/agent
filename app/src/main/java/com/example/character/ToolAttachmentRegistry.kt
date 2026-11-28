package com.example.character

import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.ToolCatalog
import com.example.physics.ToolDefinition
import com.example.physics.ToolInteractionKind
import com.example.weapons.FirearmCatalog

enum class ToolActionProfile {
    SWING_STRIKE,
    ROTARY_TORQUE,
    CONTINUOUS_SPIN_TRIGGER,
    PRECISION_PROBE,
    HEAVY_HOLD_NOZZLE,
    TWO_HAND_OPERATE,
    TETHER_AIM,
    OPTICAL_LOOK
}

data class ToolGripSpec(
    val toolId: String,
    val primaryHandPose: HandPoseId,
    val secondaryHandPose: HandPoseId? = null,
    val isTwoHanded: Boolean = false,
    val primaryOffset: Vec3 = Vec3(0.03f, -0.02f, -0.05f),
    val primaryRotDeg: Vec3 = Vec3(-75f, 10f, 0f),
    val secondaryLocalGripOffset: Vec3 = Vec3(0f, 0.04f, -0.18f),
    val actionProfile: ToolActionProfile = ToolActionProfile.SWING_STRIKE,
    val equippedScale: Float = 0.56f
)

object ToolAttachmentRegistry {

    private val byToolId: Map<String, ToolGripSpec> by lazy {
        ToolCatalog.all.associate { def -> def.id to buildSpecForTool(def) }
    }

    fun getSpec(toolId: String): ToolGripSpec {
        return byToolId[toolId] ?: ToolCatalog.get(toolId)?.let(::buildSpecForTool) ?: defaultSpec(toolId)
    }

    fun allRegisteredToolIds(): Set<String> = byToolId.keys

    fun gripLocalQuat(spec: ToolGripSpec): Quat {
        val r = (Math.PI / 180.0).toFloat()
        return Quat.fromAxisAngle(Vec3.UP, spec.primaryRotDeg.y * r) *
            Quat.fromAxisAngle(Vec3.RIGHT, spec.primaryRotDeg.x * r) *
            Quat.fromAxisAngle(Vec3(0f, 0f, 1f), spec.primaryRotDeg.z * r)
    }

    private fun buildSpecForTool(def: ToolDefinition): ToolGripSpec {
        return when (def.interactionKind) {
            ToolInteractionKind.STRIKE_HAMMER,
            ToolInteractionKind.STRIKE_MALLET -> ToolGripSpec(
                toolId = def.id,
                primaryHandPose = HandPoseId.POWER_GRIP,
                primaryOffset = Vec3(0.02f, -0.03f, -0.04f),
                primaryRotDeg = Vec3(-82f, 5f, 0f),
                actionProfile = ToolActionProfile.SWING_STRIKE,
                equippedScale = 0.58f
            )

            ToolInteractionKind.WRENCH_TORQUE -> {
                val isDriver = def.id.contains("SCREWDRIVER")
                ToolGripSpec(
                    toolId = def.id,
                    primaryHandPose = if (isDriver) HandPoseId.PRECISION_PINCH else HandPoseId.POWER_GRIP,
                    primaryOffset = Vec3(0.02f, -0.02f, -0.05f),
                    primaryRotDeg = Vec3(-78f, 0f, 0f),
                    actionProfile = ToolActionProfile.ROTARY_TORQUE,
                    equippedScale = if (isDriver) 0.52f else 0.58f
                )
            }

            ToolInteractionKind.PLIERS_GRIP,
            ToolInteractionKind.WIRE_CUTTER -> ToolGripSpec(
                toolId = def.id,
                primaryHandPose = HandPoseId.PRECISION_PINCH,
                primaryOffset = Vec3(0.02f, -0.02f, -0.05f),
                primaryRotDeg = Vec3(-85f, 0f, 0f),
                actionProfile = ToolActionProfile.PRECISION_PROBE,
                equippedScale = 0.52f
            )

            ToolInteractionKind.SAW_CUT -> {
                val isElectricSaw = def.id == "CIRCULAR_SAW" || def.id == "JIGSAW"
                ToolGripSpec(
                    toolId = def.id,
                    primaryHandPose = if (isElectricSaw) HandPoseId.TRIGGER_GRIP else HandPoseId.POWER_GRIP,
                    secondaryHandPose = if (isElectricSaw) HandPoseId.TWO_HAND_GRIP else null,
                    isTwoHanded = isElectricSaw,
                    primaryOffset = Vec3(0.03f, -0.02f, -0.06f),
                    primaryRotDeg = Vec3(-76f, 0f, 0f),
                    secondaryLocalGripOffset = Vec3(-0.14f, 0.04f, -0.14f),
                    actionProfile = if (isElectricSaw) ToolActionProfile.CONTINUOUS_SPIN_TRIGGER else ToolActionProfile.SWING_STRIKE,
                    equippedScale = if (isElectricSaw) 0.58f else 0.56f
                )
            }

            ToolInteractionKind.DRILL_SPIN -> ToolGripSpec(
                toolId = def.id,
                primaryHandPose = HandPoseId.TRIGGER_GRIP,
                primaryOffset = Vec3(0.02f, -0.02f, -0.04f),
                primaryRotDeg = Vec3(-82f, 0f, 0f),
                actionProfile = ToolActionProfile.CONTINUOUS_SPIN_TRIGGER,
                equippedScale = 0.55f
            )

            ToolInteractionKind.GRIND_SPARK,
            ToolInteractionKind.SAND_POLISH -> ToolGripSpec(
                toolId = def.id,
                primaryHandPose = HandPoseId.TRIGGER_GRIP,
                secondaryHandPose = HandPoseId.TWO_HAND_GRIP,
                isTwoHanded = true,
                primaryOffset = Vec3(0.03f, -0.02f, -0.06f),
                primaryRotDeg = Vec3(-80f, 8f, 0f),
                secondaryLocalGripOffset = Vec3(-0.15f, 0.05f, -0.15f),
                actionProfile = ToolActionProfile.TWO_HAND_OPERATE,
                equippedScale = 0.56f
            )

            ToolInteractionKind.WELD_ARC,
            ToolInteractionKind.HEAT_THERMAL -> {
                val twoHand = def.id.contains("WELDING_UNIT") || def.id.contains("WELDING_EQUIPMENT") || def.id.contains("ELECTRIC_WELDER")
                ToolGripSpec(
                    toolId = def.id,
                    primaryHandPose = HandPoseId.TRIGGER_GRIP,
                    secondaryHandPose = if (twoHand) HandPoseId.TWO_HAND_GRIP else null,
                    isTwoHanded = twoHand,
                    primaryOffset = Vec3(0.03f, -0.02f, -0.05f),
                    primaryRotDeg = Vec3(-82f, 4f, 0f),
                    secondaryLocalGripOffset = Vec3(-0.16f, 0.03f, -0.14f),
                    actionProfile = if (twoHand) ToolActionProfile.TWO_HAND_OPERATE else ToolActionProfile.HEAVY_HOLD_NOZZLE,
                    equippedScale = if (twoHand) 0.52f else 0.56f
                )
            }

            ToolInteractionKind.SOLDER_JOIN,
            ToolInteractionKind.MULTIMETER_PROBE,
            ToolInteractionKind.MEASURE_THERMOMETER,
            ToolInteractionKind.MEASURE_CALIPER,
            ToolInteractionKind.STOPWATCH_TIMER -> ToolGripSpec(
                toolId = def.id,
                primaryHandPose = HandPoseId.PRECISION_PINCH,
                primaryOffset = Vec3(0.02f, -0.01f, -0.05f),
                primaryRotDeg = Vec3(-74f, 12f, 0f),
                actionProfile = ToolActionProfile.PRECISION_PROBE,
                equippedScale = 0.50f
            )

            ToolInteractionKind.MEASURE_SCALE,
            ToolInteractionKind.MEASURE_DISTANCE,
            ToolInteractionKind.MEASURE_PRESSURE,
            ToolInteractionKind.OPTICAL_INSPECT,
            ToolInteractionKind.LAB_VESSEL,
            ToolInteractionKind.CAMERA_SNAP -> ToolGripSpec(
                toolId = def.id,
                primaryHandPose = HandPoseId.OPEN_PALM_SUPPORT,
                primaryOffset = Vec3(0.03f, -0.02f, -0.06f),
                primaryRotDeg = Vec3(-65f, 8f, 0f),
                actionProfile = ToolActionProfile.OPTICAL_LOOK,
                equippedScale = 0.48f
            )

            ToolInteractionKind.CLAMP_LOCK,
            ToolInteractionKind.JACK_LIFT,
            ToolInteractionKind.WINCH_HOIST,
            ToolInteractionKind.AIR_BLOW,
            ToolInteractionKind.GENERATOR_POWER,
            ToolInteractionKind.EXTINGUISHER_FOAM,
            ToolInteractionKind.MAGNET_FIELD,
            ToolInteractionKind.METAL_DETECTOR,
            ToolInteractionKind.SUCTION_GRAB -> {
                val twoHand = def.interactionKind == ToolInteractionKind.EXTINGUISHER_FOAM ||
                    def.interactionKind == ToolInteractionKind.AIR_BLOW ||
                    def.interactionKind == ToolInteractionKind.GENERATOR_POWER ||
                    def.interactionKind == ToolInteractionKind.JACK_LIFT
                ToolGripSpec(
                    toolId = def.id,
                    primaryHandPose = if (twoHand) HandPoseId.TWO_HAND_GRIP else HandPoseId.POWER_GRIP,
                    secondaryHandPose = if (twoHand) HandPoseId.OPEN_PALM_SUPPORT else null,
                    isTwoHanded = twoHand,
                    primaryOffset = Vec3(0.03f, -0.03f, -0.06f),
                    primaryRotDeg = Vec3(-78f, 0f, 0f),
                    secondaryLocalGripOffset = Vec3(-0.16f, 0.04f, -0.18f),
                    actionProfile = if (twoHand) ToolActionProfile.TWO_HAND_OPERATE else ToolActionProfile.HEAVY_HOLD_NOZZLE,
                    equippedScale = 0.54f
                )
            }

            ToolInteractionKind.LIGHT_BEAM -> ToolGripSpec(
                toolId = def.id,
                primaryHandPose = HandPoseId.TRIGGER_GRIP,
                primaryOffset = Vec3(0.02f, -0.02f, -0.05f),
                primaryRotDeg = Vec3(-86f, 0f, 0f),
                actionProfile = ToolActionProfile.HEAVY_HOLD_NOZZLE,
                equippedScale = 0.54f
            )

            ToolInteractionKind.ROPE_TETHER,
            ToolInteractionKind.CABLE_TETHER,
            ToolInteractionKind.SPRING_TETHER,
            ToolInteractionKind.PULLEY_HOIST,
            ToolInteractionKind.LEVEL_ALIGN -> ToolGripSpec(
                toolId = def.id,
                primaryHandPose = HandPoseId.CARRY_HOLD,
                secondaryHandPose = HandPoseId.OPEN_PALM_SUPPORT,
                isTwoHanded = def.interactionKind == ToolInteractionKind.LEVEL_ALIGN,
                primaryOffset = Vec3(0.03f, -0.02f, -0.05f),
                primaryRotDeg = Vec3(-72f, 5f, 0f),
                secondaryLocalGripOffset = Vec3(-0.18f, 0.0f, -0.10f),
                actionProfile = ToolActionProfile.TETHER_AIM,
                equippedScale = 0.52f
            )

            ToolInteractionKind.FIREARM_SHOOT -> {
                val firearm = FirearmCatalog.byToolId(def.id)
                val twoHand = firearm?.isTwoHanded == true
                ToolGripSpec(
                    toolId = def.id,
                    primaryHandPose = HandPoseId.TRIGGER_GRIP,
                    secondaryHandPose = if (twoHand) HandPoseId.TWO_HAND_GRIP else null,
                    isTwoHanded = twoHand,
                    primaryOffset = Vec3(0.015f, -0.02f, -0.02f),
                    primaryRotDeg = Vec3(-6f, 3f, 0f),
                    secondaryLocalGripOffset = Vec3(-0.07f, 0.01f, -0.19f),
                    actionProfile = if (twoHand) ToolActionProfile.TWO_HAND_OPERATE else ToolActionProfile.PRECISION_PROBE,
                    equippedScale = 0.88f
                )
            }
        }
    }

    private fun defaultSpec(toolId: String) = ToolGripSpec(
        toolId = toolId,
        primaryHandPose = HandPoseId.POWER_GRIP,
        actionProfile = ToolActionProfile.SWING_STRIKE
    )
}
