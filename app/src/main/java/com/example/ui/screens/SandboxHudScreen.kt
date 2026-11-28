package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import com.example.data.BuiltInPreset
import com.example.electrical.ElectricalRole
import com.example.math.Vec3
import com.example.physics.CameraMode
import com.example.physics.CollisionMode
import com.example.physics.ConstraintType
import com.example.physics.InventoryCategory
import com.example.physics.MaterialSpec
import com.example.physics.ObjectType
import com.example.physics.ToolCatalog
import com.example.physics.ToolInteractionKind
import com.example.physics.ToolType
import com.example.physics.WeatherType
import com.example.ui.hud.ToolInfoPopupData
import com.example.ui.hud.ToolPickerMode
import com.example.ui.theme.LabColors
import com.example.ui.viewmodel.InventoryTabState
import com.example.ui.viewmodel.SandboxUiState
import com.example.weapons.FirearmHudState
import com.example.weapons.FirearmId
import com.example.weapons.NearbyAmmoBoxPrompt
import com.example.weapons.WeaponActionState
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun SandboxHudScreen(
    state: SandboxUiState,
    onBack: () -> Unit,
    onJoystickMove: (Float, Float) -> Unit,
    onCameraLookDelta: (Float, Float) -> Unit,
    onJump: () -> Unit,
    onToggleCrouch: () -> Unit,
    onToggleSprint: () -> Unit,
    onPrimaryAction: () -> Unit,
    onReleaseGrab: () -> Unit,
    onSecondaryAction: () -> Unit,
    onRotateHeld: () -> Unit,
    onFireHoldChanged: (Boolean) -> Unit = {},
    onFireTap: () -> Unit = {},
    onAimToggle: () -> Unit = {},
    onAimHoldChanged: (Boolean) -> Unit = {},
    onReloadFirearm: () -> Unit = {},
    onToggleHolster: () -> Unit = {},
    onInspectWeapon: () -> Unit = {},
    onPickupNearbyAmmo: () -> Unit = {},
    onSelectTool: (ToolType) -> Unit,
    onEquipCatalogTool: (String) -> Unit,
    onSpawnCatalogTool: (String) -> Unit,
    onSelectAttachMode: (ConstraintType) -> Unit,
    onDismissToolInfo: () -> Unit = {},
    onToggleToolPicker: (ToolPickerMode?) -> Unit = {},
    onCloseToolPicker: () -> Unit = {},
    onToggleCamera: () -> Unit,
    onToggleInventory: (Boolean?) -> Unit,
    onUpdateInventoryFilter: ((InventoryTabState) -> InventoryTabState) -> Unit,
    onToggleInspector: (Boolean?) -> Unit,
    onToggleEnvironment: (Boolean?) -> Unit,
    onTogglePresets: (Boolean?) -> Unit,
    onToggleScreenshotMode: (Boolean) -> Unit,
    onCaptureScreenshot: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResetScene: () -> Unit,
    onToggleFreeze: () -> Unit,
    onGroupTargeted: () -> Unit,
    onUngroupTargeted: () -> Unit,
    onDuplicate: () -> Unit,
    onDetach: () -> Unit,
    onToggleElectrical: () -> Unit,
    onDelete: () -> Unit,
    onTogglePause: (Boolean) -> Unit,
    onToggleSaveLoad: (Boolean) -> Unit,
    onSetSpawnQuantity: (Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSpawnObject: (ObjectType) -> Unit,
    onSpawnMaterialBlock: (MaterialSpec) -> Unit,
    onUpdateBodyProperties: (
        bodyId: Int,
        massOverride: Float?,
        clearMassOverride: Boolean,
        scale: Float?,
        frictionOverride: Float?,
        restitutionOverride: Float?,
        gravityScale: Float?,
        collisionMode: CollisionMode?,
        materialSpecId: String?,
        posOffset: Vec3?,
        rotEulerDeg: Vec3?,
        isFrozen: Boolean?
    ) -> Unit,
    onChangeMaterialSpec: (MaterialSpec) -> Unit,
    onUpdateEnvironment: (Float?, WeatherType?, Float?, Boolean?, Float?) -> Unit,
    onToggleMagnetRepel: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onToggleStopwatch: () -> Unit,
    onLoadPreset: (BuiltInPreset) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: (String) -> Unit,
    onSaveSlot: (String, String) -> Unit,
    onLoadSlot: (String) -> Unit,
    onClearObjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onExitToMenu: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Multi-touch pointer-isolated open-viewport swipe-to-look layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera_look_surface")
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        var activeId: PointerId? = null
                        var prevPos = Offset.Zero
                        while (true) {
                            val event = awaitPointerEvent()
                            if (activeId == null) {
                                val down = event.changes.firstOrNull { it.pressed && !it.isConsumed && it.changedToDown() }
                                if (down != null) {
                                    activeId = down.id
                                    prevPos = down.position
                                    down.consume()
                                }
                            } else {
                                val current = event.changes.firstOrNull { it.id == activeId }
                                if (current == null || !current.pressed) {
                                    activeId = null
                                } else if (!current.isConsumed) {
                                    val delta = current.position - prevPos
                                    prevPos = current.position
                                    if (delta.x != 0f || delta.y != 0f) {
                                        onCameraLookDelta(delta.x, delta.y)
                                    }
                                    current.consume()
                                }
                            }
                        }
                    }
                }
        )

        if (state.screenshotMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = onCaptureScreenshot,
                    containerColor = LabColors.AmberAccent,
                    contentColor = LabColors.VoidBg,
                    modifier = Modifier.testTag("screenshot_capture_button")
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Capture Frame")
                }
                SmallFloatingActionButton(
                    onClick = { onToggleScreenshotMode(false) },
                    containerColor = LabColors.PanelSurface.copy(alpha = 0.88f),
                    contentColor = LabColors.TextPrimary,
                    modifier = Modifier.testTag("screenshot_exit_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Exit Screenshot Mode")
                }
            }
            return@Box
        }

        // 2. Dynamic Crosshair & Sniper Optic Overlay
        val isSniperScoped = state.isFirearmActive &&
            !state.firearmHud.isHolstered &&
            state.firearmHud.equippedFirearmId == FirearmId.BOLT_SNIPER &&
            state.firearmHud.isAiming
        if (isSniperScoped) {
            SniperScopeOverlay(
                recoilOffset = state.firearmHud.recoilVisualOffset,
                modifier = Modifier.fillMaxSize().testTag("hud_crosshair")
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(34.dp).testTag("hud_crosshair")) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val color = when {
                        state.isGrabbing -> LabColors.CyanAccent
                        state.isFirearmActive && !state.firearmHud.isHolstered && state.firearmHud.isAiming -> LabColors.AmberAccent
                        state.targetedBody != null -> LabColors.AmberAccent
                        else -> Color.White.copy(alpha = 0.78f)
                    }
                    val baseGap = when {
                        state.isFirearmActive && !state.firearmHud.isHolstered -> {
                            val gunGap = if (state.firearmHud.equippedFirearmId == FirearmId.PUMP_SHOTGUN) 12.0f else 6.5f
                            val aimScale = if (state.firearmHud.isAiming) 0.45f else 1.0f
                            gunGap * aimScale + state.firearmHud.recoilVisualOffset * 10.0f
                        }
                        else -> 4.5f
                    }
                    val arm = baseGap + 5.5f
                    drawCircle(color = color, radius = if (state.firearmHud.isAiming) 1.7f else 2.0f, center = c)
                    drawLine(color.copy(alpha = 0.72f), Offset(c.x - arm, c.y), Offset(c.x - baseGap, c.y), strokeWidth = 1.6f)
                    drawLine(color.copy(alpha = 0.72f), Offset(c.x + baseGap, c.y), Offset(c.x + arm, c.y), strokeWidth = 1.6f)
                    drawLine(color.copy(alpha = 0.72f), Offset(c.x, c.y - arm), Offset(c.x, c.y - baseGap), strokeWidth = 1.6f)
                    drawLine(color.copy(alpha = 0.72f), Offset(c.x, c.y + baseGap), Offset(c.x, c.y + arm), strokeWidth = 1.6f)
                }
            }
        }

        // 3. Safe-area Adaptive Minimalist HUD
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
        ) {
            val isCompactWidth = maxWidth < 600.dp

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    FilledIconButton(
                        onClick = { onTogglePause(true) },
                        modifier = Modifier.size(48.dp).testTag("hud_pause_button"),
                        shape = RoundedCornerShape(6.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = LabColors.PanelSurface.copy(alpha = 0.86f),
                            contentColor = LabColors.TextPrimary
                        )
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = "Pause")
                    }

                    Surface(
                        color = LabColors.PanelSurface.copy(alpha = 0.82f),
                        border = BorderStroke(1.dp, LabColors.BorderSubtle),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(
                                state.activeMap.displayName + if (state.timeScale != 1.0f) " • ${String.format("%.1f", state.timeScale)}x" else "",
                                style = MaterialTheme.typography.titleSmall,
                                color = LabColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (isCompactWidth) {
                                    "${state.fps} FPS • ${state.objectCount} objects"
                                } else {
                                    "${state.fps} FPS • ${String.format("%.1f", state.physicsMs)} ms • ${state.objectCount} objects"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = LabColors.TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.width(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isCompactWidth) {
                        HudIconButton(
                            icon = Icons.AutoMirrored.Filled.Undo,
                            description = "Undo",
                            tint = if (state.canUndo) LabColors.TextPrimary else LabColors.TextMuted,
                            onClick = onUndo,
                            tag = "hud_undo_button"
                        )
                        HudIconButton(
                            icon = Icons.AutoMirrored.Filled.Redo,
                            description = "Redo",
                            tint = if (state.canRedo) LabColors.TextPrimary else LabColors.TextMuted,
                            onClick = onRedo,
                            tag = "hud_redo_button"
                        )
                    }
                    HudIconButton(
                        icon = Icons.Filled.WbSunny,
                        description = "Environment & Speed",
                        tint = LabColors.AmberAccent,
                        onClick = { onToggleEnvironment(null) },
                        tag = "hud_environment_button"
                    )
                    HudIconButton(
                        icon = Icons.Filled.PhotoCamera,
                        description = "Screenshot Mode",
                        tint = LabColors.TextPrimary,
                        onClick = { onToggleScreenshotMode(true) },
                        tag = "hud_screenshot_button"
                    )

                    FilledIconButton(
                        onClick = onToggleCamera,
                        modifier = Modifier.size(48.dp).testTag("hud_camera_toggle"),
                        shape = RoundedCornerShape(6.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = LabColors.PanelSurface.copy(alpha = 0.86f),
                            contentColor = when (state.cameraMode) {
                                CameraMode.FIRST_PERSON -> LabColors.TextPrimary
                                CameraMode.THIRD_PERSON -> LabColors.CyanAccent
                                CameraMode.FREE_CAMERA -> LabColors.AmberAccent
                            }
                        )
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Toggle Camera Mode")
                    }

                    FilledIconButton(
                        onClick = { onToggleInventory(null) },
                        modifier = Modifier.size(48.dp).testTag("hud_inventory_button"),
                        shape = RoundedCornerShape(6.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = LabColors.AmberAccent,
                            contentColor = LabColors.VoidBg
                        )
                    ) {
                        Icon(Icons.Filled.AddBox, contentDescription = "Open Inventory")
                    }
                }
            }

            // Context-sensitive Target Bar
            AnimatedVisibility(
                visible = state.targetedBody != null || state.isGrabbing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 58.dp)
            ) {
                val info = state.targetedBody
                if (info != null) {
                    Surface(
                        color = LabColors.PanelSurface.copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, LabColors.BorderStrong),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(modifier = Modifier.padding(end = 6.dp)) {
                                Text(info.name, style = MaterialTheme.typography.titleSmall, color = LabColors.TextPrimary, maxLines = 1)
                                Text(
                                    "${info.materialSpec.displayName} • ${String.format("%.1f", info.massKg)} kg • ${String.format("%.1f", info.speedMs)} m/s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LabColors.TextSecondary,
                                    maxLines = 1
                                )
                            }
                            if (info.electricalRole == ElectricalRole.SWITCH || info.electricalRole == ElectricalRole.FUSE || info.electricalRole == ElectricalRole.ELECTRICAL_PANEL) {
                                HudIconButton(Icons.Filled.ElectricBolt, "Toggle Switch or Reset Fuse", LabColors.AmberAccent, onToggleElectrical)
                            }
                            HudIconButton(Icons.Filled.AcUnit, "Freeze", if (info.isFrozen) LabColors.CyanAccent else LabColors.TextPrimary, onToggleFreeze)
                            HudIconButton(Icons.Filled.ContentCopy, "Duplicate", LabColors.TextPrimary, onDuplicate)
                            if (info.constraintCount > 0) {
                                HudIconButton(Icons.Filled.LinkOff, "Detach", LabColors.AmberAccent, onDetach)
                            }
                            HudIconButton(Icons.Filled.Tune, "Properties", LabColors.AmberAccent, { onToggleInspector(true) }, tag = "hud_properties_button")
                            HudIconButton(Icons.Filled.Delete, "Delete", LabColors.CrimsonWarn, onDelete)
                        }
                    }
                }
            }

            // Status toast banner
            state.statusBanner?.let { banner ->
                Surface(
                    color = LabColors.PanelSurface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, LabColors.BorderStrong),
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 62.dp)
                ) {
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LabColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Bottom-Left Customizable Virtual Movement Joystick
            VirtualJoystick(
                onMove = onJoystickMove,
                outerDiameterDp = if (isCompactWidth) 94.dp else 112.dp,
                deadzone = state.settings.joystickDeadzone,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(
                        x = state.settings.joystickOffsetX.dp,
                        y = (-state.settings.joystickOffsetY).dp
                    )
                    .testTag("virtual_joystick")
            )

            // Bottom-Center Tool Dock + Compact Contextual Panels
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = if (isCompactWidth) 310.dp else 560.dp)
                    .padding(bottom = if (isCompactWidth) 114.dp else 0.dp)
            ) {
                // Separate Component A: Explicit Interactive Tool Selection Panel (opens only on user trigger; stays open until close/select)
                AnimatedVisibility(
                    visible = state.hudPanels.isPickerRendered,
                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.95f, animationSpec = tween(150)),
                    exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150))
                ) {
                    CompactToolSelectionPanel(
                        mode = state.hudPanels.pickerMode,
                        activeCatalogToolId = state.activeCatalogToolId,
                        attachConstraintType = state.attachConstraintType,
                        quickToolIds = state.quickToolIds,
                        isCompactWidth = isCompactWidth,
                        onEquipCatalogTool = onEquipCatalogTool,
                        onSelectAttachMode = onSelectAttachMode,
                        onBrowseAllTools = {
                            onCloseToolPicker()
                            onUpdateInventoryFilter {
                                it.copy(showFavoritesOnly = false, selectedInvCat = InventoryCategory.TOOLS)
                            }
                            onToggleInventory(true)
                        },
                        onClose = onCloseToolPicker
                    )
                }

                // Nearby Ammo Crate Pickup Pill (clear of center crosshair)
                state.firearmHud.nearbyAmmoBox?.let { ammoBox ->
                    AmmoPickupPill(
                        prompt = ammoBox,
                        onPickup = onPickupNearbyAmmo
                    )
                }

                // Separate Component B: Temporary Contextual Tool Info Popup (auto-dismisses cleanly; manually closeable)
                AnimatedVisibility(
                    visible = !state.hudPanels.isPickerRendered && state.hudPanels.isInfoRendered && state.hudPanels.infoData != null,
                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.94f, animationSpec = tween(150)),
                    exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.94f, animationSpec = tween(150))
                ) {
                    state.hudPanels.infoData?.let { info ->
                        CompactToolInfoPopup(
                            data = info,
                            isCompactWidth = isCompactWidth,
                            onDismiss = onDismissToolInfo
                        )
                    }
                }

                // Preserved Bottom Action Toolbar + inline compact picker/mode pills
                Surface(
                    color = LabColors.PanelSurface.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, LabColors.BorderSubtle),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToolType.entries.forEachIndexed { idx, tool ->
                            val selected = state.selectedTool == tool
                            val icon = when (tool) {
                                ToolType.GRAB -> Icons.Filled.FrontHand
                                ToolType.IMPULSE -> Icons.Filled.Bolt
                                ToolType.CONSTRAINT -> Icons.Filled.Link
                                ToolType.INSPECT -> Icons.Filled.Tune
                                ToolType.CATALOG_TOOL -> Icons.Filled.Build
                            }
                            Surface(
                                color = if (selected) LabColors.AmberAccent else Color.Transparent,
                                shape = RoundedCornerShape(4.dp),
                                onClick = { onSelectTool(tool) },
                                modifier = Modifier.testTag("tool_${tool.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = if (isCompactWidth) 8.dp else 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = tool.displayName,
                                        tint = if (selected) LabColors.VoidBg else LabColors.TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    val label = if (tool == ToolType.CATALOG_TOOL) {
                                        ToolCatalog.get(state.activeCatalogToolId)?.displayName?.take(8) ?: tool.shortName
                                    } else tool.shortName
                                    Text(
                                        text = if (!isCompactWidth) "${idx + 1} $label" else label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) LabColors.VoidBg else LabColors.TextPrimary
                                    )
                                }
                            }
                        }

                        if (state.selectedTool == ToolType.CATALOG_TOOL || state.selectedTool == ToolType.CONSTRAINT) {
                            val pickerOpen = state.hudPanels.isPickerRendered
                            Surface(
                                color = if (pickerOpen) LabColors.CyanAccent else LabColors.CardSurface,
                                shape = RoundedCornerShape(4.dp),
                                onClick = {
                                    onToggleToolPicker(
                                        if (state.selectedTool == ToolType.CONSTRAINT) ToolPickerMode.CONSTRAINT_MODES
                                        else ToolPickerMode.CATALOG_TOOLS
                                    )
                                },
                                modifier = Modifier.testTag("hud_tool_picker_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (pickerOpen) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                        contentDescription = "Select Tool Options",
                                        tint = if (pickerOpen) LabColors.VoidBg else LabColors.CyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (state.selectedTool == ToolType.CATALOG_TOOL) {
                            val activeDef = ToolCatalog.get(state.activeCatalogToolId)
                            when (activeDef?.interactionKind) {
                                ToolInteractionKind.MAGNET_FIELD -> {
                                    Surface(
                                        color = LabColors.CardSurface,
                                        shape = RoundedCornerShape(4.dp),
                                        onClick = onToggleMagnetRepel,
                                        modifier = Modifier.testTag("tool_quick_mode_pill")
                                    ) {
                                        Text(
                                            if (state.toolTelemetry.magnetRepelMode) "Repel" else "Attract",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LabColors.CyanAccent,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                                ToolInteractionKind.LIGHT_BEAM -> {
                                    Surface(
                                        color = LabColors.CardSurface,
                                        shape = RoundedCornerShape(4.dp),
                                        onClick = onToggleFlashlight,
                                        modifier = Modifier.testTag("tool_quick_mode_pill")
                                    ) {
                                        Text(
                                            if (state.toolTelemetry.flashlightOn) "On" else "Off",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LabColors.CyanAccent,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                                ToolInteractionKind.STOPWATCH_TIMER -> {
                                    Surface(
                                        color = LabColors.CardSurface,
                                        shape = RoundedCornerShape(4.dp),
                                        onClick = onToggleStopwatch,
                                        modifier = Modifier.testTag("tool_quick_mode_pill")
                                    ) {
                                        Text(
                                            String.format("%.1fs", state.toolTelemetry.stopwatchSeconds),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LabColors.CyanAccent,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }

            // Bottom-Right Action Buttons Cluster (+ Translucent Firearm Controls & Readout)
            val btnSize = (50f * state.settings.buttonsScale).dp
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(
                        x = (-state.settings.buttonsOffsetX).dp,
                        y = (-state.settings.buttonsOffsetY).dp
                    )
            ) {
                if (state.isFirearmActive) {
                    CompactFirearmReadoutCard(
                        hud = state.firearmHud,
                        onInspectWeapon = onInspectWeapon
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledIconButton(
                            onClick = onToggleHolster,
                            modifier = Modifier.size(btnSize).testTag("hud_holster_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (state.firearmHud.isHolstered) {
                                    LabColors.AmberAccent.copy(alpha = 0.82f)
                                } else {
                                    LabColors.PanelSurface.copy(alpha = 0.72f)
                                },
                                contentColor = if (state.firearmHud.isHolstered) LabColors.VoidBg else LabColors.TextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = if (state.firearmHud.isHolstered) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (state.firearmHud.isHolstered) "Draw Firearm" else "Holster Firearm"
                            )
                        }

                        FilledIconButton(
                            onClick = onReloadFirearm,
                            modifier = Modifier.size(btnSize).testTag("hud_reload_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (state.firearmHud.actionState == WeaponActionState.RELOADING) {
                                    LabColors.CyanAccent.copy(alpha = 0.84f)
                                } else {
                                    LabColors.PanelSurface.copy(alpha = 0.72f)
                                },
                                contentColor = if (state.firearmHud.actionState == WeaponActionState.RELOADING) LabColors.VoidBg else LabColors.TextPrimary
                            )
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reload Firearm")
                        }

                        if (!state.firearmHud.isHolstered) {
                            TranslucentAimButton(
                                isAiming = state.firearmHud.isAiming,
                                holdToAim = state.settings.holdToAim,
                                btnSize = btnSize,
                                onAimToggle = onAimToggle,
                                onAimHoldChanged = onAimHoldChanged
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledIconButton(
                        onClick = onToggleCrouch,
                        modifier = Modifier.size(btnSize).testTag("hud_crouch_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (state.isCrouching) LabColors.CyanAccent else LabColors.PanelSurface.copy(alpha = 0.76f),
                            contentColor = if (state.isCrouching) LabColors.VoidBg else LabColors.TextPrimary
                        )
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = if (state.isCrouching) "Stand Up" else "Crouch")
                    }

                    FilledIconButton(
                        onClick = onToggleSprint,
                        modifier = Modifier.size(btnSize).testTag("hud_sprint_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (state.isSprinting) LabColors.AmberAccent else LabColors.PanelSurface.copy(alpha = 0.76f),
                            contentColor = if (state.isSprinting) LabColors.VoidBg else LabColors.TextPrimary
                        )
                    ) {
                        Icon(Icons.Filled.Speed, contentDescription = if (state.isSprinting) "Walk" else "Sprint")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.isGrabbing) {
                        FilledIconButton(
                            onClick = onRotateHeld,
                            modifier = Modifier.size(btnSize).testTag("hud_rotate_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = LabColors.CardSurface.copy(alpha = 0.90f),
                                contentColor = LabColors.TextPrimary
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate Object")
                        }
                        FilledIconButton(
                            onClick = onSecondaryAction,
                            modifier = Modifier.size(btnSize).testTag("hud_throw_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = LabColors.AmberAccent,
                                contentColor = LabColors.VoidBg
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Throw Object")
                        }
                    } else if (state.targetedBody != null && !state.isFirearmActive) {
                        FilledIconButton(
                            onClick = onToggleFreeze,
                            modifier = Modifier.size(btnSize).testTag("hud_freeze_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = LabColors.CardSurface.copy(alpha = 0.90f),
                                contentColor = LabColors.CyanAccent
                            )
                        ) {
                            Icon(Icons.Filled.AcUnit, contentDescription = "Freeze Object")
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (state.isFirearmActive && !state.firearmHud.isHolstered) {
                        TranslucentFireButton(
                            isFiring = state.firearmHud.actionState == WeaponActionState.FIRING,
                            isEmpty = state.firearmHud.magAmmo <= 0,
                            btnSize = btnSize,
                            onFireHoldChanged = onFireHoldChanged,
                            onFireTap = onFireTap
                        )
                    } else {
                        FilledIconButton(
                            onClick = {
                                if (state.isGrabbing) onReleaseGrab() else onPrimaryAction()
                            },
                            modifier = Modifier.size(btnSize + 6.dp).testTag("hud_primary_action_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (state.isGrabbing) LabColors.CyanAccent else LabColors.AmberAccent,
                                contentColor = LabColors.VoidBg
                            )
                        ) {
                            val actionIcon = when {
                                state.isGrabbing -> Icons.Filled.FrontHand
                                state.selectedTool == ToolType.IMPULSE -> Icons.Filled.Bolt
                                state.selectedTool == ToolType.CONSTRAINT -> Icons.Filled.Link
                                state.selectedTool == ToolType.INSPECT -> Icons.Filled.Tune
                                state.selectedTool == ToolType.CATALOG_TOOL -> Icons.Filled.Build
                                else -> Icons.Filled.FrontHand
                            }
                            Icon(actionIcon, contentDescription = if (state.isGrabbing) "Release Object" else "Use Tool or Grab")
                        }
                    }

                    FilledIconButton(
                        onClick = onJump,
                        modifier = Modifier.size(btnSize + 6.dp).testTag("hud_jump_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = LabColors.PanelSurface.copy(alpha = 0.78f),
                            contentColor = LabColors.TextPrimary
                        )
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Jump")
                    }
                }
            }
        }

        // 4. Modals & Overlays
        if (state.showInventory) {
            SpawnCatalogOverlay(
                tabState = state.inventoryTabState,
                onUpdateTabState = onUpdateInventoryFilter,
                favoriteIds = state.settings.favoriteIds,
                recentIds = state.settings.recentIds,
                spawnQuantity = state.spawnQuantity,
                onSetQuantity = onSetSpawnQuantity,
                onToggleFavorite = onToggleFavorite,
                onSpawnObject = onSpawnObject,
                onSpawnMaterialBlock = onSpawnMaterialBlock,
                onEquipTool = onEquipCatalogTool,
                onSpawnToolObject = onSpawnCatalogTool,
                onClose = { onToggleInventory(false) }
            )
        }

        val inspectedInfo = state.latchedInspectedBody ?: state.targetedBody
        if (state.showInspectorSheet && inspectedInfo != null) {
            InspectorPropertiesOverlay(
                info = inspectedInfo,
                onUpdateProperties = onUpdateBodyProperties,
                onChangeMaterialSpec = onChangeMaterialSpec,
                onToggleFreeze = onToggleFreeze,
                onGroupWithNext = onGroupTargeted,
                onUngroup = onUngroupTargeted,
                onDuplicate = onDuplicate,
                onDetach = onDetach,
                onToggleElectrical = onToggleElectrical,
                onDelete = onDelete,
                onClose = { onToggleInspector(false) }
            )
        }

        if (state.showEnvironmentSheet) {
            EnvironmentAndWeatherDialog(
                timeOfDayHours = state.timeOfDayHours,
                weatherType = state.weatherType,
                windStrength = state.windStrength,
                fluidBasinEnabled = state.fluidBasinEnabled,
                timeScale = state.timeScale,
                onUpdateEnvironment = onUpdateEnvironment,
                onClose = { onToggleEnvironment(false) }
            )
        }

        if (state.showPresetsDialog) {
            PresetsImportExportDialog(
                exportedJsonPreview = state.exportedJsonPreview,
                onLoadPreset = onLoadPreset,
                onExportJson = onExportJson,
                onImportJson = onImportJson,
                onClose = { onTogglePresets(false) }
            )
        }

        if (state.isPaused) {
            PauseAndSaveOverlay(
                saveSlots = state.saveSlots,
                showSaveDialog = state.showSaveLoadDialog,
                onResume = { onTogglePause(false) },
                onOpenSaveDialog = onToggleSaveLoad,
                onOpenPresets = {
                    onTogglePause(false)
                    onTogglePresets(true)
                },
                onResetScene = onResetScene,
                onSaveSlot = onSaveSlot,
                onLoadSlot = onLoadSlot,
                onClearObjects = onClearObjects,
                onOpenSettings = onOpenSettings,
                onExitToMenu = onExitToMenu
            )
        }
    }
}

@Composable
private fun HudIconButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit,
    tag: String? = null
) {
    FilledIconButton(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = LabColors.PanelSurface.copy(alpha = 0.86f),
            contentColor = tint
        ),
        modifier = if (tag != null) Modifier.size(48.dp).testTag(tag) else Modifier.size(48.dp)
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(19.dp))
    }
}

@Composable
fun VirtualJoystick(
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    outerDiameterDp: androidx.compose.ui.unit.Dp = 112.dp,
    deadzone: Float = 0.12f
) {
    val maxRadiusPx = with(LocalDensity.current) { (outerDiameterDp * 0.36f).toPx() }
    val dz = deadzone.coerceIn(0.02f, 0.40f)
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(outerDiameterDp)
            .pointerInput(maxRadiusPx, dz) {
                awaitPointerEventScope {
                    var activeId: PointerId? = null
                    val center = Offset(size.width / 2f, size.height / 2f)

                    fun updateFromPos(pos: Offset) {
                        val delta = pos - center
                        val dist = hypot(delta.x, delta.y)
                        val clamped = if (dist > maxRadiusPx && dist > 1e-4f) {
                            Offset(delta.x / dist * maxRadiusPx, delta.y / dist * maxRadiusPx)
                        } else delta
                        knobOffset = clamped
                        val rawNorm = (dist / maxRadiusPx).coerceIn(0f, 1f)
                        if (rawNorm <= dz || dist < 1e-4f) {
                            onMove(0f, 0f)
                        } else {
                            val u = ((rawNorm - dz) / (1f - dz)).coerceIn(0f, 1f)
                            val curved = 0.35f * u + 0.65f * u * u
                            val dirX = delta.x / dist
                            val dirY = delta.y / dist
                            onMove(dirX * curved, -dirY * curved)
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        if (activeId == null) {
                            val down = event.changes.firstOrNull { it.pressed && it.changedToDown() }
                            if (down != null) {
                                activeId = down.id
                                down.consume()
                                updateFromPos(down.position)
                            }
                        } else {
                            val change = event.changes.firstOrNull { it.id == activeId }
                            if (change == null || !change.pressed) {
                                activeId = null
                                knobOffset = Offset.Zero
                                onMove(0f, 0f)
                            } else {
                                change.consume()
                                updateFromPos(change.position)
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = LabColors.PanelSurface.copy(alpha = 0.55f),
                radius = size.minDimension * 0.48f,
                center = c
            )
            drawCircle(
                color = LabColors.BorderSubtle.copy(alpha = 0.45f),
                radius = size.minDimension * 0.36f * dz,
                center = c,
                style = Stroke(width = 1.0f)
            )
            drawCircle(
                color = LabColors.BorderStrong.copy(alpha = 0.78f),
                radius = size.minDimension * 0.48f,
                center = c,
                style = Stroke(width = 2.0f)
            )
        }
        Surface(
            color = LabColors.AmberAccent.copy(alpha = 0.88f),
            shape = CircleShape,
            shadowElevation = 3.dp,
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size(42.dp)
        ) {}
    }
}

@Composable
private fun CompactToolInfoPopup(
    data: ToolInfoPopupData,
    isCompactWidth: Boolean,
    onDismiss: () -> Unit
) {
    val icon = when (data.toolType) {
        ToolType.GRAB -> Icons.Filled.FrontHand
        ToolType.IMPULSE -> Icons.Filled.Bolt
        ToolType.CONSTRAINT -> Icons.Filled.Link
        ToolType.INSPECT -> Icons.Filled.Tune
        ToolType.CATALOG_TOOL -> iconForToolKind(data.interactionKind)
    }

    Surface(
        color = LabColors.PanelSurface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .padding(bottom = 5.dp)
            .widthIn(min = 160.dp, max = if (isCompactWidth) 256.dp else 310.dp)
            .testTag("tool_info_popup")
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LabColors.AmberAccent,
                modifier = Modifier.size(15.dp)
            )
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = LabColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (data.description.isNotBlank()) {
                    Text(
                        text = data.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = LabColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onDismiss)
                    .testTag("tool_info_close_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss Tool Info",
                    tint = LabColors.TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactToolSelectionPanel(
    mode: ToolPickerMode,
    activeCatalogToolId: String,
    attachConstraintType: ConstraintType,
    quickToolIds: List<String>,
    isCompactWidth: Boolean,
    onEquipCatalogTool: (String) -> Unit,
    onSelectAttachMode: (ConstraintType) -> Unit,
    onBrowseAllTools: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = LabColors.PanelSurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .padding(bottom = 5.dp)
            .widthIn(min = 210.dp, max = if (isCompactWidth) 304.dp else 430.dp)
            .testTag("tool_selection_panel")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mode == ToolPickerMode.CONSTRAINT_MODES) "Attach Mode" else "Select Tool",
                    style = MaterialTheme.typography.labelMedium,
                    color = LabColors.TextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mode == ToolPickerMode.CATALOG_TOOLS) {
                        Surface(
                            color = LabColors.CardSurface,
                            shape = RoundedCornerShape(4.dp),
                            onClick = onBrowseAllTools,
                            modifier = Modifier.testTag("tool_picker_browse_all")
                        ) {
                            Text(
                                text = "All (${ToolCatalog.all.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = LabColors.CyanAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .clickable(onClick = onClose)
                            .testTag("tool_picker_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Tool Selection",
                            tint = LabColors.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mode == ToolPickerMode.CONSTRAINT_MODES) {
                    ConstraintType.entries.forEach { cType ->
                        val selected = attachConstraintType == cType
                        Surface(
                            color = if (selected) LabColors.CyanAccent else LabColors.CardSurface,
                            shape = RoundedCornerShape(4.dp),
                            onClick = { onSelectAttachMode(cType) },
                            modifier = Modifier.testTag("picker_attach_${cType.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Link,
                                    contentDescription = null,
                                    tint = if (selected) LabColors.VoidBg else LabColors.TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = cType.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) LabColors.VoidBg else LabColors.TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else {
                    quickToolIds.forEach { tid ->
                        val td = ToolCatalog.get(tid) ?: return@forEach
                        val selected = activeCatalogToolId == tid
                        Surface(
                            color = if (selected) LabColors.AmberAccent else LabColors.CardSurface,
                            shape = RoundedCornerShape(4.dp),
                            onClick = { onEquipCatalogTool(tid) },
                            modifier = Modifier.testTag("picker_tool_${tid.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = iconForToolKind(td.interactionKind),
                                    contentDescription = null,
                                    tint = if (selected) LabColors.VoidBg else LabColors.AmberAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = td.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) LabColors.VoidBg else LabColors.TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            val shortHint = if (mode == ToolPickerMode.CONSTRAINT_MODES) {
                when (attachConstraintType) {
                    ConstraintType.WELD -> "Rigidly lock two objects together."
                    ConstraintType.SPRING -> "Connect objects with elastic suspension."
                    ConstraintType.ROPE -> "Tether objects with flexible tension line."
                    ConstraintType.HINGE -> "Join objects along a rotational pivot."
                }
            } else {
                ToolCatalog.get(activeCatalogToolId)?.description?.take(62) ?: ""
            }
            if (shortHint.isNotBlank()) {
                Text(
                    text = shortHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = LabColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun iconForToolKind(kind: ToolInteractionKind?): ImageVector = when (kind) {
    ToolInteractionKind.MULTIMETER_PROBE, ToolInteractionKind.GENERATOR_POWER -> Icons.Filled.ElectricBolt
    ToolInteractionKind.LIGHT_BEAM -> Icons.Filled.FlashlightOn
    ToolInteractionKind.STOPWATCH_TIMER -> Icons.Filled.Timer
    ToolInteractionKind.MAGNET_FIELD -> Icons.Filled.Bolt
    ToolInteractionKind.WELD_ARC, ToolInteractionKind.ROPE_TETHER,
    ToolInteractionKind.CABLE_TETHER, ToolInteractionKind.SPRING_TETHER -> Icons.Filled.Link
    ToolInteractionKind.CAMERA_SNAP -> Icons.Filled.CameraAlt
    ToolInteractionKind.FIREARM_SHOOT -> Icons.Filled.GpsFixed
    else -> Icons.Filled.Build
}

@Composable
private fun SniperScopeOverlay(
    recoilOffset: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f - recoilOffset * 14f
        val radius = size.minDimension * 0.42f

        // Dark peripheral lens vignette
        drawCircle(
            color = Color.Black.copy(alpha = 0.84f),
            radius = size.maxDimension,
            center = Offset(cx, cy),
            style = Stroke(width = (size.maxDimension - radius) * 2f)
        )
        // Outer optic ring
        drawCircle(
            color = LabColors.TextPrimary.copy(alpha = 0.78f),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 3.0f)
        )
        // Precision crosshairs
        drawLine(Color.Black.copy(alpha = 0.82f), Offset(cx - radius, cy), Offset(cx + radius, cy), strokeWidth = 1.6f)
        drawLine(Color.Black.copy(alpha = 0.82f), Offset(cx, cy - radius), Offset(cx, cy + radius), strokeWidth = 1.6f)
        // Mil-dot stadia ticks
        for (i in 1..4) {
            val d = i * (radius * 0.16f)
            drawCircle(Color.Black.copy(alpha = 0.86f), radius = 2.5f, center = Offset(cx - d, cy))
            drawCircle(Color.Black.copy(alpha = 0.86f), radius = 2.5f, center = Offset(cx + d, cy))
            drawCircle(Color.Black.copy(alpha = 0.86f), radius = 2.5f, center = Offset(cx, cy - d))
            drawCircle(Color.Black.copy(alpha = 0.86f), radius = 2.5f, center = Offset(cx, cy + d))
        }
        // Subtle dashed inner range circle
        drawCircle(
            color = LabColors.AmberAccent.copy(alpha = 0.65f),
            radius = 5.0f,
            center = Offset(cx, cy),
            style = Stroke(
                width = 1.4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )
        )
    }
}

@Composable
private fun AmmoPickupPill(
    prompt: NearbyAmmoBoxPrompt,
    onPickup: () -> Unit
) {
    Surface(
        onClick = onPickup,
        color = LabColors.PanelSurface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, LabColors.AmberAccent.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .padding(bottom = 6.dp)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("hud_pickup_ammo_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Inventory2,
                contentDescription = "Pickup Ammo Box",
                tint = LabColors.AmberAccent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Pickup +${prompt.amount} ${prompt.caliberLabel}",
                style = MaterialTheme.typography.labelLarge,
                color = LabColors.TextPrimary
            )
        }
    }
}

@Composable
private fun CompactFirearmReadoutCard(
    hud: FirearmHudState,
    onInspectWeapon: () -> Unit
) {
    val badgeText = when {
        hud.isHolstered -> "HOLSTERED"
        hud.actionState == WeaponActionState.RELOADING -> "RELOADING"
        hud.isAiming -> "ADS"
        hud.magAmmo <= 0 -> "EMPTY"
        else -> "READY"
    }
    val badgeColor = when {
        hud.isHolstered -> LabColors.TextSecondary
        hud.actionState == WeaponActionState.RELOADING -> LabColors.CyanAccent
        hud.magAmmo <= 0 -> LabColors.CrimsonWarn
        hud.isAiming -> LabColors.AmberAccent
        else -> LabColors.EmeraldOk
    }

    Surface(
        color = LabColors.PanelSurface.copy(alpha = 0.74f),
        border = BorderStroke(1.dp, LabColors.BorderSubtle.copy(alpha = 0.68f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .widthIn(min = 176.dp, max = 232.dp)
            .testTag("hud_firearm_readout")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hud.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = LabColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${hud.caliberLabel} • $badgeText",
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        maxLines = 1
                    )
                }
                FilledIconButton(
                    onClick = onInspectWeapon,
                    modifier = Modifier.size(48.dp).testTag("hud_inspect_weapon_button"),
                    shape = RoundedCornerShape(6.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = LabColors.CardSurface.copy(alpha = 0.68f),
                        contentColor = LabColors.TextSecondary
                    )
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = "Inspect Firearm", modifier = Modifier.size(18.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${hud.magAmmo}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (hud.magAmmo <= 0) LabColors.CrimsonWarn else LabColors.AmberAccent
                )
                Text(
                    text = "/ ${hud.reserveAmmo}",
                    style = MaterialTheme.typography.titleSmall,
                    color = LabColors.TextSecondary
                )
            }

            if (hud.actionState == WeaponActionState.RELOADING) {
                LinearProgressIndicator(
                    progress = { hud.reloadProgress.coerceIn(0f, 1f) },
                    color = LabColors.CyanAccent,
                    trackColor = LabColors.CardSurface,
                    modifier = Modifier.fillMaxWidth().height(3.dp)
                )
            }
        }
    }
}

@Composable
private fun TranslucentAimButton(
    isAiming: Boolean,
    holdToAim: Boolean,
    btnSize: androidx.compose.ui.unit.Dp,
    onAimToggle: () -> Unit,
    onAimHoldChanged: (Boolean) -> Unit
) {
    Surface(
        color = if (isAiming) LabColors.CyanAccent.copy(alpha = 0.82f) else LabColors.PanelSurface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, if (isAiming) LabColors.CyanAccent else LabColors.BorderSubtle.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(btnSize)
            .testTag("hud_aim_button")
            .then(
                if (holdToAim) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                onAimHoldChanged(true)
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        onAimHoldChanged(false)
                                        break
                                    }
                                    change.consume()
                                }
                            }
                        }
                    }
                } else {
                    Modifier.clickable(onClick = onAimToggle)
                }
            )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Filled.CenterFocusStrong,
                contentDescription = if (isAiming) "Exit Aim Down Sights" else "Aim Down Sights",
                tint = if (isAiming) LabColors.VoidBg else LabColors.TextPrimary
            )
        }
    }
}

@Composable
private fun TranslucentFireButton(
    isFiring: Boolean,
    isEmpty: Boolean,
    btnSize: androidx.compose.ui.unit.Dp,
    onFireHoldChanged: (Boolean) -> Unit,
    onFireTap: () -> Unit
) {
    val bgColor = when {
        isEmpty -> LabColors.CrimsonWarn.copy(alpha = 0.78f)
        isFiring -> LabColors.AmberAccent.copy(alpha = 0.92f)
        else -> LabColors.AmberAccent.copy(alpha = 0.76f)
    }
    Surface(
        color = bgColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .size(width = btnSize + 24.dp, height = btnSize + 6.dp)
            .testTag("hud_fire_button")
            .semantics { role = Role.Button }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        onFireHoldChanged(true)
                        onFireTap()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                onFireHoldChanged(false)
                                break
                            }
                            change.consume()
                        }
                    }
                }
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Filled.GpsFixed,
                contentDescription = "Fire Weapon",
                tint = LabColors.VoidBg,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

