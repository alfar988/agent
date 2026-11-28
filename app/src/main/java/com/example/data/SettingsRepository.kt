package com.example.data

import android.content.Context
import android.view.KeyEvent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.character.CharacterProfilePreset
import com.example.physics.CameraMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "kineticlab_settings")

enum class GraphicsPreset(val displayName: String, val qualityIdx: Int) {
    LOW("LOW", 0),
    MEDIUM("MEDIUM", 1),
    HIGH("HIGH", 2),
    ULTRA("ULTRA", 3)
}

data class PcKeyBindings(
    val forward: Int = KeyEvent.KEYCODE_W,
    val backward: Int = KeyEvent.KEYCODE_S,
    val left: Int = KeyEvent.KEYCODE_A,
    val right: Int = KeyEvent.KEYCODE_D,
    val jump: Int = KeyEvent.KEYCODE_SPACE,
    val interact: Int = KeyEvent.KEYCODE_E,
    val inventory: Int = KeyEvent.KEYCODE_TAB,
    val freeze: Int = KeyEvent.KEYCODE_F,
    val cameraToggle: Int = KeyEvent.KEYCODE_C,
    val crouch: Int = KeyEvent.KEYCODE_CTRL_LEFT,
    val sprint: Int = KeyEvent.KEYCODE_SHIFT_LEFT,
    val reload: Int = KeyEvent.KEYCODE_R,
    val holster: Int = KeyEvent.KEYCODE_H
)

