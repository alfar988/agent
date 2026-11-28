package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.electrical.ElectricalRole
import com.example.math.Vec3
import com.example.physics.CollisionMode
import com.example.physics.InspectedBodyInfo
import com.example.physics.MaterialLibrary
import com.example.physics.MaterialSpec
import com.example.ui.theme.LabColors

@Composable
fun InspectorPropertiesOverlay(
    info: InspectedBodyInfo,
    onUpdateProperties: (
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
    onToggleFreeze: () -> Unit,
    onGroupWithNext: () -> Unit,
    onUngroup: () -> Unit,
    onDuplicate: () -> Unit,
    onDetach: () -> Unit,
    onToggleElectrical: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    var matSearch by remember { mutableStateOf("") }
    var showMaterialPicker by remember { mutableStateOf(false) }
    var scaleSlider by remember(info.id) { mutableFloatStateOf(info.scale.coerceIn(0.4f, 3.0f)) }
    var massSlider by remember(info.id) { mutableFloatStateOf(info.massKg.coerceIn(0.5f, 300f)) }
    var frictionSlider by remember(info.id) { mutableFloatStateOf(info.friction.coerceIn(0.02f, 1.0f)) }
    var restitutionSlider by remember(info.id) { mutableFloatStateOf(info.restitution.coerceIn(0f, 0.98f)) }
    var gravitySlider by remember(info.id) { mutableFloatStateOf(info.gravityScale.coerceIn(-1f, 2.5f)) }
    var yawSlider by remember(info.id) { mutableFloatStateOf(info.eulerDeg.y.coerceIn(-180f, 180f)) }

    val matchingMats = remember(matSearch) {
        val q = matSearch.trim().lowercase()
        MaterialLibrary.all.filter {
            q.isEmpty() || it.displayName.lowercase().contains(q) || it.category.displayName.lowercase().contains(q)
        }.take(24)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.VoidBg.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .testTag("property_editor_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = LabColors.PanelSurface,
            border = BorderStroke(1.dp, LabColors.BorderStrong),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 580.dp)
                .heightIn(max = 580.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Object Properties • #${info.id}" + (info.groupId?.let { " • Group $it" } ?: ""),
                            style = MaterialTheme.typography.labelMedium,
                            color = LabColors.AmberAccent
                        )
                        Text(info.name, style = MaterialTheme.typography.titleLarge, color = LabColors.TextPrimary)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.testTag("property_editor_close")) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Inspector", tint = LabColors.TextPrimary)
                    }
                }

                HorizontalDivider(color = LabColors.BorderSubtle)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PropBadge("Mass", "${String.format("%.1f", info.massKg)} kg", Modifier.weight(1f))
                    PropBadge("Temp", "${info.temperatureC.toInt()} °C", Modifier.weight(1f))
                    PropBadge("Damage", "${(info.crackLevel * 100).toInt()}%", Modifier.weight(1f))
                    PropBadge("Joints", "${info.constraintCount}", Modifier.weight(1f))
                }

                if (info.electricalRole != ElectricalRole.NONE) {
                    Surface(
                        color = LabColors.CardSurface,
                        border = BorderStroke(1.dp, LabColors.BorderStrong),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(info.electricalRole.displayName, style = MaterialTheme.typography.titleSmall, color = LabColors.CyanAccent)
                                Text(
                                    "${String.format("%.1f", info.electricalVoltage)} V • ${String.format("%.2f", info.electricalCurrent)} A" +
                                        if (info.electricalBlown) " • Blown Fuse" else "",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (info.electricalBlown) LabColors.CrimsonWarn else LabColors.TextSecondary
                                )
                            }
                            if (info.electricalRole == ElectricalRole.SWITCH || info.electricalRole == ElectricalRole.FUSE || info.electricalRole == ElectricalRole.ELECTRICAL_PANEL) {
                                Button(
                                    onClick = onToggleElectrical,
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = LabColors.AmberAccent, contentColor = LabColors.VoidBg)
                                ) {
                                    Icon(Icons.Filled.ElectricBolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        when {
                                            info.electricalRole == ElectricalRole.FUSE -> "Reset Fuse"
                                            info.electricalSwitchClosed -> "Switch Off"
                                            else -> "Switch On"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Material: ${info.materialSpec.displayName}", style = MaterialTheme.typography.titleSmall, color = LabColors.TextPrimary)
                        Text(
                            "${info.materialSpec.category.displayName} • ${info.materialSpec.densityKgM3.toInt()} kg/m³",
                            style = MaterialTheme.typography.labelMedium,
                            color = LabColors.TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = { showMaterialPicker = !showMaterialPicker },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.testTag("property_change_material_btn")
                    ) {
                        Text(if (showMaterialPicker) "Hide Materials" else "Change Material")
                    }
                }

                if (showMaterialPicker) {
                    OutlinedTextField(
                        value = matSearch,
                        onValueChange = { matSearch = it },
                        singleLine = true,
                        placeholder = { Text("Filter materials (e.g. Tungsten, Cork, Glass)...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("property_material_search")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        matchingMats.forEach { mat ->
                            val sw = Color(mat.tintR.coerceIn(0f, 1f), mat.tintG.coerceIn(0f, 1f), mat.tintB.coerceIn(0f, 1f))
                            FilterChip(
                                selected = info.materialSpec.id == mat.id,
                                onClick = { onChangeMaterialSpec(mat) },
                                leadingIcon = { Box(Modifier.size(10.dp).background(sw, CircleShape)) },
                                label = { Text(mat.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LabColors.CyanAccent,
                                    selectedLabelColor = LabColors.VoidBg
                                )
                            )
                        }
                    }
                }

                if (info.resizable) {
                    PropSlider(
                        "Scale (${String.format("%.2f", scaleSlider)}x)",
                        scaleSlider,
                        0.4f..3.0f,
                        onValueChangeFinished = {
                            onUpdateProperties(info.id, null, false, scaleSlider, null, null, null, null, null, null, null, null)
                        }
                    ) { scaleSlider = it }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Mass (${String.format("%.1f", massSlider)} kg)", style = MaterialTheme.typography.titleSmall, color = LabColors.TextPrimary)
                    TextButton(onClick = {
                        onUpdateProperties(info.id, null, true, null, null, null, null, null, null, null, null, null)
                    }) { Text("Auto Density") }
                }
                Slider(
                    value = massSlider,
                    onValueChange = { massSlider = it },
                    onValueChangeFinished = {
                        onUpdateProperties(info.id, massSlider, false, null, null, null, null, null, null, null, null, null)
                    },
                    valueRange = 0.5f..300f,
                    colors = SliderDefaults.colors(thumbColor = LabColors.AmberAccent, activeTrackColor = LabColors.AmberAccent)
                )

                PropSlider(
                    "Surface Friction (${String.format("%.2f", frictionSlider)})",
                    frictionSlider,
                    0.02f..1.0f,
                    onValueChangeFinished = {
                        onUpdateProperties(info.id, null, false, null, frictionSlider, null, null, null, null, null, null, null)
                    }
                ) { frictionSlider = it }

                PropSlider(
                    "Restitution (${String.format("%.2f", restitutionSlider)})",
                    restitutionSlider,
                    0.0f..0.98f,
                    onValueChangeFinished = {
                        onUpdateProperties(info.id, null, false, null, null, restitutionSlider, null, null, null, null, null, null)
                    }
                ) { restitutionSlider = it }

                PropSlider(
                    "Gravity Scale (${String.format("%.2f", gravitySlider)}x)",
                    gravitySlider,
                    -1.0f..2.5f,
                    onValueChangeFinished = {
                        onUpdateProperties(info.id, null, false, null, null, null, gravitySlider, null, null, null, null, null)
                    }
                ) { gravitySlider = it }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("0G" to 0f, "Moon" to 0.16f, "Earth" to 1f, "2x" to 2f).forEach { (lbl, gVal) ->
                        FilterChip(
                            selected = kotlin.math.abs(gravitySlider - gVal) < 0.05f,
                            onClick = {
                                gravitySlider = gVal
                                onUpdateProperties(info.id, null, false, null, null, null, gVal, null, null, null, null, null)
                            },
                            label = { Text(lbl) }
                        )
                    }
                }

                Text("Collision Mode", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CollisionMode.entries.forEach { mode ->
                        FilterChip(
                            selected = info.collisionMode == mode,
                            onClick = {
                                onUpdateProperties(info.id, null, false, null, null, null, null, mode, null, null, null, null)
                            },
                            label = { Text(mode.displayName) }
                        )
                    }
                }

                Text("Position Offset (±0.25 m) & Yaw", style = MaterialTheme.typography.titleSmall, color = LabColors.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    NudgeBtn("-X", Modifier.weight(1f)) { onUpdateProperties(info.id, null, false, null, null, null, null, null, null, Vec3(-0.25f, 0f, 0f), null, null) }
                    NudgeBtn("+X", Modifier.weight(1f)) { onUpdateProperties(info.id, null, false, null, null, null, null, null, null, Vec3(0.25f, 0f, 0f), null, null) }
                    NudgeBtn("+Y", Modifier.weight(1f)) { onUpdateProperties(info.id, null, false, null, null, null, null, null, null, Vec3(0f, 0.25f, 0f), null, null) }
                    NudgeBtn("-Z", Modifier.weight(1f)) { onUpdateProperties(info.id, null, false, null, null, null, null, null, null, Vec3(0f, 0f, -0.25f), null, null) }
                    NudgeBtn("+Z", Modifier.weight(1f)) { onUpdateProperties(info.id, null, false, null, null, null, null, null, null, Vec3(0f, 0f, 0.25f), null, null) }
                }
                PropSlider(
                    "Yaw Rotation (${yawSlider.toInt()}°)",
                    yawSlider,
                    -180f..180f,
                    onValueChangeFinished = {
                        onUpdateProperties(info.id, null, false, null, null, null, null, null, null, null, Vec3(info.eulerDeg.x, yawSlider, info.eulerDeg.z), null)
                    }
                ) { yawSlider = it }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onToggleFreeze, shape = RoundedCornerShape(4.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.AcUnit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (info.isFrozen) "Unfreeze" else "Freeze")
                    }
                    OutlinedButton(onClick = onDuplicate, shape = RoundedCornerShape(4.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Duplicate")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = if (info.groupId != null) onUngroup else onGroupWithNext,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.GroupWork, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (info.groupId != null) "Ungroup" else "Group")
                    }
                    if (info.constraintCount > 0) {
                        OutlinedButton(onClick = onDetach, shape = RoundedCornerShape(4.dp), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Detach")
                        }
                    }
                    Button(
                        onClick = onDelete,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = LabColors.CrimsonWarn, contentColor = LabColors.TextPrimary)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun PropBadge(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = LabColors.CardSurface, shape = RoundedCornerShape(5.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = LabColors.TextMuted)
            Text(value, style = MaterialTheme.typography.titleSmall, color = LabColors.TextPrimary)
        }
    }
}

@Composable
private fun PropSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChangeFinished: (() -> Unit)? = null,
    onChange: (Float) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall, color = LabColors.TextPrimary)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = LabColors.AmberAccent, activeTrackColor = LabColors.AmberAccent)
        )
    }
}

@Composable
private fun NudgeBtn(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
