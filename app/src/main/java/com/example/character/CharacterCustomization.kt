package com.example.character

import com.example.math.Vec3

enum class CharacterProfilePreset(
    val id: String,
    val displayName: String,
    val roleTitle: String,
    val summary: String,
    val defaultAppearance: CharacterAppearance
) {
    VANCE(
        id = "VANCE",
        displayName = "Dr. Alex Vance",
        roleTitle = "Senior Applied Physicist",
        summary = "Equipped in a tailored chemical-resistant lab coat, precision tactical gloves, and dielectric safety boots.",
        defaultAppearance = CharacterAppearance(
            profileId = "VANCE",
            skinTone = SkinTonePreset.WARM_LIGHT,
            eyeColor = EyeColorPreset.HAZEL,
            hairStyle = HairStyle.SIDE_PART,
            hairColor = HairColorPreset.DARK_CHESTNUT,
            jacketStyle = JacketStyle.LAB_COAT,
            jacketColor = ClothingColorPreset.LAB_WHITE,
            trouserStyle = TrouserStyle.UTILITY,
            trouserColor = ClothingColorPreset.SLATE_NAVY,
            gloveStyle = GloveStyle.TACTICAL_GLOVES,
            gloveColor = ClothingColorPreset.CHARCOAL,
            bootStyle = BootStyle.WORK_BOOTS,
            bootColor = ClothingColorPreset.LEATHER_BROWN
        )
    ),
    CHEN(
        id = "CHEN",
        displayName = "Eng. Marcus Chen",
        roleTitle = "Structural & Mechanics Lead",
        summary = "Outfitted in heavy ripstop industrial shell jacket, reinforced cargo trousers, and steel-toe field boots.",
        defaultAppearance = CharacterAppearance(
            profileId = "CHEN",
            skinTone = SkinTonePreset.OLIVE_TAN,
            eyeColor = EyeColorPreset.DARK_BROWN,
            hairStyle = HairStyle.CREW_CUT,
            hairColor = HairColorPreset.JET_BLACK,
            jacketStyle = JacketStyle.INDUSTRIAL_SHELL,
            jacketColor = ClothingColorPreset.SLATE_NAVY,
            trouserStyle = TrouserStyle.CARGO,
            trouserColor = ClothingColorPreset.GRAPHITE,
            gloveStyle = GloveStyle.TACTICAL_GLOVES,
            gloveColor = ClothingColorPreset.SAFETY_AMBER,
            bootStyle = BootStyle.WORK_BOOTS,
            bootColor = ClothingColorPreset.CHARCOAL
        )
    ),
    REYES(
        id = "REYES",
        displayName = "Spec. Elena Reyes",
        roleTitle = "Field Instrumentation Operator",
        summary = "Outfitted in technical harness vest over articulated sleeves, cargo trousers, and high-grip field boots.",
        defaultAppearance = CharacterAppearance(
            profileId = "REYES",
            skinTone = SkinTonePreset.DEEP_BRONZE,
            eyeColor = EyeColorPreset.GREEN,
            hairStyle = HairStyle.TIED_BACK,
            hairColor = HairColorPreset.ESPRESSO,
            jacketStyle = JacketStyle.TECH_VEST,
            jacketColor = ClothingColorPreset.OLIVE_DRAB,
            trouserStyle = TrouserStyle.CARGO,
            trouserColor = ClothingColorPreset.CHARCOAL,
            gloveStyle = GloveStyle.TACTICAL_GLOVES,
            gloveColor = ClothingColorPreset.CHARCOAL,
            bootStyle = BootStyle.TECH_BOOTS,
            bootColor = ClothingColorPreset.GRAPHITE
        )
    )
}

