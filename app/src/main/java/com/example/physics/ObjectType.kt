package com.example.physics

import com.example.electrical.ElectricalRole
import com.example.math.Vec3

enum class ColliderShapeType {
    BOX,
    SPHERE
}

enum class InventoryCategory(val displayName: String) {
    MATERIALS("Materials"),
    TOOLS("Tools"),
    MECHANICAL_PARTS("Mechanical Parts"),
    ELECTRICAL_COMPONENTS("Electrical Components"),
    LABORATORY_EQUIPMENT("Laboratory Equipment"),
    CONSTRUCTION_OBJECTS("Construction Objects"),
    FURNITURE("Furniture"),
    CONTAINERS("Containers"),
    AMMUNITION("Ammunition"),
    EXPERIMENTAL_OBJECTS("Experimental Objects")
}

enum class MeshId {
    REINFORCED_CRATE,
    WOOD_PALLET_CRATE,
    TREADED_TIRE,
    GEODESIC_SPHERE,
    GYRO_FLYWHEEL,
    STEEL_IBEAM,
    GLASS_CONTAINER,
    CONCRETE_MASONRY,
    DOMINO_MONOLITH,
    RAMP_WEDGE,
    CENTRIFUGE_POD,
    SPRING_PISTON,
    // Architectural / Environment meshes
    LAB_CONSOLE,
    SHELF_RACK,
    CONVEYOR_UNIT,
    SHIPPING_CONTAINER,
    SCAFFOLD_FRAME,
    BARREL_DRUM,
    FLOOR_SLAB,
    WALL_PANEL,
    PILLAR_COLUMN,
    GLASS_PARTITION,
    CRANE_BOOM,
    // Phase 2 Electrical, Furniture, Mechanical & Experimental meshes
    BATTERY_UNIT,
    SWITCH_BOX,
    LIGHT_BULB_LAMP,
    LED_INDICATOR,
    DC_MOTOR,
    KINETIC_GENERATOR,
    FUSE_BLOCK,
    CIRCUIT_CONNECTOR,
    ELECTRICAL_PANEL,
    LAB_WORKBENCH,
    OFFICE_CHAIR,
    METAL_CABINET,
    GEAR_WHEEL,
    AXLE_ROD,
    SUSPENSION_BOGIE,
    SOFT_FOAM_BLOCK,
    BUOYANCY_FLOAT,
    // Phase 3 Architectural & Heavy Equipment meshes
    CEILING_LIGHT,
    DOOR_FRAME,
    WINDOW_FRAME,
    WALL_CABINET,
    LAB_TABLE,
    CNC_MACHINE,
    HYDRAULIC_PRESS,
    ROOF_TRUSS,
    STAIR_FLIGHT,
    // Phase 5 Architectural & Utility Detail meshes
    PIPE_CONDUIT,
    SAFETY_RAIL,
    WARNING_STRIPE_CURB,
    CEILING_VENT,
    // Phase 7 Ammunition Box GLB Meshes
    AMMO_BOX_9MM_AUTO,
    AMMO_BOX_45_SERVICE,
    AMMO_BOX_SMG_9MM,
    AMMO_BOX_556_CARBINE,
    AMMO_BOX_12G_BUCKSHOT,
    AMMO_BOX_762_MATCH,
    AMMO_BOX_762_AK47,
    // Phase 8 Ordnance & Tactical Props
    GRENADE_FRAG,
    GRENADE_SMOKE
}

