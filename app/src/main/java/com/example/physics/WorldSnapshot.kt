package com.example.physics

import com.example.electrical.ElectricalRole
import com.example.math.Quat
import com.example.math.Vec3
import com.example.weapons.FirearmHudState
import com.example.weapons.WeaponActionState

enum class MapId(
    val displayName: String,
    val code: String,
    val subtitle: String,
    val description: String,
    val sunDir: Vec3,
    val sunColor: Vec3,
    val skyColor: Vec3,
    val groundAmbient: Vec3,
    val fogColor: Vec3,
    val fogDensity: Float,
    val isOutdoor: Boolean
) {
    LAB(
        displayName = "Modern Science Laboratory",
        code = "01 · Laboratory",
        subtitle = "Research Suites, Observation Wing & Electrical Control",
        description = "Multi-room research facility with reception entrance, prep and storage bays, glass observation wing, electrical room, and central testing chamber.",
        sunDir = Vec3(0.32f, 0.90f, 0.28f).normalized(),
        sunColor = Vec3(0.98f, 0.97f, 0.95f),
        skyColor = Vec3(0.76f, 0.82f, 0.88f),
        groundAmbient = Vec3(0.28f, 0.30f, 0.34f),
        fogColor = Vec3(0.74f, 0.78f, 0.82f),
        fogDensity = 0.005f,
        isOutdoor = false
    ),
    FACTORY(
        displayName = "Industrial Factory",
        code = "02 · Factory",
        subtitle = "Production Hall, CNC Bays & Conveyor Lines",
        description = "High-bay manufacturing floor featuring CNC stations, hydraulic presses, dual motorized conveyors, maintenance bay, and elevated control room.",
        sunDir = Vec3(-0.35f, 0.86f, 0.36f).normalized(),
        sunColor = Vec3(0.98f, 0.90f, 0.78f),
        skyColor = Vec3(0.46f, 0.48f, 0.52f),
        groundAmbient = Vec3(0.24f, 0.24f, 0.25f),
        fogColor = Vec3(0.34f, 0.35f, 0.37f),
        fogDensity = 0.007f,
        isOutdoor = false
    ),
    WAREHOUSE(
        displayName = "Abandoned Warehouse",
        code = "03 · Warehouse",
        subtitle = "Storage Hall, Loading Docks & Office Wing",
        description = "Weathered concrete depot with loading bays, heavy pallet racking, shipping containers, corner office suite, and floor-settled cargo.",
        sunDir = Vec3(0.48f, 0.74f, -0.44f).normalized(),
        sunColor = Vec3(0.94f, 0.89f, 0.80f),
        skyColor = Vec3(0.45f, 0.48f, 0.51f),
        groundAmbient = Vec3(0.22f, 0.22f, 0.23f),
        fogColor = Vec3(0.36f, 0.37f, 0.39f),
        fogDensity = 0.008f,
        isOutdoor = false
    ),
    CONSTRUCTION(
        displayName = "Construction Site",
        code = "04 · Construction",
        subtitle = "Concrete Frame, Scaffolding & Tower Crane",
        description = "Open-air structural yard with poured concrete foundation, two-story frame and stairs, multi-tier scaffolding, material staging, and wrecking crane.",
        sunDir = Vec3(0.50f, 0.80f, 0.34f).normalized(),
        sunColor = Vec3(1.0f, 0.95f, 0.88f),
        skyColor = Vec3(0.56f, 0.72f, 0.88f),
        groundAmbient = Vec3(0.30f, 0.31f, 0.33f),
        fogColor = Vec3(0.68f, 0.75f, 0.82f),
        fogDensity = 0.0035f,
        isOutdoor = true
    ),
    FACILITY(
        displayName = "Physics Testing Facility",
        code = "05 · Testing Facility",
        subtitle = "Test Chambers, Ramps & Observation Gallery",
        description = "Engineered proving hall with central test bay, elevated mezzanine and staircase, calibration ramps, glass observation gallery, and experiment stations.",
        sunDir = Vec3(0.28f, 0.92f, 0.26f).normalized(),
        sunColor = Vec3(0.96f, 0.96f, 0.98f),
        skyColor = Vec3(0.54f, 0.62f, 0.70f),
        groundAmbient = Vec3(0.24f, 0.26f, 0.30f),
        fogColor = Vec3(0.46f, 0.51f, 0.58f),
        fogDensity = 0.0045f,
        isOutdoor = false
    ),
    UNDERGROUND_MAINTENANCE(
        displayName = "Underground Maintenance",
        code = "06 · Underground",
        subtitle = "Subterranean Tunnel, Pump Room & Elevator Bay",
        description = "Enclosed concrete service bunker with electrical control room, mechanical workshop, utility pipe corridor, filtration pumps, elevator bay, and observation deck.",
        sunDir = Vec3(-0.22f, 0.94f, 0.25f).normalized(),
        sunColor = Vec3(0.92f, 0.88f, 0.78f),
        skyColor = Vec3(0.22f, 0.25f, 0.28f),
        groundAmbient = Vec3(0.16f, 0.18f, 0.21f),
        fogColor = Vec3(0.18f, 0.20f, 0.23f),
        fogDensity = 0.011f,
        isOutdoor = false
    ),
    OUTDOOR_TESTING_YARD(
        displayName = "Outdoor Testing Yard",
        code = "07 · Outdoor Yard",
        subtitle = "Asphalt Proving Ground, Canopy & Watch Platform",
        description = "Open-air proving yard featuring covered fabrication canopy, shipping storage containers, mechanical test rigs, loading bay, elevated platform, and equipment park.",
        sunDir = Vec3(0.56f, 0.78f, 0.30f).normalized(),
        sunColor = Vec3(1.0f, 0.96f, 0.89f),
        skyColor = Vec3(0.48f, 0.68f, 0.88f),
        groundAmbient = Vec3(0.28f, 0.29f, 0.30f),
        fogColor = Vec3(0.66f, 0.74f, 0.82f),
        fogDensity = 0.0028f,
        isOutdoor = true
    )
}

