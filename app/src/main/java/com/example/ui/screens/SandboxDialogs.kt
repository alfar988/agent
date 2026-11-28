package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.data.BuiltInPreset
import com.example.data.PresetLibrary
import com.example.data.SaveSlotSummary
import com.example.physics.WeatherType
import com.example.ui.theme.LabColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PauseAndSaveOverlay(
    saveSlots: List<SaveSlotSummary>,
    showSaveDialog: Boolean,
    onResume: () -> Unit,
    onOpenSaveDialog: (Boolean) -> Unit,
    onOpenPresets: () -> Unit,
    onResetScene: () -> Unit,
    onSaveSlot: (String, String) -> Unit,
    onLoadSlot: (String) -> Unit,
    onClearObjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onExitToMenu: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.VoidBg.copy(alpha = 0.80f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = LabColors.PanelSurface,
            border = BorderStroke(1.dp, LabColors.BorderStrong),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 440.dp)
                .heightIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!showSaveDialog) {
                    Text("Simulation Paused", style = MaterialTheme.typography.titleLarge, color = LabColors.TextPrimary)
                    HorizontalDivider(color = LabColors.BorderSubtle)
                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = onResume,
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_resume_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = LabColors.AmberAccent, contentColor = LabColors.VoidBg)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Resume", style = MaterialTheme.typography.titleSmall)
                    }
                    OutlinedButton(
                        onClick = { onOpenSaveDialog(true) },
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_save_load_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, tint = LabColors.CyanAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("Save / Load Session")
                    }
                    OutlinedButton(
                        onClick = onOpenPresets,
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_presets_button")
                    ) {
                        Icon(Icons.Filled.Science, contentDescription = null, tint = LabColors.CyanAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("Experiment Presets & JSON")
                    }
                    OutlinedButton(
                        onClick = onResetScene,
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_reset_scene_button")
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = LabColors.AmberAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("Reset Scene")
                    }
                    OutlinedButton(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_settings_button")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Settings")
                    }
                    OutlinedButton(
                        onClick = onClearObjects,
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_clear_button")
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = LabColors.AmberAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear Spawned Objects")
                    }
                    Button(
                        onClick = onExitToMenu,
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_exit_menu_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = LabColors.CrimsonWarn, contentColor = LabColors.TextPrimary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Exit to Main Menu")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Save & Load Slots", style = MaterialTheme.typography.titleLarge, color = LabColors.TextPrimary)
                        IconButton(onClick = { onOpenSaveDialog(false) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Back", tint = LabColors.TextPrimary)
                        }
                    }
                    HorizontalDivider(color = LabColors.BorderSubtle)
                    saveSlots.forEach { slot ->
                        Surface(
                            color = LabColors.CardSurface,
                            border = BorderStroke(1.dp, LabColors.BorderSubtle),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(slot.label, style = MaterialTheme.typography.titleMedium, color = LabColors.TextPrimary)
                                Text(
                                    if (slot.mapId != null) {
                                        "${slot.mapId.displayName} • ${slot.objectCount} objects • ${formatter.format(Date(slot.savedAtMs))}"
                                    } else "Empty Slot",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LabColors.TextSecondary
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!slot.isAutosave) {
                                        Button(
                                            onClick = { onSaveSlot(slot.slotId, slot.label) },
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = LabColors.AmberAccent, contentColor = LabColors.VoidBg)
                                        ) {
                                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Save")
                                        }
                                    }
                                    if (slot.mapId != null) {
                                        OutlinedButton(
                                            onClick = { onLoadSlot(slot.slotId) },
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Load")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnvironmentAndWeatherDialog(
    timeOfDayHours: Float,
    weatherType: WeatherType,
    windStrength: Float,
    fluidBasinEnabled: Boolean,
    timeScale: Float,
    onUpdateEnvironment: (
        timeOfDayHours: Float?,
        weatherType: WeatherType?,
        windStrength: Float?,
        fluidBasinEnabled: Boolean?,
        timeScale: Float?
    ) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.VoidBg.copy(alpha = 0.72f))
            .clickable(onClick = onClose)
            .testTag("environment_dialog"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = LabColors.PanelSurface,
            border = BorderStroke(1.dp, LabColors.BorderStrong),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .heightIn(max = 580.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WbSunny, contentDescription = null, tint = LabColors.AmberAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("Environment & Time", style = MaterialTheme.typography.titleLarge, color = LabColors.TextPrimary)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.testTag("environment_close_button")) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = LabColors.TextPrimary)
                    }
                }
                HorizontalDivider(color = LabColors.BorderSubtle)
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Simulation Speed (${String.format("%.2f", timeScale)}x)", style = MaterialTheme.typography.titleSmall, color = LabColors.TextPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                            FilterChip(
                                selected = kotlin.math.abs(timeScale - spd) < 0.05f,
                                onClick = { onUpdateEnvironment(null, null, null, null, spd) },
                                label = { Text("${spd}x") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LabColors.AmberAccent, selectedLabelColor = LabColors.VoidBg)
                            )
                        }
                    }
                    Slider(
                        value = timeScale,
                        onValueChange = { onUpdateEnvironment(null, null, null, null, it) },
                        valueRange = 0.1f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = LabColors.AmberAccent, activeTrackColor = LabColors.AmberAccent)
                    )

                    val hr = timeOfDayHours.toInt()
                    val min = ((timeOfDayHours - hr) * 60f).toInt()
                    Text("Time of Day (${String.format("%02d:%02d", hr, min)})", style = MaterialTheme.typography.titleSmall, color = LabColors.TextPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Dawn" to 6.5f, "Noon" to 13.0f, "Dusk" to 18.2f, "Night" to 22.5f).forEach { (lbl, h) ->
                            FilterChip(
                                selected = kotlin.math.abs(timeOfDayHours - h) < 0.8f,
                                onClick = { onUpdateEnvironment(h, null, null, null, null) },
                                label = { Text(lbl) }
                            )
                        }
                    }
                    Slider(
                        value = timeOfDayHours,
                        onValueChange = { onUpdateEnvironment(it, null, null, null, null) },
                        valueRange = 0f..24f
                    )

                    Text("Weather Conditions", style = MaterialTheme.typography.titleSmall, color = LabColors.TextPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WeatherType.entries.forEach { w ->
                            FilterChip(
                                selected = weatherType == w,
                                onClick = { onUpdateEnvironment(null, w, null, null, null) },
                                label = { Text(w.displayName) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LabColors.CyanAccent, selectedLabelColor = LabColors.VoidBg)
                            )
                        }
                    }

                    Text("Wind Force (${String.format("%.1f", windStrength)} m/s)", style = MaterialTheme.typography.titleSmall, color = LabColors.TextPrimary)
                    Slider(
                        value = windStrength,
                        onValueChange = { onUpdateEnvironment(null, null, it, null, null) },
                        valueRange = 0f..20f
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = LabColors.CyanAccent)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Hydrostatic Buoyancy Basin", style = MaterialTheme.typography.titleMedium, color = LabColors.TextPrimary)
                                Text("Flood floor level (0.65 m); light materials float, dense sink", style = MaterialTheme.typography.bodySmall, color = LabColors.TextSecondary)
                            }
                        }
                        Switch(
                            checked = fluidBasinEnabled,
                            onCheckedChange = { onUpdateEnvironment(null, null, null, it, null) },
                            modifier = Modifier.testTag("fluid_basin_switch")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetsImportExportDialog(
    exportedJsonPreview: String,
    onLoadPreset: (BuiltInPreset) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: (String) -> Unit,
    onClose: () -> Unit
) {
    var jsonText by remember(exportedJsonPreview) { mutableStateOf(exportedJsonPreview) }
    val clipboard = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.VoidBg.copy(alpha = 0.78f))
            .clickable(onClick = onClose)
            .testTag("presets_dialog"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = LabColors.PanelSurface,
            border = BorderStroke(1.dp, LabColors.BorderStrong),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 640.dp)
                .heightIn(max = 580.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Experiment Presets & JSON", style = MaterialTheme.typography.titleLarge, color = LabColors.TextPrimary)
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = LabColors.TextPrimary) }
                }
                HorizontalDivider(color = LabColors.BorderSubtle)

                PresetLibrary.all.forEach { preset ->
                    Surface(
                        color = LabColors.CardSurface,
                        border = BorderStroke(1.dp, LabColors.BorderSubtle),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onLoadPreset(preset) }.testTag("preset_card_${preset.id.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(preset.title, style = MaterialTheme.typography.titleMedium, color = LabColors.TextPrimary)
                                Text(preset.subtitle, style = MaterialTheme.typography.bodySmall, color = LabColors.TextSecondary)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(preset.recommendedMap.displayName, style = MaterialTheme.typography.labelSmall, color = LabColors.AmberAccent)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("Scene JSON Import & Export", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExportJson, shape = RoundedCornerShape(4.dp), modifier = Modifier.weight(1f)) {
                        Text("Generate JSON")
                    }
                    if (jsonText.isNotBlank()) {
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(jsonText)) },
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Copy JSON")
                        }
                    }
                }
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    placeholder = { Text("Paste exported scene JSON here to import...") },
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onImportJson(jsonText) },
                    enabled = jsonText.isNotBlank(),
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LabColors.AmberAccent, contentColor = LabColors.VoidBg)
                ) {
                    Text("Import Scene JSON")
                }
            }
        }
    }
}
