package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LabColors

private data class TutorialModule(
    val title: String,
    val label: String,
    val steps: List<String>
)

@Composable
fun TutorialScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var stepIndex by remember { mutableIntStateOf(0) }

    val modules = remember {
        listOf(
            TutorialModule(
                title = "Movement & Camera",
                label = "Section 1 of 4",
                steps = listOf(
                    "Touch Controls: Drag the virtual left joystick to walk or strafe, swipe the viewport to look, and use the Jump, Crouch, and Sprint buttons.",
                    "Keyboard & Mouse: Use WASD to move, Space to jump, C / Ctrl to crouch, Shift to sprint, and mouse look to aim.",
                    "Camera & Operator: Tap the camera button (or V) to switch First-Person, Third-Person, and Free Camera, and customize your 3D operator from the Main Menu."
                )
            ),
            TutorialModule(
                title = "Tools & Manipulation",
                label = "Section 2 of 4",
                steps = listOf(
                    "Grab (1): Aim at any dynamic object and tap Primary Action or Left-Click to lift it. Use Rotate or Mouse Wheel to orient and adjust reach.",
                    "Throw: While holding an object, tap Throw or Right-Click to launch it along your aim direction.",
                    "Impulse (2): Fires a directional force pulse to test structural stability, spin flywheels, or launch spheres.",
                    "Inspect (4): Target an object to resize it, swap among 105 materials, freeze it in mid-air, duplicate, or adjust physical properties."
                )
            ),
            TutorialModule(
                title = "Materials & Physics",
                label = "Section 3 of 4",
                steps = listOf(
                    "Every dynamic object computes mass, inertia tensor, friction, restitution, thermal transfer, and buoyancy directly from its assigned material.",
                    "Metals such as Steel, Copper, and Tungsten carry high density and structural rigidity; Vulcanized Rubber delivers high grip and rebound.",
                    "Brittle materials such as Tempered Glass and Ceramic can fracture under severe impact velocity, while light woods and foams float in the water basin."
                )
            ),
            TutorialModule(
                title = "Joints & Electrical Circuits",
                label = "Section 4 of 4",
                steps = listOf(
                    "Attach (3): Select Weld, Spring, Rope, or Hinge, tap Body A, then tap Body B or any static wall, gantry, or floor to anchor a constraint.",
                    "Detach: Select an attached object and press Detach in the target bar or Right-Click in Attach mode to remove its links.",
                    "Electrical Circuits: Connect Power Packs, Switches, Fuses, Lamps, and Motors with conductive links or inspect active laboratory panels."
                )
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.VoidBg.copy(alpha = 0.94f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp)
                .widthIn(max = 820.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("tutorial_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LabColors.TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Tutorial", style = MaterialTheme.typography.titleLarge, color = LabColors.TextPrimary)
                    Text("Controls, tools, materials, and simulation systems", style = MaterialTheme.typography.bodySmall, color = LabColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                modules.forEachIndexed { idx, mod ->
                    FilterChip(
                        selected = stepIndex == idx,
                        onClick = { stepIndex = idx },
                        label = { Text("${idx + 1}. ${mod.title.substringBefore(" ")}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LabColors.AmberAccent,
                            selectedLabelColor = LabColors.VoidBg
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val current = modules[stepIndex]
            Surface(
                color = LabColors.PanelSurface,
                border = BorderStroke(1.dp, LabColors.BorderSubtle),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(current.label, style = MaterialTheme.typography.labelMedium, color = LabColors.CyanAccent)
                    Text(current.title, style = MaterialTheme.typography.headlineMedium, color = LabColors.TextPrimary)
                    HorizontalDivider(color = LabColors.BorderSubtle)
                    current.steps.forEach { line ->
                        Surface(
                            color = LabColors.CardSurface,
                            border = BorderStroke(1.dp, LabColors.BorderSubtle),
                            shape = RoundedCornerShape(5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = LabColors.EmeraldOk)
                                Spacer(Modifier.width(10.dp))
                                Text(line, style = MaterialTheme.typography.bodyLarge, color = LabColors.TextPrimary)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { if (stepIndex > 0) stepIndex-- },
                    enabled = stepIndex > 0,
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LabColors.CardSurface, contentColor = LabColors.TextPrimary)
                ) { Text("Previous") }

                Button(
                    onClick = { if (stepIndex < modules.lastIndex) stepIndex++ else onBack() },
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LabColors.AmberAccent, contentColor = LabColors.VoidBg)
                ) { Text(if (stepIndex < modules.lastIndex) "Next Section" else "Done") }
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.VoidBg.copy(alpha = 0.94f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp)
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("about_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LabColors.TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text("About KineticLab 3D", style = MaterialTheme.typography.titleLarge, color = LabColors.TextPrimary)
            }

            Spacer(Modifier.height(14.dp))

            Surface(
                color = LabColors.PanelSurface,
                border = BorderStroke(1.dp, LabColors.BorderSubtle),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("KineticLab 3D", style = MaterialTheme.typography.headlineMedium, color = LabColors.TextPrimary)
                    Text(
                        "An interactive 3D rigid-body physics sandbox built with a dedicated OpenGL ES 3.0 rendering pipeline, a 60 Hz sequential-impulse solver, spatial audio synthesis, and persistent experiment serialization.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LabColors.TextSecondary
                    )
                    HorizontalDivider(color = LabColors.BorderSubtle)
                    SpecLine("Rendering Engine", "OpenGL ES 3.0 PBR shading, ACES tone mapping, 3x3 PCF shadow maps, normal/roughness/metallic textures, frustum culling, and 3-tier LOD.")
                    SpecLine("Physics Simulation", "60 Hz fixed-timestep sequential impulse solver, SAT OBB-OBB & sphere contacts, weld/spring/rope/hinge joints, buoyancy, fracture, and DC circuits.")
                    SpecLine("Environments", "Science Laboratory, Industrial Factory, Abandoned Warehouse, Construction Site, Testing Facility, Underground Maintenance, and Outdoor Testing Yard.")
                    SpecLine("Input & Persistence", "Touch joystick & gestures, keyboard/mouse controls with rebindable keys, JSON import/export, and multi-slot session saves.")
                }
            }
        }
    }
}

@Composable
private fun SpecLine(label: String, value: String) {
    Surface(color = LabColors.CardSurface, border = BorderStroke(1.dp, LabColors.BorderSubtle), shape = RoundedCornerShape(5.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = LabColors.AmberAccent)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = LabColors.TextSecondary)
        }
    }
}