enum class SkinTonePreset(val displayName: String, val rgb: Vec3, val sssColor: Vec3) {
    PORCELAIN("Fair Ivory", Vec3(0.92f, 0.77f, 0.68f), Vec3(0.88f, 0.34f, 0.24f)),
    WARM_LIGHT("Warm Light", Vec3(0.86f, 0.69f, 0.57f), Vec3(0.84f, 0.31f, 0.20f)),
    OLIVE_TAN("Olive Tan", Vec3(0.76f, 0.58f, 0.44f), Vec3(0.78f, 0.28f, 0.18f)),
    GOLDEN_AMBER("Golden Amber", Vec3(0.64f, 0.45f, 0.32f), Vec3(0.72f, 0.24f, 0.14f)),
    DEEP_BRONZE("Deep Bronze", Vec3(0.49f, 0.33f, 0.23f), Vec3(0.62f, 0.20f, 0.12f)),
    RICH_UMBER("Rich Umber", Vec3(0.34f, 0.22f, 0.16f), Vec3(0.50f, 0.16f, 0.10f))
}

enum class EyeColorPreset(val displayName: String, val rgb: Vec3) {
    DARK_BROWN("Dark Brown", Vec3(0.24f, 0.15f, 0.09f)),
    HAZEL("Hazel Amber", Vec3(0.46f, 0.36f, 0.18f)),
    BLUE("Steel Blue", Vec3(0.24f, 0.44f, 0.66f)),
    GREEN("Forest Green", Vec3(0.22f, 0.46f, 0.31f)),
    SLATE_GRAY("Slate Gray", Vec3(0.42f, 0.48f, 0.54f))
}

enum class HairStyle(val displayName: String, val meshNodeName: String) {
    CREW_CUT("Crew Cut", "Hair_CrewCut"),
    SIDE_PART("Side Part", "Hair_SidePart"),
    TIED_BACK("Tied Back", "Hair_TiedBack"),
    TECH_CAP("Operator Cap", "Hair_TechCap")
}

enum class HairColorPreset(val displayName: String, val rgb: Vec3) {
    JET_BLACK("Jet Black", Vec3(0.08f, 0.08f, 0.09f)),
    ESPRESSO("Dark Espresso", Vec3(0.16f, 0.11f, 0.08f)),
    DARK_CHESTNUT("Chestnut", Vec3(0.28f, 0.17f, 0.11f)),
    AUBURN("Auburn", Vec3(0.44f, 0.20f, 0.11f)),
    ASH_BLONDE("Ash Blonde", Vec3(0.64f, 0.54f, 0.38f)),
    SILVER_GRAY("Silver Steel", Vec3(0.56f, 0.58f, 0.61f))
}

enum class JacketStyle(val displayName: String, val meshNodeName: String) {
    INDUSTRIAL_SHELL("Industrial Shell", "Outfit_Jacket_Industrial"),
    LAB_COAT("Research Lab Coat", "Outfit_Jacket_LabCoat"),
    TECH_VEST("Technical Vest", "Outfit_Jacket_TechVest")
}

enum class TrouserStyle(val displayName: String, val meshNodeName: String) {
    CARGO("Cargo Pants", "Outfit_Trousers_Cargo"),
    UTILITY("Utility Trousers", "Outfit_Trousers_Utility")
}

enum class GloveStyle(val displayName: String, val meshNodeName: String, val isSkinHand: Boolean) {
    TACTICAL_GLOVES("Tactical Gloves", "Outfit_Gloves_Tactical", false),
    BARE_HANDS("Bare Hands", "Body_HandsSkin", true)
}

enum class BootStyle(val displayName: String, val meshNodeName: String) {
    WORK_BOOTS("Steel-Toe Work Boots", "Outfit_Boots_Work"),
    TECH_BOOTS("High-Cut Tech Boots", "Outfit_Boots_Tech")
}

