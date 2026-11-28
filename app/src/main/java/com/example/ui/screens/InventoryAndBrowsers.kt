package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.electrical.ElectricalRole
import com.example.physics.InventoryCategory
import com.example.physics.MaterialCategory
import com.example.physics.MaterialLibrary
import com.example.physics.MaterialSpec
import com.example.physics.ObjectType
import com.example.physics.ToolCatalog
import com.example.physics.ToolCategory
import com.example.physics.ToolDefinition
import com.example.ui.theme.LabColors
import com.example.ui.viewmodel.InventoryTabState
import com.example.weapons.AmmoBoxVariant
import com.example.weapons.FirearmCatalog

private enum class BrowserMode {
    ALL_OBJECTS,
    CATEGORY,
    MATERIALS,
    TOOLS,
    FAVORITES
}

@Composable
fun SpawnCatalogOverlay(
    tabState: InventoryTabState,
    onUpdateTabState: ((InventoryTabState) -> InventoryTabState) -> Unit,
    favoriteIds: Set<String>,
    recentIds: List<String>,
    spawnQuantity: Int,
    onSetQuantity: (Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSpawnObject: (ObjectType) -> Unit,
    onSpawnMaterialBlock: (MaterialSpec) -> Unit,
    onEquipTool: (String) -> Unit,
    onSpawnToolObject: (String) -> Unit,
    onClose: () -> Unit
) {
    val searchQuery = tabState.searchQuery
    val selectedInvCat = tabState.selectedInvCat
    val showFavoritesOnly = tabState.showFavoritesOnly
    val materialSubFilter = tabState.materialSubFilter
    val toolSubFilter = tabState.toolSubFilter

    val browserMode = when {
        showFavoritesOnly -> BrowserMode.FAVORITES
        selectedInvCat == InventoryCategory.MATERIALS -> BrowserMode.MATERIALS
        selectedInvCat == InventoryCategory.TOOLS -> BrowserMode.TOOLS
        selectedInvCat != null -> BrowserMode.CATEGORY
        else -> BrowserMode.ALL_OBJECTS
    }

    val query = searchQuery.trim().lowercase()

    val filteredObjects = remember(browserMode, selectedInvCat, query, favoriteIds) {
        ObjectType.entries.filter { obj ->
            obj.spawnable &&
                (selectedInvCat == null || obj.inventoryCategory == selectedInvCat) &&
                (!showFavoritesOnly || "obj:${obj.name}" in favoriteIds) &&
                (query.isEmpty() || obj.displayName.lowercase().contains(query) || obj.defaultMaterialId.lowercase().contains(query))
        }
    }

    val filteredMaterials = remember(materialSubFilter, query, favoriteIds, showFavoritesOnly) {
        MaterialLibrary.all.filter { mat ->
            (materialSubFilter == null || mat.category == materialSubFilter) &&
                (!showFavoritesOnly || "mat:${mat.id}" in favoriteIds) &&
                (query.isEmpty() || mat.displayName.lowercase().contains(query) || mat.category.displayName.lowercase().contains(query))
        }
    }

    val filteredTools = remember(toolSubFilter, query, favoriteIds, showFavoritesOnly) {
        ToolCatalog.all.filter { tool ->
            (toolSubFilter == null || tool.category == toolSubFilter) &&
                (!showFavoritesOnly || "tool:${tool.id}" in favoriteIds) &&
                (query.isEmpty() || tool.displayName.lowercase().contains(query) || tool.description.lowercase().contains(query))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.VoidBg.copy(alpha = 0.78f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = LabColors.PanelSurface,
            border = BorderStroke(1.dp, LabColors.BorderStrong),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 860.dp)
                .heightIn(max = 600.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Inventory", style = MaterialTheme.typography.titleLarge, color = LabColors.TextPrimary)
                        Text(
                            "${ObjectType.entries.count { it.spawnable }} Objects • ${MaterialLibrary.all.size} Materials • ${ToolCatalog.all.size} Tools",
                            style = MaterialTheme.typography.bodySmall,
                            color = LabColors.TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(1, 3, 5).forEach { q ->
                            FilterChip(
                                selected = spawnQuantity == q,
                                onClick = { onSetQuantity(q) },
                                label = { Text("${q}x") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LabColors.CyanAccent,
                                    selectedLabelColor = LabColors.VoidBg
                                )
                            )
                        }
                        IconButton(onClick = onClose, modifier = Modifier.testTag("inventory_close_button")) {
                            Icon(Icons.Filled.Close, contentDescription = "Close Inventory", tint = LabColors.TextPrimary)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { v -> onUpdateTabState { it.copy(searchQuery = v) } },
                    singleLine = true,
                    placeholder = { Text("Search objects, materials, or tools...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onUpdateTabState { it.copy(searchQuery = "") } },
                                modifier = Modifier.testTag("inventory_clear_search_button")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear Search")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LabColors.AmberAccent,
                        unfocusedBorderColor = LabColors.BorderSubtle,
                        focusedTextColor = LabColors.TextPrimary,
                        unfocusedTextColor = LabColors.TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inventory_search_field")
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = !showFavoritesOnly && selectedInvCat == null,
                        onClick = { onUpdateTabState { it.copy(showFavoritesOnly = false, selectedInvCat = null) } },
                        label = { Text("All Objects") },
                        modifier = Modifier.testTag("inv_cat_all"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LabColors.AmberAccent,
                            selectedLabelColor = LabColors.VoidBg
                        )
                    )
                    FilterChip(
                        selected = showFavoritesOnly,
                        onClick = { onUpdateTabState { it.copy(showFavoritesOnly = !it.showFavoritesOnly) } },
                        label = { Text("Favorites (${favoriteIds.size})") },
                        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        modifier = Modifier.testTag("inv_cat_favorites"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LabColors.AmberAccent,
                            selectedLabelColor = LabColors.VoidBg
                        )
                    )
                    InventoryCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = !showFavoritesOnly && selectedInvCat == cat,
                            onClick = { onUpdateTabState { it.copy(showFavoritesOnly = false, selectedInvCat = cat) } },
                            label = { Text(cat.displayName) },
                            modifier = Modifier.testTag("inv_cat_${cat.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LabColors.AmberAccent,
                                selectedLabelColor = LabColors.VoidBg
                            )
                        )
                    }
                }

                if (recentIds.isNotEmpty() && query.isEmpty() && selectedInvCat == null && !showFavoritesOnly) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent:", style = MaterialTheme.typography.labelMedium, color = LabColors.TextMuted)
                        recentIds.take(6).forEach { key ->
                            val label = when {
                                key.startsWith("obj:") -> runCatching { ObjectType.valueOf(key.removePrefix("obj:")).displayName }.getOrNull()
                                key.startsWith("tool:") -> ToolCatalog.get(key.removePrefix("tool:"))?.displayName
                                key.startsWith("mat:") -> MaterialLibrary.get(key.removePrefix("mat:")).displayName
                                else -> null
                            }
                            if (label != null) {
                                Surface(
                                    color = LabColors.CardSurface,
                                    border = BorderStroke(1.dp, LabColors.BorderSubtle),
                                    shape = RoundedCornerShape(4.dp),
                                    onClick = {
                                        when {
                                            key.startsWith("obj:") -> runCatching { ObjectType.valueOf(key.removePrefix("obj:")) }.getOrNull()?.let(onSpawnObject)
                                            key.startsWith("tool:") -> onEquipTool(key.removePrefix("tool:"))
                                            key.startsWith("mat:") -> onSpawnMaterialBlock(MaterialLibrary.get(key.removePrefix("mat:")))
                                        }
                                    }
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelMedium, color = LabColors.CyanAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }

                if (browserMode == BrowserMode.MATERIALS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = materialSubFilter == null,
                            onClick = { onUpdateTabState { it.copy(materialSubFilter = null) } },
                            label = { Text("All (${MaterialLibrary.all.size})") }
                        )
                        MaterialCategory.entries.forEach { mc ->
                            FilterChip(
                                selected = materialSubFilter == mc,
                                onClick = { onUpdateTabState { it.copy(materialSubFilter = mc) } },
                                label = { Text(mc.displayName) }
                            )
                        }
                    }
                } else if (browserMode == BrowserMode.TOOLS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = toolSubFilter == null,
                            onClick = { onUpdateTabState { it.copy(toolSubFilter = null) } },
                            label = { Text("All (${ToolCatalog.all.size})") }
                        )
                        ToolCategory.entries.forEach { tc ->
                            FilterChip(
                                selected = toolSubFilter == tc,
                                onClick = { onUpdateTabState { it.copy(toolSubFilter = tc) } },
                                label = { Text(tc.displayName) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = LabColors.BorderSubtle)
                Spacer(Modifier.height(4.dp))

                when {
                    browserMode == BrowserMode.ALL_OBJECTS && query.isNotEmpty() -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 175.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredObjects, key = { "o_${it.name}" }) { obj ->
                                ObjectSpawnCard(obj, "obj:${obj.name}" in favoriteIds, { onToggleFavorite("obj:${obj.name}") }) {
                                    onSpawnObject(obj)
                                }
                            }
                            items(filteredTools, key = { "t_${it.id}" }) { tool ->
                                ToolCatalogCard(
                                    tool = tool,
                                    isFav = "tool:${tool.id}" in favoriteIds,
                                    onFav = { onToggleFavorite("tool:${tool.id}") },
                                    onEquip = { onEquipTool(tool.id) },
                                    onSpawn3d = { onSpawnToolObject(tool.id) }
                                )
                            }
                            items(filteredMaterials, key = { "m_${it.id}" }) { mat ->
                                MaterialCatalogCard(mat, "mat:${mat.id}" in favoriteIds, { onToggleFavorite("mat:${mat.id}") }) {
                                    onSpawnMaterialBlock(mat)
                                }
                            }
                        }
                    }

                    browserMode == BrowserMode.MATERIALS -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 170.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredMaterials, key = { it.id }) { mat ->
                                MaterialCatalogCard(mat, "mat:${mat.id}" in favoriteIds, { onToggleFavorite("mat:${mat.id}") }) {
                                    onSpawnMaterialBlock(mat)
                                }
                            }
                        }
                    }

                    browserMode == BrowserMode.TOOLS -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 210.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredTools, key = { it.id }) { tool ->
                                ToolCatalogCard(
                                    tool = tool,
                                    isFav = "tool:${tool.id}" in favoriteIds,
                                    onFav = { onToggleFavorite("tool:${tool.id}") },
                                    onEquip = { onEquipTool(tool.id) },
                                    onSpawn3d = { onSpawnToolObject(tool.id) }
                                )
                            }
                        }
                    }

                    browserMode == BrowserMode.FAVORITES -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 175.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredObjects, key = { "fo_${it.name}" }) { obj ->
                                ObjectSpawnCard(obj, true, { onToggleFavorite("obj:${obj.name}") }) { onSpawnObject(obj) }
                            }
                            items(filteredTools, key = { "ft_${it.id}" }) { tool ->
                                ToolCatalogCard(tool, true, { onToggleFavorite("tool:${tool.id}") }, { onEquipTool(tool.id) }, { onSpawnToolObject(tool.id) })
                            }
                            items(filteredMaterials, key = { "fm_${it.id}" }) { mat ->
                                MaterialCatalogCard(mat, true, { onToggleFavorite("mat:${mat.id}") }) { onSpawnMaterialBlock(mat) }
                            }
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 152.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredObjects, key = { it.name }) { obj ->
                                ObjectSpawnCard(obj, "obj:${obj.name}" in favoriteIds, { onToggleFavorite("obj:${obj.name}") }) {
                                    onSpawnObject(obj)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ObjectSpawnCard(
    obj: ObjectType,
    isFav: Boolean,
    onFav: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        color = LabColors.CardSurface,
        border = BorderStroke(1.dp, LabColors.BorderSubtle),
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("spawn_${obj.name.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(obj.inventoryCategory.displayName, style = MaterialTheme.typography.labelSmall, color = LabColors.CyanAccent, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (obj.electricalRole != ElectricalRole.NONE) {
                    Icon(Icons.Filled.ElectricBolt, contentDescription = "Electrical", tint = LabColors.AmberAccent, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onFav, modifier = Modifier.size(28.dp)) {
                    Icon(if (isFav) Icons.Filled.Star else Icons.Outlined.StarOutline, contentDescription = "Favorite", tint = if (isFav) LabColors.AmberAccent else LabColors.TextMuted, modifier = Modifier.size(16.dp))
                }
            }
            Text(obj.displayName, style = MaterialTheme.typography.titleMedium, color = LabColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            val ammoVariant = AmmoBoxVariant.fromId(obj.defaultAmmoVariantId())
            val subtitle = if (ammoVariant != null) {
                "+${ammoVariant.ammoAmount} rds • ${ammoVariant.caliberLabel}"
            } else {
                MaterialLibrary.get(obj.defaultMaterialId).displayName
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (ammoVariant != null) LabColors.AmberAccent else LabColors.TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MaterialCatalogCard(
    mat: MaterialSpec,
    isFav: Boolean,
    onFav: () -> Unit,
    onSpawnBlock: () -> Unit
) {
    val swatchColor = Color(mat.tintR.coerceIn(0f, 1f), mat.tintG.coerceIn(0f, 1f), mat.tintB.coerceIn(0f, 1f), 1f)
    Surface(
        color = LabColors.CardSurface,
        border = BorderStroke(1.dp, LabColors.BorderSubtle),
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSpawnBlock)
            .testTag("spawn_mat_${mat.id.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(12.dp).background(swatchColor, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(mat.category.displayName, style = MaterialTheme.typography.labelSmall, color = LabColors.CyanAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onFav, modifier = Modifier.size(28.dp)) {
                    Icon(if (isFav) Icons.Filled.Star else Icons.Outlined.StarOutline, contentDescription = "Favorite", tint = if (isFav) LabColors.AmberAccent else LabColors.TextMuted, modifier = Modifier.size(16.dp))
                }
            }
            Text(mat.displayName, style = MaterialTheme.typography.titleMedium, color = LabColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${mat.densityKgM3.toInt()} kg/m³ • Restitution ${mat.restitution} • ${if (mat.floatsInWater) "Floats" else "Sinks"}",
                style = MaterialTheme.typography.labelSmall,
                color = LabColors.TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ToolCatalogCard(
    tool: ToolDefinition,
    isFav: Boolean,
    onFav: () -> Unit,
    onEquip: () -> Unit,
    onSpawn3d: () -> Unit
) {
    Surface(
        color = LabColors.CardSurface,
        border = BorderStroke(1.dp, LabColors.BorderSubtle),
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Build, contentDescription = null, tint = LabColors.AmberAccent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(tool.category.displayName, style = MaterialTheme.typography.labelSmall, color = LabColors.CyanAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onFav, modifier = Modifier.size(24.dp)) {
                    Icon(if (isFav) Icons.Filled.Star else Icons.Outlined.StarOutline, contentDescription = "Favorite", tint = if (isFav) LabColors.AmberAccent else LabColors.TextMuted, modifier = Modifier.size(15.dp))
                }
            }
            Text(tool.displayName, style = MaterialTheme.typography.titleMedium, color = LabColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val firearmSpec = FirearmCatalog.byToolId(tool.id)
            if (firearmSpec != null) {
                val fireMode = when {
                    firearmSpec.automatic -> "AUTO"
                    firearmSpec.pellets > 1 -> "PUMP ${firearmSpec.pellets}x"
                    firearmSpec.adsFovDeg <= 38f -> "BOLT OPTIC"
                    else -> "SEMI"
                }
                Text(
                    text = "${firearmSpec.caliberLabel} • ${firearmSpec.magazineCapacity} rd • $fireMode",
                    style = MaterialTheme.typography.labelSmall,
                    color = LabColors.AmberAccent,
                    maxLines = 1
                )
            }
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = LabColors.TextSecondary,
                maxLines = if (firearmSpec != null) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onEquip,
                    modifier = Modifier.weight(1f).testTag("equip_tool_${tool.id.lowercase()}"),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LabColors.AmberAccent, contentColor = LabColors.VoidBg)
                ) {
                    Text("Equip", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onSpawn3d,
                    modifier = Modifier.weight(1f).testTag("spawn_tool_${tool.id.lowercase()}"),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Spawn 3D", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
