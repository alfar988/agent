package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import com.example.physics.MapId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainMenuNavigatesToAllFiveMapsAndLaunchesSandboxHud() {
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("menu_play_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("menu_settings_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("menu_tutorial_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("menu_about_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("menu_exit_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("menu_play_button").performScrollTo()

        // Navigate to Map Selection
        composeRule.onNodeWithTag("menu_play_button").performClick()
        composeRule.waitForIdle()

        for (mapId in MapId.entries) {
            composeRule.onNodeWithTag("map_grid").performScrollToKey(mapId.name)
            composeRule.onNodeWithTag("map_card_${mapId.name.lowercase()}").assertExists()
        }
        composeRule.onNodeWithTag("map_grid").performScrollToKey(MapId.LAB.name)

        // Launch Laboratory sector
        composeRule.onNodeWithTag("launch_map_lab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("hud_pause_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_inventory_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_jump_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_primary_action_button").assertIsDisplayed()
        composeRule.onNodeWithTag("virtual_joystick").assertIsDisplayed()

        // Open Inventory & spawn a steel crate
        composeRule.onNodeWithTag("hud_inventory_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spawn_steel_crate").performClick()
        composeRule.waitForIdle()
    }
}
