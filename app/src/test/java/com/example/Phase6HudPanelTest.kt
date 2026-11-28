package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import com.example.physics.ConstraintType
import com.example.physics.MapId
import com.example.physics.ToolType
import com.example.ui.hud.PanelVisibilityController
import com.example.ui.hud.PanelVisibilityState
import com.example.ui.hud.ToolPickerMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Phase6HudPanelTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun panelVisibilityControllerTransitionsAndAutoDismissLifecycleWorkCleanly() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val controller = PanelVisibilityController(
            scope = scope,
            transitionDurationMs = 150L,
            autoDismissDurationMs = 1000L
        )

        assertEquals(PanelVisibilityState.HIDDEN, controller.state.value.infoState)
        assertEquals(PanelVisibilityState.HIDDEN, controller.state.value.pickerState)

        // 1. Opening temporary tool info popup transitions HIDDEN -> OPENING -> VISIBLE -> CLOSING -> HIDDEN
        val hammerInfo = PanelVisibilityController.buildToolInfoData(ToolType.CATALOG_TOOL, "HAMMER", ConstraintType.WELD)
        controller.showToolInfo(hammerInfo)
        assertEquals(PanelVisibilityState.OPENING, controller.state.value.infoState)

        scope.advanceTimeBy(160L)
        scope.runCurrent()
        assertEquals(PanelVisibilityState.VISIBLE, controller.state.value.infoState)
        assertEquals("Hammer", controller.state.value.infoData?.title)

        // Repeated per-frame call with identical key does not restart/reopen
        controller.showToolInfo(hammerInfo, deduplicateSameKey = true)
        assertEquals(PanelVisibilityState.VISIBLE, controller.state.value.infoState)

        // Wait for auto-dismiss (1000ms + 150ms closing)
        scope.advanceTimeBy(1050L)
        scope.runCurrent()
        assertEquals(PanelVisibilityState.CLOSING, controller.state.value.infoState)

        scope.advanceTimeBy(160L)
        scope.runCurrent()
        assertEquals(PanelVisibilityState.HIDDEN, controller.state.value.infoState)

        // 2. Switching tools updates popup content & manual close dismisses immediately
        controller.showToolInfo(PanelVisibilityController.buildToolInfoData(ToolType.IMPULSE, "HAMMER", ConstraintType.WELD))
        scope.advanceTimeBy(160L)
        scope.runCurrent()
        assertEquals("Impulse", controller.state.value.infoData?.title)
        controller.dismissToolInfo()
        assertEquals(PanelVisibilityState.CLOSING, controller.state.value.infoState)
        scope.advanceTimeBy(160L)
        scope.runCurrent()
        assertEquals(PanelVisibilityState.HIDDEN, controller.state.value.infoState)

        // 3. Interactive tool picker NEVER auto-dismisses until closed explicitly
        controller.openToolPicker(ToolPickerMode.CATALOG_TOOLS)
        scope.advanceTimeBy(160L)
        scope.runCurrent()
        assertEquals(PanelVisibilityState.VISIBLE, controller.state.value.pickerState)
        scope.advanceTimeBy(10_000L)
        scope.runCurrent()
        assertEquals(PanelVisibilityState.VISIBLE, controller.state.value.pickerState)
        controller.closeToolPicker()
        scope.advanceTimeBy(160L)
        scope.runCurrent()
        assertEquals(PanelVisibilityState.HIDDEN, controller.state.value.pickerState)
    }

    @Test
    fun hudPreservesControlsRemovesPersistentBlackPanelAndSeparatesToolInfoFromPicker() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_play_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("map_grid").performScrollToKey(MapId.LAB.name)
        composeRule.onNodeWithTag("launch_map_lab").performClick()
        composeRule.waitForIdle()

        // Existing HUD preserved
        composeRule.onNodeWithTag("hud_pause_button").assertIsDisplayed()
        composeRule.onNodeWithTag("virtual_joystick").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_jump_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_primary_action_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_camera_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("tool_grab").assertIsDisplayed()
        composeRule.onNodeWithTag("tool_catalog_tool").assertIsDisplayed()

        // No contextual popup or picker is permanently shown on entry
        assertTrue(composeRule.onAllNodesWithTag("tool_info_popup").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("tool_selection_panel").fetchSemanticsNodes().isEmpty())

        // Selecting 5 Tool shows temporary compact Tool Info Popup (NOT the picker)
        composeRule.onNodeWithTag("tool_catalog_tool").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tool_info_popup").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("tool_selection_panel").fetchSemanticsNodes().isEmpty())

        // Manual close dismisses Tool Info Popup cleanly
        composeRule.onNodeWithTag("tool_info_close_button").performClick()
        composeRule.mainClock.advanceTimeBy(300L)
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodesWithTag("tool_info_popup").fetchSemanticsNodes().isEmpty())

        // Opening Tool Selection Panel explicitly displays picker, and selecting a tool closes picker
        composeRule.onNodeWithTag("hud_tool_picker_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tool_selection_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("picker_tool_electric_drill").performClick()
        composeRule.mainClock.advanceTimeBy(300L)
        composeRule.waitForIdle()

        assertFalse(
            "Picker must close automatically after selecting a tool",
            composeRule.onAllNodesWithTag("tool_selection_panel").fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onNodeWithTag("tool_info_popup").assertIsDisplayed()
    }
}