enum class ToolType(val displayName: String, val shortName: String) {
    GRAB("Grab", "Grab"),
    IMPULSE("Impulse", "Impulse"),
    CONSTRAINT("Attach", "Attach"),
    INSPECT("Properties", "Inspect"),
    CATALOG_TOOL("Tool", "Tool")
}

enum class CameraMode {
    FIRST_PERSON,
    THIRD_PERSON,
    FREE_CAMERA
}

enum class WeatherType(val displayName: String) {
    CLEAR("Clear"),
    RAIN("Rain"),
    SNOW("Snow"),
    WINDY("High Wind")
}

data class WireRenderLine(
    val startWorld: Vec3,
    val endWorld: Vec3,
    val energized: Boolean,
    val overloaded: Boolean
)

data class ToolTelemetrySnapshot(
    val activeToolId: String = "HAMMER",
    val activeToolName: String = "Hammer",
    val readoutTitle: String = "READY",
    val readoutDetail: String = "",
    val batteryPercent: Float = 100f,
    val magnetRangeMeters: Float = 8f,
    val magnetRepelMode: Boolean = false,
    val stopwatchSeconds: Float = 0f,
    val stopwatchRunning: Boolean = false,
    val flashlightOn: Boolean = false
)

data class RenderBodyInstance(
    val id: Int,
    val meshId: MeshId,
    val material: PhysicsMaterial,
    val materialSpec: MaterialSpec,
    val position: Vec3,
    val prevPosition: Vec3,
    val orientation: Quat,
    val prevOrientation: Quat,
    val scale: Vec3,
    val boundingRadius: Float,
    val isFrozen: Boolean,
    val isHighlighted: Boolean,
    val toolMeshStyle: ToolMeshStyle? = null,
    val crackLevel: Float = 0f,
    val squashAmount: Float = 0f,
    val emissiveColor: Vec3 = Vec3.ZERO,
    val emissiveIntensity: Float = 0f
)

