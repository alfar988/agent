package com.example.physics

enum class MaterialCategory(val displayName: String) {
    METALS("Metals"),
    WOOD("Wood"),
    PLASTICS("Plastics & Polymers"),
    GLASS_CERAMICS("Glass & Ceramics"),
    CONSTRUCTION("Construction"),
    FABRICS_SOFT("Fabrics & Soft"),
    RUBBER_ELASTIC("Rubber & Elastic"),
    SPECIAL("Special"),
    ADDITIONAL("Composites & Resins")
}

enum class DestructionProfile(val displayName: String) {
    NONE("Indestructible"),
    DEFORM_METAL("Metal Yield & Spark"),
    SPLINTER_WOOD("Timber Splintering"),
    SHATTER_GLASS("Glass Fragmentation"),
    FRACTURE_CONCRETE("Masonry Fracture"),
    SNAP_PLASTIC("Polymer Snap"),
    TEAR_SOFT("Soft Material Tear"),
    MELT_WAX("Thermal Softening")
}

enum class TexturePattern {
    BRUSHED_METAL,
    WOOD_GRAIN,
    SPECKLED_STONE,
    WEAVE_FABRIC,
    CELLULAR_FOAM,
    CARBON_TWILL,
    GLASS_SMOOTH,
    CORRUGATED_RUST,
    CARDBOARD_KRAFT,
    ICE_CRYSTAL,
    DIAMOND_PLATE,
    EPOXY_FLOOR_TILE,
    CEILING_PANEL_GRID,
    BRICK_MASONRY,
    ASPHALT_PAVEMENT,
    GRAVEL_EARTH
}

data class MaterialSpec(
    val id: String,
    val number: Int,
    val displayName: String,
    val category: MaterialCategory,
    val densityKgM3: Float,
    val staticFriction: Float,
    val dynamicFriction: Float,
    val restitution: Float,
    val hardness: Float,          // 0.0 (ultra soft) .. 10.0 (tungsten/ceramic)
    val elasticity: Float,        // 0.0 .. 1.0
    val softnessSquish: Float,    // 0.0 (rigid) .. 0.45 (sponge/foam)
    val metallic: Float,
    val roughness: Float,
    val tintR: Float,
    val tintG: Float,
    val tintB: Float,
    val alpha: Float,
    val thermalConductivity: Float, // W/m·K scaled
    val meltingPointC: Float,
    val electricalConductivity: Float, // 0..1 normalized
    val isMagnetic: Boolean,
    val destructionProfile: DestructionProfile,
    val texturePattern: TexturePattern,
    val acousticClass: AcousticClass
) {
    val floatsInWater: Boolean get() = densityKgM3 < 1000f
}

object MaterialLibrary {

    private fun m(
        num: Int, id: String, name: String, cat: MaterialCategory,
        density: Float, sFric: Float, dFric: Float, rest: Float,
        hard: Float, elast: Float, squish: Float,
        metal: Float, rough: Float, r: Float, g: Float, b: Float, alpha: Float = 1.0f,
        therm: Float, melt: Float, elec: Float, mag: Boolean,
        destr: DestructionProfile, pat: TexturePattern, ac: AcousticClass
    ) = MaterialSpec(
        id, num, name, cat, density, sFric, dFric, rest, hard, elast, squish,
        metal, rough, r, g, b, alpha, therm, melt, elec, mag, destr, pat, ac
    )