enum class ClothingColorPreset(
    val displayName: String,
    val rgb: Vec3,
    val roughnessBias: Float = 0f,
    val metallicBias: Float = 0f
) {
    LAB_WHITE("Lab White", Vec3(0.84f, 0.86f, 0.88f), 0.02f, 0f),
    SLATE_NAVY("Slate Navy", Vec3(0.19f, 0.25f, 0.33f), 0.0f, 0.02f),
    SAFETY_AMBER("Safety Amber", Vec3(0.76f, 0.45f, 0.14f), -0.04f, 0.03f),
    CHARCOAL("Charcoal Dark", Vec3(0.16f, 0.17f, 0.19f), 0.04f, 0f),
    GRAPHITE("Graphite Gray", Vec3(0.28f, 0.30f, 0.33f), 0.0f, 0.02f),
    OLIVE_DRAB("Field Olive", Vec3(0.27f, 0.32f, 0.23f), 0.04f, 0f),
    LEATHER_BROWN("Oiled Leather", Vec3(0.30f, 0.20f, 0.14f), -0.06f, 0.02f),
    COBALT_WORK("Cobalt Industrial", Vec3(0.17f, 0.31f, 0.48f), 0.0f, 0.02f)
}

data class CharacterAppearance(
    val profileId: String = "VANCE",
    val skinTone: SkinTonePreset = SkinTonePreset.WARM_LIGHT,
    val eyeColor: EyeColorPreset = EyeColorPreset.HAZEL,
    val hairStyle: HairStyle = HairStyle.SIDE_PART,
    val hairColor: HairColorPreset = HairColorPreset.DARK_CHESTNUT,
    val jacketStyle: JacketStyle = JacketStyle.LAB_COAT,
    val jacketColor: ClothingColorPreset = ClothingColorPreset.LAB_WHITE,
    val trouserStyle: TrouserStyle = TrouserStyle.UTILITY,
    val trouserColor: ClothingColorPreset = ClothingColorPreset.SLATE_NAVY,
    val gloveStyle: GloveStyle = GloveStyle.TACTICAL_GLOVES,
    val gloveColor: ClothingColorPreset = ClothingColorPreset.CHARCOAL,
    val bootStyle: BootStyle = BootStyle.WORK_BOOTS,
    val bootColor: ClothingColorPreset = ClothingColorPreset.LEATHER_BROWN
) {
    fun encodeCompact(): String = listOf(
        profileId,
        skinTone.name,
        eyeColor.name,
        hairStyle.name,
        hairColor.name,
        jacketStyle.name,
        jacketColor.name,
        trouserStyle.name,
        trouserColor.name,
        gloveStyle.name,
        gloveColor.name,
        bootStyle.name,
        bootColor.name
    ).joinToString("|")

    companion object {
        fun decodeCompact(raw: String?): CharacterAppearance {
            if (raw.isNullOrBlank()) return CharacterProfilePreset.VANCE.defaultAppearance
            val parts = raw.split("|")
            if (parts.size < 13) return CharacterProfilePreset.VANCE.defaultAppearance
            return CharacterAppearance(
                profileId = parts[0],
                skinTone = enumOrDefault(parts[1], SkinTonePreset.WARM_LIGHT),
                eyeColor = enumOrDefault(parts[2], EyeColorPreset.HAZEL),
                hairStyle = enumOrDefault(parts[3], HairStyle.SIDE_PART),
                hairColor = enumOrDefault(parts[4], HairColorPreset.DARK_CHESTNUT),
                jacketStyle = enumOrDefault(parts[5], JacketStyle.LAB_COAT),
                jacketColor = enumOrDefault(parts[6], ClothingColorPreset.LAB_WHITE),
                trouserStyle = enumOrDefault(parts[7], TrouserStyle.UTILITY),
                trouserColor = enumOrDefault(parts[8], ClothingColorPreset.SLATE_NAVY),
                gloveStyle = enumOrDefault(parts[9], GloveStyle.TACTICAL_GLOVES),
                gloveColor = enumOrDefault(parts[10], ClothingColorPreset.CHARCOAL),
                bootStyle = enumOrDefault(parts[11], BootStyle.WORK_BOOTS),
                bootColor = enumOrDefault(parts[12], ClothingColorPreset.LEATHER_BROWN)
            )
        }

        private inline fun <reified T : Enum<T>> enumOrDefault(name: String, fallback: T): T =
            runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)
    }
}
