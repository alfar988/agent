package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.render.SandboxGLSurfaceView
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.CharacterSelectScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.MapSelectScreen
import com.example.ui.screens.SandboxHudScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TutorialScreen
import com.example.ui.theme.KineticLabTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SandboxViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SandboxViewModel by viewModels()
    private var glSurfaceView: SandboxGLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBarsSticky()

        setContent {
            KineticLabTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                Box(modifier = Modifier.fillMaxSize()) {
                    // Persistent OpenGL ES 3.0 3D Viewport (runs live physics demo behind menu & full sandbox in play)
                    AndroidView(
                        factory = { ctx ->
                            SandboxGLSurfaceView(ctx, viewModel.glRenderer).also { view ->
                                view.onKeyAction = { keyCode, isDown ->
                                    viewModel.handleKeyEvent(keyCode, isDown)
                                }
                                view.onMouseLookDelta = { dx, dy ->
                                    viewModel.addCameraLookDelta(dx, dy)
                                }
                                view.onMouseButtonAction = { buttonState, isDown ->
                                    viewModel.handleMouseButtonAction(buttonState, isDown)
                                }
                                view.onMouseScroll = { vScroll ->
                                    viewModel.handleMouseScroll(vScroll)
                                }
                                glSurfaceView = view
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    AppContentOverlay(
                        viewModel = viewModel,
                        state = state,
                        onExitApp = { finish() }
                    )
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isDown = event.action == KeyEvent.ACTION_DOWN
        val isUp = event.action == KeyEvent.ACTION_UP
        if ((isDown || isUp) && viewModel.handleKeyEvent(event.keyCode, isDown)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBarsSticky()
    }

    private fun hideSystemBarsSticky() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView?.onResume()
    }

    override fun onPause() {
        glSurfaceView?.onPause()
        super.onPause()
    }
}

@Composable
private fun AppContentOverlay(
    viewModel: SandboxViewModel,
    state: com.example.ui.viewmodel.SandboxUiState,
    onExitApp: () -> Unit
) {
    when (state.screen) {
        AppScreen.MAIN_MENU -> MainMenuScreen(
            autosaveSlot = state.saveSlots.firstOrNull { it.isAutosave },
            onPlayClick = { viewModel.navigateTo(AppScreen.MAP_SELECT) },
            onContinueAutosave = { viewModel.loadSavedSession("autosave") },
            onCharacterClick = { viewModel.navigateTo(AppScreen.CHARACTER_SELECT) },
            onSettingsClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
            onTutorialClick = { viewModel.navigateTo(AppScreen.TUTORIAL) },
            onAboutClick = { viewModel.navigateTo(AppScreen.ABOUT) },
            onExitClick = onExitApp
        )

        AppScreen.MAP_SELECT -> MapSelectScreen(
            saveSlots = state.saveSlots,
            onSelectMap = viewModel::startMap,
            onLoadSlot = viewModel::loadSavedSession,
            onDeleteSlot = viewModel::deleteSaveSlot,
            onBack = viewModel::navigateBack
        )

        AppScreen.CHARACTER_SELECT -> CharacterSelectScreen(
            appearance = state.characterAppearance,
            focusPreset = state.characterFocusPreset,
            onSelectAppearance = viewModel::updateCharacterAppearance,
            onPreviewOrbit = viewModel::orbitCharacterPreview,
            onPreviewZoom = viewModel::zoomCharacterPreview,
            onFocusChange = viewModel::setCharacterFocus,
            onConfirmAndBack = viewModel::navigateBack
        )

        AppScreen.PLAYING -> SandboxHudScreen(
            state = state,
            onBack = viewModel::navigateBack,
            onJoystickMove = viewModel::setJoystickVector,
            onCameraLookDelta = viewModel::addCameraLookDelta,
            onJump = viewModel::jump,
            onToggleCrouch = { viewModel.toggleCrouch() },
            onToggleSprint = { viewModel.toggleSprint() },
            onPrimaryAction = viewModel::primaryAction,
            onReleaseGrab = viewModel::releaseGrab,
            onSecondaryAction = viewModel::secondaryAction,
            onRotateHeld = { viewModel.rotateHeld(0.6f, 0f) },
            onFireHoldChanged = viewModel::onFireHoldChanged,
            onFireTap = viewModel::onFireTap,
            onAimToggle = viewModel::onAimToggle,
            onAimHoldChanged = viewModel::onAimHoldChanged,
            onReloadFirearm = viewModel::onReloadFirearm,
            onToggleHolster = viewModel::onToggleHolster,
            onInspectWeapon = viewModel::onInspectWeapon,
            onPickupNearbyAmmo = viewModel::onPickupNearbyAmmo,
            onSelectTool = viewModel::selectTool,
            onEquipCatalogTool = viewModel::equipCatalogTool,
            onSpawnCatalogTool = viewModel::spawnCatalogToolObject,
            onSelectAttachMode = viewModel::selectAttachMode,
            onDismissToolInfo = viewModel::dismissToolInfoPopup,
            onToggleToolPicker = { viewModel.toggleToolSelectionPanel(it) },
            onCloseToolPicker = viewModel::closeToolSelectionPanel,
            onToggleCamera = viewModel::toggleCameraMode,
            onToggleInventory = viewModel::toggleInventory,
            onUpdateInventoryFilter = viewModel::updateInventoryTabState,
            onToggleInspector = viewModel::toggleInspectorSheet,
            onToggleEnvironment = viewModel::toggleEnvironmentSheet,
            onTogglePresets = viewModel::togglePresetsDialog,
            onToggleScreenshotMode = viewModel::setScreenshotMode,
            onCaptureScreenshot = { viewModel.captureScreenshot(autoHideBriefly = false) },
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onResetScene = viewModel::resetScene,
            onToggleFreeze = viewModel::toggleFreezeTargeted,
            onGroupTargeted = viewModel::groupTargeted,
            onUngroupTargeted = viewModel::ungroupTargeted,
            onDuplicate = viewModel::duplicateTargeted,
            onDetach = viewModel::detachTargeted,
            onToggleElectrical = viewModel::toggleElectricalSwitchTargeted,
            onDelete = viewModel::deleteTargeted,
            onTogglePause = viewModel::togglePause,
            onToggleSaveLoad = viewModel::toggleSaveLoadDialog,
            onSetSpawnQuantity = viewModel::setSpawnQuantity,
            onToggleFavorite = viewModel::toggleFavorite,
            onSpawnObject = viewModel::spawnObject,
            onSpawnMaterialBlock = viewModel::spawnMaterialBlock,
            onUpdateBodyProperties = viewModel::updateBodyProperties,
            onChangeMaterialSpec = viewModel::setMaterialSpecTargeted,
            onUpdateEnvironment = { tod, w, wind, fluid, spd ->
                viewModel.setEnvironment(tod, w, wind, fluid, spd)
            },
            onToggleMagnetRepel = viewModel::toggleMagnetRepel,
            onToggleFlashlight = viewModel::toggleFlashlight,
            onToggleStopwatch = viewModel::toggleStopwatch,
            onLoadPreset = viewModel::loadPreset,
            onExportJson = viewModel::exportCurrentJsonPreview,
            onImportJson = viewModel::importJsonScene,
            onSaveSlot = viewModel::saveToSlot,
            onLoadSlot = viewModel::loadSavedSession,
            onClearObjects = viewModel::clearDynamicObjects,
            onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
            onExitToMenu = viewModel::exitToMainMenu
        )

        AppScreen.SETTINGS -> SettingsScreen(
            settings = state.settings,
            onUpdateSettings = viewModel::updateSettings,
            onRestoreDefaults = viewModel::restoreDefaultSettings,
            onBack = viewModel::navigateBack
        )

        AppScreen.TUTORIAL -> TutorialScreen(onBack = viewModel::navigateBack)

        AppScreen.ABOUT -> AboutScreen(onBack = viewModel::navigateBack)
    }
}