data class RenderConstraintLine(
    val type: ConstraintType,
    val startWorld: Vec3,
    val endWorld: Vec3
)

data class WorldRenderSnapshot(
    val mapId: MapId,
    val timestampNs: Long,
    val alpha: Float,
    val cameraPos: Vec3,
    val cameraYaw: Float,
    val cameraPitch: Float,
    val cameraMode: CameraMode,
    val playerPos: Vec3,
    val playerVel: Vec3 = Vec3.ZERO,
    val playerOnGround: Boolean = true,
    val playerGroundAcoustic: AcousticClass = AcousticClass.CONCRETE,
    val isCrouching: Boolean = false,
    val isSprinting: Boolean = false,
    val crouchFraction: Float = 0f,
    val isGrabbing: Boolean = false,
    val grabbedObjectPos: Vec3? = null,
    val targetedWorldPos: Vec3? = null,
    val selectedTool: ToolType = ToolType.GRAB,
    val activeCatalogToolId: String = "HAMMER",
    val interactPulse: Float = 0f,
    val releasePulse: Float = 0f,
    val landingImpulse: Float = 0f,
    val dynamicBodies: List<RenderBodyInstance>,
    val staticProps: List<StaticProp>,
    val constraints: List<RenderConstraintLine>,
    val wires: List<WireRenderLine>,
    val particles: List<ParticleRenderData>,
    val grabbedBeamStart: Vec3?,
    val grabbedBeamEnd: Vec3?,
    val attachPreviewStart: Vec3?,
    val attachPreviewEnd: Vec3?,
    val isMenuDemo: Boolean,
    val timeOfDayHours: Float = 13.5f,
    val weatherType: WeatherType = WeatherType.CLEAR,
    val fluidBasinEnabled: Boolean = false,
    val fluidLevelY: Float = -0.35f,
    val equippedToolStyle: ToolMeshStyle? = null,
    val equippedToolMaterialSpec: MaterialSpec? = null,
    val equippedToolSpinAngle: Float = 0f,
    val equippedToolActionPulse: Float = 0f,
    val flashlightActive: Boolean = false,
    val firearmState: FirearmHudState = FirearmHudState(),
    val weaponActionState: WeaponActionState = WeaponActionState.HOLSTERED,
    val isAiming: Boolean = false,
    val adsFovDegrees: Float = 0f,
    val adsBlend: Float = 0f,
    val recoilVisualOffset: Float = 0f,
    val muzzleFlashIntensity: Float = 0f
)

data class InspectedBodyInfo(
    val id: Int,
    val name: String,
    val type: ObjectType,
    val material: PhysicsMaterial,
    val materialSpec: MaterialSpec,
    val massKg: Float,
    val speedMs: Float,
    val scale: Float,
    val isFrozen: Boolean,
    val constraintCount: Int,
    val resizable: Boolean,
    val gravityScale: Float = 1f,
    val collisionMode: CollisionMode = CollisionMode.SOLID,
    val friction: Float = 0.4f,
    val restitution: Float = 0.25f,
    val temperatureC: Float = 22f,
    val crackLevel: Float = 0f,
    val groupId: Int? = null,
    val position: Vec3 = Vec3.ZERO,
    val eulerDeg: Vec3 = Vec3.ZERO,
    val electricalRole: ElectricalRole = ElectricalRole.NONE,
    val electricalVoltage: Float = 0f,
    val electricalCurrent: Float = 0f,
    val electricalSwitchClosed: Boolean = true,
    val electricalBlown: Boolean = false,
    val toolId: String? = null,
    val ammoVariantId: String? = null,
    val ammoAmount: Int = 0
)

data class CollisionAudioEvent(
    val materialA: AcousticClass,
    val materialB: AcousticClass,
    val impactSpeed: Float,
    val worldPos: Vec3
)
