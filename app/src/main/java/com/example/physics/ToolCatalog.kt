package com.example.physics

enum class ToolCategory(val displayName: String) {
    MECHANICAL("Mechanical Tools"),
    ELECTRIC("Electric Tools"),
    INDUSTRIAL("Industrial Tools"),
    SCIENCE("Science & Lab Tools"),
    SANDBOX_EQUIPMENT("Sandbox Equipment"),
    FIREARMS("Tactical Firearms")
}

enum class ToolInteractionKind {
    STRIKE_HAMMER,
    STRIKE_MALLET,
    WRENCH_TORQUE,
    PLIERS_GRIP,
    WIRE_CUTTER,
    SAW_CUT,
    DRILL_SPIN,
    GRIND_SPARK,
    SAND_POLISH,
    WELD_ARC,
    SOLDER_JOIN,
    HEAT_THERMAL,
    MULTIMETER_PROBE,
    CLAMP_LOCK,
    JACK_LIFT,
    WINCH_HOIST,
    AIR_BLOW,
    GENERATOR_POWER,
    MEASURE_THERMOMETER,
    MEASURE_SCALE,
    MEASURE_DISTANCE,
    MEASURE_CALIPER,
    MEASURE_PRESSURE,
    STOPWATCH_TIMER,
    OPTICAL_INSPECT,
    LAB_VESSEL,
    EXTINGUISHER_FOAM,
    LIGHT_BEAM,
    CAMERA_SNAP,
    METAL_DETECTOR,
    MAGNET_FIELD,
    SUCTION_GRAB,
    ROPE_TETHER,
    CABLE_TETHER,
    SPRING_TETHER,
    PULLEY_HOIST,
    LEVEL_ALIGN,
    FIREARM_SHOOT
}

enum class ToolMeshStyle {
    HAMMER_CLAW,
    MALLET_HEAD,
    WRENCH_OPEN,
    RATCHET_SOCKET,
    SCREWDRIVER_DRIVER,
    PLIERS_JAWS,
    HAND_SAW_BLADE,
    POWER_DRILL,
    ANGLE_GRINDER,
    CIRCULAR_SAW,
    HEAT_GUN_NOZZLE,
    WELDING_UNIT,
    WELDING_TORCH,
    CLAMP_C_FRAME,
    HYDRAULIC_JACK,
    HOIST_WINCH,
    COMPRESSOR_TANK,
    FAN_SHROUD,
    GENERATOR_FRAME,
    DIGITAL_METER,
    THERMOMETER_PROBE,
    SCALE_PLATFORM,
    CALIPER_SLIDE,
    PRESSURE_DIAL,
    OPTICAL_MICROSCOPE,
    LAB_BEAKER,
    EXTINGUISHER_CYL,
    FLASHLIGHT_TORCH,
    WORKLIGHT_TRIPOD,
    DETECTOR_WAND,
    MAGNET_HORSESHOE,
    SUCTION_CUP_HANDLE,
    PULLEY_BLOCK,
    SPIRIT_LEVEL,
    FIREARM_GLOCK18,
    FIREARM_SERVICE_PISTOL,
    FIREARM_MP5,
    FIREARM_M4A1,
    FIREARM_SHOTGUN,
    FIREARM_SNIPER,
    FIREARM_AK47
}

data class ToolDefinition(
    val id: String,
    val number: Int,
    val displayName: String,
    val category: ToolCategory,
    val description: String,
    val meshStyle: ToolMeshStyle,
    val materialId: String,
    val interactionKind: ToolInteractionKind,
    val powerRating: Float = 1.0f,
    val rangeMeters: Float = 8.0f,
    val animatedSpin: Boolean = false
)

object ToolCatalog {

    private fun t(
        num: Int, id: String, name: String, cat: ToolCategory,
        desc: String, style: ToolMeshStyle, matId: String,
        kind: ToolInteractionKind, power: Float = 1f, range: Float = 8f, spin: Boolean = false
    ) = ToolDefinition(id, num, name, cat, desc, style, matId, kind, power, range, spin)

