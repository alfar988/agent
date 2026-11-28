package com.example.physics

enum class AcousticClass {
    METAL,
    RUBBER,
    WOOD,
    CONCRETE,
    GLASS
}

enum class PhysicsMaterial(
    val displayName: String,
    val densityKgM3: Float,
    val restitution: Float,
    val staticFriction: Float,
    val dynamicFriction: Float,
    val linearDamping: Float,
    val angularDamping: Float,
    val metallic: Float,
    val roughness: Float,
    val tintR: Float,
    val tintG: Float,
    val tintB: Float,
    val alpha: Float,
    val acousticClass: AcousticClass
) {
    STEEL(
        displayName = "Brushed Steel",
        densityKgM3 = 7850f,
        restitution = 0.22f,
        staticFriction = 0.42f,
        dynamicFriction = 0.32f,
        linearDamping = 0.04f,
        angularDamping = 0.06f,
        metallic = 0.92f,
        roughness = 0.28f,
        tintR = 0.72f, tintG = 0.76f, tintB = 0.80f,
        alpha = 1.0f,
        acousticClass = AcousticClass.METAL
    ),
    COPPER(
        displayName = "Milled Copper",
        densityKgM3 = 8960f,
        restitution = 0.26f,
        staticFriction = 0.45f,
        dynamicFriction = 0.35f,
        linearDamping = 0.04f,
        angularDamping = 0.06f,
        metallic = 0.95f,
        roughness = 0.24f,
        tintR = 0.84f, tintG = 0.49f, tintB = 0.30f,
        alpha = 1.0f,
        acousticClass = AcousticClass.METAL
    ),
    RUBBER(
        displayName = "Vulcanized Rubber",
        densityKgM3 = 1150f,
        restitution = 0.84f,
        staticFriction = 0.95f,
        dynamicFriction = 0.82f,
        linearDamping = 0.08f,
        angularDamping = 0.12f,
        metallic = 0.02f,
        roughness = 0.86f,
        tintR = 0.14f, tintG = 0.15f, tintB = 0.17f,
        alpha = 1.0f,
        acousticClass = AcousticClass.RUBBER
    ),
    WOOD(
        displayName = "Oak Timber",
        densityKgM3 = 680f,
        restitution = 0.30f,
        staticFriction = 0.62f,
        dynamicFriction = 0.48f,
        linearDamping = 0.06f,
        angularDamping = 0.09f,
        metallic = 0.0f,
        roughness = 0.72f,
        tintR = 0.66f, tintG = 0.47f, tintB = 0.29f,
        alpha = 1.0f,
        acousticClass = AcousticClass.WOOD
    ),
    CONCRETE(
        displayName = "Reinforced Concrete",
        densityKgM3 = 2400f,
        restitution = 0.08f,
        staticFriction = 0.78f,
        dynamicFriction = 0.65f,
        linearDamping = 0.05f,
        angularDamping = 0.08f,
        metallic = 0.0f,
        roughness = 0.90f,
        tintR = 0.56f, tintG = 0.58f, tintB = 0.59f,
        alpha = 1.0f,
        acousticClass = AcousticClass.CONCRETE
    ),
    GLASS(
        displayName = "Tempered Glass",
        densityKgM3 = 2500f,
        restitution = 0.42f,
        staticFriction = 0.25f,
        dynamicFriction = 0.18f,
        linearDamping = 0.03f,
        angularDamping = 0.04f,
        metallic = 0.10f,
        roughness = 0.08f,
        tintR = 0.68f, tintG = 0.88f, tintB = 0.94f,
        alpha = 0.48f,
        acousticClass = AcousticClass.GLASS
    ),
    ICE(
        displayName = "Low-Friction Polymer",
        densityKgM3 = 920f,
        restitution = 0.12f,
        staticFriction = 0.05f,
        dynamicFriction = 0.03f,
        linearDamping = 0.01f,
        angularDamping = 0.02f,
        metallic = 0.05f,
        roughness = 0.14f,
        tintR = 0.74f, tintG = 0.87f, tintB = 0.96f,
        alpha = 0.85f,
        acousticClass = AcousticClass.GLASS
    ),
    RUSTED_IRON(
        displayName = "Rusted Iron",
        densityKgM3 = 7400f,
        restitution = 0.16f,
        staticFriction = 0.68f,
        dynamicFriction = 0.55f,
        linearDamping = 0.05f,
        angularDamping = 0.07f,
        metallic = 0.58f,
        roughness = 0.78f,
        tintR = 0.48f, tintG = 0.28f, tintB = 0.20f,
        alpha = 1.0f,
        acousticClass = AcousticClass.METAL
    )
}
