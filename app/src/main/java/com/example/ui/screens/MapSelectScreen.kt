package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SaveSlotSummary
import com.example.physics.MapId
import com.example.render.MapPreviewRenderer
import com.example.ui.theme.LabColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MapSelectScreen(
    saveSlots: List<SaveSlotSummary>,
    onSelectMap: (MapId) -> Unit,
    onLoadSlot: (String) -> Unit,
    onDeleteSlot: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Maps, 1 = Saved Sessions
    var selectedMap by remember { mutableStateOf(MapId.LAB) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.VoidBg.copy(alpha = 0.94f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("map_select_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LabColors.TextPrimary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Environments", style = MaterialTheme.typography.titleLarge, color = LabColors.TextPrimary)
                        Text("Select a facility or resume a saved session", style = MaterialTheme.typography.bodyMedium, color = LabColors.TextSecondary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Maps (${MapId.entries.size})") },
                        modifier = Modifier.testTag("tab_sectors"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LabColors.AmberAccent,
                            selectedLabelColor = LabColors.VoidBg
                        )
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Saved Sessions") },
                        modifier = Modifier.testTag("tab_saved_sessions"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LabColors.CyanAccent,
                            selectedLabelColor = LabColors.VoidBg
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = LabColors.BorderSubtle)
            Spacer(Modifier.height(14.dp))

            if (selectedTab == 0) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useSplitDetail = maxWidth >= 840.dp
                    if (useSplitDetail) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 260.dp),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .fillMaxHeight()
                                    .testTag("map_grid")
                            ) {
                                items(MapId.entries, key = { it.name }) { mapId ->
                                    MapPreviewCard(
                                        mapId = mapId,
                                        isSelected = selectedMap == mapId,
                                        onSelect = { selectedMap = mapId },
                                        onLaunch = { onSelectMap(mapId) }
                                    )
                                }
                            }

                            MapDetailPane(
                                mapId = selectedMap,
                                onLaunch = { onSelectMap(selectedMap) },
                                modifier = Modifier
                                    .weight(0.95f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 290.dp),
                                contentPadding = PaddingValues(bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag("map_grid")
                            ) {
                                items(MapId.entries, key = { it.name }) { mapId ->
                                    MapPreviewCard(
                                        mapId = mapId,
                                        isSelected = selectedMap == mapId,
                                        onSelect = { selectedMap = mapId },
                                        onLaunch = { onSelectMap(mapId) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Surface(
                                color = LabColors.PanelSurface,
                                border = BorderStroke(1.dp, LabColors.BorderSubtle),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(selectedMap.displayName, style = MaterialTheme.typography.titleMedium, color = LabColors.TextPrimary)
                                        Text(selectedMap.subtitle, style = MaterialTheme.typography.bodySmall, color = LabColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Button(
                                        onClick = { onSelectMap(selectedMap) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LabColors.AmberAccent,
                                            contentColor = LabColors.VoidBg
                                        ),
                                        shape = RoundedCornerShape(5.dp),
                                        modifier = Modifier.height(44.dp).testTag("play_selected_map_button")
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Play Selected Map", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(saveSlots, key = { it.slotId }) { slot ->
                        SaveSlotRow(
                            slot = slot,
                            onLoad = { onLoadSlot(slot.slotId) },
                            onDelete = { onDeleteSlot(slot.slotId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberMapPreviewBitmap(mapId: MapId): ImageBitmap? {
    val state by produceState<ImageBitmap?>(initialValue = null, key1 = mapId) {
        value = withContext(Dispatchers.Default) {
            MapPreviewRenderer.getOrRenderPreview(mapId)
        }
    }
    return state
}

@Composable
private fun MapPreviewCard(
    mapId: MapId,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onLaunch: () -> Unit
) {
    val preview = rememberMapPreviewBitmap(mapId)

    Surface(
        color = if (isSelected) LabColors.CardSurface else LabColors.PanelSurface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) LabColors.AmberAccent else LabColors.BorderSubtle
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("map_card_${mapId.name.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Authentic 16:9 rendered 3D scene preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LabColors.VoidBg),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null) {
                    Image(
                        bitmap = preview,
                        contentDescription = "${mapId.displayName} preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Rendering preview...", style = MaterialTheme.typography.labelMedium, color = LabColors.TextMuted)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(mapId.code, style = MaterialTheme.typography.labelMedium, color = LabColors.CyanAccent)
                    Text(mapId.displayName, style = MaterialTheme.typography.titleMedium, color = LabColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onLaunch,
                    border = BorderStroke(1.dp, if (isSelected) LabColors.AmberAccent else LabColors.BorderStrong),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) LabColors.AmberAccent else LabColors.CardSurface,
                        contentColor = if (isSelected) LabColors.VoidBg else LabColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("launch_map_${mapId.name.lowercase()}")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play ${mapId.displayName}", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                mapId.description,
                style = MaterialTheme.typography.bodySmall,
                color = LabColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MapDetailPane(
    mapId: MapId,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preview = rememberMapPreviewBitmap(mapId)
    Surface(
        color = LabColors.PanelSurface,
        border = BorderStroke(1.dp, LabColors.BorderSubtle),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(LabColors.VoidBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (preview != null) {
                        Image(
                            bitmap = preview,
                            contentDescription = "${mapId.displayName} large preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(mapId.code, style = MaterialTheme.typography.labelMedium, color = LabColors.CyanAccent)
                Spacer(Modifier.height(4.dp))
                Text(mapId.displayName, style = MaterialTheme.typography.headlineMedium, color = LabColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(mapId.subtitle, style = MaterialTheme.typography.titleSmall, color = LabColors.AmberAccent)
                Spacer(Modifier.height(10.dp))
                Text(mapId.description, style = MaterialTheme.typography.bodyLarge, color = LabColors.TextSecondary)
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onLaunch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("play_selected_map_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LabColors.AmberAccent,
                    contentColor = LabColors.VoidBg
                ),
                shape = RoundedCornerShape(5.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Play Selected Map", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SaveSlotRow(
    slot: SaveSlotSummary,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    Surface(
        color = LabColors.PanelSurface,
        border = BorderStroke(1.dp, LabColors.BorderSubtle),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Filled.FolderOpen,
                    contentDescription = null,
                    tint = if (slot.mapId != null) LabColors.CyanAccent else LabColors.TextMuted
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(slot.label, style = MaterialTheme.typography.titleMedium, color = LabColors.TextPrimary)
                    if (slot.mapId != null) {
                        Text(
                            "${slot.mapId.displayName} • ${slot.objectCount} objects • ${slot.constraintCount} constraints • ${formatter.format(Date(slot.savedAtMs))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LabColors.TextSecondary
                        )
                    } else {
                        Text("Empty Slot", style = MaterialTheme.typography.bodyMedium, color = LabColors.TextMuted)
                    }
                }
            }
            if (slot.mapId != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onLoad,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LabColors.CyanAccent,
                            contentColor = LabColors.VoidBg
                        )
                    ) {
                        Text("Load")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete slot", tint = LabColors.CrimsonWarn)
                    }
                }
            }
        }
    }
}