    val all: List<ToolDefinition> = listOf(
        // CATEGORY A — MECHANICAL TOOLS (1-15)
        t(1, "HAMMER", "Hammer", ToolCategory.MECHANICAL, "Forged steel claw hammer for high-impulse physical strikes and material impact tests.", ToolMeshStyle.HAMMER_CLAW, "CARBON_STEEL", ToolInteractionKind.STRIKE_HAMMER, 1.4f, 4.5f),
        t(2, "RUBBER_MALLET", "Rubber Mallet", ToolCategory.MECHANICAL, "Soft rubber-head mallet delivering damped impulses without shattering fragile glass.", ToolMeshStyle.MALLET_HEAD, "RUBBER", ToolInteractionKind.STRIKE_MALLET, 1.1f, 4.5f),
        t(3, "ADJUSTABLE_WRENCH", "Adjustable Wrench", ToolCategory.MECHANICAL, "Precision worm-gear wrench applying rotational torque and tightening joints.", ToolMeshStyle.WRENCH_OPEN, "CHROME", ToolInteractionKind.WRENCH_TORQUE, 1.0f, 5.0f),
        t(4, "COMBINATION_WRENCH", "Combination Wrench", ToolCategory.MECHANICAL, "Box-and-open steel spanner for rotating structural fasteners and flywheels.", ToolMeshStyle.WRENCH_OPEN, "STAINLESS_STEEL", ToolInteractionKind.WRENCH_TORQUE, 1.1f, 5.0f),
        t(5, "SCREWDRIVER", "Screwdriver", ToolCategory.MECHANICAL, "General-purpose mechanical driver for adjusting component mounts and switches.", ToolMeshStyle.SCREWDRIVER_DRIVER, "ABS", ToolInteractionKind.WRENCH_TORQUE, 0.7f, 4.5f),
        t(6, "PHILLIPS_SCREWDRIVER", "Phillips Screwdriver", ToolCategory.MECHANICAL, "Cross-tip driver applying precision axial torque to panels and electrical housings.", ToolMeshStyle.SCREWDRIVER_DRIVER, "POLYCARBONATE", ToolInteractionKind.WRENCH_TORQUE, 0.75f, 4.5f),
        t(7, "FLATHEAD_SCREWDRIVER", "Flathead Screwdriver", ToolCategory.MECHANICAL, "Slotted driver for prying, nudging, and rotating test assemblies.", ToolMeshStyle.SCREWDRIVER_DRIVER, "CARBON_STEEL", ToolInteractionKind.WRENCH_TORQUE, 0.8f, 4.5f),
        t(8, "PLIERS", "Pliers", ToolCategory.MECHANICAL, "Heavy-duty gripping pliers for pulling, twisting, and positioning dynamic parts.", ToolMeshStyle.PLIERS_JAWS, "CARBON_STEEL", ToolInteractionKind.PLIERS_GRIP, 1.0f, 5.0f),
        t(9, "NEEDLE_NOSE_PLIERS", "Needle-Nose Pliers", ToolCategory.MECHANICAL, "Tapered precision pliers for fine electrical terminal alignment and small objects.", ToolMeshStyle.PLIERS_JAWS, "STAINLESS_STEEL", ToolInteractionKind.PLIERS_GRIP, 0.8f, 5.0f),
        t(10, "WIRE_CUTTERS", "Wire Cutters", ToolCategory.MECHANICAL, "Diagonal cutters that cleanly sever ropes, cables, wires, and hinge joints.", ToolMeshStyle.PLIERS_JAWS, "CARBON_STEEL", ToolInteractionKind.WIRE_CUTTER, 1.0f, 6.0f),
        t(11, "PIPE_WRENCH", "Pipe Wrench", ToolCategory.MECHANICAL, "High-leverage jaw wrench generating strong angular torque on heavy pipes and drums.", ToolMeshStyle.WRENCH_OPEN, "CAST_IRON", ToolInteractionKind.WRENCH_TORQUE, 1.8f, 5.0f),
        t(12, "RATCHET_WRENCH", "Ratchet Wrench", ToolCategory.MECHANICAL, "Reversible ratchet drive for rapid clockwise or counter-clockwise torque pulses.", ToolMeshStyle.RATCHET_SOCKET, "CHROME", ToolInteractionKind.WRENCH_TORQUE, 1.3f, 5.0f),
        t(13, "SOCKET_SET", "Socket Set", ToolCategory.MECHANICAL, "Multi-drive chrome socket assembly for locking and unfastening mechanical joints.", ToolMeshStyle.RATCHET_SOCKET, "CHROME", ToolInteractionKind.WRENCH_TORQUE, 1.2f, 5.0f),
        t(14, "HAND_SAW", "Hand Saw", ToolCategory.MECHANICAL, "Toothed steel saw for cutting timber crates, wooden beams, and breaking joints.", ToolMeshStyle.HAND_SAW_BLADE, "STEEL", ToolInteractionKind.SAW_CUT, 1.0f, 4.5f),
        t(15, "HACKSAW", "Hacksaw", ToolCategory.MECHANICAL, "Fine-tooth tension frame saw for slicing metal rods, plastics, and cables.", ToolMeshStyle.HAND_SAW_BLADE, "ALUMINUM", ToolInteractionKind.SAW_CUT, 1.2f, 4.5f),

        // CATEGORY B — ELECTRIC TOOLS (16-30)
        t(16, "ELECTRIC_DRILL", "Electric Drill", ToolCategory.ELECTRIC, "Cordless rotary drill with animated spinning chuck, motor audio, battery meter, and impact torque.", ToolMeshStyle.POWER_DRILL, "ABS", ToolInteractionKind.DRILL_SPIN, 1.4f, 6.5f, spin = true),
        t(17, "IMPACT_DRIVER", "Impact Driver", ToolCategory.ELECTRIC, "High-torque percussive electric driver imparting rapid rotational impulses.", ToolMeshStyle.POWER_DRILL, "POLYCARBONATE", ToolInteractionKind.DRILL_SPIN, 1.9f, 6.5f, spin = true),
        t(18, "ANGLE_GRINDER", "Angle Grinder", ToolCategory.ELECTRIC, "High-RPM abrasive disc grinder emitting spark streams and severing welds cleanly.", ToolMeshStyle.ANGLE_GRINDER, "ALUMINUM", ToolInteractionKind.GRIND_SPARK, 1.6f, 5.5f, spin = true),
        t(19, "ELECTRIC_SCREWDRIVER", "Electric Screwdriver", ToolCategory.ELECTRIC, "Compact motorized precision driver for controlled low-impact rotation.", ToolMeshStyle.POWER_DRILL, "ABS", ToolInteractionKind.DRILL_SPIN, 0.9f, 5.5f, spin = true),
        t(20, "CIRCULAR_SAW", "Circular Saw", ToolCategory.ELECTRIC, "Motorized toothed disc saw delivering fast material cuts and wood splintering.", ToolMeshStyle.CIRCULAR_SAW, "ALUMINUM", ToolInteractionKind.SAW_CUT, 1.8f, 5.5f, spin = true),
        t(21, "JIGSAW", "Jigsaw", ToolCategory.ELECTRIC, "Reciprocating blade cutter for plastics, composites, and wood panels.", ToolMeshStyle.CIRCULAR_SAW, "ABS", ToolInteractionKind.SAW_CUT, 1.3f, 5.0f, spin = true),
        t(22, "ROTARY_TOOL", "Rotary Tool", ToolCategory.ELECTRIC, "High-speed precision rotary bit for fine material scoring and light sparks.", ToolMeshStyle.POWER_DRILL, "NYLON", ToolInteractionKind.GRIND_SPARK, 0.9f, 5.0f, spin = true),
        t(23, "HEAT_GUN", "Heat Gun", ToolCategory.ELECTRIC, "Convection thermal blower raising target surface temperature and melting wax/ice.", ToolMeshStyle.HEAT_GUN_NOZZLE, "ABS", ToolInteractionKind.HEAT_THERMAL, 1.5f, 6.5f),
        t(24, "ELECTRIC_SANDING", "Electric Sander", ToolCategory.ELECTRIC, "Orbital finishing sander emitting fine surface dust and smoothing roughness.", ToolMeshStyle.ANGLE_GRINDER, "POLYPROPYLENE", ToolInteractionKind.SAND_POLISH, 0.8f, 4.5f, spin = true),
        t(25, "POWER_POLISHER", "Power Polisher", ToolCategory.ELECTRIC, "Rotating buffing wheel polishing surface roughness and imparting gentle spin.", ToolMeshStyle.ANGLE_GRINDER, "ABS", ToolInteractionKind.SAND_POLISH, 0.7f, 4.5f, spin = true),
        t(26, "ELECTRIC_WELDER", "Electric Welding Machine", ToolCategory.ELECTRIC, "High-ampere arc welder fusing compatible metal bodies with glowing weld arcs and sparks.", ToolMeshStyle.WELDING_UNIT, "STEEL", ToolInteractionKind.WELD_ARC, 2.0f, 8.0f),
        t(27, "SOLDERING_IRON", "Soldering Iron", ToolCategory.ELECTRIC, "Precision heated tip for joining electrical terminals and light components.", ToolMeshStyle.SCREWDRIVER_DRIVER, "COPPER", ToolInteractionKind.SOLDER_JOIN, 1.0f, 5.5f),
        t(28, "BENCH_GRINDER", "Bench Grinder", ToolCategory.ELECTRIC, "Dual-wheel stationary abrasive unit generating continuous sparks on contact.", ToolMeshStyle.GENERATOR_FRAME, "CAST_IRON", ToolInteractionKind.GRIND_SPARK, 1.5f, 5.0f, spin = true),
        t(29, "MULTIMETER", "Electric Multimeter", ToolCategory.ELECTRIC, "Digital diagnostic probe displaying simulated voltage, current, and circuit state.", ToolMeshStyle.DIGITAL_METER, "ABS", ToolInteractionKind.MULTIMETER_PROBE, 1.0f, 9.0f),
        t(30, "VOLTAGE_TESTER", "Voltage Tester", ToolCategory.ELECTRIC, "Non-contact circuit probe checking energized panels, wires, and blown fuses.", ToolMeshStyle.DIGITAL_METER, "POLYCARBONATE", ToolInteractionKind.MULTIMETER_PROBE, 1.0f, 9.0f),

        // CATEGORY C — INDUSTRIAL TOOLS (31-40)
        t(31, "WELDING_TORCH", "Welding Torch", ToolCategory.INDUSTRIAL, "Precision nozzle torch creating glowing thermal arcs and rigid metal welds.", ToolMeshStyle.WELDING_TORCH, "BRASS", ToolInteractionKind.WELD_ARC, 1.8f, 7.5f),
        t(32, "WELDING_EQUIPMENT", "Welding Equipment", ToolCategory.INDUSTRIAL, "Heavy industrial regulator rig supporting continuous high-strength welds.", ToolMeshStyle.WELDING_UNIT, "STEEL", ToolInteractionKind.WELD_ARC, 2.2f, 8.5f),
        t(33, "INDUSTRIAL_CLAMP", "Industrial Clamp", ToolCategory.INDUSTRIAL, "Heavy forged clamp that freezes or welds objects firmly to world anchors.", ToolMeshStyle.CLAMP_C_FRAME, "CAST_IRON", ToolInteractionKind.CLAMP_LOCK, 1.5f, 7.0f),
        t(34, "HYDRAULIC_JACK", "Hydraulic Jack", ToolCategory.INDUSTRIAL, "High-force vertical lifting cylinder capable of hoisting heavy steel stacks.", ToolMeshStyle.HYDRAULIC_JACK, "STEEL", ToolInteractionKind.JACK_LIFT, 2.5f, 6.0f),
        t(35, "HAND_WINCH", "Hand Winch", ToolCategory.INDUSTRIAL, "Geared drum winch tensioning cable tethers to haul heavy loads.", ToolMeshStyle.HOIST_WINCH, "GALVANIZED_STEEL", ToolInteractionKind.WINCH_HOIST, 1.6f, 12.0f),
        t(36, "CHAIN_HOIST", "Chain Hoist", ToolCategory.INDUSTRIAL, "Overhead differential pulley block suspending and lifting dense machinery.", ToolMeshStyle.PULLEY_BLOCK, "CARBON_STEEL", ToolInteractionKind.WINCH_HOIST, 2.0f, 12.0f),
        t(37, "AIR_COMPRESSOR", "Air Compressor", ToolCategory.INDUSTRIAL, "Pressurized air tank emitting high-velocity pneumatic air streams.", ToolMeshStyle.COMPRESSOR_TANK, "STEEL", ToolInteractionKind.AIR_BLOW, 1.8f, 10.0f),
        t(38, "PNEUMATIC_TOOL", "Pneumatic Tool", ToolCategory.INDUSTRIAL, "Air-driven percussive driver delivering rapid pneumatic impulses and exhaust.", ToolMeshStyle.POWER_DRILL, "ALUMINUM", ToolInteractionKind.DRILL_SPIN, 2.0f, 6.5f, spin = true),
        t(39, "INDUSTRIAL_FAN", "Industrial Fan", ToolCategory.INDUSTRIAL, "High-CFM shrouded turbine fan producing steady directional wind forces.", ToolMeshStyle.FAN_SHROUD, "GALVANIZED_STEEL", ToolInteractionKind.AIR_BLOW, 2.4f, 14.0f, spin = true),
        t(40, "PORTABLE_GENERATOR", "Portable Generator", ToolCategory.INDUSTRIAL, "Mobile power frame supplying 24V electrical power to connected circuit nodes.", ToolMeshStyle.GENERATOR_FRAME, "STEEL", ToolInteractionKind.GENERATOR_POWER, 2.0f, 8.0f),

        // CATEGORY D — SCIENCE AND EXPERIMENTAL TOOLS (41-50)
        t(41, "THERMOMETER", "Thermometer", ToolCategory.SCIENCE, "Infrared thermal sensor displaying target object temperature and melting point.", ToolMeshStyle.THERMOMETER_PROBE, "ABS", ToolInteractionKind.MEASURE_THERMOMETER, 1.0f, 14.0f),
        t(42, "DIGITAL_SCALE", "Digital Scale", ToolCategory.SCIENCE, "Precision laboratory load-cell measuring exact mass (kg) and gravitational weight (N).", ToolMeshStyle.SCALE_PLATFORM, "STAINLESS_STEEL", ToolInteractionKind.MEASURE_SCALE, 1.0f, 12.0f),
        t(43, "MEASURING_TAPE", "Measuring Tape", ToolCategory.SCIENCE, "Retractable metric tape measuring exact point-to-point distance in meters.", ToolMeshStyle.DIGITAL_METER, "ABS", ToolInteractionKind.MEASURE_DISTANCE, 1.0f, 24.0f),
        t(44, "CALIPER", "Caliper", ToolCategory.SCIENCE, "Vernier stainless caliper reading object bounding width, height, and depth.", ToolMeshStyle.CALIPER_SLIDE, "STAINLESS_STEEL", ToolInteractionKind.MEASURE_CALIPER, 1.0f, 10.0f),
        t(45, "PRESSURE_GAUGE", "Pressure Gauge", ToolCategory.SCIENCE, "Analog dial gauge measuring atmospheric wind pressure and impact stress.", ToolMeshStyle.PRESSURE_DIAL, "BRASS", ToolInteractionKind.MEASURE_PRESSURE, 1.0f, 10.0f),
        t(46, "STOPWATCH", "Stopwatch", ToolCategory.SCIENCE, "Precision lab timer tracking elapsed experiment intervals to milliseconds.", ToolMeshStyle.PRESSURE_DIAL, "CHROME", ToolInteractionKind.STOPWATCH_TIMER, 1.0f, 12.0f),
        t(47, "MAGNIFYING_GLASS", "Magnifying Glass", ToolCategory.SCIENCE, "Optical lens magnifying surface composition, hardness, and fracture state.", ToolMeshStyle.OPTICAL_MICROSCOPE, "CLEAR_GLASS", ToolInteractionKind.OPTICAL_INSPECT, 1.0f, 12.0f),
        t(48, "LAB_BEAKER", "Laboratory Beaker", ToolCategory.SCIENCE, "Graduated borosilicate vessel for containment and fluid buoyancy tests.", ToolMeshStyle.LAB_BEAKER, "TEMPERED_GLASS", ToolInteractionKind.LAB_VESSEL, 1.0f, 8.0f),
        t(49, "TEST_TUBE_HOLDER", "Test Tube Holder", ToolCategory.SCIENCE, "Multi-slot rack holding glass specimen tubes for impact & vibration demos.", ToolMeshStyle.LAB_BEAKER, "POLYPROPYLENE", ToolInteractionKind.LAB_VESSEL, 1.0f, 8.0f),
        t(50, "MICROSCOPE", "Microscope", ToolCategory.SCIENCE, "Benchtop optical microscope inspecting material microstructure and conductivity.", ToolMeshStyle.OPTICAL_MICROSCOPE, "ALUMINUM", ToolInteractionKind.OPTICAL_INSPECT, 1.0f, 10.0f),

        // CATEGORY E — ADDITIONAL SANDBOX EQUIPMENT (51-65)
        t(51, "FIRE_EXTINGUISHER", "Fire Extinguisher", ToolCategory.SANDBOX_EQUIPMENT, "Pressurized cylinder spraying cooling foam stream that reduces thermal heat.", ToolMeshStyle.EXTINGUISHER_CYL, "STEEL", ToolInteractionKind.EXTINGUISHER_FOAM, 1.5f, 9.0f),
        t(52, "FLASHLIGHT", "Flashlight", ToolCategory.SANDBOX_EQUIPMENT, "High-output anodized torch projecting a dynamic point light into dark sectors.", ToolMeshStyle.FLASHLIGHT_TORCH, "ALUMINUM", ToolInteractionKind.LIGHT_BEAM, 1.0f, 18.0f),
        t(53, "PORTABLE_WORK_LIGHT", "Portable Work Light", ToolCategory.SANDBOX_EQUIPMENT, "Rugged halogen floodlight illuminating large indoor construction bays.", ToolMeshStyle.WORKLIGHT_TRIPOD, "ALUMINUM", ToolInteractionKind.LIGHT_BEAM, 1.8f, 22.0f),
        t(54, "TRIPOD", "Tripod", ToolCategory.SANDBOX_EQUIPMENT, "Adjustable three-legged mount for stabilizing sensors, cameras, and lights.", ToolMeshStyle.WORKLIGHT_TRIPOD, "CARBON_FIBER", ToolInteractionKind.CLAMP_LOCK, 1.0f, 8.0f),
        t(55, "CAMERA", "Camera", ToolCategory.SANDBOX_EQUIPMENT, "Experimental observation camera capturing clean HUD-free viewport snapshots.", ToolMeshStyle.DIGITAL_METER, "CARBON_FIBER", ToolInteractionKind.CAMERA_SNAP, 1.0f, 20.0f),
        t(56, "METAL_DETECTOR", "Metal Detector", ToolCategory.SANDBOX_EQUIPMENT, "Induction coil wand detecting ferromagnetic and conductive metallic targets.", ToolMeshStyle.DETECTOR_WAND, "ABS", ToolInteractionKind.METAL_DETECTOR, 1.0f, 12.0f),
        t(57, "MAGNETIC_TOOL", "Magnetic Tool", ToolCategory.SANDBOX_EQUIPMENT, "Electromagnetic wand attracting or repelling ferromagnetic objects across range.", ToolMeshStyle.MAGNET_HORSESHOE, "IRON", ToolInteractionKind.MAGNET_FIELD, 2.2f, 14.0f),
        t(58, "SUCTION_CUP", "Suction Cup", ToolCategory.SANDBOX_EQUIPMENT, "Vacuum-grip manipulator ideal for lifting smooth glass and panel surfaces.", ToolMeshStyle.SUCTION_CUP_HANDLE, "SILICONE", ToolInteractionKind.SUCTION_GRAB, 1.4f, 9.0f),
        t(59, "ROPE", "Rope", ToolCategory.SANDBOX_EQUIPMENT, "Braided fiber tether creating flexible tension-only rope constraints.", ToolMeshStyle.PULLEY_BLOCK, "NYLON", ToolInteractionKind.ROPE_TETHER, 1.0f, 14.0f),
        t(60, "CABLE", "Cable", ToolCategory.SANDBOX_EQUIPMENT, "High-tensile steel/copper cable creating physical tethers and electrical links.", ToolMeshStyle.PULLEY_BLOCK, "COPPER", ToolInteractionKind.CABLE_TETHER, 1.2f, 14.0f),
        t(61, "PULLEY", "Pulley", ToolCategory.SANDBOX_EQUIPMENT, "Sheave pulley rigging wheel for lifting and redirecting rope tension.", ToolMeshStyle.PULLEY_BLOCK, "STEEL", ToolInteractionKind.PULLEY_HOIST, 1.4f, 12.0f),
        t(62, "SPRING_TOOL", "Spring Tool", ToolCategory.SANDBOX_EQUIPMENT, "Coil-suspension rig linking objects with damped Hooke-law elastic springs.", ToolMeshStyle.HYDRAULIC_JACK, "COPPER", ToolInteractionKind.SPRING_TETHER, 1.2f, 14.0f),
        t(63, "MEASURING_LASER", "Measuring Laser", ToolCategory.SANDBOX_EQUIPMENT, "Coherent laser rangefinder measuring exact distance and surface normal angle.", ToolMeshStyle.FLASHLIGHT_TORCH, "POLYCARBONATE", ToolInteractionKind.MEASURE_DISTANCE, 1.0f, 35.0f),
        t(64, "CONSTRUCTION_LEVEL", "Construction Level", ToolCategory.SANDBOX_EQUIPMENT, "Digital spirit level inspecting pitch/roll tilt and snapping objects level.", ToolMeshStyle.SPIRIT_LEVEL, "ALUMINUM", ToolInteractionKind.LEVEL_ALIGN, 1.0f, 10.0f),
        t(65, "HAND_DRILL", "Hand Drill", ToolCategory.SANDBOX_EQUIPMENT, "Manual geared brace drill applying steady human-powered rotational torque.", ToolMeshStyle.POWER_DRILL, "WOOD", ToolInteractionKind.DRILL_SPIN, 0.9f, 5.0f, spin = true),

        // CATEGORY F — TACTICAL SANDBOX FIREARMS (66-71, Virtual Ballistic Testing Hardware)
        t(66, "FIREARM_GLOCK18", "P-18 Auto Pistol", ToolCategory.FIREARMS, "9x19mm automatic polymer-frame pistol for rapid-impulse sandbox testing (20rd mag).", ToolMeshStyle.FIREARM_GLOCK18, "CARBON_STEEL", ToolInteractionKind.FIREARM_SHOOT, 1.5f, 65.0f),
        t(67, "FIREARM_SERVICE_PISTOL", "M45 Service Pistol", ToolCategory.FIREARMS, "Semi-automatic .45 ACP steel-slide service pistol for precision target impacts (12rd mag).", ToolMeshStyle.FIREARM_SERVICE_PISTOL, "CARBON_STEEL", ToolInteractionKind.FIREARM_SHOOT, 1.8f, 75.0f),
        t(68, "FIREARM_MP5", "SMG-9 Tactical", ToolCategory.FIREARMS, "Compact 9x19mm submachine gun with collapsible brace and high cyclic impulse (30rd mag).", ToolMeshStyle.FIREARM_MP5, "CARBON_STEEL", ToolInteractionKind.FIREARM_SHOOT, 2.0f, 85.0f),
        t(69, "FIREARM_M4A1", "CR-4 Carbine", ToolCategory.FIREARMS, "Modular 5.56mm tactical carbine with flat-top rail optic and quad handguard (30rd mag).", ToolMeshStyle.FIREARM_M4A1, "TITANIUM", ToolInteractionKind.FIREARM_SHOOT, 2.5f, 135.0f),
        t(70, "FIREARM_SHOTGUN", "SG-12 Pump Shotgun", ToolCategory.FIREARMS, "12-gauge pump-action tactical shotgun firing 7-pellet spread patterns (8rd tube).", ToolMeshStyle.FIREARM_SHOTGUN, "CARBON_STEEL", ToolInteractionKind.FIREARM_SHOOT, 3.2f, 55.0f),
        t(71, "FIREARM_SNIPER", "SR-700 Marksman", ToolCategory.FIREARMS, "Bolt-action 7.62mm long-range marksman rifle with magnification scope and bipod (5rd mag).", ToolMeshStyle.FIREARM_SNIPER, "CARBON_STEEL", ToolInteractionKind.FIREARM_SHOOT, 4.0f, 200.0f),
        t(72, "FIREARM_AK47", "CR-47 Assault Rifle", ToolCategory.FIREARMS, "7.62x39mm automatic tactical rifle with wood stock and high-impulse recoil (30rd mag).", ToolMeshStyle.FIREARM_AK47, "CARBON_STEEL", ToolInteractionKind.FIREARM_SHOOT, 2.8f, 145.0f)
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): ToolDefinition? = byId[id]
}