data class AppSettings(
    val graphicsQuality: Int = 2, // 0 Low, 1 Med, 2 High, 3 Ultra
    val graphicsPreset: GraphicsPreset = GraphicsPreset.HIGH,
    val shadowsEnabled: Boolean = true,
    val resolutionScale: Float = 1.0f,
    val shadowQuality: Int = 2, // 0 Off, 1 512, 2 1024, 3 1536
    val textureQuality: Int = 2, // 0 Low, 1 Med, 2 High
    val aoQuality: Int = 2, // 0 Low, 1 Med, 2 High
    val reflectionQuality: Int = 2, // 0 Low, 1 Med, 2 High
    val antiAliasing: Boolean = true,
    val renderDistance: Float = 140f,
    val physicsQuality: Int = 2, // 0=6iter, 1=8iter, 2=10iter, 3=14iter
    val particleQuality: Int = 2, // 0=120, 1=300, 2=550, 3=900
    val maxPhysicsObjects: Int = 160,
    val fpsLimit: Int = 60, // 0=Unlimited, 30, 60, 90, 120
    val fovDegrees: Float = 70f,
    val defaultCameraMode: CameraMode = CameraMode.FIRST_PERSON,
    val joystickSensitivity: Float = 1.0f,
    val cameraSensitivity: Float = 1.0f,
    val cameraSmoothing: Float = 0.25f,
    val joystickDeadzone: Float = 0.12f,
    val invertCameraY: Boolean = false,
    val movementSpeedScale: Float = 1.0f,
    val sprintToggleMode: Boolean = true,
    val joystickOffsetX: Float = 0f,
    val joystickOffsetY: Float = 0f,
    val buttonsOffsetX: Float = 0f,
    val buttonsOffsetY: Float = 0f,
    val buttonsScale: Float = 1.0f,
    val masterVolume: Float = 0.85f,
    val sfxVolume: Float = 0.85f,
    val ambientVolume: Float = 0.40f,
    val uiVolume: Float = 0.85f,
    val cameraBobEnabled: Boolean = true,
    val cameraShakeEnabled: Boolean = false,
    val interactionSensitivity: Float = 1.0f,
    val thirdPersonDistance: Float = 3.6f,
    val thirdPersonHeight: Float = 0.45f,
    val characterCustomizationEncoded: String = CharacterProfilePreset.VANCE.defaultAppearance.encodeCompact(),
    val keyBindings: PcKeyBindings = PcKeyBindings(),
    val favoriteIds: Set<String> = emptySet(),
    val recentIds: List<String> = emptyList(),
    val aimSensitivity: Float = 0.65f,
    val autoReloadEnabled: Boolean = true,
    val holdToAim: Boolean = false
) {
    companion object {
        fun defaults(): AppSettings = AppSettings()
    }

    fun solverIterationsCount(): Int = when (physicsQuality) {
        0 -> 6
        1 -> 8
        2 -> 10
        else -> 14
    }

    fun particleBudgetCount(): Int = when (particleQuality) {
        0 -> 120
        1 -> 300
        2 -> 550
        else -> 900
    }

    fun withPreset(preset: GraphicsPreset): AppSettings = when (preset) {
        GraphicsPreset.LOW -> copy(
            graphicsPreset = preset,
            graphicsQuality = 0,
            shadowsEnabled = false,
            shadowQuality = 0,
            resolutionScale = 0.70f,
            textureQuality = 0,
            aoQuality = 0,
            reflectionQuality = 0,
            antiAliasing = false,
            renderDistance = 80f,
            physicsQuality = 0,
            particleQuality = 0,
            maxPhysicsObjects = 96
        )
        GraphicsPreset.MEDIUM -> copy(
            graphicsPreset = preset,
            graphicsQuality = 1,
            shadowsEnabled = true,
            shadowQuality = 1,
            resolutionScale = 0.85f,
            textureQuality = 1,
            aoQuality = 1,
            reflectionQuality = 1,
            antiAliasing = false,
            renderDistance = 115f,
            physicsQuality = 1,
            particleQuality = 1,
            maxPhysicsObjects = 130
        )
        GraphicsPreset.HIGH -> copy(
            graphicsPreset = preset,
            graphicsQuality = 2,
            shadowsEnabled = true,
            shadowQuality = 2,
            resolutionScale = 1.0f,
            textureQuality = 2,
            aoQuality = 2,
            reflectionQuality = 2,
            antiAliasing = true,
            renderDistance = 150f,
            physicsQuality = 2,
            particleQuality = 2,
            maxPhysicsObjects = 180
        )
        GraphicsPreset.ULTRA -> copy(
            graphicsPreset = preset,
            graphicsQuality = 3,
            shadowsEnabled = true,
            shadowQuality = 3,
            resolutionScale = 1.0f,
            textureQuality = 2,
            aoQuality = 2,
            reflectionQuality = 2,
            antiAliasing = true,
            renderDistance = 210f,
            physicsQuality = 3,
            particleQuality = 3,
            maxPhysicsObjects = 240
        )
    }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val GRAPHICS_QUALITY = intPreferencesKey("graphics_quality")
        val GRAPHICS_PRESET = intPreferencesKey("graphics_preset")
        val SHADOWS = booleanPreferencesKey("shadows_enabled")
        val RES_SCALE = floatPreferencesKey("res_scale")
        val SHADOW_QUAL = intPreferencesKey("shadow_qual")
        val TEX_QUAL = intPreferencesKey("tex_qual")
        val AO_QUAL = intPreferencesKey("ao_qual")
        val REFL_QUAL = intPreferencesKey("refl_qual")
        val AA_ENABLED = booleanPreferencesKey("aa_enabled")
        val RENDER_DIST = floatPreferencesKey("render_dist")
        val PHYS_QUAL = intPreferencesKey("phys_qual")
        val PARTICLE_QUAL = intPreferencesKey("particle_qual")
        val MAX_PHYS_OBJS = intPreferencesKey("max_phys_objs")
        val FPS_LIMIT = intPreferencesKey("fps_limit")
        val FOV = floatPreferencesKey("fov_degrees")
        val CAMERA_MODE = intPreferencesKey("camera_mode")
        val JOYSTICK_SENS = floatPreferencesKey("joystick_sens")
        val CAMERA_SENS = floatPreferencesKey("camera_sens")
        val CAM_SMOOTH = floatPreferencesKey("cam_smooth")
        val JOY_DEADZONE = floatPreferencesKey("joy_deadzone")
        val INVERT_Y = booleanPreferencesKey("invert_y")
        val MOVE_SCALE = floatPreferencesKey("move_scale")
        val SPRINT_TOGGLE = booleanPreferencesKey("sprint_toggle")
        val JOY_OFF_X = floatPreferencesKey("joy_off_x")
        val JOY_OFF_Y = floatPreferencesKey("joy_off_y")
        val BTN_OFF_X = floatPreferencesKey("btn_off_x")
        val BTN_OFF_Y = floatPreferencesKey("btn_off_y")
        val BTN_SCALE = floatPreferencesKey("btn_scale")
        val MASTER_VOL = floatPreferencesKey("master_vol")
        val SFX_VOL = floatPreferencesKey("sfx_vol")
        val AMBIENT_VOL = floatPreferencesKey("ambient_vol")
        val UI_VOL = floatPreferencesKey("ui_vol")
        val CAM_BOB = booleanPreferencesKey("cam_bob")
        val CAM_SHAKE = booleanPreferencesKey("cam_shake")
        val INTERACT_SENS = floatPreferencesKey("interact_sens")
        val TP_DIST = floatPreferencesKey("tp_dist")
        val TP_HEIGHT = floatPreferencesKey("tp_height")
        val CHAR_CUSTOM = stringPreferencesKey("char_custom_encoded")

        val KEY_FWD = intPreferencesKey("key_fwd")
        val KEY_BACK = intPreferencesKey("key_back")
        val KEY_LEFT = intPreferencesKey("key_left")
        val KEY_RIGHT = intPreferencesKey("key_right")
        val KEY_JUMP = intPreferencesKey("key_jump")
        val KEY_INTERACT = intPreferencesKey("key_interact")
        val KEY_INV = intPreferencesKey("key_inv")
        val KEY_FREEZE = intPreferencesKey("key_freeze")
        val KEY_CAM = intPreferencesKey("key_cam")
        val KEY_CROUCH = intPreferencesKey("key_crouch")
        val KEY_SPRINT = intPreferencesKey("key_sprint")
        val KEY_RELOAD = intPreferencesKey("key_reload")
        val KEY_HOLSTER = intPreferencesKey("key_holster")

        val AIM_SENS = floatPreferencesKey("aim_sens")
        val AUTO_RELOAD = booleanPreferencesKey("auto_reload")
        val HOLD_TO_AIM = booleanPreferencesKey("hold_to_aim")

        val FAVORITES = stringSetPreferencesKey("favorite_ids")
        val RECENTS = stringPreferencesKey("recent_ids_csv")
    }

    private fun decodeSettings(prefs: Preferences): AppSettings = AppSettings(
        graphicsQuality = prefs[Keys.GRAPHICS_QUALITY] ?: 2,
        graphicsPreset = GraphicsPreset.entries.getOrElse(prefs[Keys.GRAPHICS_PRESET] ?: 2) { GraphicsPreset.HIGH },
        shadowsEnabled = prefs[Keys.SHADOWS] ?: true,
        resolutionScale = prefs[Keys.RES_SCALE] ?: 1.0f,
        shadowQuality = prefs[Keys.SHADOW_QUAL] ?: 2,
        textureQuality = prefs[Keys.TEX_QUAL] ?: 2,
        aoQuality = prefs[Keys.AO_QUAL] ?: 2,
        reflectionQuality = prefs[Keys.REFL_QUAL] ?: 2,
        antiAliasing = prefs[Keys.AA_ENABLED] ?: true,
        renderDistance = prefs[Keys.RENDER_DIST] ?: 140f,
        physicsQuality = prefs[Keys.PHYS_QUAL] ?: 2,
        particleQuality = prefs[Keys.PARTICLE_QUAL] ?: 2,
        maxPhysicsObjects = prefs[Keys.MAX_PHYS_OBJS] ?: 160,
        fpsLimit = prefs[Keys.FPS_LIMIT] ?: 60,
        fovDegrees = prefs[Keys.FOV] ?: 70f,
        defaultCameraMode = CameraMode.entries.getOrElse(prefs[Keys.CAMERA_MODE] ?: 0) { CameraMode.FIRST_PERSON },
        joystickSensitivity = prefs[Keys.JOYSTICK_SENS] ?: 1.0f,
        cameraSensitivity = prefs[Keys.CAMERA_SENS] ?: 1.0f,
        cameraSmoothing = prefs[Keys.CAM_SMOOTH] ?: 0.25f,
        joystickDeadzone = prefs[Keys.JOY_DEADZONE] ?: 0.12f,
        invertCameraY = prefs[Keys.INVERT_Y] ?: false,
        movementSpeedScale = prefs[Keys.MOVE_SCALE] ?: 1.0f,
        sprintToggleMode = prefs[Keys.SPRINT_TOGGLE] ?: true,
        joystickOffsetX = prefs[Keys.JOY_OFF_X] ?: 0f,
        joystickOffsetY = prefs[Keys.JOY_OFF_Y] ?: 0f,
        buttonsOffsetX = prefs[Keys.BTN_OFF_X] ?: 0f,
        buttonsOffsetY = prefs[Keys.BTN_OFF_Y] ?: 0f,
        buttonsScale = prefs[Keys.BTN_SCALE] ?: 1.0f,
        masterVolume = prefs[Keys.MASTER_VOL] ?: 0.85f,
        sfxVolume = prefs[Keys.SFX_VOL] ?: 0.85f,
        ambientVolume = prefs[Keys.AMBIENT_VOL] ?: 0.40f,
        uiVolume = prefs[Keys.UI_VOL] ?: 0.85f,
        cameraBobEnabled = prefs[Keys.CAM_BOB] ?: true,
        cameraShakeEnabled = prefs[Keys.CAM_SHAKE] ?: false,
        interactionSensitivity = prefs[Keys.INTERACT_SENS] ?: 1.0f,
        thirdPersonDistance = prefs[Keys.TP_DIST] ?: 3.6f,
        thirdPersonHeight = prefs[Keys.TP_HEIGHT] ?: 0.45f,
        characterCustomizationEncoded = prefs[Keys.CHAR_CUSTOM] ?: CharacterProfilePreset.VANCE.defaultAppearance.encodeCompact(),
        keyBindings = PcKeyBindings(
            forward = prefs[Keys.KEY_FWD] ?: KeyEvent.KEYCODE_W,
            backward = prefs[Keys.KEY_BACK] ?: KeyEvent.KEYCODE_S,
            left = prefs[Keys.KEY_LEFT] ?: KeyEvent.KEYCODE_A,
            right = prefs[Keys.KEY_RIGHT] ?: KeyEvent.KEYCODE_D,
            jump = prefs[Keys.KEY_JUMP] ?: KeyEvent.KEYCODE_SPACE,
            interact = prefs[Keys.KEY_INTERACT] ?: KeyEvent.KEYCODE_E,
            inventory = prefs[Keys.KEY_INV] ?: KeyEvent.KEYCODE_TAB,
            freeze = prefs[Keys.KEY_FREEZE] ?: KeyEvent.KEYCODE_F,
            cameraToggle = prefs[Keys.KEY_CAM] ?: KeyEvent.KEYCODE_C,
            crouch = prefs[Keys.KEY_CROUCH] ?: KeyEvent.KEYCODE_CTRL_LEFT,
            sprint = prefs[Keys.KEY_SPRINT] ?: KeyEvent.KEYCODE_SHIFT_LEFT,
            reload = prefs[Keys.KEY_RELOAD] ?: KeyEvent.KEYCODE_R,
            holster = prefs[Keys.KEY_HOLSTER] ?: KeyEvent.KEYCODE_H
        ),
        favoriteIds = prefs[Keys.FAVORITES] ?: emptySet(),
        recentIds = prefs[Keys.RECENTS]?.split(",")?.filter { it.isNotBlank() }.orEmpty(),
        aimSensitivity = prefs[Keys.AIM_SENS] ?: 0.65f,
        autoReloadEnabled = prefs[Keys.AUTO_RELOAD] ?: true,
        holdToAim = prefs[Keys.HOLD_TO_AIM] ?: false
    )

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map(::decodeSettings)

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val current = decodeSettings(prefs)
            val updated = transform(current)
            prefs[Keys.GRAPHICS_QUALITY] = updated.graphicsQuality
            prefs[Keys.GRAPHICS_PRESET] = updated.graphicsPreset.ordinal
            prefs[Keys.SHADOWS] = updated.shadowsEnabled
            prefs[Keys.RES_SCALE] = updated.resolutionScale
            prefs[Keys.SHADOW_QUAL] = updated.shadowQuality
            prefs[Keys.TEX_QUAL] = updated.textureQuality
            prefs[Keys.AO_QUAL] = updated.aoQuality
            prefs[Keys.REFL_QUAL] = updated.reflectionQuality
            prefs[Keys.AA_ENABLED] = updated.antiAliasing
            prefs[Keys.RENDER_DIST] = updated.renderDistance
            prefs[Keys.PHYS_QUAL] = updated.physicsQuality
            prefs[Keys.PARTICLE_QUAL] = updated.particleQuality
            prefs[Keys.MAX_PHYS_OBJS] = updated.maxPhysicsObjects
            prefs[Keys.FPS_LIMIT] = updated.fpsLimit
            prefs[Keys.FOV] = updated.fovDegrees
            prefs[Keys.CAMERA_MODE] = updated.defaultCameraMode.ordinal
            prefs[Keys.JOYSTICK_SENS] = updated.joystickSensitivity
            prefs[Keys.CAMERA_SENS] = updated.cameraSensitivity
            prefs[Keys.CAM_SMOOTH] = updated.cameraSmoothing
            prefs[Keys.JOY_DEADZONE] = updated.joystickDeadzone
            prefs[Keys.INVERT_Y] = updated.invertCameraY
            prefs[Keys.MOVE_SCALE] = updated.movementSpeedScale
            prefs[Keys.SPRINT_TOGGLE] = updated.sprintToggleMode
            prefs[Keys.JOY_OFF_X] = updated.joystickOffsetX
            prefs[Keys.JOY_OFF_Y] = updated.joystickOffsetY
            prefs[Keys.BTN_OFF_X] = updated.buttonsOffsetX
            prefs[Keys.BTN_OFF_Y] = updated.buttonsOffsetY
            prefs[Keys.BTN_SCALE] = updated.buttonsScale
            prefs[Keys.MASTER_VOL] = updated.masterVolume
            prefs[Keys.SFX_VOL] = updated.sfxVolume
            prefs[Keys.AMBIENT_VOL] = updated.ambientVolume
            prefs[Keys.UI_VOL] = updated.uiVolume
            prefs[Keys.CAM_BOB] = updated.cameraBobEnabled
            prefs[Keys.CAM_SHAKE] = updated.cameraShakeEnabled
            prefs[Keys.INTERACT_SENS] = updated.interactionSensitivity
            prefs[Keys.TP_DIST] = updated.thirdPersonDistance
            prefs[Keys.TP_HEIGHT] = updated.thirdPersonHeight
            prefs[Keys.CHAR_CUSTOM] = updated.characterCustomizationEncoded
            prefs[Keys.KEY_FWD] = updated.keyBindings.forward
            prefs[Keys.KEY_BACK] = updated.keyBindings.backward
            prefs[Keys.KEY_LEFT] = updated.keyBindings.left
            prefs[Keys.KEY_RIGHT] = updated.keyBindings.right
            prefs[Keys.KEY_JUMP] = updated.keyBindings.jump
            prefs[Keys.KEY_INTERACT] = updated.keyBindings.interact
            prefs[Keys.KEY_INV] = updated.keyBindings.inventory
            prefs[Keys.KEY_FREEZE] = updated.keyBindings.freeze
            prefs[Keys.KEY_CAM] = updated.keyBindings.cameraToggle
            prefs[Keys.KEY_CROUCH] = updated.keyBindings.crouch
            prefs[Keys.KEY_SPRINT] = updated.keyBindings.sprint
            prefs[Keys.KEY_RELOAD] = updated.keyBindings.reload
            prefs[Keys.KEY_HOLSTER] = updated.keyBindings.holster
            prefs[Keys.AIM_SENS] = updated.aimSensitivity
            prefs[Keys.AUTO_RELOAD] = updated.autoReloadEnabled
            prefs[Keys.HOLD_TO_AIM] = updated.holdToAim
            prefs[Keys.FAVORITES] = updated.favoriteIds
            prefs[Keys.RECENTS] = updated.recentIds.take(12).joinToString(",")
        }
    }
}
