package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.character.BootStyle
import com.example.character.CharacterAppearance
import com.example.character.CharacterProfilePreset
import com.example.character.ClothingColorPreset
import com.example.character.EyeColorPreset
import com.example.character.GloveStyle
import com.example.character.HairColorPreset
import com.example.character.HairStyle
import com.example.character.JacketStyle
import com.example.character.SkinTonePreset
import com.example.character.TrouserStyle
import com.example.math.Vec3
import com.example.render.CharacterCameraFocus
import com.example.ui.theme.LabColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterSelectScreen(
    appearance: CharacterAppearance,
    focusPreset: CharacterCameraFocus,
    onSelectAppearance: (CharacterAppearance) -> Unit,
    onPreviewOrbit: (Float, Float) -> Unit,
    onPreviewZoom: (Float) -> Unit,
    onFocusChange: (CharacterCameraFocus) -> Unit,
    onConfirmAndBack: () -> Unit
) {
    BackHandler(onBack = onConfirmAndBack)
    var categoryTab by remember { mutableIntStateOf(0) } // 0 Profile/Face, 1 Hair, 2 Apparel, 3 Gloves/Boots

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        LabColors.VoidBg.copy(alpha = 0.72f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 680.dp

            // Interactive 3D Viewport Drag Zone (left / center stage)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = if (isWide) 390.dp else 0.dp, bottom = if (isWide) 0.dp else 260.dp)
                    .testTag("camera_preview_drag")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var prev = down.position
                            while (true) {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!ch.pressed) break
                                val d = ch.position - prev
                                prev = ch.position
                                if (d.x != 0f || d.y != 0f) {
                                    onPreviewOrbit(d.x * 0.42f, d.y * 0.30f)
                                    ch.consume()
                                }
                            }
                        }
                    }
            ) {
                // Top-left back header + active operator summary
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = LabColors.PanelSurface.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, LabColors.BorderSubtle),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 14.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            IconButton(
                                onClick = onConfirmAndBack,
                                modifier = Modifier.testTag("character_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LabColors.TextPrimary)
                            }
                            Column {
                                Text("Operator Turntable", style = MaterialTheme.typography.titleMedium, color = LabColors.TextPrimary)
                                val activePreset = CharacterProfilePreset.entries.firstOrNull { it.id == appearance.profileId }
                                Text(
                                    activePreset?.displayName ?: "Custom Operator",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LabColors.CyanAccent
                                )
                            }
                        }
                    }
                }

                // Bottom-left camera focus & zoom controls
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        CharacterCameraFocus.FULL to "Full Body",
                        CharacterCameraFocus.HEAD to "Face & Hair",
                        CharacterCameraFocus.HANDS to "Hands",
                        CharacterCameraFocus.BOOTS to "Boots"
                    ).forEach { (preset, label) ->
                        FilterChip(
                            selected = focusPreset == preset,
                            onClick = { onFocusChange(preset) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LabColors.AmberAccent,
                                selectedLabelColor = LabColors.VoidBg,
                                containerColor = LabColors.PanelSurface.copy(alpha = 0.88f),
                                labelColor = LabColors.TextPrimary
                            )
                        )
                    }
                    Surface(
                        color = LabColors.PanelSurface.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, LabColors.BorderSubtle),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row {
                            IconButton(
                                onClick = { onPreviewZoom(-0.18f) },
                                modifier = Modifier.testTag("character_zoom_out")
                            ) {
                                Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out", tint = LabColors.TextPrimary)
                            }
                            IconButton(
                                onClick = { onPreviewZoom(0.18f) },
                                modifier = Modifier.testTag("character_zoom_in")
                            ) {
                                Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In", tint = LabColors.TextPrimary)
                            }
                        }
                    }
                }
            }

            // Customization Rail
            Surface(
                color = LabColors.PanelSurface.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, LabColors.BorderStrong),
                shape = RoundedCornerShape(8.dp),
                modifier = if (isWide) {
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(16.dp)
                        .width(376.dp)
                        .fillMaxHeight()
                } else {
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(10.dp)
                        .fillMaxWidth()
                        .height(255.dp)
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Presets Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CharacterProfilePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = appearance.profileId == preset.id,
                                onClick = { onSelectAppearance(preset.defaultAppearance) },
                                label = { Text(preset.displayName.substringAfter(' ')) },
                                modifier = Modifier.testTag("char_preset_${preset.id.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LabColors.CyanAccent,
                                    selectedLabelColor = LabColors.VoidBg
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Category Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Face", "Hair", "Apparel", "Hands & Boots").forEachIndexed { idx, title ->
                            FilterChip(
                                selected = categoryTab == idx,
                                onClick = { categoryTab = idx },
                                label = { Text(title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LabColors.AmberAccent,
                                    selectedLabelColor = LabColors.VoidBg
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = LabColors.BorderSubtle)
                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (categoryTab) {
                            0 -> {
                                SectionLabel("Skin Tone")
                                ColorSwatchRow(
                                    items = SkinTonePreset.entries.map { it to it.rgb },
                                    selected = appearance.skinTone,
                                    onSelect = { onSelectAppearance(appearance.copy(skinTone = it)) }
                                )
                                SectionLabel("Eye Color")
                                ColorSwatchRow(
                                    items = EyeColorPreset.entries.map { it to it.rgb },
                                    selected = appearance.eyeColor,
                                    onSelect = { onSelectAppearance(appearance.copy(eyeColor = it)) }
                                )
                            }
                            1 -> {
                                SectionLabel("Hairstyle")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    HairStyle.entries.forEach { hs ->
                                        FilterChip(
                                            selected = appearance.hairStyle == hs,
                                            onClick = { onSelectAppearance(appearance.copy(hairStyle = hs)) },
                                            label = { Text(hs.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                        )
                                    }
                                }
                                SectionLabel("Hair Color")
                                ColorSwatchRow(
                                    items = HairColorPreset.entries.map { it to it.rgb },
                                    selected = appearance.hairColor,
                                    onSelect = { onSelectAppearance(appearance.copy(hairColor = it)) }
                                )
                            }
                            2 -> {
                                SectionLabel("Jacket / Coat")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    JacketStyle.entries.forEach { js ->
                                        FilterChip(
                                            selected = appearance.jacketStyle == js,
                                            onClick = { onSelectAppearance(appearance.copy(jacketStyle = js)) },
                                            label = { Text(js.displayName) }
                                        )
                                    }
                                }
                                ColorSwatchRow(
                                    items = ClothingColorPreset.entries.map { it to it.rgb },
                                    selected = appearance.jacketColor,
                                    onSelect = { onSelectAppearance(appearance.copy(jacketColor = it)) }
                                )
                                SectionLabel("Trousers")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TrouserStyle.entries.forEach { ts ->
                                        FilterChip(
                                            selected = appearance.trouserStyle == ts,
                                            onClick = { onSelectAppearance(appearance.copy(trouserStyle = ts)) },
                                            label = { Text(ts.displayName) }
                                        )
                                    }
                                }
                                ColorSwatchRow(
                                    items = ClothingColorPreset.entries.map { it to it.rgb },
                                    selected = appearance.trouserColor,
                                    onSelect = { onSelectAppearance(appearance.copy(trouserColor = it)) }
                                )
                            }
                            else -> {
                                SectionLabel("Gloves / Hands")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GloveStyle.entries.forEach { gs ->
                                        FilterChip(
                                            selected = appearance.gloveStyle == gs,
                                            onClick = { onSelectAppearance(appearance.copy(gloveStyle = gs)) },
                                            label = { Text(gs.displayName) }
                                        )
                                    }
                                }
                                if (appearance.gloveStyle == GloveStyle.TACTICAL_GLOVES) {
                                    ColorSwatchRow(
                                        items = ClothingColorPreset.entries.map { it to it.rgb },
                                        selected = appearance.gloveColor,
                                        onSelect = { onSelectAppearance(appearance.copy(gloveColor = it)) }
                                    )
                                }
                                SectionLabel("Footwear")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BootStyle.entries.forEach { bs ->
                                        FilterChip(
                                            selected = appearance.bootStyle == bs,
                                            onClick = { onSelectAppearance(appearance.copy(bootStyle = bs)) },
                                            label = { Text(bs.displayName) }
                                        )
                                    }
                                }
                                ColorSwatchRow(
                                    items = ClothingColorPreset.entries.map { it to it.rgb },
                                    selected = appearance.bootColor,
                                    onSelect = { onSelectAppearance(appearance.copy(bootColor = it)) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = onConfirmAndBack,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LabColors.AmberAccent,
                            contentColor = LabColors.VoidBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("character_confirm_button")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm & Apply", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = LabColors.TextSecondary)
}

@Composable
private fun <T> ColorSwatchRow(
    items: List<Pair<T, Vec3>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (item, rgb) ->
            val isSel = item == selected
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(rgb.x.coerceIn(0f, 1f), rgb.y.coerceIn(0f, 1f), rgb.z.coerceIn(0f, 1f), 1f))
                    .border(
                        width = if (isSel) 2.5.dp else 1.dp,
                        color = if (isSel) LabColors.AmberAccent else LabColors.BorderStrong,
                        shape = CircleShape
                    )
                    .clickable { onSelect(item) }
            )
        }
    }
}
