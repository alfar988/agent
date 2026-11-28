package com.example.weapons

enum class WeaponActionState {
    HOLSTERED,
    DRAWING,
    EQUIPPED_IDLE,
    AIMING,
    FIRING,
    RELOADING,
    HOLSTERING,
    SWITCHING,
    INSPECTING
}

enum class FirearmId {
    GLOCK18_STYLE,
    SERVICE_PISTOL,
    MP5_STYLE,
    M4A1_STYLE,
    PUMP_SHOTGUN,
    BOLT_SNIPER,
    AK47_STYLE
}

enum class AmmoBoxVariant(
    val id: String,
    val displayName: String,
    val caliberLabel: String,
    val firearmId: FirearmId,
    val ammoAmount: Int,
    val modelAssetPath: String,
    val bandColorRgb: Triple<Float, Float, Float>
) {
    AMMO_9MM_AUTO(
        id = "AMMO_9MM_AUTO",
        displayName = "9mm Auto Box",
        caliberLabel = "9x19mm Auto",
        firearmId = FirearmId.GLOCK18_STYLE,
        ammoAmount = 40,
        modelAssetPath = "models/ammo/ammo_box_9mm_pistol.glb",
        bandColorRgb = Triple(0.88f, 0.52f, 0.14f)
    ),
    AMMO_45_SERVICE(
        id = "AMMO_45_SERVICE",
        displayName = ".45 ACP Box",
        caliberLabel = ".45 ACP",
        firearmId = FirearmId.SERVICE_PISTOL,
        ammoAmount = 24,
        modelAssetPath = "models/ammo/ammo_box_45_service.glb",
        bandColorRgb = Triple(0.28f, 0.50f, 0.72f)
    ),
    AMMO_SMG_9MM(
        id = "AMMO_SMG_9MM",
        displayName = "9mm SMG Box",
        caliberLabel = "9x19mm SMG",
        firearmId = FirearmId.MP5_STYLE,
        ammoAmount = 60,
        modelAssetPath = "models/ammo/ammo_box_smg_9mm.glb",
        bandColorRgb = Triple(0.22f, 0.64f, 0.62f)
    ),
    AMMO_556_CARBINE(
        id = "AMMO_556_CARBINE",
        displayName = "5.56mm Rifle Case",
        caliberLabel = "5.56x45mm",
        firearmId = FirearmId.M4A1_STYLE,
        ammoAmount = 60,
        modelAssetPath = "models/ammo/ammo_box_556_rifle.glb",
        bandColorRgb = Triple(0.32f, 0.52f, 0.24f)
    ),
    AMMO_12G_BUCKSHOT(
        id = "AMMO_12G_BUCKSHOT",
        displayName = "12G Shell Case",
        caliberLabel = "12 Gauge",
        firearmId = FirearmId.PUMP_SHOTGUN,
        ammoAmount = 16,
        modelAssetPath = "models/ammo/ammo_box_12g_shotgun.glb",
        bandColorRgb = Triple(0.78f, 0.20f, 0.18f)
    ),
    AMMO_762_MATCH(
        id = "AMMO_762_MATCH",
        displayName = "7.62mm Match Case",
        caliberLabel = "7.62x51mm",
        firearmId = FirearmId.BOLT_SNIPER,
        ammoAmount = 10,
        modelAssetPath = "models/ammo/ammo_box_762_sniper.glb",
        bandColorRgb = Triple(0.80f, 0.64f, 0.22f)
    ),
    AMMO_762_AK47(
        id = "AMMO_762_AK47",
        displayName = "7.62x39mm Ammo Box",
        caliberLabel = "7.62x39mm",
        firearmId = FirearmId.AK47_STYLE,
        ammoAmount = 60,
        modelAssetPath = "models/ammo/ammo_box_762_ak47.glb",
        bandColorRgb = Triple(0.84f, 0.42f, 0.16f)
    );

    companion object {
        private val byId = entries.associateBy { it.id }
        private val byFirearm = entries.associateBy { it.firearmId }
        fun fromId(id: String?): AmmoBoxVariant? = if (id == null) null else byId[id]
        fun forFirearm(firearmId: FirearmId): AmmoBoxVariant = byFirearm.getValue(firearmId)
    }
}

