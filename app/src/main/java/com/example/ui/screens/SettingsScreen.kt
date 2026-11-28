package com.example.ui.screens

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.GraphicsPreset
import com.example.physics.CameraMode
import com.example.ui.theme.LabColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onRestoreDefaults: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var tab by remember { mutableIntStateOf(0) } // 0 Graphics, 1 Controls, 2 Audio, 3 Gameplay
    var listeningAction by remember { mutableStateOf<String?>(null) }
    val keyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(listeningAction) {
        if (listeningAction != null) {
            runCatching { keyFocusRequester.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.VoidBg.copy(alpha = 0.95f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .focusRequester(keyFocusRequester)
            .focusable()
            .onKeyEvent { ev ->
                val target = listeningAction
                if (target != null && ev.type == KeyEventType.KeyDown) {
                    val code = ev.nativeKeyEvent.keyCode
                    onUpdateSettings { s ->
                        val kb = s.keyBindings
                        val nextKb = when (target) {
                            "forward" -> kb.copy(forward = code)
                            "backward" -> kb.copy(backward = code)
                            "left" -> kb.copy(left = code)
                            "right" -> kb.copy(right = code)
                            "jump" -> kb.copy(jump = code)
                            "crouch" -> kb.copy(crouch = code)
                            "sprint" -> kb.copy(sprint = code)
                            "interact" -> kb.copy(interact = code)
                            "reload" -> kb.copy(reload = code)
                            "holster" -> kb.copy(holster = code)
                            "inventory" -> kb.copy(inventory = code)
                            "freeze" -> kb.copy(freeze = code)
                            "camera" -> kb.copy(cameraToggle = code)
                            else -> kb
                        }
                        s.copy(keyBindings = nextKb)
                    }
                    listeningAction = null
                    true
                } else false
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .widthIn(max = 840.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LabColors.TextPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Settings", style = MaterialTheme.typography.titleLarge, color = LabColors.TextPrimary)
                        Text(
                            "Graphics, simulation fidelity, controls, audio, and camera behavior",
                            style = MaterialTheme.typography.bodySmall,
                            color = LabColors.TextSecondary
                        )
                    }
                }
                OutlinedButton(
                    onClick = onRestoreDefaults,
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, LabColors.BorderStrong),
                    modifier = Modifier.testTag("settings_restore_defaults_button")
                ) {
                    Text("Restore Defaults", style = MaterialTheme.typography.labelLarge, color = LabColors.AmberAccent)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    "Graphics" to "settings_tab_graphics",
                    "Controls" to "settings_tab_controls",
                    "Audio" to "settings_tab_audio",
                    "Gameplay" to "settings_tab_gameplay"
                )
                tabs.forEachIndexed { idx, (label, tag) ->
                    FilterChip(
                        selected = tab == idx,
                        onClick = { tab = idx },
                        label = { Text(label) },
                        modifier = Modifier.testTag(tag),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LabColors.AmberAccent,
                            selectedLabelColor = LabColors.VoidBg
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                color = LabColors.PanelSurface,
                border = BorderStroke(1.dp, LabColors.BorderSubtle),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (tab) {
                        0 -> {
                            Text("Quality Preset", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GraphicsPreset.entries.forEach { preset ->
                                    FilterChip(
                                        selected = settings.graphicsPreset == preset,
                                        onClick = { onUpdateSettings { it.withPreset(preset) } },
                                        label = { Text(preset.displayName) },
                                        modifier = Modifier.testTag("preset_${preset.name.lowercase()}"),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LabColors.CyanAccent,
                                            selectedLabelColor = LabColors.VoidBg
                                        )
                                    )
                                }
                            }

                            LabeledSlider(
                                label = "Render Resolution Scale (${(settings.resolutionScale * 100).toInt()}%)",
                                value = settings.resolutionScale,
                                range = 0.50f..1.00f,
                                onChange = { v -> onUpdateSettings { it.copy(resolutionScale = v) } }
                            )

                            Text("Shadow Resolution", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Off", "512 px", "1024 px", "1536 px").forEachIndexed { idx, lbl ->
                                    FilterChip(
                                        selected = settings.shadowQuality == idx,
                                        onClick = { onUpdateSettings { it.copy(shadowQuality = idx, shadowsEnabled = idx > 0) } },
                                        label = { Text(lbl) }
                                    )
                                }
                            }

                            Text("Texture Detail", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Low", "Medium", "High").forEachIndexed { idx, lbl ->
                                    FilterChip(
                                        selected = settings.textureQuality == idx,
                                        onClick = { onUpdateSettings { it.copy(textureQuality = idx) } },
                                        label = { Text(lbl) }
                                    )
                                }
                            }

                            Text("Ambient Occlusion Quality", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Low", "Medium", "High").forEachIndexed { idx, lbl ->
                                    FilterChip(
                                        selected = settings.aoQuality == idx,
                                        onClick = { onUpdateSettings { it.copy(aoQuality = idx) } },
                                        label = { Text(lbl) }
                                    )
                                }
                            }

                            Text("Surface Reflection Quality", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Low", "Medium", "High").forEachIndexed { idx, lbl ->
                                    FilterChip(
                                        selected = settings.reflectionQuality == idx,
                                        onClick = { onUpdateSettings { it.copy(reflectionQuality = idx) } },
                                        label = { Text(lbl) }
                                    )
                                }
                            }

                            SettingsSwitchRow(
                                title = "Anti-Aliasing Smoothing",
                                subtitle = "Edge smoothing and anisotropic mipmap filtering",
                                checked = settings.antiAliasing,
                                onCheckedChange = { v -> onUpdateSettings { it.copy(antiAliasing = v) } }
                            )

                            LabeledSlider(
                                label = "View Distance (${settings.renderDistance.toInt()} m)",
                                value = settings.renderDistance,
                                range = 60f..220f,
                                onChange = { v -> onUpdateSettings { it.copy(renderDistance = v) } }
                            )

                            Text("Physics Solver Iterations", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Fast (6)", "Standard (8)", "High (10)", "Ultra (14)").forEachIndexed { idx, lbl ->
                                    FilterChip(
                                        selected = settings.physicsQuality == idx,
                                        onClick = { onUpdateSettings { it.copy(physicsQuality = idx) } },
                                        label = { Text(lbl) }
                                    )
                                }
                            }

                            Text("Particle Budget", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Low (120)", "Medium (300)", "High (550)", "Ultra (900)").forEachIndexed { idx, lbl ->
                                    FilterChip(
                                        selected = settings.particleQuality == idx,
                                        onClick = { onUpdateSettings { it.copy(particleQuality = idx) } },
                                        label = { Text(lbl) }
                                    )
                                }
                            }

                            LabeledSlider(
                                label = "Max Dynamic Objects (${settings.maxPhysicsObjects})",
                                value = settings.maxPhysicsObjects.toFloat(),
                                range = 60f..240f,
                                onChange = { v -> onUpdateSettings { it.copy(maxPhysicsObjects = v.toInt()) } }
                            )

                            Text("Frame Rate Limit", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(30 to "30 FPS", 60 to "60 FPS", 90 to "90 FPS", 0 to "Unlimited").forEach { (lim, lbl) ->
                                    FilterChip(
                                        selected = settings.fpsLimit == lim,
                                        onClick = { onUpdateSettings { it.copy(fpsLimit = lim) } },
                                        label = { Text(lbl) }
                                    )
                                }
                            }

                            LabeledSlider(
                                label = "Field of View (${settings.fovDegrees.toInt()}°)",
                                value = settings.fovDegrees,
                                range = 55f..90f,
                                onChange = { v -> onUpdateSettings { it.copy(fovDegrees = v) } }
                            )
                        }

                        1 -> {
                            Text("Touch Controls & Camera Look", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)
                            LabeledSlider(
                                label = "Joystick Sensitivity (${String.format("%.2f", settings.joystickSensitivity)}x)",
                                value = settings.joystickSensitivity,
                                range = 0.5f..2.0f,
                                onChange = { v -> onUpdateSettings { it.copy(joystickSensitivity = v) } }
                            )
                            LabeledSlider(
                                label = "Joystick Deadzone (${(settings.joystickDeadzone * 100).toInt()}%)",
                                value = settings.joystickDeadzone,
                                range = 0.04f..0.30f,
                                onChange = { v -> onUpdateSettings { it.copy(joystickDeadzone = v) } }
                            )
                            LabeledSlider(
                                label = "Camera Look Sensitivity (${String.format("%.2f", settings.cameraSensitivity)}x)",
                                value = settings.cameraSensitivity,
                                range = 0.4f..2.5f,
                                onChange = { v -> onUpdateSettings { it.copy(cameraSensitivity = v) } }
                            )
                            LabeledSlider(
                                label = "Camera Look Smoothing (${(settings.cameraSmoothing * 100).toInt()}%)",
                                value = settings.cameraSmoothing,
                                range = 0.0f..0.85f,
                                onChange = { v -> onUpdateSettings { it.copy(cameraSmoothing = v) } }
                            )
                            SettingsSwitchRow(
                                title = "Invert Camera Y Axis",
                                subtitle = "Flip vertical pitch swipe direction",
                                checked = settings.invertCameraY,
                                onCheckedChange = { v -> onUpdateSettings { it.copy(invertCameraY = v) } }
                            )
                            LabeledSlider(
                                label = "Joystick Horizontal Offset (${settings.joystickOffsetX.toInt()} dp)",
                                value = settings.joystickOffsetX,
                                range = -20f..80f,
                                onChange = { v -> onUpdateSettings { it.copy(joystickOffsetX = v) } }
                            )
                            LabeledSlider(
                                label = "Joystick Vertical Offset (${settings.joystickOffsetY.toInt()} dp)",
                                value = settings.joystickOffsetY,
                                range = -20f..80f,
                                onChange = { v -> onUpdateSettings { it.copy(joystickOffsetY = v) } }
                            )
                            LabeledSlider(
                                label = "Action Buttons Scale (${String.format("%.2f", settings.buttonsScale)}x)",
                                value = settings.buttonsScale,
                                range = 0.80f..1.30f,
                                onChange = { v -> onUpdateSettings { it.copy(buttonsScale = v) } }
                            )
                            LabeledSlider(
                                label = "Action Buttons Horizontal Offset (${settings.buttonsOffsetX.toInt()} dp)",
                                value = settings.buttonsOffsetX,
                                range = -20f..80f,
                                onChange = { v -> onUpdateSettings { it.copy(buttonsOffsetX = v) } }
                            )
                            LabeledSlider(
                                label = "Action Buttons Vertical Offset (${settings.buttonsOffsetY.toInt()} dp)",
                                value = settings.buttonsOffsetY,
                                range = -20f..80f,
                                onChange = { v -> onUpdateSettings { it.copy(buttonsOffsetY = v) } }
                            )

                            HorizontalDivider(color = LabColors.BorderSubtle)
                            Text("Keyboard Bindings (Tap to Rebind)", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)

                            val kb = settings.keyBindings
                            KeyBindRow("Move Forward", kb.forward, listeningAction == "forward") { listeningAction = "forward" }
                            KeyBindRow("Move Backward", kb.backward, listeningAction == "backward") { listeningAction = "backward" }
                            KeyBindRow("Strafe Left", kb.left, listeningAction == "left") { listeningAction = "left" }
                            KeyBindRow("Strafe Right", kb.right, listeningAction == "right") { listeningAction = "right" }
                            KeyBindRow("Jump", kb.jump, listeningAction == "jump") { listeningAction = "jump" }
                            KeyBindRow("Crouch", kb.crouch, listeningAction == "crouch") { listeningAction = "crouch" }
                            KeyBindRow("Sprint", kb.sprint, listeningAction == "sprint") { listeningAction = "sprint" }
                            KeyBindRow("Interact / Pickup Ammo", kb.interact, listeningAction == "interact") { listeningAction = "interact" }
                            KeyBindRow("Reload / Rotate Held", kb.reload, listeningAction == "reload") { listeningAction = "reload" }
                            KeyBindRow("Holster / Draw Firearm", kb.holster, listeningAction == "holster") { listeningAction = "holster" }
                            KeyBindRow("Inventory", kb.inventory, listeningAction == "inventory") { listeningAction = "inventory" }
                            KeyBindRow("Freeze Target", kb.freeze, listeningAction == "freeze") { listeningAction = "freeze" }
                            KeyBindRow("Toggle Camera", kb.cameraToggle, listeningAction == "camera") { listeningAction = "camera" }
                        }

                        2 -> {
                            LabeledSlider(
                                label = "Master Volume (${(settings.masterVolume * 100).toInt()}%)",
                                value = settings.masterVolume,
                                range = 0f..1f,
                                onChange = { v -> onUpdateSettings { it.copy(masterVolume = v) } }
                            )
                            LabeledSlider(
                                label = "Impact, Footstep & Tool SFX (${(settings.sfxVolume * 100).toInt()}%)",
                                value = settings.sfxVolume,
                                range = 0f..1f,
                                onChange = { v -> onUpdateSettings { it.copy(sfxVolume = v) } }
                            )
                            LabeledSlider(
                                label = "Environmental Ambience (${(settings.ambientVolume * 100).toInt()}%)",
                                value = settings.ambientVolume,
                                range = 0f..1f,
                                onChange = { v -> onUpdateSettings { it.copy(ambientVolume = v) } }
                            )
                            LabeledSlider(
                                label = "Interface & Menu Click Audio (${(settings.uiVolume * 100).toInt()}%)",
                                value = settings.uiVolume,
                                range = 0f..1f,
                                onChange = { v -> onUpdateSettings { it.copy(uiVolume = v) } }
                            )
                        }

                        else -> {
                            Text("Default Camera Mode", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CameraMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = settings.defaultCameraMode == mode,
                                        onClick = { onUpdateSettings { it.copy(defaultCameraMode = mode) } },
                                        label = { Text(mode.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LabColors.AmberAccent,
                                            selectedLabelColor = LabColors.VoidBg
                                        )
                                    )
                                }
                            }

                            LabeledSlider(
                                label = "Player Movement Speed Scale (${String.format("%.2f", settings.movementSpeedScale)}x)",
                                value = settings.movementSpeedScale,
                                range = 0.70f..1.60f,
                                onChange = { v -> onUpdateSettings { it.copy(movementSpeedScale = v) } }
                            )

                            LabeledSlider(
                                label = "Interaction & Look Precision (${String.format("%.2f", settings.interactionSensitivity)}x)",
                                value = settings.interactionSensitivity,
                                range = 0.50f..2.00f,
                                onChange = { v -> onUpdateSettings { it.copy(interactionSensitivity = v) } }
                            )

                            LabeledSlider(
                                label = "Third-Person Camera Distance (${String.format("%.1f", settings.thirdPersonDistance)} m)",
                                value = settings.thirdPersonDistance,
                                range = 2.2f..6.5f,
                                onChange = { v -> onUpdateSettings { it.copy(thirdPersonDistance = v) } }
                            )

                            LabeledSlider(
                                label = "Third-Person Camera Height (${String.format("%.2f", settings.thirdPersonHeight)} m)",
                                value = settings.thirdPersonHeight,
                                range = 0.10f..1.40f,
                                onChange = { v -> onUpdateSettings { it.copy(thirdPersonHeight = v) } }
                            )

                            SettingsSwitchRow(
                                title = "Toggle Sprint Mode",
                                subtitle = "Tap sprint key once to sprint until released/stopped (off = hold to sprint)",
                                checked = settings.sprintToggleMode,
                                onCheckedChange = { v -> onUpdateSettings { it.copy(sprintToggleMode = v) } }
                            )

                            SettingsSwitchRow(
                                title = "First-Person Walk Head Bob",
                                subtitle = "Subtle synchronized stride cadence in first-person view",
                                checked = settings.cameraBobEnabled,
                                onCheckedChange = { v -> onUpdateSettings { it.copy(cameraBobEnabled = v) } }
                            )

                            SettingsSwitchRow(
                                title = "Heavy Impact Camera Shake",
                                subtitle = "Gentle trauma impulse on heavy nearby collisions and landings",
                                checked = settings.cameraShakeEnabled,
                                onCheckedChange = { v -> onUpdateSettings { it.copy(cameraShakeEnabled = v) } }
                            )

                            HorizontalDivider(color = LabColors.BorderSubtle)
                            Text("Firearm Handling & Optics", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)

                            LabeledSlider(
                                label = "ADS Aim Look Sensitivity (${String.format("%.2f", settings.aimSensitivity)}x)",
                                value = settings.aimSensitivity,
                                range = 0.25f..1.50f,
                                onChange = { v -> onUpdateSettings { it.copy(aimSensitivity = v) } }
                            )

                            SettingsSwitchRow(
                                title = "Hold to Aim Down Sights",
                                subtitle = "Hold Aim button or right mouse button instead of toggling ADS",
                                checked = settings.holdToAim,
                                onCheckedChange = { v -> onUpdateSettings { it.copy(holdToAim = v) } }
                            )

                            SettingsSwitchRow(
                                title = "Auto-Reload on Empty Magazine",
                                subtitle = "Automatically begin reloading when pulling trigger on an empty mag",
                                checked = settings.autoReloadEnabled,
                                onCheckedChange = { v -> onUpdateSettings { it.copy(autoReloadEnabled = v) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = LabColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = LabColors.TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.titleMedium, color = LabColors.TextPrimary)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = LabColors.AmberAccent,
                activeTrackColor = LabColors.AmberAccent
            )
        )
    }
}

@Composable
private fun KeyBindRow(
    actionLabel: String,
    keyCode: Int,
    isListening: Boolean,
    onRebindClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(actionLabel, style = MaterialTheme.typography.bodyLarge, color = LabColors.TextPrimary)
        Button(
            onClick = onRebindClick,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListening) LabColors.AmberAccent else LabColors.CardSurface,
                contentColor = if (isListening) LabColors.VoidBg else LabColors.CyanAccent
            )
        ) {
            Text(
                text = if (isListening) "Press key..." else KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_"),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