    val all: List<MaterialSpec> = listOf(
        // 1-20: METALS
        m(1, "STEEL", "Steel", MaterialCategory.METALS, 7850f, 0.44f, 0.34f, 0.24f, 7.5f, 0.45f, 0f, 0.92f, 0.28f, 0.72f, 0.76f, 0.80f, 1f, 50f, 1450f, 0.85f, true, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(2, "STAINLESS_STEEL", "Stainless Steel", MaterialCategory.METALS, 8000f, 0.38f, 0.28f, 0.28f, 7.8f, 0.48f, 0f, 0.96f, 0.18f, 0.84f, 0.87f, 0.90f, 1f, 16f, 1510f, 0.75f, false, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(3, "ALUMINUM", "Aluminum", MaterialCategory.METALS, 2700f, 0.46f, 0.36f, 0.30f, 4.5f, 0.42f, 0f, 0.90f, 0.26f, 0.82f, 0.85f, 0.89f, 1f, 205f, 660f, 0.92f, false, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(4, "COPPER", "Copper", MaterialCategory.METALS, 8960f, 0.48f, 0.38f, 0.25f, 4.2f, 0.40f, 0f, 0.95f, 0.22f, 0.86f, 0.50f, 0.32f, 1f, 385f, 1085f, 1.00f, false, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(5, "BRASS", "Brass", MaterialCategory.METALS, 8500f, 0.40f, 0.30f, 0.28f, 5.2f, 0.44f, 0f, 0.94f, 0.20f, 0.86f, 0.72f, 0.30f, 1f, 109f, 930f, 0.82f, false, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(6, "BRONZE", "Bronze", MaterialCategory.METALS, 8800f, 0.42f, 0.32f, 0.25f, 5.8f, 0.42f, 0f, 0.90f, 0.30f, 0.68f, 0.46f, 0.24f, 1f, 65f, 950f, 0.70f, false, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(7, "IRON", "Iron", MaterialCategory.METALS, 7870f, 0.52f, 0.42f, 0.18f, 6.2f, 0.35f, 0f, 0.84f, 0.48f, 0.46f, 0.48f, 0.50f, 1f, 80f, 1538f, 0.80f, true, DestructionProfile.DEFORM_METAL, TexturePattern.CORRUGATED_RUST, AcousticClass.METAL),
        m(8, "CAST_IRON", "Cast Iron", MaterialCategory.METALS, 7200f, 0.56f, 0.46f, 0.12f, 6.8f, 0.22f, 0f, 0.72f, 0.62f, 0.28f, 0.29f, 0.31f, 1f, 52f, 1200f, 0.65f, true, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.METAL),
        m(9, "TITANIUM", "Titanium", MaterialCategory.METALS, 4500f, 0.36f, 0.28f, 0.38f, 8.2f, 0.62f, 0f, 0.90f, 0.28f, 0.62f, 0.64f, 0.68f, 1f, 22f, 1668f, 0.60f, false, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(10, "ZINC", "Zinc", MaterialCategory.METALS, 7140f, 0.45f, 0.35f, 0.16f, 3.8f, 0.30f, 0f, 0.85f, 0.38f, 0.68f, 0.72f, 0.76f, 1f, 116f, 420f, 0.76f, false, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(11, "TIN", "Tin", MaterialCategory.METALS, 7310f, 0.42f, 0.33f, 0.14f, 2.8f, 0.25f, 0f, 0.88f, 0.30f, 0.76f, 0.78f, 0.80f, 1f, 67f, 232f, 0.72f, false, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(12, "LEAD", "Lead", MaterialCategory.METALS, 11340f, 0.65f, 0.55f, 0.04f, 1.8f, 0.08f, 0.02f, 0.78f, 0.54f, 0.34f, 0.36f, 0.40f, 1f, 35f, 327f, 0.62f, false, DestructionProfile.DEFORM_METAL, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(13, "NICKEL", "Nickel", MaterialCategory.METALS, 8900f, 0.42f, 0.32f, 0.26f, 6.4f, 0.45f, 0f, 0.92f, 0.22f, 0.78f, 0.76f, 0.71f, 1f, 91f, 1455f, 0.84f, true, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(14, "CHROME", "Chrome", MaterialCategory.METALS, 7190f, 0.24f, 0.18f, 0.34f, 8.8f, 0.52f, 0f, 0.99f, 0.08f, 0.90f, 0.92f, 0.95f, 1f, 94f, 1907f, 0.78f, false, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(15, "SILVER", "Silver", MaterialCategory.METALS, 10490f, 0.34f, 0.26f, 0.24f, 3.6f, 0.38f, 0f, 0.98f, 0.12f, 0.94f, 0.94f, 0.92f, 1f, 429f, 962f, 1.00f, false, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(16, "GOLD", "Gold", MaterialCategory.METALS, 19300f, 0.40f, 0.32f, 0.14f, 2.9f, 0.24f, 0f, 0.98f, 0.14f, 0.96f, 0.78f, 0.24f, 1f, 318f, 1064f, 0.96f, false, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(17, "TUNGSTEN", "Tungsten", MaterialCategory.METALS, 19250f, 0.36f, 0.28f, 0.30f, 9.6f, 0.55f, 0f, 0.90f, 0.32f, 0.40f, 0.42f, 0.46f, 1f, 173f, 3422f, 0.86f, false, DestructionProfile.NONE, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(18, "MAGNESIUM", "Magnesium", MaterialCategory.METALS, 1738f, 0.44f, 0.34f, 0.26f, 3.4f, 0.38f, 0f, 0.86f, 0.34f, 0.80f, 0.82f, 0.84f, 1f, 156f, 650f, 0.82f, false, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(19, "CARBON_STEEL", "Carbon Steel", MaterialCategory.METALS, 7850f, 0.48f, 0.38f, 0.26f, 8.4f, 0.50f, 0f, 0.88f, 0.34f, 0.54f, 0.56f, 0.60f, 1f, 45f, 1430f, 0.80f, true, DestructionProfile.DEFORM_METAL, TexturePattern.BRUSHED_METAL, AcousticClass.METAL),
        m(20, "GALVANIZED_STEEL", "Galvanized Steel", MaterialCategory.METALS, 7800f, 0.42f, 0.33f, 0.24f, 7.2f, 0.42f, 0f, 0.86f, 0.36f, 0.70f, 0.74f, 0.77f, 1f, 52f, 1420f, 0.82f, true, DestructionProfile.DEFORM_METAL, TexturePattern.CORRUGATED_RUST, AcousticClass.METAL),

        // 21-30: WOOD
        m(21, "OAK", "Oak", MaterialCategory.WOOD, 720f, 0.62f, 0.48f, 0.28f, 4.2f, 0.32f, 0f, 0f, 0.72f, 0.64f, 0.45f, 0.27f, 1f, 0.17f, 300f, 0f, false, DestructionProfile.SPLINTER_WOOD, TexturePattern.WOOD_GRAIN, AcousticClass.WOOD),
        m(22, "PINE", "Pine", MaterialCategory.WOOD, 520f, 0.58f, 0.44f, 0.32f, 2.8f, 0.36f, 0f, 0f, 0.74f, 0.82f, 0.67f, 0.43f, 1f, 0.12f, 280f, 0f, false, DestructionProfile.SPLINTER_WOOD, TexturePattern.WOOD_GRAIN, AcousticClass.WOOD),
        m(23, "WALNUT", "Walnut", MaterialCategory.WOOD, 640f, 0.56f, 0.43f, 0.28f, 4.0f, 0.34f, 0f, 0f, 0.66f, 0.42f, 0.28f, 0.18f, 1f, 0.15f, 300f, 0f, false, DestructionProfile.SPLINTER_WOOD, TexturePattern.WOOD_GRAIN, AcousticClass.WOOD),
        m(24, "BIRCH", "Birch", MaterialCategory.WOOD, 660f, 0.55f, 0.42f, 0.30f, 3.8f, 0.36f, 0f, 0f, 0.64f, 0.85f, 0.76f, 0.58f, 1f, 0.16f, 290f, 0f, false, DestructionProfile.SPLINTER_WOOD, TexturePattern.WOOD_GRAIN, AcousticClass.WOOD),
        m(25, "MAPLE", "Maple", MaterialCategory.WOOD, 700f, 0.54f, 0.40f, 0.34f, 4.5f, 0.38f, 0f, 0f, 0.58f, 0.78f, 0.62f, 0.42f, 1f, 0.17f, 300f, 0f, false, DestructionProfile.SPLINTER_WOOD, TexturePattern.WOOD_GRAIN, AcousticClass.WOOD),
        m(26, "MAHOGANY", "Mahogany", MaterialCategory.WOOD, 680f, 0.52f, 0.40f, 0.30f, 4.1f, 0.35f, 0f, 0f, 0.56f, 0.52f, 0.24f, 0.16f, 1f, 0.16f, 300f, 0f, false, DestructionProfile.SPLINTER_WOOD, TexturePattern.WOOD_GRAIN, AcousticClass.WOOD),
        m(27, "BAMBOO", "Bamboo", MaterialCategory.WOOD, 480f, 0.42f, 0.32f, 0.42f, 4.4f, 0.52f, 0f, 0f, 0.52f, 0.76f, 0.70f, 0.42f, 1f, 0.14f, 280f, 0f, false, DestructionProfile.SPLINTER_WOOD, TexturePattern.WOOD_GRAIN, AcousticClass.WOOD),
        m(28, "PLYWOOD", "Plywood", MaterialCategory.WOOD, 580f, 0.60f, 0.46f, 0.26f, 3.4f, 0.30f, 0f, 0f, 0.76f, 0.74f, 0.59f, 0.39f, 1f, 0.13f, 270f, 0f, false, DestructionProfile.SPLINTER_WOOD, TexturePattern.WOOD_GRAIN, AcousticClass.WOOD),
        m(29, "MDF", "MDF", MaterialCategory.WOOD, 750f, 0.64f, 0.50f, 0.18f, 3.2f, 0.22f, 0f, 0f, 0.84f, 0.60f, 0.48f, 0.34f, 1f, 0.14f, 260f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.CARDBOARD_KRAFT, AcousticClass.WOOD),
        m(30, "PARTICLE_BOARD", "Particle Board", MaterialCategory.WOOD, 650f, 0.68f, 0.54f, 0.15f, 2.6f, 0.18f, 0f, 0f, 0.88f, 0.66f, 0.54f, 0.38f, 1f, 0.12f, 250f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.WOOD),

        // 31-40: PLASTICS AND POLYMERS
        m(31, "ABS", "ABS Polymer", MaterialCategory.PLASTICS, 1050f, 0.38f, 0.28f, 0.46f, 4.2f, 0.48f, 0f, 0f, 0.36f, 0.26f, 0.30f, 0.36f, 1f, 0.20f, 210f, 0f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.GLASS_SMOOTH, AcousticClass.WOOD),
        m(32, "PVC", "PVC", MaterialCategory.PLASTICS, 1380f, 0.42f, 0.32f, 0.38f, 4.0f, 0.40f, 0f, 0f, 0.38f, 0.86f, 0.88f, 0.90f, 1f, 0.19f, 180f, 0f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.GLASS_SMOOTH, AcousticClass.WOOD),
        m(33, "POLYCARBONATE", "Polycarbonate", MaterialCategory.PLASTICS, 1200f, 0.32f, 0.24f, 0.50f, 5.4f, 0.60f, 0f, 0.04f, 0.16f, 0.74f, 0.86f, 0.92f, 0.72f, 0.21f, 260f, 0f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.GLASS_SMOOTH, AcousticClass.GLASS),
        m(34, "ACRYLIC", "Acrylic", MaterialCategory.PLASTICS, 1180f, 0.34f, 0.25f, 0.44f, 4.6f, 0.42f, 0f, 0.04f, 0.12f, 0.84f, 0.92f, 0.98f, 0.62f, 0.19f, 160f, 0f, false, DestructionProfile.SHATTER_GLASS, TexturePattern.GLASS_SMOOTH, AcousticClass.GLASS),
        m(35, "NYLON", "Nylon", MaterialCategory.PLASTICS, 1140f, 0.26f, 0.18f, 0.52f, 4.4f, 0.58f, 0f, 0f, 0.44f, 0.82f, 0.80f, 0.72f, 1f, 0.25f, 265f, 0f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.WEAVE_FABRIC, AcousticClass.WOOD),
        m(36, "POLYETHYLENE", "Polyethylene", MaterialCategory.PLASTICS, 940f, 0.22f, 0.16f, 0.42f, 3.2f, 0.50f, 0.04f, 0f, 0.42f, 0.88f, 0.90f, 0.92f, 1f, 0.42f, 130f, 0f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.GLASS_SMOOTH, AcousticClass.WOOD),
        m(37, "POLYPROPYLENE", "Polypropylene", MaterialCategory.PLASTICS, 905f, 0.28f, 0.20f, 0.48f, 3.6f, 0.54f, 0.02f, 0f, 0.40f, 0.74f, 0.78f, 0.82f, 1f, 0.22f, 165f, 0f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.GLASS_SMOOTH, AcousticClass.WOOD),
        m(38, "RUBBER", "Vulcanized Rubber", MaterialCategory.PLASTICS, 1150f, 0.95f, 0.82f, 0.84f, 2.2f, 0.90f, 0.14f, 0f, 0.86f, 0.16f, 0.17f, 0.19f, 1f, 0.16f, 220f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CELLULAR_FOAM, AcousticClass.RUBBER),
        m(39, "SILICONE", "Silicone", MaterialCategory.PLASTICS, 1120f, 0.78f, 0.65f, 0.72f, 1.8f, 0.84f, 0.22f, 0f, 0.48f, 0.78f, 0.82f, 0.86f, 0.92f, 0.20f, 300f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.GLASS_SMOOTH, AcousticClass.RUBBER),
        m(40, "FIBERGLASS", "Fiberglass", MaterialCategory.PLASTICS, 1850f, 0.46f, 0.35f, 0.36f, 6.4f, 0.46f, 0f, 0.04f, 0.46f, 0.84f, 0.78f, 0.56f, 1f, 0.04f, 450f, 0f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.CARBON_TWILL, AcousticClass.WOOD),

        // 41-50: GLASS AND CERAMICS
        m(41, "CLEAR_GLASS", "Clear Glass", MaterialCategory.GLASS_CERAMICS, 2500f, 0.22f, 0.15f, 0.40f, 6.0f, 0.22f, 0f, 0.08f, 0.06f, 0.78f, 0.92f, 0.96f, 0.42f, 1.0f, 1400f, 0f, false, DestructionProfile.SHATTER_GLASS, TexturePattern.GLASS_SMOOTH, AcousticClass.GLASS),
        m(42, "TEMPERED_GLASS", "Tempered Glass", MaterialCategory.GLASS_CERAMICS, 2520f, 0.24f, 0.17f, 0.44f, 7.2f, 0.28f, 0f, 0.10f, 0.08f, 0.68f, 0.88f, 0.94f, 0.48f, 1.0f, 1450f, 0f, false, DestructionProfile.SHATTER_GLASS, TexturePattern.GLASS_SMOOTH, AcousticClass.GLASS),
        m(43, "FROSTED_GLASS", "Frosted Glass", MaterialCategory.GLASS_CERAMICS, 2500f, 0.38f, 0.28f, 0.38f, 5.8f, 0.20f, 0f, 0.05f, 0.42f, 0.84f, 0.90f, 0.94f, 0.68f, 1.0f, 1400f, 0f, false, DestructionProfile.SHATTER_GLASS, TexturePattern.SPECKLED_STONE, AcousticClass.GLASS),
        m(44, "TINTED_GLASS", "Tinted Glass", MaterialCategory.GLASS_CERAMICS, 2530f, 0.24f, 0.16f, 0.42f, 6.2f, 0.24f, 0f, 0.14f, 0.08f, 0.24f, 0.34f, 0.42f, 0.58f, 1.0f, 1400f, 0f, false, DestructionProfile.SHATTER_GLASS, TexturePattern.GLASS_SMOOTH, AcousticClass.GLASS),
        m(45, "CERAMIC", "Ceramic", MaterialCategory.GLASS_CERAMICS, 2400f, 0.46f, 0.36f, 0.28f, 7.6f, 0.22f, 0f, 0f, 0.22f, 0.90f, 0.88f, 0.84f, 1f, 1.8f, 1800f, 0f, false, DestructionProfile.SHATTER_GLASS, TexturePattern.GLASS_SMOOTH, AcousticClass.GLASS),
        m(46, "PORCELAIN", "Porcelain", MaterialCategory.GLASS_CERAMICS, 2450f, 0.36f, 0.26f, 0.32f, 7.8f, 0.24f, 0f, 0.02f, 0.12f, 0.95f, 0.95f, 0.96f, 1f, 1.5f, 1850f, 0f, false, DestructionProfile.SHATTER_GLASS, TexturePattern.GLASS_SMOOTH, AcousticClass.GLASS),
        m(47, "CONCRETE_TILE", "Concrete Tile", MaterialCategory.GLASS_CERAMICS, 2300f, 0.72f, 0.60f, 0.12f, 6.4f, 0.14f, 0f, 0f, 0.78f, 0.64f, 0.66f, 0.68f, 1f, 1.4f, 1500f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(48, "STONEWARE", "Stoneware", MaterialCategory.GLASS_CERAMICS, 2350f, 0.56f, 0.44f, 0.20f, 7.2f, 0.18f, 0f, 0f, 0.54f, 0.56f, 0.50f, 0.44f, 1f, 1.6f, 1600f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(49, "BRICK", "Brick", MaterialCategory.GLASS_CERAMICS, 1920f, 0.78f, 0.66f, 0.10f, 5.8f, 0.12f, 0f, 0f, 0.88f, 0.66f, 0.29f, 0.22f, 1f, 0.72f, 1550f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(50, "MARBLE", "Marble", MaterialCategory.GLASS_CERAMICS, 2710f, 0.42f, 0.32f, 0.22f, 6.5f, 0.20f, 0f, 0.02f, 0.24f, 0.88f, 0.87f, 0.85f, 1f, 2.8f, 1300f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),

        // 51-60: CONSTRUCTION MATERIALS
        m(51, "CONCRETE", "Concrete", MaterialCategory.CONSTRUCTION, 2400f, 0.78f, 0.65f, 0.08f, 6.8f, 0.12f, 0f, 0f, 0.90f, 0.56f, 0.58f, 0.59f, 1f, 1.4f, 1500f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(52, "REINFORCED_CONCRETE", "Reinforced Concrete", MaterialCategory.CONSTRUCTION, 2520f, 0.76f, 0.64f, 0.10f, 7.8f, 0.16f, 0f, 0.08f, 0.86f, 0.50f, 0.52f, 0.55f, 1f, 2.2f, 1500f, 0.1f, true, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(53, "CEMENT", "Cement", MaterialCategory.CONSTRUCTION, 2100f, 0.82f, 0.70f, 0.06f, 5.5f, 0.10f, 0f, 0f, 0.94f, 0.62f, 0.62f, 0.60f, 1f, 0.9f, 1500f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(54, "ASPHALT", "Asphalt", MaterialCategory.CONSTRUCTION, 2240f, 0.88f, 0.76f, 0.10f, 4.8f, 0.14f, 0.01f, 0f, 0.92f, 0.22f, 0.23f, 0.25f, 1f, 0.75f, 160f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(55, "GRANITE", "Granite", MaterialCategory.CONSTRUCTION, 2750f, 0.66f, 0.54f, 0.18f, 7.6f, 0.18f, 0f, 0f, 0.64f, 0.48f, 0.46f, 0.45f, 1f, 2.9f, 1260f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(56, "LIMESTONE", "Limestone", MaterialCategory.CONSTRUCTION, 2550f, 0.70f, 0.58f, 0.12f, 5.2f, 0.14f, 0f, 0f, 0.82f, 0.78f, 0.75f, 0.66f, 1f, 1.3f, 1300f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(57, "SANDSTONE", "Sandstone", MaterialCategory.CONSTRUCTION, 2320f, 0.76f, 0.64f, 0.10f, 4.6f, 0.12f, 0f, 0f, 0.90f, 0.78f, 0.64f, 0.44f, 1f, 1.8f, 1400f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(58, "GRAVEL", "Gravel", MaterialCategory.CONSTRUCTION, 1680f, 0.84f, 0.74f, 0.05f, 4.2f, 0.06f, 0.04f, 0f, 0.96f, 0.46f, 0.45f, 0.43f, 1f, 0.4f, 1400f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(59, "SAND", "Sand", MaterialCategory.CONSTRUCTION, 1600f, 0.78f, 0.68f, 0.02f, 2.0f, 0.04f, 0.10f, 0f, 0.96f, 0.84f, 0.74f, 0.52f, 1f, 0.25f, 1700f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.SPECKLED_STONE, AcousticClass.RUBBER),
        m(60, "CLAY", "Clay", MaterialCategory.CONSTRUCTION, 1760f, 0.72f, 0.60f, 0.03f, 1.4f, 0.06f, 0.16f, 0f, 0.78f, 0.62f, 0.39f, 0.28f, 1f, 1.1f, 1100f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CELLULAR_FOAM, AcousticClass.RUBBER),

        // 61-70: FABRICS AND SOFT MATERIALS
        m(61, "COTTON", "Cotton", MaterialCategory.FABRICS_SOFT, 320f, 0.72f, 0.62f, 0.08f, 0.6f, 0.22f, 0.30f, 0f, 0.96f, 0.92f, 0.91f, 0.88f, 1f, 0.04f, 220f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.WEAVE_FABRIC, AcousticClass.RUBBER),
        m(62, "DENIM", "Denim", MaterialCategory.FABRICS_SOFT, 480f, 0.76f, 0.66f, 0.10f, 1.0f, 0.24f, 0.22f, 0f, 0.92f, 0.22f, 0.34f, 0.52f, 1f, 0.05f, 230f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.WEAVE_FABRIC, AcousticClass.RUBBER),
        m(63, "LEATHER", "Leather", MaterialCategory.FABRICS_SOFT, 860f, 0.68f, 0.54f, 0.18f, 1.8f, 0.34f, 0.14f, 0f, 0.62f, 0.44f, 0.26f, 0.16f, 1f, 0.14f, 210f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.WEAVE_FABRIC, AcousticClass.RUBBER),
        m(64, "SYNTHETIC_LEATHER", "Synthetic Leather", MaterialCategory.FABRICS_SOFT, 820f, 0.60f, 0.48f, 0.22f, 1.7f, 0.36f, 0.12f, 0f, 0.50f, 0.24f, 0.22f, 0.23f, 1f, 0.12f, 190f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.WEAVE_FABRIC, AcousticClass.RUBBER),
        m(65, "WOOL", "Wool", MaterialCategory.FABRICS_SOFT, 260f, 0.78f, 0.68f, 0.12f, 0.5f, 0.28f, 0.34f, 0f, 0.98f, 0.82f, 0.78f, 0.72f, 1f, 0.03f, 220f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.WEAVE_FABRIC, AcousticClass.RUBBER),
        m(66, "POLYESTER", "Polyester", MaterialCategory.FABRICS_SOFT, 420f, 0.52f, 0.42f, 0.16f, 1.1f, 0.32f, 0.20f, 0f, 0.68f, 0.36f, 0.48f, 0.62f, 1f, 0.05f, 255f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.WEAVE_FABRIC, AcousticClass.RUBBER),
        m(67, "FOAM", "Acoustic Foam", MaterialCategory.FABRICS_SOFT, 95f, 0.85f, 0.76f, 0.22f, 0.3f, 0.45f, 0.42f, 0f, 0.98f, 0.24f, 0.26f, 0.30f, 1f, 0.03f, 180f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CELLULAR_FOAM, AcousticClass.RUBBER),
        m(68, "SPONGE", "Sponge", MaterialCategory.FABRICS_SOFT, 110f, 0.88f, 0.78f, 0.18f, 0.25f, 0.40f, 0.45f, 0f, 0.98f, 0.88f, 0.76f, 0.32f, 1f, 0.03f, 175f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CELLULAR_FOAM, AcousticClass.RUBBER),
        m(69, "FELT", "Industrial Felt", MaterialCategory.FABRICS_SOFT, 300f, 0.74f, 0.64f, 0.08f, 0.7f, 0.22f, 0.28f, 0f, 0.96f, 0.52f, 0.54f, 0.56f, 1f, 0.04f, 210f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.WEAVE_FABRIC, AcousticClass.RUBBER),
        m(70, "CANVAS", "Heavy Canvas", MaterialCategory.FABRICS_SOFT, 540f, 0.70f, 0.58f, 0.10f, 1.4f, 0.24f, 0.16f, 0f, 0.90f, 0.68f, 0.64f, 0.52f, 1f, 0.06f, 240f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.WEAVE_FABRIC, AcousticClass.RUBBER),

        // 71-75: RUBBER AND ELASTIC MATERIALS
        m(71, "NATURAL_RUBBER", "Natural Rubber", MaterialCategory.RUBBER_ELASTIC, 930f, 0.92f, 0.80f, 0.88f, 2.0f, 0.94f, 0.16f, 0f, 0.76f, 0.64f, 0.52f, 0.36f, 1f, 0.14f, 180f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CELLULAR_FOAM, AcousticClass.RUBBER),
        m(72, "NEOPRENE", "Neoprene", MaterialCategory.RUBBER_ELASTIC, 1230f, 0.86f, 0.74f, 0.70f, 2.2f, 0.82f, 0.20f, 0f, 0.82f, 0.18f, 0.22f, 0.26f, 1f, 0.05f, 210f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CELLULAR_FOAM, AcousticClass.RUBBER),
        m(73, "LATEX", "Latex", MaterialCategory.RUBBER_ELASTIC, 940f, 0.84f, 0.72f, 0.90f, 1.6f, 0.96f, 0.24f, 0f, 0.44f, 0.84f, 0.78f, 0.62f, 0.95f, 0.13f, 160f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.GLASS_SMOOTH, AcousticClass.RUBBER),
        m(74, "EPDM_RUBBER", "EPDM Rubber", MaterialCategory.RUBBER_ELASTIC, 1180f, 0.94f, 0.84f, 0.76f, 2.4f, 0.86f, 0.14f, 0f, 0.88f, 0.14f, 0.15f, 0.16f, 1f, 0.25f, 230f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CELLULAR_FOAM, AcousticClass.RUBBER),
        m(75, "SOFT_RUBBER", "Soft Rubber", MaterialCategory.RUBBER_ELASTIC, 980f, 0.96f, 0.86f, 0.82f, 1.4f, 0.92f, 0.28f, 0f, 0.80f, 0.32f, 0.36f, 0.42f, 1f, 0.12f, 175f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CELLULAR_FOAM, AcousticClass.RUBBER),

        // 76-90: SPECIAL MATERIALS
        m(76, "CARBON_FIBER", "Carbon Fiber", MaterialCategory.SPECIAL, 1600f, 0.38f, 0.28f, 0.48f, 8.6f, 0.64f, 0f, 0.36f, 0.28f, 0.18f, 0.20f, 0.23f, 1f, 24f, 3500f, 0.45f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.CARBON_TWILL, AcousticClass.WOOD),
        m(77, "KEVLAR", "Kevlar Composite", MaterialCategory.SPECIAL, 1440f, 0.48f, 0.36f, 0.42f, 8.0f, 0.60f, 0.02f, 0f, 0.62f, 0.78f, 0.68f, 0.24f, 1f, 0.04f, 500f, 0f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.CARBON_TWILL, AcousticClass.WOOD),
        m(78, "GRAPHITE", "Graphite", MaterialCategory.SPECIAL, 2150f, 0.14f, 0.09f, 0.16f, 2.4f, 0.14f, 0f, 0.42f, 0.48f, 0.22f, 0.23f, 0.25f, 1f, 140f, 3600f, 0.55f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(79, "CORK", "Cork", MaterialCategory.SPECIAL, 240f, 0.82f, 0.70f, 0.36f, 1.2f, 0.48f, 0.18f, 0f, 0.92f, 0.68f, 0.50f, 0.32f, 1f, 0.04f, 260f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CELLULAR_FOAM, AcousticClass.WOOD),
        m(80, "PAPER", "Pressed Paper", MaterialCategory.SPECIAL, 780f, 0.58f, 0.46f, 0.06f, 0.9f, 0.14f, 0.06f, 0f, 0.88f, 0.94f, 0.93f, 0.88f, 1f, 0.05f, 233f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CARDBOARD_KRAFT, AcousticClass.WOOD),
        m(81, "CARDBOARD", "Corrugated Cardboard", MaterialCategory.SPECIAL, 360f, 0.64f, 0.52f, 0.12f, 1.1f, 0.20f, 0.12f, 0f, 0.92f, 0.66f, 0.50f, 0.33f, 1f, 0.06f, 230f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CARDBOARD_KRAFT, AcousticClass.WOOD),
        m(82, "WAX", "Paraffin Wax", MaterialCategory.SPECIAL, 900f, 0.24f, 0.16f, 0.08f, 1.2f, 0.12f, 0.08f, 0f, 0.38f, 0.92f, 0.88f, 0.72f, 0.94f, 0.25f, 58f, 0f, false, DestructionProfile.MELT_WAX, TexturePattern.GLASS_SMOOTH, AcousticClass.RUBBER),
        m(83, "ICE", "Glacial Ice", MaterialCategory.SPECIAL, 917f, 0.05f, 0.03f, 0.16f, 3.5f, 0.18f, 0f, 0.05f, 0.12f, 0.72f, 0.88f, 0.98f, 0.80f, 2.2f, 0f, 0f, false, DestructionProfile.SHATTER_GLASS, TexturePattern.ICE_CRYSTAL, AcousticClass.GLASS),
        m(84, "SNOW", "Packed Snow", MaterialCategory.SPECIAL, 420f, 0.34f, 0.24f, 0.03f, 0.8f, 0.06f, 0.28f, 0f, 0.94f, 0.94f, 0.96f, 0.99f, 1f, 0.15f, 0f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.ICE_CRYSTAL, AcousticClass.RUBBER),
        m(85, "FROZEN_WATER", "Frozen Water", MaterialCategory.SPECIAL, 920f, 0.04f, 0.02f, 0.18f, 3.8f, 0.20f, 0f, 0.04f, 0.08f, 0.66f, 0.85f, 0.96f, 0.72f, 2.2f, 0f, 0f, false, DestructionProfile.SHATTER_GLASS, TexturePattern.ICE_CRYSTAL, AcousticClass.GLASS),
        m(86, "DRY_LEAVES", "Compressed Dry Leaves", MaterialCategory.SPECIAL, 220f, 0.72f, 0.62f, 0.05f, 0.6f, 0.10f, 0.24f, 0f, 0.95f, 0.56f, 0.39f, 0.18f, 1f, 0.06f, 180f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.WEAVE_FABRIC, AcousticClass.WOOD),
        m(87, "WET_LEAVES", "Wet Leaves", MaterialCategory.SPECIAL, 520f, 0.16f, 0.10f, 0.02f, 0.5f, 0.06f, 0.26f, 0f, 0.42f, 0.34f, 0.38f, 0.18f, 1f, 0.35f, 100f, 0.05f, false, DestructionProfile.TEAR_SOFT, TexturePattern.WEAVE_FABRIC, AcousticClass.RUBBER),
        m(88, "MOSS", "Moss Mat", MaterialCategory.SPECIAL, 340f, 0.82f, 0.72f, 0.06f, 0.4f, 0.14f, 0.32f, 0f, 0.96f, 0.28f, 0.46f, 0.22f, 1f, 0.18f, 100f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CELLULAR_FOAM, AcousticClass.RUBBER),
        m(89, "SOIL", "Compacted Soil", MaterialCategory.SPECIAL, 1450f, 0.80f, 0.68f, 0.02f, 1.8f, 0.05f, 0.12f, 0f, 0.96f, 0.38f, 0.28f, 0.19f, 1f, 0.5f, 1200f, 0.02f, false, DestructionProfile.TEAR_SOFT, TexturePattern.SPECKLED_STONE, AcousticClass.RUBBER),
        m(90, "MUD", "Wet Mud", MaterialCategory.SPECIAL, 1750f, 0.28f, 0.18f, 0.01f, 0.5f, 0.02f, 0.30f, 0f, 0.34f, 0.28f, 0.21f, 0.15f, 1f, 0.8f, 100f, 0.08f, false, DestructionProfile.TEAR_SOFT, TexturePattern.SPECKLED_STONE, AcousticClass.RUBBER),

        // 91-105: ADDITIONAL MATERIALS
        m(91, "FOAM_GLASS", "Foam Glass", MaterialCategory.ADDITIONAL, 165f, 0.66f, 0.54f, 0.14f, 2.2f, 0.18f, 0.04f, 0f, 0.88f, 0.42f, 0.46f, 0.50f, 1f, 0.04f, 700f, 0f, false, DestructionProfile.SHATTER_GLASS, TexturePattern.CELLULAR_FOAM, AcousticClass.GLASS),
        m(92, "ACRYLIC_RESIN", "Acrylic Resin", MaterialCategory.ADDITIONAL, 1190f, 0.32f, 0.24f, 0.44f, 4.8f, 0.44f, 0f, 0.04f, 0.14f, 0.82f, 0.88f, 0.92f, 0.78f, 0.19f, 175f, 0f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.GLASS_SMOOTH, AcousticClass.GLASS),
        m(93, "EPOXY_RESIN", "Epoxy Resin", MaterialCategory.ADDITIONAL, 1250f, 0.34f, 0.26f, 0.38f, 5.6f, 0.42f, 0f, 0.04f, 0.16f, 0.72f, 0.66f, 0.44f, 0.88f, 0.22f, 220f, 0f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.GLASS_SMOOTH, AcousticClass.WOOD),
        m(94, "PLASTER", "Gypsum Plaster", MaterialCategory.ADDITIONAL, 1300f, 0.74f, 0.62f, 0.06f, 2.4f, 0.08f, 0f, 0f, 0.92f, 0.92f, 0.90f, 0.86f, 1f, 0.28f, 1400f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(95, "GYPSUM", "Gypsum Board", MaterialCategory.ADDITIONAL, 950f, 0.72f, 0.60f, 0.08f, 2.1f, 0.10f, 0f, 0f, 0.90f, 0.86f, 0.85f, 0.82f, 1f, 0.17f, 1450f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.CARDBOARD_KRAFT, AcousticClass.CONCRETE),
        m(96, "FRP", "Fiberglass Reinforced Plastic", MaterialCategory.ADDITIONAL, 1800f, 0.44f, 0.34f, 0.40f, 6.8f, 0.50f, 0f, 0.04f, 0.38f, 0.32f, 0.56f, 0.62f, 1f, 0.28f, 380f, 0f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.CARBON_TWILL, AcousticClass.WOOD),
        m(97, "RECYCLED_PLASTIC", "Recycled Plastic", MaterialCategory.ADDITIONAL, 980f, 0.42f, 0.32f, 0.38f, 3.6f, 0.42f, 0.02f, 0f, 0.56f, 0.36f, 0.42f, 0.38f, 1f, 0.20f, 155f, 0f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.SPECKLED_STONE, AcousticClass.WOOD),
        m(98, "SYNTHETIC_STONE", "Synthetic Stone", MaterialCategory.ADDITIONAL, 2250f, 0.54f, 0.42f, 0.18f, 6.6f, 0.20f, 0f, 0.02f, 0.36f, 0.72f, 0.70f, 0.66f, 1f, 1.5f, 1400f, 0f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.SPECKLED_STONE, AcousticClass.CONCRETE),
        m(99, "COMPOSITE_WOOD", "Composite Wood", MaterialCategory.ADDITIONAL, 860f, 0.58f, 0.45f, 0.26f, 4.4f, 0.32f, 0f, 0f, 0.68f, 0.52f, 0.38f, 0.28f, 1f, 0.15f, 240f, 0f, false, DestructionProfile.SPLINTER_WOOD, TexturePattern.WOOD_GRAIN, AcousticClass.WOOD),
        m(100, "CERAMIC_COMPOSITE", "Ceramic Matrix Composite", MaterialCategory.ADDITIONAL, 2850f, 0.40f, 0.30f, 0.34f, 9.2f, 0.36f, 0f, 0.12f, 0.28f, 0.36f, 0.38f, 0.42f, 1f, 18f, 2400f, 0.15f, false, DestructionProfile.FRACTURE_CONCRETE, TexturePattern.CARBON_TWILL, AcousticClass.GLASS),
        m(101, "RUBBER_COMPOSITE", "Rubber Composite", MaterialCategory.ADDITIONAL, 1320f, 0.90f, 0.78f, 0.74f, 2.8f, 0.84f, 0.12f, 0.06f, 0.78f, 0.20f, 0.22f, 0.24f, 1f, 0.22f, 240f, 0.05f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CARBON_TWILL, AcousticClass.RUBBER),
        m(102, "MINERAL_WOOL", "Mineral Wool", MaterialCategory.ADDITIONAL, 140f, 0.84f, 0.74f, 0.08f, 0.4f, 0.20f, 0.38f, 0f, 0.98f, 0.64f, 0.62f, 0.46f, 1f, 0.035f, 1000f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CELLULAR_FOAM, AcousticClass.RUBBER),
        m(103, "INSULATION_FOAM", "Insulation Foam", MaterialCategory.ADDITIONAL, 45f, 0.74f, 0.62f, 0.24f, 0.35f, 0.38f, 0.40f, 0f, 0.96f, 0.90f, 0.56f, 0.64f, 1f, 0.025f, 120f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CELLULAR_FOAM, AcousticClass.RUBBER),
        m(104, "POLYURETHANE", "Polyurethane", MaterialCategory.ADDITIONAL, 1180f, 0.62f, 0.50f, 0.64f, 2.6f, 0.78f, 0.16f, 0f, 0.48f, 0.86f, 0.64f, 0.20f, 1f, 0.03f, 210f, 0f, false, DestructionProfile.TEAR_SOFT, TexturePattern.CELLULAR_FOAM, AcousticClass.RUBBER),
        m(105, "CARBON_COMPOSITE", "Carbon Composite", MaterialCategory.ADDITIONAL, 1580f, 0.36f, 0.26f, 0.50f, 8.8f, 0.66f, 0f, 0.40f, 0.22f, 0.16f, 0.18f, 0.22f, 1f, 32f, 3200f, 0.50f, false, DestructionProfile.SNAP_PLASTIC, TexturePattern.CARBON_TWILL, AcousticClass.METAL)
    )

    private val byId: Map<String, MaterialSpec> = all.associateBy { it.id }

    fun get(id: String): MaterialSpec = byId[id] ?: byId.getValue("STEEL")

    fun fromLegacy(mat: PhysicsMaterial): MaterialSpec = when (mat) {
        PhysicsMaterial.STEEL -> get("STEEL")
        PhysicsMaterial.COPPER -> get("COPPER")
        PhysicsMaterial.RUBBER -> get("RUBBER")
        PhysicsMaterial.WOOD -> get("OAK")
        PhysicsMaterial.CONCRETE -> get("CONCRETE")
        PhysicsMaterial.GLASS -> get("TEMPERED_GLASS")
        PhysicsMaterial.ICE -> get("ICE")
        PhysicsMaterial.RUSTED_IRON -> get("IRON")
    }

    fun toLegacyFallback(spec: MaterialSpec): PhysicsMaterial = when (spec.category) {
        MaterialCategory.METALS -> if (spec.id == "COPPER" || spec.id == "BRASS" || spec.id == "BRONZE") PhysicsMaterial.COPPER else PhysicsMaterial.STEEL
        MaterialCategory.WOOD -> PhysicsMaterial.WOOD
        MaterialCategory.GLASS_CERAMICS -> if (spec.alpha < 0.99f) PhysicsMaterial.GLASS else PhysicsMaterial.CONCRETE
        MaterialCategory.RUBBER_ELASTIC -> PhysicsMaterial.RUBBER
        MaterialCategory.CONSTRUCTION -> PhysicsMaterial.CONCRETE
        else -> if (spec.id == "ICE" || spec.id == "FROZEN_WATER") PhysicsMaterial.ICE else PhysicsMaterial.STEEL
    }
}