data class FirearmSpec(
    val id: FirearmId,
    val toolId: String,
    val displayName: String,
    val roleLabel: String,
    val caliberLabel: String,
    val modelAssetPath: String,
    val magazineCapacity: Int,
    val initialReserve: Int,
    val maxReserve: Int,
    val automatic: Boolean,
    val fireIntervalSec: Float,
    val reloadDurationSec: Float,
    val pellets: Int,
    val hipSpreadRad: Float,
    val adsSpreadRad: Float,
    val rangeMeters: Float,
    val impulseNewtonSec: Float,
    val damageRatio: Float,
    val recoilPitchRad: Float,
    val recoilYawRad: Float,
    val recoilKickBack: Float,
    val isTwoHanded: Boolean,
    val adsFovDeg: Float,
    val shotSfxKey: String
)

object FirearmCatalog {

    val all: List<FirearmSpec> = listOf(
        FirearmSpec(
            id = FirearmId.GLOCK18_STYLE,
            toolId = "FIREARM_GLOCK18",
            displayName = "P-18 Auto Pistol",
            roleLabel = "Automatic Pistol",
            caliberLabel = "9x19mm",
            modelAssetPath = "models/firearms/glock18_style.glb",
            magazineCapacity = 20,
            initialReserve = 60,
            maxReserve = 160,
            automatic = true,
            fireIntervalSec = 0.072f,
            reloadDurationSec = 1.45f,
            pellets = 1,
            hipSpreadRad = 0.024f,
            adsSpreadRad = 0.008f,
            rangeMeters = 65f,
            impulseNewtonSec = 11.5f,
            damageRatio = 0.22f,
            recoilPitchRad = 0.028f,
            recoilYawRad = 0.011f,
            recoilKickBack = 0.042f,
            isTwoHanded = false,
            adsFovDeg = 58f,
            shotSfxKey = "gun_pistol"
        ),
        FirearmSpec(
            id = FirearmId.SERVICE_PISTOL,
            toolId = "FIREARM_SERVICE_PISTOL",
            displayName = "M45 Service Pistol",
            roleLabel = "Standard Pistol",
            caliberLabel = ".45 ACP",
            modelAssetPath = "models/firearms/service_pistol.glb",
            magazineCapacity = 12,
            initialReserve = 48,
            maxReserve = 120,
            automatic = false,
            fireIntervalSec = 0.19f,
            reloadDurationSec = 1.35f,
            pellets = 1,
            hipSpreadRad = 0.016f,
            adsSpreadRad = 0.0045f,
            rangeMeters = 75f,
            impulseNewtonSec = 16.0f,
            damageRatio = 0.34f,
            recoilPitchRad = 0.036f,
            recoilYawRad = 0.008f,
            recoilKickBack = 0.048f,
            isTwoHanded = false,
            adsFovDeg = 58f,
            shotSfxKey = "gun_pistol"
        ),
        FirearmSpec(
            id = FirearmId.MP5_STYLE,
            toolId = "FIREARM_MP5",
            displayName = "SMG-9 Tactical",
            roleLabel = "Submachine Gun",
            caliberLabel = "9x19mm",
            modelAssetPath = "models/firearms/mp5_style.glb",
            magazineCapacity = 30,
            initialReserve = 90,
            maxReserve = 210,
            automatic = true,
            fireIntervalSec = 0.078f,
            reloadDurationSec = 1.75f,
            pellets = 1,
            hipSpreadRad = 0.022f,
            adsSpreadRad = 0.007f,
            rangeMeters = 85f,
            impulseNewtonSec = 14.0f,
            damageRatio = 0.28f,
            recoilPitchRad = 0.022f,
            recoilYawRad = 0.009f,
            recoilKickBack = 0.036f,
            isTwoHanded = true,
            adsFovDeg = 52f,
            shotSfxKey = "gun_smg"
        ),
        FirearmSpec(
            id = FirearmId.M4A1_STYLE,
            toolId = "FIREARM_M4A1",
            displayName = "CR-4 Carbine",
            roleLabel = "Tactical Rifle",
            caliberLabel = "5.56mm",
            modelAssetPath = "models/firearms/m4a1_style.glb",
            magazineCapacity = 30,
            initialReserve = 90,
            maxReserve = 210,
            automatic = true,
            fireIntervalSec = 0.092f,
            reloadDurationSec = 1.95f,
            pellets = 1,
            hipSpreadRad = 0.018f,
            adsSpreadRad = 0.0035f,
            rangeMeters = 135f,
            impulseNewtonSec = 22.0f,
            damageRatio = 0.44f,
            recoilPitchRad = 0.029f,
            recoilYawRad = 0.008f,
            recoilKickBack = 0.044f,
            isTwoHanded = true,
            adsFovDeg = 48f,
            shotSfxKey = "gun_rifle"
        ),
        FirearmSpec(
            id = FirearmId.PUMP_SHOTGUN,
            toolId = "FIREARM_SHOTGUN",
            displayName = "SG-12 Pump Shotgun",
            roleLabel = "Pump Shotgun",
            caliberLabel = "12G",
            modelAssetPath = "models/firearms/pump_shotgun.glb",
            magazineCapacity = 8,
            initialReserve = 24,
            maxReserve = 64,
            automatic = false,
            fireIntervalSec = 0.72f,
            reloadDurationSec = 2.15f,
            pellets = 7,
            hipSpreadRad = 0.082f,
            adsSpreadRad = 0.048f,
            rangeMeters = 55f,
            impulseNewtonSec = 9.5f,
            damageRatio = 0.18f,
            recoilPitchRad = 0.072f,
            recoilYawRad = 0.015f,
            recoilKickBack = 0.078f,
            isTwoHanded = true,
            adsFovDeg = 54f,
            shotSfxKey = "gun_shotgun"
        ),
        FirearmSpec(
            id = FirearmId.BOLT_SNIPER,
            toolId = "FIREARM_SNIPER",
            displayName = "SR-700 Marksman",
            roleLabel = "Sniper Rifle",
            caliberLabel = "7.62mm",
            modelAssetPath = "models/firearms/bolt_sniper.glb",
            magazineCapacity = 5,
            initialReserve = 15,
            maxReserve = 45,
            automatic = false,
            fireIntervalSec = 1.10f,
            reloadDurationSec = 2.40f,
            pellets = 1,
            hipSpreadRad = 0.030f,
            adsSpreadRad = 0.0008f,
            rangeMeters = 200f,
            impulseNewtonSec = 58.0f,
            damageRatio = 1.0f,
            recoilPitchRad = 0.095f,
            recoilYawRad = 0.012f,
            recoilKickBack = 0.090f,
            isTwoHanded = true,
            adsFovDeg = 34f,
            shotSfxKey = "gun_sniper"
        ),
        FirearmSpec(
            id = FirearmId.AK47_STYLE,
            toolId = "FIREARM_AK47",
            displayName = "CR-47 Assault Rifle",
            roleLabel = "Assault Rifle",
            caliberLabel = "7.62x39mm",
            modelAssetPath = "models/firearms/ak47_style.glb",
            magazineCapacity = 30,
            initialReserve = 90,
            maxReserve = 210,
            automatic = true,
            fireIntervalSec = 0.10f,
            reloadDurationSec = 2.05f,
            pellets = 1,
            hipSpreadRad = 0.022f,
            adsSpreadRad = 0.0045f,
            rangeMeters = 145f,
            impulseNewtonSec = 27.5f,
            damageRatio = 0.54f,
            recoilPitchRad = 0.036f,
            recoilYawRad = 0.011f,
            recoilKickBack = 0.052f,
            isTwoHanded = true,
            adsFovDeg = 47f,
            shotSfxKey = "gun_rifle"
        )
    )

    private val byId = all.associateBy { it.id }
    private val byToolId = all.associateBy { it.toolId }

    fun get(id: FirearmId): FirearmSpec = byId.getValue(id)
    fun byToolId(toolId: String): FirearmSpec? = byToolId[toolId]
    fun isFirearmTool(toolId: String): Boolean = byToolId.containsKey(toolId)
}

data class NearbyAmmoBoxPrompt(
    val bodyId: Int,
    val variantId: String,
    val displayName: String,
    val caliberLabel: String,
    val amount: Int
)

data class FirearmHudState(
    val equippedFirearmId: FirearmId? = null,
    val displayName: String = "",
    val caliberLabel: String = "",
    val actionState: WeaponActionState = WeaponActionState.HOLSTERED,
    val isAiming: Boolean = false,
    val isHolstered: Boolean = false,
    val magAmmo: Int = 0,
    val magCapacity: Int = 0,
    val reserveAmmo: Int = 0,
    val reloadProgress: Float = 0f,
    val recoilVisualOffset: Float = 0f,
    val muzzleFlashIntensity: Float = 0f,
    val nearbyAmmoBox: NearbyAmmoBoxPrompt? = null,
    val allAmmoReserves: Map<FirearmId, Int> = emptyMap()
)
