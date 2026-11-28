package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.SaveSlotSummary
import com.example.ui.theme.LabColors

@Composable
fun MainMenuScreen(
    autosaveSlot: SaveSlotSummary?,
    onPlayClick: () -> Unit,
    onContinueAutosave: () -> Unit,
    onCharacterClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTutorialClick: () -> Unit,
    onAboutClick: () -> Unit,
    onExitClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        LabColors.VoidBg.copy(alpha = 0.86f),
                        LabColors.VoidBg.copy(alpha = 0.42f),
                        Color.Transparent
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 18.dp)
        ) {
            val isCompactWidth = maxWidth < 540.dp

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = if (isCompactWidth) 360.dp else 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand Title Block (Clean PC-simulation aesthetic, no decorative pill badges)
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically { -18 }
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(28.dp)
                                    .background(LabColors.AmberAccent, RoundedCornerShape(1.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "KineticLab 3D",
                                style = if (isCompactWidth) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                                color = LabColors.TextPrimary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "3D Physics Simulation Sandbox",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LabColors.TextSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = LabColors.BorderSubtle, thickness = 1.dp)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Launcher Action Column
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically { 20 }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPlayClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("menu_play_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LabColors.AmberAccent,
                                contentColor = LabColors.VoidBg
                            ),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Play", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        if (autosaveSlot?.mapId != null) {
                            OutlinedButton(
                                onClick = onContinueAutosave,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("menu_continue_button"),
                                border = BorderStroke(1.dp, LabColors.BorderStrong),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = LabColors.PanelSurface.copy(alpha = 0.88f),
                                    contentColor = LabColors.TextPrimary
                                ),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Icon(Icons.Filled.Restore, contentDescription = "Resume", tint = LabColors.CyanAccent, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Resume ${autosaveSlot.mapId.displayName}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        MenuLauncherButton(
                            label = "Character",
                            icon = Icons.Filled.Person,
                            tag = "menu_character_button",
                            accentTint = LabColors.CyanAccent,
                            onClick = onCharacterClick
                        )
                        MenuLauncherButton(
                            label = "Settings",
                            icon = Icons.Filled.Settings,
                            tag = "menu_settings_button",
                            onClick = onSettingsClick
                        )
                        MenuLauncherButton(
                            label = "Tutorial",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            tag = "menu_tutorial_button",
                            onClick = onTutorialClick
                        )
                        MenuLauncherButton(
                            label = "About",
                            icon = Icons.Filled.Info,
                            tag = "menu_about_button",
                            onClick = onAboutClick
                        )

                        Spacer(Modifier.height(4.dp))

                        MenuLauncherButton(
                            label = "Exit",
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            tag = "menu_exit_button",
                            accentTint = LabColors.TextMuted,
                            onClick = onExitClick
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "7 Environments • 105 Materials • 65 Tools",
                    style = MaterialTheme.typography.labelMedium,
                    color = LabColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun MenuLauncherButton(
    label: String,
    icon: ImageVector,
    tag: String,
    accentTint: Color = LabColors.TextSecondary,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(tag),
        border = BorderStroke(1.dp, LabColors.BorderSubtle),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = LabColors.PanelSurface.copy(alpha = 0.82f),
            contentColor = LabColors.TextPrimary
        ),
        shape = RoundedCornerShape(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(icon, contentDescription = label, tint = accentTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}
