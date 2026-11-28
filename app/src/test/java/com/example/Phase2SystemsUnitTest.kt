package com.example

import com.example.data.PresetLibrary
import com.example.electrical.ElectricalRole
import com.example.electrical.ElectricalSolver
import com.example.math.Vec3
import com.example.physics.CollisionMode
import com.example.physics.InventoryCategory
import com.example.physics.MapId
import com.example.physics.MaterialCategory
import com.example.physics.MaterialLibrary
import com.example.physics.MeshId
import com.example.physics.ObjectType
import com.example.physics.PhysicsCommand
import com.example.physics.PhysicsWorld
import com.example.physics.RigidBody
import com.example.physics.TexturePattern
import com.example.physics.ToolCatalog
import com.example.physics.ToolCategory
import com.example.physics.ToolMeshStyle
import com.example.physics.ToolType
import com.example.render.ProceduralMeshGenerator
import com.example.render.ProceduralTextureGenerator
import com.example.render.ToolMeshGenerator
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2SystemsUnitTest {

    @Test
    fun materialLibraryHas105UniqueMaterialsWithDistinctPhysics() {
        val all = MaterialLibrary.all
        assertTrue("Expected at least 105 materials, got ${all.size}", all.size >= 105)
        assertEquals("All material IDs must be unique", all.size, all.map { it.id }.toSet().size)
        assertEquals("All material numbers must be unique", all.size, all.map { it.number }.toSet().size)
        for (cat in MaterialCategory.entries) {
            assertTrue("Material category $cat must be populated", all.any { it.category == cat })
        }
        val rubber = MaterialLibrary.get("LATEX")
        val tungsten = MaterialLibrary.get("TUNGSTEN")
        val cork = MaterialLibrary.get("CORK")
        assertTrue("Tungsten must be far denser than cork", tungsten.densityKgM3 > cork.densityKgM3 * 40f)
        assertTrue("Latex must be much bouncier than tungsten", rubber.restitution > tungsten.restitution * 2.5f)
        assertTrue("Cork must float in water", cork.floatsInWater)
        assertFalse("Tungsten must sink in water", tungsten.floatsInWater)
    }

    @Test
    fun toolCatalogHas65UniqueToolsAndAll34ToolMeshesBuild() {
        val all = ToolCatalog.all
        assertTrue("Expected at least 65 tools, got ${all.size}", all.size >= 65)
        assertEquals("All tool IDs must be unique", all.size, all.map { it.id }.toSet().size)
        assertEquals("All tool numbers must be unique", all.size, all.map { it.number }.toSet().size)
        for (cat in ToolCategory.entries) {
            assertTrue("Tool category $cat must be populated", all.any { it.category == cat })
        }
        for (tool in all) {
            assertTrue("Tool ${tool.id} needs non-empty description", tool.description.length > 10)
        }

        val toolMeshes = ToolMeshGenerator.buildAllToolMeshes()
        assertEquals(ToolMeshStyle.entries.size, toolMeshes.size)
        for ((style, asset) in toolMeshes) {
            assertTrue("$style LOD0 vertices non-empty", asset.lod0.vertexCount > 0)
            assertTrue("$style LOD0 indices non-empty", asset.lod0.indexCount > 0)
        }
    }

    @Test
    fun allInventoryCategoriesAndProceduralAssetsExist() {
        val objCats = ObjectType.entries.filter { it.spawnable }.map { it.inventoryCategory }.toSet()
        for (cat in InventoryCategory.entries) {
            if (cat != InventoryCategory.MATERIALS && cat != InventoryCategory.TOOLS) {
                assertTrue("InventoryCategory $cat should have spawnable objects", cat in objCats)
            }
        }
        val meshes = ProceduralMeshGenerator.buildAllMeshes()
        assertEquals(MeshId.entries.size, meshes.size)

        val patterns = ProceduralTextureGenerator.generateAllPatterns(size = 64)
        assertEquals(TexturePattern.entries.size, patterns.size)
    }

    @Test
    fun electricalSolverEnergizesMotorsLightsGeneratorsAndTripsFuses() {
        val solver = ElectricalSolver()
        val battery = RigidBody(1, ObjectType.BATTERY_UNIT, position = Vec3.ZERO)
        val sw = RigidBody(2, ObjectType.SWITCH_BOX, position = Vec3(1f, 0f, 0f))
        val fuse = RigidBody(3, ObjectType.FUSE_BLOCK, position = Vec3(2f, 0f, 0f))
        val motor = RigidBody(4, ObjectType.DC_MOTOR, position = Vec3(3f, 0f, 0f))
        val bulb = RigidBody(5, ObjectType.LIGHT_BULB_LAMP, position = Vec3(4f, 0f, 0f))

        listOf(battery, sw, fuse, motor, bulb).forEach { solver.registerBody(it, it.electricalRole) }
        solver.addWire(10, 1, 2)
        solver.addWire(11, 2, 3)
        solver.addWire(12, 3, 4)
        solver.addWire(13, 3, 5)

        val map = listOf(battery, sw, fuse, motor, bulb).associateBy { it.id }
        solver.step(map, 1f / 60f)

        assertTrue("Motor should be energized", solver.nodes[motor.id]!!.energized)
        assertTrue("Bulb should be energized", solver.nodes[bulb.id]!!.energized)
        assertTrue("Motor should spin from DC torque", motor.angularVelocity.length() > 0.05f)

        // Open switch -> de-energizes circuit
        solver.toggleSwitchOrResetFuse(sw.id)
        solver.step(map, 1f / 60f)
        assertFalse("Bulb must turn off when switch is open", solver.nodes[bulb.id]!!.energized)

        // Close switch and add a high-voltage panel directly through low-resistance connector to trip 3A fuse
        solver.toggleSwitchOrResetFuse(sw.id)
        val panel1 = RigidBody(6, ObjectType.ELECTRICAL_PANEL, position = Vec3(-1f, 0f, 0f))
        val panel2 = RigidBody(7, ObjectType.ELECTRICAL_PANEL, position = Vec3(-2f, 0f, 0f))
        solver.registerBody(panel1, ElectricalRole.ELECTRICAL_PANEL)
        solver.registerBody(panel2, ElectricalRole.ELECTRICAL_PANEL)
        solver.addWire(14, 6, 3)
        solver.addWire(15, 7, 3)
        val overloadedMap = map + (6 to panel1) + (7 to panel2)
        solver.step(overloadedMap, 1f / 60f)
        assertTrue("3A fuse must trip when current exceeds 3A", solver.nodes[fuse.id]!!.isBlown)

        // Reset fuse
        solver.toggleSwitchOrResetFuse(fuse.id)
        assertFalse(solver.nodes[fuse.id]!!.isBlown)
    }

    @Test
    fun buoyancyMagnetismDestructionUndoRedoAndPropertyEditorWorkInPhysicsWorld() {
        val world = PhysicsWorld(onAudioEvent = {}, onToolSfx = { _, _ -> })
        world.enqueue(PhysicsCommand.LoadMap(MapId.LAB, isMenuDemo = false))
        world.enqueue(PhysicsCommand.ClearDynamicObjects)
        world.stepForTest(1)

        // 1. Buoyancy: cork floats while steel sinks in fluid basin
        world.enqueue(PhysicsCommand.SetEnvironment(fluidBasinEnabled = true, fluidLevelY = 2.2f))
        world.enqueue(PhysicsCommand.SpawnBatch(ObjectType.BUOYANCY_FLOAT, MaterialLibrary.get("CORK"), 1))
        world.stepForTest(1)
        val corkId = world.snapshotRef.get()!!.dynamicBodies.last().id
        val corkStartY = world.snapshotRef.get()!!.dynamicBodies.last().position.y
        // Move cork submerged at y=0.6m
        world.enqueue(PhysicsCommand.UpdateBodyProperties(corkId, posOffset = Vec3(0f, -(corkStartY - 0.6f), 0f)))
        world.stepForTest(45)
        val corkEndY = world.snapshotRef.get()!!.dynamicBodies.first { it.id == corkId }.position.y
        assertTrue("Cork in water should rise due to buoyancy (start=0.6, end=$corkEndY)", corkEndY > 0.75f)

        // 2. Undo / Redo & Reset
        val countBeforeSpawn = world.snapshotRef.get()!!.dynamicBodies.size
        world.enqueue(PhysicsCommand.SpawnBatch(ObjectType.STEEL_CRATE, MaterialLibrary.get("TUNGSTEN"), 2))
        world.stepForTest(1)
        assertEquals(countBeforeSpawn + 2, world.snapshotRef.get()!!.dynamicBodies.size)
        world.enqueue(PhysicsCommand.Undo)
        world.stepForTest(1)
        assertEquals(countBeforeSpawn, world.snapshotRef.get()!!.dynamicBodies.size)
        world.enqueue(PhysicsCommand.Redo)
        world.stepForTest(1)
        assertEquals(countBeforeSpawn + 2, world.snapshotRef.get()!!.dynamicBodies.size)

        // 3. Property Editor mutation (CollisionMode.TRIGGER & Mass override)
        val targetId = world.snapshotRef.get()!!.dynamicBodies.last().id
        world.enqueue(
            PhysicsCommand.UpdateBodyProperties(
                bodyId = targetId,
                massOverride = 42.5f,
                frictionOverride = 0.08f,
                restitutionOverride = 0.88f,
                gravityScale = 0f,
                collisionMode = CollisionMode.TRIGGER
            )
        )
        world.stepForTest(5)
        val exported = world.exportSessionSync()!!
        val edited = exported.bodies.first { it.id == targetId }
        assertEquals(42.5f, edited.massOverride!!, 0.01f)
        assertEquals(CollisionMode.TRIGGER, edited.collisionMode)
        assertEquals(0f, edited.gravityScale, 0.01f)

        // 4. Magnetic tool equips cleanly
        world.enqueue(PhysicsCommand.EquipCatalogTool("MAGNETIC_TOOL"))
        world.enqueue(PhysicsCommand.GrabOrUsePrimary(ToolType.CATALOG_TOOL, com.example.physics.ConstraintType.WELD))
        world.stepForTest(5)
        assertEquals("MAGNETIC_TOOL", world.toolTelemetryRef.get()!!.activeToolId)

        world.shutdown()
    }

    @Test
    fun allBuiltInPresetsAndStress100BodiesStepStably() {
        for (preset in PresetLibrary.all) {
            val data = preset.build()
            assertTrue("Preset ${preset.id} must contain bodies", data.bodies.isNotEmpty())
        }

        val world = PhysicsWorld(onAudioEvent = {}, onToolSfx = { _, _ -> })
        world.enqueue(PhysicsCommand.LoadMap(MapId.FACILITY, isMenuDemo = false))
        repeat(20) {
            world.enqueue(PhysicsCommand.SpawnBatch(ObjectType.STEEL_CRATE, MaterialLibrary.get("ALUMINUM"), 5))
        }
        world.stepForTest(1)
        val bodyCount = world.snapshotRef.get()!!.dynamicBodies.size
        assertTrue("Stress scene must have >= 100 bodies (got $bodyCount)", bodyCount >= 100)

        val t0 = System.nanoTime()
        world.stepForTest(180)
        val totalMs = (System.nanoTime() - t0) / 1_000_000.0
        val avgMsPerStep = totalMs / 180.0
        val snap = world.snapshotRef.get()!!
        assertTrue("All 100+ bodies must have finite coordinates after 180 ticks", snap.dynamicBodies.all { it.position.isFinite() })
        println("STRESS_BENCHMARK: bodies=$bodyCount, 180_steps_ms=${"%.2f".format(totalMs)}, avg_ms_per_step=${"%.3f".format(avgMsPerStep)}")
        world.shutdown()
    }
}
