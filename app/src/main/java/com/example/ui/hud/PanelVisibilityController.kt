package com.example.ui.hud

import com.example.physics.ConstraintType
import com.example.physics.ToolCatalog
import com.example.physics.ToolInteractionKind
import com.example.physics.ToolType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PanelVisibilityState {
    HIDDEN,
    OPENING,
    VISIBLE,
    CLOSING
}

enum class ToolPickerMode {
    CATALOG_TOOLS,
    CONSTRAINT_MODES
}

data class ToolInfoPopupData(
    val key: String,
    val title: String,
    val description: String,
    val toolType: ToolType,
    val catalogToolId: String? = null,
    val interactionKind: ToolInteractionKind? = null
)

data class HudPanelsState(
    val infoState: PanelVisibilityState = PanelVisibilityState.HIDDEN,
    val infoData: ToolInfoPopupData? = null,
    val pickerState: PanelVisibilityState = PanelVisibilityState.HIDDEN,
    val pickerMode: ToolPickerMode = ToolPickerMode.CATALOG_TOOLS
) {
    val isInfoRendered: Boolean
        get() = infoState == PanelVisibilityState.OPENING || infoState == PanelVisibilityState.VISIBLE

    val isPickerRendered: Boolean
        get() = pickerState == PanelVisibilityState.OPENING || pickerState == PanelVisibilityState.VISIBLE
}

/**
 * Independent lifecycle controller for temporary contextual info popups and
 * explicit interactive tool selection panels.
 *
 * - Tool info popups auto-dismiss after [autoDismissDurationMs], deduplicate identical
 *   keys so per-frame updates never reopen them, and can be dismissed manually anytime.
 * - Tool selection panels never auto-dismiss while the user is browsing/interacting;
 *   they close only on explicit close, dismissal, or tool selection.
 */
