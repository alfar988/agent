package com.example.ui.viewmodel

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.character.AnimationWorker
import com.example.character.CharacterAppearance
import com.example.character.CharacterProfilePreset
import com.example.data.AppSettings
import com.example.data.BuiltInPreset
import com.example.data.SaveRepository
import com.example.data.SaveSlotSummary
import com.example.data.SettingsRepository
import com.example.math.Vec3
import com.example.physics.CameraMode
import com.example.physics.CollisionMode
import com.example.physics.ConstraintType
import com.example.physics.InspectedBodyInfo
import com.example.physics.InventoryCategory
import com.example.physics.MapId
import com.example.physics.MaterialCategory
import com.example.physics.MaterialLibrary
import com.example.physics.MaterialSpec
import com.example.physics.ObjectType
import com.example.physics.PhysicsCommand
import com.example.physics.PhysicsMaterial
import com.example.physics.PhysicsWorld
import com.example.physics.SessionExportData
import com.example.physics.ToolCatalog
import com.example.physics.ToolCategory
import com.example.physics.ToolTelemetrySnapshot
import com.example.physics.ToolType
import com.example.physics.WeatherType
import com.example.render.CharacterCameraFocus
import com.example.render.SandboxGLRenderer
import com.example.ui.hud.HudPanelsState
import com.example.ui.hud.PanelVisibilityController
import com.example.ui.hud.ToolInfoPopupData
import com.example.ui.hud.ToolPickerMode
import com.example.weapons.FirearmCatalog
import com.example.weapons.FirearmHudState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class AppScreen {
    MAIN_MENU,
    MAP_SELECT,
    CHARACTER_SELECT,
    PLAYING,
    SETTINGS,
    TUTORIAL,
    ABOUT
}

data class InventoryTabState(
    val searchQuery: String = "",
    val selectedInvCat: InventoryCategory? = null,
    val showFavoritesOnly: Boolean = false,
    val materialSubFilter: MaterialCategory? = null,
    val toolSubFilter: ToolCategory? = null
)

data class SandboxUiState(
    val screen: AppScreen = AppScreen.MAIN_MENU,
    val returnScreenFromSettings: AppScreen = AppScreen.MAIN_MENU,
    val activeMap: MapId = MapId.LAB,
    val isPaused: Boolean = false,
    val showInventory: Boolean = false,
    val showInspectorSheet: Boolean = false,
    val showSaveLoadDialog: Boolean = false,
    val showEnvironmentSheet: Boolean = false,
    val showPresetsDialog: Boolean = false,
    val screenshotMode: Boolean = false,
    val isCrouching: Boolean = false,
    val isSprinting: Boolean = false,
    val selectedTool: ToolType = ToolType.GRAB,
    val activeCatalogToolId: String = "HAMMER",
    val quickToolIds: List<String> = listOf("HAMMER", "ELECTRIC_DRILL", "ANGLE_GRINDER", "ELECTRIC_WELDER", "MAGNETIC_TOOL", "MULTIMETER"),
    val spawnQuantity: Int = 1,
    val attachConstraintType: ConstraintType = ConstraintType.WELD,
    val cameraMode: CameraMode = CameraMode.FIRST_PERSON,
    val targetedBody: InspectedBodyInfo? = null,
    val latchedInspectedBody: InspectedBodyInfo? = null,
    val inventoryTabState: InventoryTabState = InventoryTabState(),
    val characterAppearance: CharacterAppearance = CharacterProfilePreset.VANCE.defaultAppearance,
    val characterFocusPreset: CharacterCameraFocus = CharacterCameraFocus.FULL,
    val toolTelemetry: ToolTelemetrySnapshot = ToolTelemetrySnapshot(),
    val isGrabbing: Boolean = false,
    val isPendingAttach: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val timeScale: Float = 1.0f,
    val timeOfDayHours: Float = 13.5f,
    val weatherType: WeatherType = WeatherType.CLEAR,
    val windStrength: Float = 0f,
    val fluidBasinEnabled: Boolean = false,
    val fps: Int = 60,
    val physicsMs: Float = 0.0f,
    val objectCount: Int = 0,
    val particleCount: Int = 0,
    val culledCount: Int = 0,
    val settings: AppSettings = AppSettings(),
    val saveSlots: List<SaveSlotSummary> = emptyList(),
    val exportedJsonPreview: String = "",
    val statusBanner: String? = null,
    val hudPanels: HudPanelsState = HudPanelsState(),
    val firearmHud: FirearmHudState = FirearmHudState()
) {
    val isFirearmActive: Boolean
        get() = selectedTool == ToolType.CATALOG_TOOL && firearmHud.equippedFirearmId != null
}

class SandboxViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val saveRepo = SaveRepository(application)
    val soundEngine = SoundEngine(application)
    val animationWorker = AnimationWorker()

    val physicsWorld = PhysicsWorld(
        onAudioEvent = { ev -> soundEngine.playCollision(ev) },
        onToolSfx = { tag, pos -> soundEngine.playToolSound(tag, pos) }
    )
    val glRenderer = SandboxGLRenderer(application, physicsWorld, animationWorker)

    private val _uiState = MutableStateFlow(SandboxUiState())
    val uiState: StateFlow<SandboxUiState> = _uiState.asStateFlow()

    val panelController = PanelVisibilityController(viewModelScope)

    private val pressedKeys = HashSet<Int>()
    private var autosaveJob: Job? = null
    private var lastScreenshotTriggerSeq = 0
    private var lastSeenTelemetryKey = ""
    private var prevLookYawDelta = 0f
    private var prevLookPitchDelta = 0f

    init {
        soundEngine.listenerProvider = { physicsWorld.listenerTransform }
        soundEngine.initialize()
        physicsWorld.start()

        viewModelScope.launch {
            panelController.state.collect { panels ->
                _uiState.update { it.copy(hudPanels = panels) }
            }
        }

        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { s ->
                val charApp = CharacterAppearance.decodeCompact(s.characterCustomizationEncoded)
                _uiState.update { it.copy(settings = s, characterAppearance = charApp) }
                glRenderer.shadowsEnabled = s.shadowsEnabled && s.shadowQuality > 0
                glRenderer.graphicsQuality = s.graphicsQuality
                glRenderer.shadowQuality = s.shadowQuality
                glRenderer.textureQuality = s.textureQuality
                glRenderer.aoQuality = s.aoQuality
                glRenderer.reflectionQuality = s.reflectionQuality
                glRenderer.resolutionScale = s.resolutionScale
                glRenderer.renderDistance = s.renderDistance
                glRenderer.fpsLimit = s.fpsLimit
                glRenderer.fovDegrees = s.fovDegrees
                glRenderer.characterAppearance = charApp
                physicsWorld.enqueue(PhysicsCommand.SetPhysicsQuality(s.solverIterationsCount()))
                physicsWorld.enqueue(PhysicsCommand.SetParticleBudget(s.particleBudgetCount()))
                physicsWorld.enqueue(PhysicsCommand.SetMaxDynamicObjects(s.maxPhysicsObjects))
                physicsWorld.enqueue(
                    PhysicsCommand.SetCameraBehavior(
                        thirdPersonDistance = s.thirdPersonDistance,
                        thirdPersonHeight = s.thirdPersonHeight,
                        headBobEnabled = s.cameraBobEnabled,
                        cameraShakeEnabled = s.cameraShakeEnabled,
                        movementSpeedScale = s.movementSpeedScale
                    )
                )
                soundEngine.masterVolume = s.masterVolume
                soundEngine.sfxVolume = s.sfxVolume
                soundEngine.ambientVolume = s.ambientVolume
                soundEngine.uiVolume = s.uiVolume
                physicsWorld.enqueue(PhysicsCommand.SetFirearmSettings(s.autoReloadEnabled))
            }
        }

        refreshSaveSlots()

        viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.screen == AppScreen.PLAYING) {
                    val triggerSeq = physicsWorld.screenshotTriggerSeq.get()
                    if (triggerSeq != lastScreenshotTriggerSeq) {
                        lastScreenshotTriggerSeq = triggerSeq
                        captureScreenshot(autoHideBriefly = true)
                    }
                    val telemetry = physicsWorld.toolTelemetryRef.get()
                    val target = physicsWorld.targetedBodyRef.get()
                    val snapUi = _uiState.value
                    val modalBlocking = snapUi.isPaused || snapUi.showInventory || snapUi.showInspectorSheet ||
                        snapUi.showEnvironmentSheet || snapUi.showPresetsDialog || snapUi.screenshotMode
                    panelController.setSuppressed(modalBlocking)

                    val firearm = physicsWorld.firearmHudRef.get() ?: FirearmHudState()
                    if (!modalBlocking && snapUi.selectedTool == ToolType.CATALOG_TOOL &&
                        !FirearmCatalog.isFirearmTool(snapUi.activeCatalogToolId) &&
                        telemetry != null && telemetry.readoutTitle.isNotBlank()
                    ) {
                        val tKey = "${snapUi.activeCatalogToolId}:${telemetry.readoutTitle}:${telemetry.readoutDetail}"
                        val defaultEquipTitle = ToolCatalog.get(snapUi.activeCatalogToolId)?.displayName?.uppercase()
                        if (tKey != lastSeenTelemetryKey) {
                            lastSeenTelemetryKey = tKey
                            if (telemetry.readoutTitle != defaultEquipTitle) {
                                panelController.showToolInfo(
                                    ToolInfoPopupData(
                                        key = "telemetry:$tKey",
                                        title = telemetry.readoutTitle,
                                        description = telemetry.readoutDetail.ifBlank { "Active measurement" },
                                        toolType = ToolType.CATALOG_TOOL,
                                        catalogToolId = snapUi.activeCatalogToolId
                                    )
                                )
                            }
                        }
                    }

                    _uiState.update { state ->
                        val nextLatched = if (state.showInspectorSheet) {
                            if (target != null && target.id == state.latchedInspectedBody?.id) {
                                target
                            } else {
                                state.latchedInspectedBody
                            }
                        } else null
                        state.copy(
                            targetedBody = target,
                            latchedInspectedBody = nextLatched,
                            toolTelemetry = telemetry ?: state.toolTelemetry,
                            firearmHud = firearm,
                            isGrabbing = physicsWorld.grabbedBodyIdRef.get() != null,
                            isPendingAttach = physicsWorld.pendingAttachBodyIdRef.get() != null,
                            canUndo = physicsWorld.canUndoRef.get() == true,
                            canRedo = physicsWorld.canRedoRef.get() == true,
                            fps = glRenderer.fpsRef.get(),
                            physicsMs = physicsWorld.physicsStepMsRef.get() ?: 0f,
                            objectCount = physicsWorld.bodyCountRef.get() ?: 0,
                            particleCount = glRenderer.particleCountRef.get() ?: 0,
                            culledCount = glRenderer.culledCountRef.get() ?: 0
                        )
                    }
                }
                delay(50L)
            }
        }
    }

    fun refreshSaveSlots() {
        viewModelScope.launch {
            val slots = saveRepo.listSlotSummaries()
            _uiState.update { it.copy(saveSlots = slots) }
        }
    }

    fun navigateTo(screen: AppScreen) {
        soundEngine.playUiClick()
        glRenderer.characterPreviewState.enabled = (screen == AppScreen.CHARACTER_SELECT)
        if (screen == AppScreen.SETTINGS) {
            _uiState.update {
                it.copy(returnScreenFromSettings = it.screen, screen = AppScreen.SETTINGS)
            }
        } else {
            _uiState.update { it.copy(screen = screen) }
        }
    }

    fun navigateBack() {
        val current = _uiState.value
        when (current.screen) {
            AppScreen.MAIN_MENU -> Unit
            AppScreen.MAP_SELECT, AppScreen.CHARACTER_SELECT, AppScreen.TUTORIAL, AppScreen.ABOUT -> navigateTo(AppScreen.MAIN_MENU)
            AppScreen.SETTINGS -> navigateTo(current.returnScreenFromSettings)
            AppScreen.PLAYING -> {
                when {
                    current.screenshotMode -> setScreenshotMode(false)
                    current.showInventory -> toggleInventory(false)
                    current.showInspectorSheet -> toggleInspectorSheet(false)
                    current.showEnvironmentSheet -> toggleEnvironmentSheet(false)
                    current.showPresetsDialog -> togglePresetsDialog(false)
                    current.showSaveLoadDialog -> toggleSaveLoadDialog(false)
                    else -> togglePause(!current.isPaused)
                }
            }
        }
    }

    fun startMap(mapId: MapId) {
        soundEngine.playUiClick()
        glRenderer.characterPreviewState.enabled = false
        panelController.dismissToolInfo()
        panelController.closeToolPicker()
        lastSeenTelemetryKey = ""
        val camMode = _uiState.value.settings.defaultCameraMode
        physicsWorld.enqueue(PhysicsCommand.LoadMap(mapId, isMenuDemo = false))
        physicsWorld.enqueue(PhysicsCommand.SetCameraMode(camMode))
        physicsWorld.enqueue(PhysicsCommand.SetPlayerPosture(crouching = false, sprinting = false))
        soundEngine.startAmbient(mapId)
        startAutosaveLoop()
        _uiState.update {
            it.copy(
                screen = AppScreen.PLAYING,
                activeMap = mapId,
                isPaused = false,
                isCrouching = false,
                isSprinting = false,
                showInventory = false,
                showInspectorSheet = false,
                latchedInspectedBody = null,
                showSaveLoadDialog = false,
                showEnvironmentSheet = false,
                showPresetsDialog = false,
                screenshotMode = false,
                timeScale = 1.0f,
                timeOfDayHours = if (mapId == MapId.CONSTRUCTION) 14.5f else 13.5f,
                weatherType = WeatherType.CLEAR,
                windStrength = 0f,
                fluidBasinEnabled = false,
                cameraMode = camMode,
                statusBanner = "Loaded ${mapId.displayName}"
            )
        }
        clearBannerDelayed()
    }

    fun loadSavedSession(slotId: String) {
        soundEngine.playUiClick()
        viewModelScope.launch {
            val data = saveRepo.loadSession(slotId) ?: return@launch
            applySessionExport(data, "Restored session ($slotId)")
        }
    }

    fun loadPreset(preset: BuiltInPreset) {
        soundEngine.playUiClick()
        val data = preset.build()
        applySessionExport(data, "Loaded preset: ${preset.title}")
    }

    fun exportCurrentJsonPreview() {
        val export = physicsWorld.exportSessionSync() ?: return
        val json = saveRepo.exportToJsonString("export", "Custom Scene (${export.mapId.code})", export)
        _uiState.update { it.copy(exportedJsonPreview = json, statusBanner = "JSON scene snapshot ready") }
        clearBannerDelayed()
    }

    fun importJsonScene(json: String) {
        val parsed = saveRepo.importFromJsonString(json.trim())
        if (parsed == null) {
            _uiState.update { it.copy(statusBanner = "Invalid JSON scene format") }
            clearBannerDelayed()
            return
        }
        applySessionExport(parsed, "Imported JSON scene (${parsed.bodies.size} objects)")
    }

    private fun applySessionExport(data: SessionExportData, banner: String) {
        val camMode = _uiState.value.settings.defaultCameraMode
        physicsWorld.enqueue(
            PhysicsCommand.RestoreState(
                mapId = data.mapId,
                playerPos = data.playerPos,
                playerYaw = data.playerYaw,
                playerPitch = data.playerPitch,
                bodies = data.bodies,
                constraints = data.constraints,
                wires = data.wires,
                weaponMagAmmo = data.weaponMagAmmo,
                weaponReserveAmmo = data.weaponReserveAmmo,
                isWeaponHolstered = data.isWeaponHolstered
            )
        )
        physicsWorld.enqueue(PhysicsCommand.SetCameraMode(camMode))
        soundEngine.startAmbient(data.mapId)
        startAutosaveLoop()
        _uiState.update {
            it.copy(
                screen = AppScreen.PLAYING,
                activeMap = data.mapId,
                isPaused = false,
                showSaveLoadDialog = false,
                showPresetsDialog = false,
                showInventory = false,
                cameraMode = camMode,
                statusBanner = banner
            )
        }
        clearBannerDelayed()
    }

    fun saveToSlot(slotId: String, label: String) {
        soundEngine.playUiClick()
        viewModelScope.launch {
            val export = physicsWorld.exportSessionSync() ?: return@launch
            saveRepo.saveSession(slotId, label, export)
            refreshSaveSlots()
            _uiState.update { it.copy(statusBanner = "Saved to $label") }
            clearBannerDelayed()
        }
    }

    fun deleteSaveSlot(slotId: String) {
        soundEngine.playUiClick()
        viewModelScope.launch {
            saveRepo.deleteSlot(slotId)
            refreshSaveSlots()
        }
    }

    fun exitToMainMenu() {
        soundEngine.playUiClick()
        autosaveNow()
        autosaveJob?.cancel()
        soundEngine.stopAmbient()
        pressedKeys.clear()
        panelController.dismissToolInfo()
        panelController.closeToolPicker()
        physicsWorld.moveInputX = 0f
        physicsWorld.moveInputY = 0f
        physicsWorld.enqueue(PhysicsCommand.LoadMap(MapId.LAB, isMenuDemo = true))
        _uiState.update {
            it.copy(
                screen = AppScreen.MAIN_MENU,
                isPaused = false,
                showInventory = false,
                showInspectorSheet = false,
                showSaveLoadDialog = false,
                showEnvironmentSheet = false,
                showPresetsDialog = false,
                screenshotMode = false
            )
        }
    }

    fun togglePause(paused: Boolean) {
        soundEngine.playUiClick()
        if (paused) autosaveNow()
        physicsWorld.enqueue(PhysicsCommand.SetPaused(paused))
        _uiState.update { it.copy(isPaused = paused) }
    }

    fun toggleInventory(show: Boolean? = null) {
        soundEngine.playUiClick()
        val next = show ?: !_uiState.value.showInventory
        _uiState.update {
            it.copy(
                showInventory = next,
                showInspectorSheet = false,
                latchedInspectedBody = null,
                showEnvironmentSheet = false,
                showPresetsDialog = false
            )
        }
    }

    fun updateInventoryTabState(transform: (InventoryTabState) -> InventoryTabState) {
        _uiState.update { it.copy(inventoryTabState = transform(it.inventoryTabState)) }
    }

    fun toggleInspectorSheet(show: Boolean? = null) {
        soundEngine.playUiClick()
        val next = show ?: !_uiState.value.showInspectorSheet
        _uiState.update {
            it.copy(
                showInspectorSheet = next,
                latchedInspectedBody = if (next) it.targetedBody else null,
                showInventory = false,
                showEnvironmentSheet = false
            )
        }
    }

    fun toggleSaveLoadDialog(show: Boolean? = null) {
        soundEngine.playUiClick()
        val next = show ?: !_uiState.value.showSaveLoadDialog
        if (next) refreshSaveSlots()
        _uiState.update { it.copy(showSaveLoadDialog = next) }
    }

    fun toggleEnvironmentSheet(show: Boolean? = null) {
        soundEngine.playUiClick()
        val next = show ?: !_uiState.value.showEnvironmentSheet
        _uiState.update { it.copy(showEnvironmentSheet = next, showInventory = false, showInspectorSheet = false, showPresetsDialog = false) }
    }

    fun togglePresetsDialog(show: Boolean? = null) {
        soundEngine.playUiClick()
        val next = show ?: !_uiState.value.showPresetsDialog
        _uiState.update { it.copy(showPresetsDialog = next, showInventory = false, showInspectorSheet = false, showEnvironmentSheet = false) }
    }

    fun setScreenshotMode(enabled: Boolean) {
        soundEngine.playUiClick()
        _uiState.update {
            it.copy(
                screenshotMode = enabled,
                showInventory = false,
                showInspectorSheet = false,
                showEnvironmentSheet = false,
                showPresetsDialog = false
            )
        }
    }

    fun captureScreenshot(autoHideBriefly: Boolean = false) {
        viewModelScope.launch {
            if (autoHideBriefly) _uiState.update { it.copy(screenshotMode = true) }
            delay(80L)
            glRenderer.requestScreenshot { bmp ->
                if (bmp != null) {
                    viewModelScope.launch {
                        val savedName = saveBitmapToStorage(bmp)
                        bmp.recycle()
                        _uiState.update {
                            it.copy(
                                screenshotMode = if (autoHideBriefly) false else it.screenshotMode,
                                statusBanner = if (savedName != null) "Screenshot saved ($savedName)" else "Screenshot captured"
                            )
                        }
                        clearBannerDelayed()
                    }
                } else if (autoHideBriefly) {
                    _uiState.update { it.copy(screenshotMode = false) }
                }
            }
        }
    }

    private suspend fun saveBitmapToStorage(bmp: Bitmap): String? = withContext(Dispatchers.IO) {
        val fileName = "KineticLab_${System.currentTimeMillis()}.png"
        val app = getApplication<Application>()
        return@withContext try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KineticLab3D")
                }
                val uri = app.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    app.contentResolver.openOutputStream(uri)?.use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                    fileName
                } else null
            } else {
                val dir = File(app.filesDir, "screenshots").apply { mkdirs() }
                FileOutputStream(File(dir, fileName)).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                fileName
            }
        } catch (_: Throwable) {
            try {
                val dir = File(app.filesDir, "screenshots").apply { mkdirs() }
                FileOutputStream(File(dir, fileName)).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                fileName
            } catch (_: Throwable) {
                null
            }
        }
    }

    fun selectTool(tool: ToolType) {
        soundEngine.playUiClick()
        val cur = _uiState.value
        if (cur.selectedTool == tool) {
            when (tool) {
                ToolType.CATALOG_TOOL -> panelController.toggleToolPicker(ToolPickerMode.CATALOG_TOOLS)
                ToolType.CONSTRAINT -> panelController.toggleToolPicker(ToolPickerMode.CONSTRAINT_MODES)
                else -> Unit
            }
            return
        }
        physicsWorld.enqueue(PhysicsCommand.SetToolSelected(tool))
        panelController.closeToolPicker()
        _uiState.update { it.copy(selectedTool = tool) }
        panelController.showToolInfo(
            PanelVisibilityController.buildToolInfoData(tool, cur.activeCatalogToolId, cur.attachConstraintType)
        )
    }

    fun equipCatalogTool(toolId: String) {
        val tool = ToolCatalog.get(toolId) ?: return
        soundEngine.playUiClick()
        physicsWorld.enqueue(PhysicsCommand.EquipCatalogTool(tool.id))
        physicsWorld.enqueue(PhysicsCommand.SetToolSelected(ToolType.CATALOG_TOOL))
        recordRecent("tool:${tool.id}")
        lastSeenTelemetryKey = "${tool.id}:${tool.displayName.uppercase()}:${tool.description.take(54)}"
        panelController.closeToolPicker()
        val firearmSpec = FirearmCatalog.byToolId(tool.id)
        _uiState.update { state ->
            val nextQuick = (listOf(tool.id) + state.quickToolIds.filter { it != tool.id }).take(6)
            val nextFirearmHud = if (firearmSpec != null) {
                val curHud = physicsWorld.firearmHudRef.get()
                if (curHud != null && curHud.equippedFirearmId == firearmSpec.id) {
                    curHud
                } else {
                    FirearmHudState(
                        equippedFirearmId = firearmSpec.id,
                        displayName = firearmSpec.displayName,
                        caliberLabel = firearmSpec.caliberLabel,
                        actionState = com.example.weapons.WeaponActionState.DRAWING,
                        magAmmo = firearmSpec.magazineCapacity,
                        magCapacity = firearmSpec.magazineCapacity,
                        reserveAmmo = firearmSpec.initialReserve
                    )
                }
            } else {
                FirearmHudState()
            }
            state.copy(
                selectedTool = ToolType.CATALOG_TOOL,
                activeCatalogToolId = tool.id,
                quickToolIds = nextQuick,
                firearmHud = nextFirearmHud
            )
        }
        panelController.showToolInfo(
            PanelVisibilityController.buildToolInfoData(ToolType.CATALOG_TOOL, tool.id, _uiState.value.attachConstraintType),
            deduplicateSameKey = false
        )
    }

    fun dismissToolInfoPopup() {
        panelController.dismissToolInfo()
    }

    fun toggleToolSelectionPanel(mode: ToolPickerMode? = null) {
        soundEngine.playUiClick()
        val resolvedMode = mode ?: if (_uiState.value.selectedTool == ToolType.CONSTRAINT) {
            ToolPickerMode.CONSTRAINT_MODES
        } else {
            ToolPickerMode.CATALOG_TOOLS
        }
        panelController.toggleToolPicker(resolvedMode)
    }

    fun closeToolSelectionPanel() {
        panelController.closeToolPicker()
    }

    fun spawnCatalogToolObject(toolId: String) {
        val tool = ToolCatalog.get(toolId) ?: return
        val qty = _uiState.value.spawnQuantity
        physicsWorld.enqueue(PhysicsCommand.SpawnCatalogToolObject(tool.id, qty))
        recordRecent("tool:${tool.id}")
        _uiState.update { it.copy(statusBanner = "Spawned ${qty}x ${tool.displayName}") }
        clearBannerDelayed()
    }

    fun selectAttachMode(type: ConstraintType) {
        soundEngine.playUiClick()
        panelController.closeToolPicker()
        _uiState.update { it.copy(attachConstraintType = type) }
        panelController.showToolInfo(
            PanelVisibilityController.buildToolInfoData(ToolType.CONSTRAINT, _uiState.value.activeCatalogToolId, type)
        )
    }

    fun toggleCameraMode() {
        soundEngine.playUiClick()
        val next = when (_uiState.value.cameraMode) {
            CameraMode.FIRST_PERSON -> CameraMode.THIRD_PERSON
            CameraMode.THIRD_PERSON -> CameraMode.FREE_CAMERA
            CameraMode.FREE_CAMERA -> CameraMode.FIRST_PERSON
        }
        physicsWorld.enqueue(PhysicsCommand.SetCameraMode(next))
        _uiState.update {
            it.copy(
                cameraMode = next,
                statusBanner = "Camera: ${next.name.replace('_', ' ')}"
            )
        }
        clearBannerDelayed()
    }

    fun setSpawnQuantity(qty: Int) {
        _uiState.update { it.copy(spawnQuantity = qty.coerceIn(1, 5)) }
    }

    fun spawnObject(type: ObjectType) {
        val qty = _uiState.value.spawnQuantity
        if (qty <= 1) {
            physicsWorld.enqueue(PhysicsCommand.SpawnObject(type))
        } else {
            physicsWorld.enqueue(PhysicsCommand.SpawnBatch(type, null, qty))
        }
        recordRecent("obj:${type.name}")
        _uiState.update { it.copy(statusBanner = "Spawned ${qty}x ${type.displayName}") }
        clearBannerDelayed()
    }

    fun spawnMaterialBlock(spec: MaterialSpec) {
        val qty = _uiState.value.spawnQuantity
        val type = when {
            spec.id == "ICE" || spec.id == "FROZEN_WATER" -> ObjectType.ICE_BLOCK
            spec.id == "WAX" -> ObjectType.WAX_BLOCK
            spec.id == "CORK" -> ObjectType.BUOYANCY_FLOAT
            spec.softnessSquish > 0.15f -> ObjectType.SOFT_FOAM_BLOCK
            spec.alpha < 0.92f -> ObjectType.GLASS_CUBE
            spec.category == com.example.physics.MaterialCategory.WOOD -> ObjectType.WOOD_CRATE
            else -> ObjectType.STEEL_CRATE
        }
        physicsWorld.enqueue(PhysicsCommand.SpawnBatch(type, spec, qty))
        recordRecent("mat:${spec.id}")
        _uiState.update { it.copy(statusBanner = "Spawned ${qty}x ${spec.displayName} Block") }
        clearBannerDelayed()
    }

    fun toggleFavorite(itemKey: String) {
        updateSettings { s ->
            val next = if (itemKey in s.favoriteIds) s.favoriteIds - itemKey else s.favoriteIds + itemKey
            s.copy(favoriteIds = next)
        }
    }

    private fun recordRecent(itemKey: String) {
        updateSettings { s ->
            val next = (listOf(itemKey) + s.recentIds.filter { it != itemKey }).take(10)
            s.copy(recentIds = next)
        }
    }

    fun setJoystickVector(x: Float, y: Float) {
        val sens = _uiState.value.settings.joystickSensitivity
        physicsWorld.moveInputX = (x * sens).coerceIn(-1f, 1f)
        physicsWorld.moveInputY = (y * sens).coerceIn(-1f, 1f)
    }

    fun addCameraLookDelta(dxPixels: Float, dyPixels: Float) {
        val st = _uiState.value
        val s = st.settings
        val aimScale = if (st.firearmHud.isAiming) s.aimSensitivity else 1.0f
        val sens = 0.0042f * s.cameraSensitivity * s.interactionSensitivity * aimScale
        val ySign = if (s.invertCameraY) 1f else -1f
        val rawYaw = dxPixels * sens
        val rawPitch = dyPixels * sens * ySign
        val smooth = s.cameraSmoothing.coerceIn(0f, 0.85f)
        val filteredYaw = if (smooth < 0.02f) rawYaw else rawYaw * (1f - smooth * 0.45f) + prevLookYawDelta * (smooth * 0.45f)
        val filteredPitch = if (smooth < 0.02f) rawPitch else rawPitch * (1f - smooth * 0.45f) + prevLookPitchDelta * (smooth * 0.45f)
        prevLookYawDelta = filteredYaw
        prevLookPitchDelta = filteredPitch
        physicsWorld.addLookDelta(filteredYaw, filteredPitch)
    }

    fun jump() {
        physicsWorld.jumpRequested = true
    }

    fun toggleCrouch(forceState: Boolean? = null) {
        val next = forceState ?: !_uiState.value.isCrouching
        physicsWorld.enqueue(PhysicsCommand.SetPlayerPosture(crouching = next, sprinting = if (next) false else null))
        _uiState.update { it.copy(isCrouching = next, isSprinting = if (next) false else it.isSprinting) }
    }

    fun toggleSprint(forceState: Boolean? = null) {
        val next = forceState ?: !_uiState.value.isSprinting
        physicsWorld.enqueue(PhysicsCommand.SetPlayerPosture(crouching = if (next) false else null, sprinting = next))
        _uiState.update { it.copy(isSprinting = next, isCrouching = if (next) false else it.isCrouching) }
    }

    fun primaryAction() {
        val st = _uiState.value
        physicsWorld.enqueue(PhysicsCommand.GrabOrUsePrimary(st.selectedTool, st.attachConstraintType))
    }

    fun releaseGrab() {
        physicsWorld.enqueue(PhysicsCommand.ReleaseGrab)
    }

    fun secondaryAction() {
        val st = _uiState.value
        physicsWorld.enqueue(PhysicsCommand.ThrowOrSecondary(st.selectedTool, 1.0f))
    }

    fun rotateHeld(deltaYaw: Float, deltaPitch: Float) {
        physicsWorld.enqueue(PhysicsCommand.RotateHeld(deltaYaw, deltaPitch))
    }

    fun onFireHoldChanged(pressed: Boolean) {
        physicsWorld.enqueue(PhysicsCommand.SetFireInput(pressed))
    }

    fun onFireTap() {
        physicsWorld.enqueue(PhysicsCommand.TriggerSingleFire)
    }

    fun onAimToggle() {
        physicsWorld.enqueue(PhysicsCommand.SetAimState(null))
    }

    fun onAimHoldChanged(pressed: Boolean) {
        physicsWorld.enqueue(PhysicsCommand.SetAimState(pressed))
    }

    fun onReloadFirearm() {
        physicsWorld.enqueue(PhysicsCommand.ReloadFirearm)
    }

    fun onToggleHolster() {
        physicsWorld.enqueue(PhysicsCommand.ToggleHolster)
    }

    fun onInspectWeapon() {
        physicsWorld.enqueue(PhysicsCommand.InspectWeapon)
    }

    fun onPickupNearbyAmmo() {
        val box = _uiState.value.firearmHud.nearbyAmmoBox
        physicsWorld.enqueue(PhysicsCommand.PickupNearbyAmmo)
        if (box != null) {
            _uiState.update { it.copy(statusBanner = "Picked up +${box.amount} ${box.caliberLabel}") }
            clearBannerDelayed()
        }
    }

    fun undo() {
        physicsWorld.enqueue(PhysicsCommand.Undo)
    }

    fun redo() {
        physicsWorld.enqueue(PhysicsCommand.Redo)
    }

    fun resetScene() {
        soundEngine.playUiClick()
        physicsWorld.enqueue(PhysicsCommand.ResetScene)
        _uiState.update { it.copy(isPaused = false, statusBanner = "Scene reset to map baseline") }
        clearBannerDelayed()
    }

    fun toggleFreezeTargeted() {
        physicsWorld.enqueue(PhysicsCommand.ToggleFreezeTargeted)
    }

    fun duplicateTargeted() {
        physicsWorld.enqueue(PhysicsCommand.DuplicateTargeted)
    }

    fun deleteTargeted() {
        physicsWorld.enqueue(PhysicsCommand.DeleteTargeted)
        _uiState.update { it.copy(showInspectorSheet = false, latchedInspectedBody = null) }
    }

    fun detachTargeted() {
        physicsWorld.enqueue(PhysicsCommand.DetachTargeted)
    }

    fun groupTargeted() {
        physicsWorld.enqueue(PhysicsCommand.GroupTargetedWithLook)
    }

    fun ungroupTargeted() {
        physicsWorld.enqueue(PhysicsCommand.UngroupTargeted)
    }

    fun toggleElectricalSwitchTargeted() {
        physicsWorld.enqueue(PhysicsCommand.ToggleElectricalSwitchTargeted)
    }

    fun resetFuseTargeted() {
        physicsWorld.enqueue(PhysicsCommand.ResetFuseTargeted)
    }

    fun resizeTargeted(scale: Float) {
        physicsWorld.enqueue(PhysicsCommand.ResizeTargeted(scale))
    }

    fun setMaterialTargeted(mat: PhysicsMaterial) {
        physicsWorld.enqueue(PhysicsCommand.SetMaterialTargeted(mat))
    }

    fun setMaterialSpecTargeted(spec: MaterialSpec) {
        physicsWorld.enqueue(PhysicsCommand.SetMaterialSpecTargeted(spec))
    }

    fun updateBodyProperties(
        bodyId: Int,
        massOverride: Float? = null,
        clearMassOverride: Boolean = false,
        scale: Float? = null,
        frictionOverride: Float? = null,
        restitutionOverride: Float? = null,
        gravityScale: Float? = null,
        collisionMode: CollisionMode? = null,
        materialSpecId: String? = null,
        posOffset: Vec3? = null,
        rotEulerDeg: Vec3? = null,
        isFrozen: Boolean? = null
    ) {
        physicsWorld.enqueue(
            PhysicsCommand.UpdateBodyProperties(
                bodyId = bodyId,
                massOverride = massOverride,
                clearMassOverride = clearMassOverride,
                scale = scale,
                frictionOverride = frictionOverride,
                restitutionOverride = restitutionOverride,
                gravityScale = gravityScale,
                collisionMode = collisionMode,
                materialSpecId = materialSpecId,
                posOffset = posOffset,
                rotEulerDeg = rotEulerDeg,
                isFrozen = isFrozen
            )
        )
    }

    fun setEnvironment(
        timeOfDayHours: Float? = null,
        weatherType: WeatherType? = null,
        windStrength: Float? = null,
        fluidBasinEnabled: Boolean? = null,
        timeScale: Float? = null
    ) {
        physicsWorld.enqueue(
            PhysicsCommand.SetEnvironment(
                timeOfDayHours = timeOfDayHours,
                weatherType = weatherType,
                windStrength = windStrength,
                fluidBasinEnabled = fluidBasinEnabled,
                timeScale = timeScale
            )
        )
        _uiState.update { state ->
            state.copy(
                timeOfDayHours = timeOfDayHours ?: state.timeOfDayHours,
                weatherType = weatherType ?: state.weatherType,
                windStrength = windStrength ?: state.windStrength,
                fluidBasinEnabled = fluidBasinEnabled ?: state.fluidBasinEnabled,
                timeScale = timeScale ?: state.timeScale
            )
        }
    }

    fun toggleMagnetRepel() {
        val next = !_uiState.value.toolTelemetry.magnetRepelMode
        physicsWorld.enqueue(PhysicsCommand.SetMagnetMode(repel = next))
    }

    fun adjustMagnetRange(range: Float) {
        physicsWorld.enqueue(PhysicsCommand.SetMagnetMode(rangeMeters = range))
    }

    fun toggleFlashlight() {
        physicsWorld.enqueue(PhysicsCommand.ToggleFlashlight)
    }

    fun toggleStopwatch() {
        physicsWorld.enqueue(PhysicsCommand.ToggleStopwatch)
    }

    fun resetStopwatch() {
        physicsWorld.enqueue(PhysicsCommand.ResetStopwatch)
    }

    fun clearDynamicObjects() {
        soundEngine.playUiClick()
        physicsWorld.enqueue(PhysicsCommand.ClearDynamicObjects)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepo.updateSettings(transform)
        }
    }

    fun restoreDefaultSettings() {
        soundEngine.playUiClick()
        val preservedFavs = _uiState.value.settings.favoriteIds
        val preservedRecents = _uiState.value.settings.recentIds
        val preservedChar = _uiState.value.settings.characterCustomizationEncoded
        updateSettings {
            AppSettings.defaults().copy(
                favoriteIds = preservedFavs,
                recentIds = preservedRecents,
                characterCustomizationEncoded = preservedChar
            )
        }
    }

    fun updateCharacterAppearance(appearance: CharacterAppearance) {
        glRenderer.characterAppearance = appearance
        _uiState.update { it.copy(characterAppearance = appearance) }
        updateSettings { s -> s.copy(characterCustomizationEncoded = appearance.encodeCompact()) }
    }

    fun orbitCharacterPreview(deltaYawDeg: Float, deltaPitchDeg: Float) {
        val ps = glRenderer.characterPreviewState
        ps.orbitYawDeg = (ps.orbitYawDeg + deltaYawDeg) % 360f
        ps.orbitPitchDeg = (ps.orbitPitchDeg + deltaPitchDeg).coerceIn(-40f, 50f)
    }

    fun zoomCharacterPreview(deltaZoom: Float) {
        val ps = glRenderer.characterPreviewState
        ps.zoom = (ps.zoom + deltaZoom).coerceIn(0.65f, 2.2f)
    }

    fun setCharacterFocus(focus: CharacterCameraFocus) {
        soundEngine.playUiClick()
        glRenderer.characterPreviewState.focusPreset = focus
        _uiState.update { it.copy(characterFocusPreset = focus) }
    }

    // PC Keyboard Input Handler
    fun handleKeyEvent(keyCode: Int, isDown: Boolean): Boolean {
        val state = _uiState.value
        if (state.screen != AppScreen.PLAYING) return false
        val kb = state.settings.keyBindings

        if (isDown) {
            if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
                navigateBack()
                return true
            }
            if (keyCode == kb.inventory) {
                toggleInventory()
                return true
            }
            if (keyCode == kb.jump) {
                jump()
                return true
            }
            if (keyCode == kb.crouch) {
                toggleCrouch()
                return true
            }
            if (keyCode == kb.sprint) {
                if (state.settings.sprintToggleMode) toggleSprint() else toggleSprint(true)
                return true
            }
            if (keyCode == kb.interact) {
                if (state.firearmHud.nearbyAmmoBox != null) {
                    onPickupNearbyAmmo()
                } else {
                    primaryAction()
                }
                return true
            }
            if (keyCode == kb.reload) {
                if (state.isFirearmActive && !state.isGrabbing) {
                    onReloadFirearm()
                } else {
                    rotateHeld(0.55f, 0f)
                }
                return true
            }
            if (keyCode == kb.holster && state.isFirearmActive) {
                onToggleHolster()
                return true
            }
            if (keyCode == kb.freeze) {
                toggleFreezeTargeted()
                return true
            }
            if (keyCode == kb.cameraToggle) {
                toggleCameraMode()
                return true
            }
            when (keyCode) {
                KeyEvent.KEYCODE_1 -> selectTool(ToolType.GRAB)
                KeyEvent.KEYCODE_2 -> selectTool(ToolType.IMPULSE)
                KeyEvent.KEYCODE_3 -> selectTool(ToolType.CONSTRAINT)
                KeyEvent.KEYCODE_4 -> selectTool(ToolType.INSPECT)
                KeyEvent.KEYCODE_5 -> selectTool(ToolType.CATALOG_TOOL)
                KeyEvent.KEYCODE_Z -> undo()
                KeyEvent.KEYCODE_Y -> redo()
                KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_DEL -> deleteTargeted()
            }
            pressedKeys.add(keyCode)
        } else {
            if (keyCode == kb.sprint && !state.settings.sprintToggleMode) {
                toggleSprint(false)
            }
            pressedKeys.remove(keyCode)
        }

        var mx = 0f
        var my = 0f
        if (kb.forward in pressedKeys || KeyEvent.KEYCODE_DPAD_UP in pressedKeys) my += 1f
        if (kb.backward in pressedKeys || KeyEvent.KEYCODE_DPAD_DOWN in pressedKeys) my -= 1f
        if (kb.right in pressedKeys || KeyEvent.KEYCODE_DPAD_RIGHT in pressedKeys) mx += 1f
        if (kb.left in pressedKeys || KeyEvent.KEYCODE_DPAD_LEFT in pressedKeys) mx -= 1f
        physicsWorld.moveInputX = mx
        physicsWorld.moveInputY = my
        return keyCode == kb.forward || keyCode == kb.backward ||
            keyCode == kb.left || keyCode == kb.right ||
            keyCode == kb.jump || keyCode == kb.interact ||
            keyCode == kb.reload || keyCode == kb.holster ||
            keyCode == kb.inventory || keyCode == kb.freeze || keyCode == kb.cameraToggle
    }

    fun handleMouseButton(buttonState: Int) {
        val state = _uiState.value
        if (state.screen != AppScreen.PLAYING || state.isPaused) return
        if (state.showInventory || state.showInspectorSheet || state.showEnvironmentSheet || state.showPresetsDialog) return
        if ((buttonState and MotionEvent.BUTTON_PRIMARY) != 0) {
            if (state.isFirearmActive && !state.firearmHud.isHolstered) {
                onFireTap()
            } else {
                primaryAction()
            }
        } else if ((buttonState and MotionEvent.BUTTON_SECONDARY) != 0) {
            if (state.isFirearmActive && !state.firearmHud.isHolstered) {
                onAimToggle()
            } else {
                secondaryAction()
            }
        }
    }

    fun handleMouseButtonAction(buttonState: Int, isDown: Boolean) {
        val state = _uiState.value
        if (state.screen != AppScreen.PLAYING || state.isPaused) {
            onFireHoldChanged(false)
            return
        }
        val primaryPressed = (buttonState and MotionEvent.BUTTON_PRIMARY) != 0
        val secondaryPressed = (buttonState and MotionEvent.BUTTON_SECONDARY) != 0
        if (state.isFirearmActive && !state.firearmHud.isHolstered) {
            onFireHoldChanged(isDown && primaryPressed)
            if (isDown && primaryPressed) onFireTap()
            if (state.settings.holdToAim) {
                onAimHoldChanged(isDown && secondaryPressed)
            } else if (isDown && secondaryPressed) {
                onAimToggle()
            }
        } else {
            onFireHoldChanged(false)
            if (isDown) {
                if (primaryPressed) primaryAction()
                else if (secondaryPressed) secondaryAction()
            }
        }
    }

    fun handleMouseScroll(vScroll: Float) {
        if (_uiState.value.screen != AppScreen.PLAYING || _uiState.value.isPaused) return
        physicsWorld.enqueue(PhysicsCommand.AdjustGrabDistance(vScroll * 0.45f))
    }

    private fun startAutosaveLoop() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            while (isActive) {
                delay(45_000L)
                if (_uiState.value.screen == AppScreen.PLAYING && !_uiState.value.isPaused) {
                    autosaveNow()
                }
            }
        }
    }

    private fun autosaveNow() {
        val export = physicsWorld.exportSessionSync() ?: return
        viewModelScope.launch {
            saveRepo.saveSession("autosave", "Autosave (${export.mapId.code})", export)
            refreshSaveSlots()
        }
    }

    private fun clearBannerDelayed() {
        viewModelScope.launch {
            delay(2400L)
            _uiState.update { it.copy(statusBanner = null) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autosaveJob?.cancel()
        glRenderer.shutdown()
        animationWorker.shutdown()
        physicsWorld.shutdown()
        soundEngine.shutdown()
    }
}
