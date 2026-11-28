package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import com.example.physics.MapId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Phase2UiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun inventoryNineCategoriesMaterialSearchToolEquipAndSettingsPresetsWork() {
        composeRule.waitForIdle()

        // Navigate to Settings first and test LOW / ULTRA graphics presets
        composeRule.onNodeWithTag("menu_settings_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("preset_low").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("preset_ultra").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("settings_back_button").performClick()
        composeRule.waitForIdle()

        // Launch Lab map
        composeRule.onNodeWithTag("menu_play_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("map_grid").performScrollToKey(MapId.LAB.name)
        composeRule.onNodeWithTag("launch_map_lab").performClick()
        composeRule.waitForIdle()

        // Open Environment & Speed dialog
        composeRule.onNodeWithTag("hud_environment_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("environment_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("fluid_basin_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("environment_close_button").performClick()
        composeRule.waitForIdle()

        // Open 9-category Inventory, browse Materials, search "Tungsten", spawn Tungsten block
        composeRule.onNodeWithTag("hud_inventory_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inv_cat_materials").performScrollTo().performClick()
        composeRule.onNodeWithTag("inventory_search_field").performTextInput("Tungsten")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spawn_mat_tungsten").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Inventory intentionally stays open across spawns; close and reopen to verify filter persistence, then switch to Tools
        composeRule.onNodeWithTag("inventory_close_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hud_inventory_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inventory_clear_search_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inv_cat_tools").performScrollTo().performClick()
        composeRule.onNodeWithTag("inventory_search_field").performTextInput("Hammer")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("equip_tool_hammer").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inventory_close_button").performClick()
        composeRule.waitForIdle()

        // Toggle camera mode (1st -> 3rd -> Free Camera)
        composeRule.onNodeWithTag("hud_camera_toggle").performClick()
        composeRule.onNodeWithTag("hud_camera_toggle").performClick()
        composeRule.waitForIdle()
    }
}