class PanelVisibilityController(
    private val scope: CoroutineScope,
    private val transitionDurationMs: Long = 150L,
    private val autoDismissDurationMs: Long = 2600L
) {
    private val _state = MutableStateFlow(HudPanelsState())
    val state: StateFlow<HudPanelsState> = _state.asStateFlow()

    private var infoTransitionJob: Job? = null
    private var infoDismissJob: Job? = null
    private var pickerTransitionJob: Job? = null

    private var lastShownInfoKey: String? = null
    private var isSuppressed: Boolean = false

    fun setSuppressed(suppressed: Boolean) {
        if (isSuppressed == suppressed) return
        isSuppressed = suppressed
        if (suppressed) {
            dismissToolInfo()
            closeToolPicker()
        }
    }

    fun showToolInfo(data: ToolInfoPopupData, deduplicateSameKey: Boolean = true) {
        if (isSuppressed) return
        val cur = _state.value
        val alreadyShowing = cur.infoState == PanelVisibilityState.OPENING || cur.infoState == PanelVisibilityState.VISIBLE
        if (deduplicateSameKey && alreadyShowing && lastShownInfoKey == data.key) {
            return
        }

        lastShownInfoKey = data.key
        infoDismissJob?.cancel()

        if (!alreadyShowing) {
            infoTransitionJob?.cancel()
            _state.update { it.copy(infoState = PanelVisibilityState.OPENING, infoData = data) }
            infoTransitionJob = scope.launch {
                if (transitionDurationMs > 0L) delay(transitionDurationMs)
                _state.update {
                    if (it.infoState == PanelVisibilityState.OPENING) it.copy(infoState = PanelVisibilityState.VISIBLE) else it
                }
            }
        } else {
            // Smoothly swap info content in-place without recreating or re-triggering open transition
            _state.update { it.copy(infoData = data) }
        }

        infoDismissJob = scope.launch {
            delay(transitionDurationMs.coerceAtLeast(0L) + autoDismissDurationMs.coerceAtLeast(100L))
            dismissToolInfo()
        }
    }

    fun dismissToolInfo() {
        infoDismissJob?.cancel()
        infoDismissJob = null
        val cur = _state.value.infoState
        if (cur == PanelVisibilityState.HIDDEN || cur == PanelVisibilityState.CLOSING) return

        infoTransitionJob?.cancel()
        _state.update { it.copy(infoState = PanelVisibilityState.CLOSING) }
        infoTransitionJob = scope.launch {
            if (transitionDurationMs > 0L) delay(transitionDurationMs)
            _state.update {
                if (it.infoState == PanelVisibilityState.CLOSING) {
                    lastShownInfoKey = null
                    it.copy(infoState = PanelVisibilityState.HIDDEN)
                } else it
            }
        }
    }

    fun openToolPicker(mode: ToolPickerMode) {
        if (isSuppressed) return
        val cur = _state.value
        if ((cur.pickerState == PanelVisibilityState.OPENING || cur.pickerState == PanelVisibilityState.VISIBLE) &&
            cur.pickerMode == mode
        ) {
            return
        }
        pickerTransitionJob?.cancel()
        _state.update { it.copy(pickerState = PanelVisibilityState.OPENING, pickerMode = mode) }
        pickerTransitionJob = scope.launch {
            if (transitionDurationMs > 0L) delay(transitionDurationMs)
            _state.update {
                if (it.pickerState == PanelVisibilityState.OPENING) it.copy(pickerState = PanelVisibilityState.VISIBLE) else it
            }
        }
    }

    fun toggleToolPicker(mode: ToolPickerMode) {
        val cur = _state.value
        val isOpen = cur.pickerState == PanelVisibilityState.OPENING || cur.pickerState == PanelVisibilityState.VISIBLE
        if (isOpen && cur.pickerMode == mode) {
            closeToolPicker()
        } else {
            openToolPicker(mode)
        }
    }

    fun closeToolPicker() {
        val cur = _state.value.pickerState
        if (cur == PanelVisibilityState.HIDDEN || cur == PanelVisibilityState.CLOSING) return
        pickerTransitionJob?.cancel()
        _state.update { it.copy(pickerState = PanelVisibilityState.CLOSING) }
        pickerTransitionJob = scope.launch {
            if (transitionDurationMs > 0L) delay(transitionDurationMs)
            _state.update {
                if (it.pickerState == PanelVisibilityState.CLOSING) it.copy(pickerState = PanelVisibilityState.HIDDEN) else it
            }
        }
    }

    companion object {
        fun buildToolInfoData(
            tool: ToolType,
            activeCatalogToolId: String,
            attachConstraintType: ConstraintType
        ): ToolInfoPopupData {
            return when (tool) {
                ToolType.GRAB -> ToolInfoPopupData(
                    key = "tool:GRAB",
                    title = "Grab",
                    description = "Lift, rotate, or throw dynamic objects.",
                    toolType = tool
                )
                ToolType.IMPULSE -> ToolInfoPopupData(
                    key = "tool:IMPULSE",
                    title = "Impulse",
                    description = "Apply directional force impulse at target.",
                    toolType = tool
                )
                ToolType.CONSTRAINT -> ToolInfoPopupData(
                    key = "tool:CONSTRAINT:${attachConstraintType.name}",
                    title = "Attach • ${attachConstraintType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    description = when (attachConstraintType) {
                        ConstraintType.WELD -> "Rigidly lock two objects together."
                        ConstraintType.SPRING -> "Connect objects with elastic suspension."
                        ConstraintType.ROPE -> "Tether objects with flexible tension line."
                        ConstraintType.HINGE -> "Join objects along a rotational pivot."
                    },
                    toolType = tool
                )
                ToolType.INSPECT -> ToolInfoPopupData(
                    key = "tool:INSPECT",
                    title = "Inspect",
                    description = "View and edit targeted object properties.",
                    toolType = tool
                )
                ToolType.CATALOG_TOOL -> {
                    val def = ToolCatalog.get(activeCatalogToolId)
                    val name = def?.displayName ?: "Tool"
                    val desc = summarizeShort(def?.description ?: "Specialized sandbox equipment.")
                    ToolInfoPopupData(
                        key = "catalog:${activeCatalogToolId}",
                        title = name,
                        description = desc,
                        toolType = tool,
                        catalogToolId = activeCatalogToolId,
                        interactionKind = def?.interactionKind
                    )
                }
            }
        }

        private fun summarizeShort(raw: String, maxChars: Int = 56): String {
            val trimmed = raw.trim().replace("\n", " ")
            return if (trimmed.length <= maxChars) trimmed else trimmed.take(maxChars - 1).trimEnd() + "…"
        }
    }
}