enum class ObjectType(
    val displayName: String,
    val category: String,
    val inventoryCategory: InventoryCategory,
    val meshId: MeshId,
    val colliderType: ColliderShapeType,
    val halfExtents: Vec3,
    val radius: Float,
    val defaultMaterial: PhysicsMaterial,
    val defaultMaterialId: String,
    val resizable: Boolean = true,
    val spawnable: Boolean = true,
    val destructible: Boolean = false,
    val electricalRole: ElectricalRole = ElectricalRole.NONE
) {
    // --- Containers ---
    STEEL_CRATE(
        displayName = "Steel Crate",
        category = "Containers",
        inventoryCategory = InventoryCategory.CONTAINERS,
        meshId = MeshId.REINFORCED_CRATE,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.55f, 0.55f, 0.55f),
        radius = 0.95f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "STEEL"
    ),
    WOOD_CRATE(
        displayName = "Timber Crate",
        category = "Containers",
        inventoryCategory = InventoryCategory.CONTAINERS,
        meshId = MeshId.WOOD_PALLET_CRATE,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.52f, 0.52f, 0.52f),
        radius = 0.90f,
        defaultMaterial = PhysicsMaterial.WOOD,
        defaultMaterialId = "OAK",
        destructible = true
    ),
    GLASS_CUBE(
        displayName = "Containment Cube",
        category = "Containers",
        inventoryCategory = InventoryCategory.CONTAINERS,
        meshId = MeshId.GLASS_CONTAINER,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.50f, 0.50f, 0.50f),
        radius = 0.87f,
        defaultMaterial = PhysicsMaterial.GLASS,
        defaultMaterialId = "CLEAR_GLASS",
        destructible = true
    ),
    INDUSTRIAL_BARREL(
        displayName = "Containment Drum",
        category = "Containers",
        inventoryCategory = InventoryCategory.CONTAINERS,
        meshId = MeshId.BARREL_DRUM,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.38f, 0.58f, 0.38f),
        radius = 0.78f,
        defaultMaterial = PhysicsMaterial.RUSTED_IRON,
        defaultMaterialId = "IRON"
    ),

    // --- Mechanical Parts ---
    RUBBER_TIRE(
        displayName = "Heavy Tire",
        category = "Kinetic",
        inventoryCategory = InventoryCategory.MECHANICAL_PARTS,
        meshId = MeshId.TREADED_TIRE,
        colliderType = ColliderShapeType.SPHERE,
        halfExtents = Vec3(0.55f, 0.24f, 0.55f),
        radius = 0.52f,
        defaultMaterial = PhysicsMaterial.RUBBER,
        defaultMaterialId = "RUBBER"
    ),
    COPPER_GYRO(
        displayName = "Gyro Flywheel",
        category = "Apparatus",
        inventoryCategory = InventoryCategory.MECHANICAL_PARTS,
        meshId = MeshId.GYRO_FLYWHEEL,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.55f, 0.55f, 0.55f),
        radius = 0.80f,
        defaultMaterial = PhysicsMaterial.COPPER,
        defaultMaterialId = "COPPER"
    ),
    SPRING_PISTON(
        displayName = "Spring Damper",
        category = "Apparatus",
        inventoryCategory = InventoryCategory.MECHANICAL_PARTS,
        meshId = MeshId.SPRING_PISTON,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.35f, 0.65f, 0.35f),
        radius = 0.82f,
        defaultMaterial = PhysicsMaterial.COPPER,
        defaultMaterialId = "BRASS"
    ),
    GEAR_WHEEL(
        displayName = "Spur Gear Wheel",
        category = "Mechanical Parts",
        inventoryCategory = InventoryCategory.MECHANICAL_PARTS,
        meshId = MeshId.GEAR_WHEEL,
        colliderType = ColliderShapeType.SPHERE,
        halfExtents = Vec3(0.48f, 0.14f, 0.48f),
        radius = 0.48f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "CARBON_STEEL"
    ),
    AXLE_ROD(
        displayName = "Drive Axle Rod",
        category = "Mechanical Parts",
        inventoryCategory = InventoryCategory.MECHANICAL_PARTS,
        meshId = MeshId.AXLE_ROD,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.95f, 0.12f, 0.12f),
        radius = 0.96f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "STAINLESS_STEEL"
    ),
    SUSPENSION_BOGIE(
        displayName = "Suspension Bogie",
        category = "Mechanical Parts",
        inventoryCategory = InventoryCategory.MECHANICAL_PARTS,
        meshId = MeshId.SUSPENSION_BOGIE,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.75f, 0.34f, 0.52f),
        radius = 0.96f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "ALUMINUM"
    ),

    // --- Construction Objects ---
    STEEL_IBEAM(
        displayName = "Steel I-Beam",
        category = "Structural",
        inventoryCategory = InventoryCategory.CONSTRUCTION_OBJECTS,
        meshId = MeshId.STEEL_IBEAM,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(1.50f, 0.22f, 0.24f),
        radius = 1.54f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "STEEL"
    ),
    CONCRETE_BLOCK(
        displayName = "Test Masonry",
        category = "Structural",
        inventoryCategory = InventoryCategory.CONSTRUCTION_OBJECTS,
        meshId = MeshId.CONCRETE_MASONRY,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.60f, 0.35f, 0.35f),
        radius = 0.78f,
        defaultMaterial = PhysicsMaterial.CONCRETE,
        defaultMaterialId = "CONCRETE",
        destructible = true
    ),
    RAMP_BLOCK(
        displayName = "Test Ramp",
        category = "Structural",
        inventoryCategory = InventoryCategory.CONSTRUCTION_OBJECTS,
        meshId = MeshId.RAMP_WEDGE,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(1.20f, 0.40f, 0.80f),
        radius = 1.50f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "GALVANIZED_STEEL"
    ),

    // --- Laboratory Equipment ---
    CENTRIFUGE_POD(
        displayName = "Centrifuge Unit",
        category = "Apparatus",
        inventoryCategory = InventoryCategory.LABORATORY_EQUIPMENT,
        meshId = MeshId.CENTRIFUGE_POD,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.45f, 0.58f, 0.45f),
        radius = 0.86f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "STAINLESS_STEEL"
    ),

    // --- Experimental Objects ---
    STEEL_SPHERE(
        displayName = "Steel Ball",
        category = "Kinetic",
        inventoryCategory = InventoryCategory.EXPERIMENTAL_OBJECTS,
        meshId = MeshId.GEODESIC_SPHERE,
        colliderType = ColliderShapeType.SPHERE,
        halfExtents = Vec3(0.45f, 0.45f, 0.45f),
        radius = 0.45f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "STEEL"
    ),
    BOUNCY_SPHERE(
        displayName = "Elastomer Sphere",
        category = "Kinetic",
        inventoryCategory = InventoryCategory.EXPERIMENTAL_OBJECTS,
        meshId = MeshId.GEODESIC_SPHERE,
        colliderType = ColliderShapeType.SPHERE,
        halfExtents = Vec3(0.42f, 0.42f, 0.42f),
        radius = 0.42f,
        defaultMaterial = PhysicsMaterial.RUBBER,
        defaultMaterialId = "LATEX"
    ),
    DOMINO_SLAB(
        displayName = "Domino Monolith",
        category = "Structural",
        inventoryCategory = InventoryCategory.EXPERIMENTAL_OBJECTS,
        meshId = MeshId.DOMINO_MONOLITH,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.14f, 0.90f, 0.45f),
        radius = 1.02f,
        defaultMaterial = PhysicsMaterial.CONCRETE,
        defaultMaterialId = "MARBLE"
    ),
    SOFT_FOAM_BLOCK(
        displayName = "Acoustic Foam Block",
        category = "Experimental Objects",
        inventoryCategory = InventoryCategory.EXPERIMENTAL_OBJECTS,
        meshId = MeshId.SOFT_FOAM_BLOCK,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.50f, 0.42f, 0.50f),
        radius = 0.82f,
        defaultMaterial = PhysicsMaterial.RUBBER,
        defaultMaterialId = "FOAM",
        destructible = true
    ),
    BUOYANCY_FLOAT(
        displayName = "Cork Buoyancy Cube",
        category = "Experimental Objects",
        inventoryCategory = InventoryCategory.EXPERIMENTAL_OBJECTS,
        meshId = MeshId.BUOYANCY_FLOAT,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.45f, 0.45f, 0.45f),
        radius = 0.78f,
        defaultMaterial = PhysicsMaterial.WOOD,
        defaultMaterialId = "CORK"
    ),
    ICE_BLOCK(
        displayName = "Glacial Ice Block",
        category = "Experimental Objects",
        inventoryCategory = InventoryCategory.EXPERIMENTAL_OBJECTS,
        meshId = MeshId.GLASS_CONTAINER,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.48f, 0.48f, 0.48f),
        radius = 0.83f,
        defaultMaterial = PhysicsMaterial.ICE,
        defaultMaterialId = "ICE",
        destructible = true
    ),
    WAX_BLOCK(
        displayName = "Thermal Wax Cylinder",
        category = "Experimental Objects",
        inventoryCategory = InventoryCategory.EXPERIMENTAL_OBJECTS,
        meshId = MeshId.BARREL_DRUM,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.36f, 0.44f, 0.36f),
        radius = 0.68f,
        defaultMaterial = PhysicsMaterial.WOOD,
        defaultMaterialId = "WAX",
        destructible = true
    ),

    // --- Furniture ---
    LAB_WORKBENCH(
        displayName = "Lab Workbench",
        category = "Furniture",
        inventoryCategory = InventoryCategory.FURNITURE,
        meshId = MeshId.LAB_WORKBENCH,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(1.10f, 0.48f, 0.55f),
        radius = 1.32f,
        defaultMaterial = PhysicsMaterial.WOOD,
        defaultMaterialId = "OAK"
    ),
    OFFICE_CHAIR(
        displayName = "Lab Swivel Chair",
        category = "Furniture",
        inventoryCategory = InventoryCategory.FURNITURE,
        meshId = MeshId.OFFICE_CHAIR,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.36f, 0.56f, 0.36f),
        radius = 0.75f,
        defaultMaterial = PhysicsMaterial.RUBBER,
        defaultMaterialId = "LEATHER"
    ),
    METAL_CABINET(
        displayName = "Storage Cabinet",
        category = "Furniture",
        inventoryCategory = InventoryCategory.FURNITURE,
        meshId = MeshId.METAL_CABINET,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.55f, 0.88f, 0.36f),
        radius = 1.10f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "GALVANIZED_STEEL"
    ),

    // --- Electrical Components ---
    BATTERY_UNIT(
        displayName = "12V DC Battery",
        category = "Electrical Components",
        inventoryCategory = InventoryCategory.ELECTRICAL_COMPONENTS,
        meshId = MeshId.BATTERY_UNIT,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.32f, 0.28f, 0.24f),
        radius = 0.49f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "ABS",
        electricalRole = ElectricalRole.BATTERY
    ),
    SWITCH_BOX(
        displayName = "Toggle Switch Box",
        category = "Electrical Components",
        inventoryCategory = InventoryCategory.ELECTRICAL_COMPONENTS,
        meshId = MeshId.SWITCH_BOX,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.24f, 0.16f, 0.24f),
        radius = 0.38f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "PVC",
        electricalRole = ElectricalRole.SWITCH
    ),
    LIGHT_BULB_LAMP(
        displayName = "Test Light Bulb",
        category = "Electrical Components",
        inventoryCategory = InventoryCategory.ELECTRICAL_COMPONENTS,
        meshId = MeshId.LIGHT_BULB_LAMP,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.22f, 0.30f, 0.22f),
        radius = 0.43f,
        defaultMaterial = PhysicsMaterial.GLASS,
        defaultMaterialId = "CLEAR_GLASS",
        destructible = true,
        electricalRole = ElectricalRole.LIGHT_BULB
    ),
    LED_INDICATOR(
        displayName = "Indicator LED",
        category = "Electrical Components",
        inventoryCategory = InventoryCategory.ELECTRICAL_COMPONENTS,
        meshId = MeshId.LED_INDICATOR,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.18f, 0.22f, 0.18f),
        radius = 0.34f,
        defaultMaterial = PhysicsMaterial.GLASS,
        defaultMaterialId = "ACRYLIC",
        electricalRole = ElectricalRole.LED
    ),
    DC_MOTOR(
        displayName = "DC Drive Motor",
        category = "Electrical Components",
        inventoryCategory = InventoryCategory.ELECTRICAL_COMPONENTS,
        meshId = MeshId.DC_MOTOR,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.36f, 0.32f, 0.36f),
        radius = 0.60f,
        defaultMaterial = PhysicsMaterial.COPPER,
        defaultMaterialId = "COPPER",
        electricalRole = ElectricalRole.MOTOR
    ),
    KINETIC_GENERATOR(
        displayName = "Kinetic Generator",
        category = "Electrical Components",
        inventoryCategory = InventoryCategory.ELECTRICAL_COMPONENTS,
        meshId = MeshId.KINETIC_GENERATOR,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.42f, 0.38f, 0.42f),
        radius = 0.70f,
        defaultMaterial = PhysicsMaterial.COPPER,
        defaultMaterialId = "BRONZE",
        electricalRole = ElectricalRole.GENERATOR
    ),
    FUSE_BLOCK(
        displayName = "Safety Fuse (3A)",
        category = "Electrical Components",
        inventoryCategory = InventoryCategory.ELECTRICAL_COMPONENTS,
        meshId = MeshId.FUSE_BLOCK,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.26f, 0.16f, 0.18f),
        radius = 0.35f,
        defaultMaterial = PhysicsMaterial.GLASS,
        defaultMaterialId = "CERAMIC",
        electricalRole = ElectricalRole.FUSE
    ),
    CIRCUIT_CONNECTOR(
        displayName = "Circuit Connector",
        category = "Electrical Components",
        inventoryCategory = InventoryCategory.ELECTRICAL_COMPONENTS,
        meshId = MeshId.CIRCUIT_CONNECTOR,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.20f, 0.16f, 0.20f),
        radius = 0.32f,
        defaultMaterial = PhysicsMaterial.COPPER,
        defaultMaterialId = "BRASS",
        electricalRole = ElectricalRole.CONNECTOR
    ),
    ELECTRICAL_PANEL(
        displayName = "24V Electrical Panel",
        category = "Electrical Components",
        inventoryCategory = InventoryCategory.ELECTRICAL_COMPONENTS,
        meshId = MeshId.ELECTRICAL_PANEL,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.48f, 0.62f, 0.20f),
        radius = 0.81f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "STEEL",
        electricalRole = ElectricalRole.ELECTRICAL_PANEL
    ),

    // --- Ammunition Crates & Boxes (Collectible Supply Objects) ---
    AMMO_BOX_9MM_AUTO(
        displayName = "9mm Auto Ammo Box",
        category = "Ammunition",
        inventoryCategory = InventoryCategory.AMMUNITION,
        meshId = MeshId.AMMO_BOX_9MM_AUTO,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.20f, 0.13f, 0.14f),
        radius = 0.28f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "STEEL",
        resizable = false
    ),
    AMMO_BOX_45_SERVICE(
        displayName = ".45 ACP Ammo Box",
        category = "Ammunition",
        inventoryCategory = InventoryCategory.AMMUNITION,
        meshId = MeshId.AMMO_BOX_45_SERVICE,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.20f, 0.13f, 0.14f),
        radius = 0.28f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "STEEL",
        resizable = false
    ),
    AMMO_BOX_SMG_9MM(
        displayName = "9mm SMG Ammo Box",
        category = "Ammunition",
        inventoryCategory = InventoryCategory.AMMUNITION,
        meshId = MeshId.AMMO_BOX_SMG_9MM,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.24f, 0.14f, 0.16f),
        radius = 0.32f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "ALUMINUM",
        resizable = false
    ),
    AMMO_BOX_556_CARBINE(
        displayName = "5.56mm Carbine Crate",
        category = "Ammunition",
        inventoryCategory = InventoryCategory.AMMUNITION,
        meshId = MeshId.AMMO_BOX_556_CARBINE,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.28f, 0.16f, 0.18f),
        radius = 0.37f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "STEEL",
        resizable = false
    ),
    AMMO_BOX_12G_BUCKSHOT(
        displayName = "12G Buckshot Case",
        category = "Ammunition",
        inventoryCategory = InventoryCategory.AMMUNITION,
        meshId = MeshId.AMMO_BOX_12G_BUCKSHOT,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.25f, 0.15f, 0.16f),
        radius = 0.33f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "POLYCARBONATE",
        resizable = false
    ),
    AMMO_BOX_762_MATCH(
        displayName = "7.62mm Match Case",
        category = "Ammunition",
        inventoryCategory = InventoryCategory.AMMUNITION,
        meshId = MeshId.AMMO_BOX_762_MATCH,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.28f, 0.15f, 0.17f),
        radius = 0.36f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "BRASS",
        resizable = false
    ),
    AMMO_BOX_762_AK47(
        displayName = "7.62x39mm Ammo Box",
        category = "Ammunition",
        inventoryCategory = InventoryCategory.AMMUNITION,
        meshId = MeshId.AMMO_BOX_762_AK47,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.27f, 0.15f, 0.17f),
        radius = 0.35f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "CARBON_STEEL",
        resizable = false
    ),

    // --- Tactical Test Ordnance ---
    GRENADE_FRAG(
        displayName = "Fragmentation Grenade",
        category = "Experimental Objects",
        inventoryCategory = InventoryCategory.EXPERIMENTAL_OBJECTS,
        meshId = MeshId.GRENADE_FRAG,
        colliderType = ColliderShapeType.SPHERE,
        halfExtents = Vec3(0.11f, 0.14f, 0.11f),
        radius = 0.13f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "CARBON_STEEL",
        resizable = false
    ),
    GRENADE_SMOKE(
        displayName = "Smoke Canister Grenade",
        category = "Experimental Objects",
        inventoryCategory = InventoryCategory.EXPERIMENTAL_OBJECTS,
        meshId = MeshId.GRENADE_SMOKE,
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.09f, 0.16f, 0.09f),
        radius = 0.17f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "ALUMINUM",
        resizable = false
    ),

    // --- Spawnable Physical Tool Wrapper ---
    TOOL_ITEM(
        displayName = "Physics Tool Item",
        category = "Tools",
        inventoryCategory = InventoryCategory.TOOLS,
        meshId = MeshId.REINFORCED_CRATE, // Overridden by RigidBody.toolId -> ToolMeshStyle
        colliderType = ColliderShapeType.BOX,
        halfExtents = Vec3(0.26f, 0.22f, 0.32f),
        radius = 0.46f,
        defaultMaterial = PhysicsMaterial.STEEL,
        defaultMaterialId = "CARBON_STEEL",
        spawnable = false
    );

    val isAmmoBox: Boolean
        get() = inventoryCategory == InventoryCategory.AMMUNITION

    fun defaultAmmoVariantId(): String? = when (this) {
        AMMO_BOX_9MM_AUTO -> "AMMO_9MM_AUTO"
        AMMO_BOX_45_SERVICE -> "AMMO_45_SERVICE"
        AMMO_BOX_SMG_9MM -> "AMMO_SMG_9MM"
        AMMO_BOX_556_CARBINE -> "AMMO_556_CARBINE"
        AMMO_BOX_12G_BUCKSHOT -> "AMMO_12G_BUCKSHOT"
        AMMO_BOX_762_MATCH -> "AMMO_762_MATCH"
        AMMO_BOX_762_AK47 -> "AMMO_762_AK47"
        else -> null
    }
}
