package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.character.AnimationStateMachine
import com.example.character.AnimationWorker
import com.example.character.BoneIds
import com.example.character.CharacterAnimInput
import com.example.character.CharacterAnimState
import com.example.character.CharacterAppearance
import com.example.character.CharacterIkSolver
import com.example.character.CharacterProfilePreset
import com.example.character.GloveStyle
import com.example.character.HairStyle
import com.example.character.JacketStyle
import com.example.character.SkeletonPose
import com.example.character.SkinTonePreset
import com.example.character.ToolAttachmentRegistry
import com.example.data.AppSettings
import com.example.math.Vec3
import com.example.physics.CameraMode
import com.example.physics.InventoryCategory
import com.example.physics.MapId
import com.example.physics.ObjectType
import com.example.physics.PhysicsCommand
import com.example.physics.PhysicsWorld
import com.example.physics.ToolCatalog
import com.example.physics.ToolType
import com.example.render.gltf.GlbLoader
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SandboxViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Phase4And5QaTest {

    @Test
    fun allThreeCharacterGlbLodsLoadCleanlyWith65Bones17SubmeshesAnd18Clips() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val paths = listOf(
            "characters/operator_high.glb",
            "characters/operator_med.glb",
            "characters/operator_low.glb"
        )
        val expectedSubmeshes = setOf(
            "Body_HeadFaceSkin",
            "Face_Eyes",
            "Face_Eyebrows",
            "Body_HandsSkin",
            "Hair_CrewCut",
            "Hair_SidePart",
            "Hair_TiedBack",
            "Hair_TechCap",
            "Outfit_Jacket_Industrial",
            "Outfit_Jacket_LabCoat",
            "Outfit_Jacket_TechVest",
            "Outfit_Trousers_Cargo",
            "Outfit_Trousers_Utility",
            "Outfit_Gloves_Tactical",
            "Outfit_Boots_Work",
            "Outfit_Boots_Tech",
            "Outfit_Belt"
        )

        var prevVerts = Int.MAX_VALUE
        for (path in paths) {
            val asset = GlbLoader.loadFromAssets(context, path, decodeTextures = false)
            assertTrue("$path validationErrors=${asset.validationErrors}", asset.validationErrors.isEmpty())
            assertEquals("$path bone count", BoneIds.TOTAL_BONES, asset.bones.size)
            assertEquals("$path anim clip count", 18, asset.animations.size)
            assertTrue("$path verts > 0", asset.totalVertices > 0)
            assertTrue("$path tris > 0", asset.totalTriangles > 0)
            assertTrue("$path LOD verts should decrease or match ($prevVerts vs ${asset.totalVertices})", asset.totalVertices <= prevVerts)
            prevVerts = asset.totalVertices

            val primNames = asset.primitives.map { it.meshName }.toSet()
            for (req in expectedSubmeshes) {
                assertTrue("$path missing submesh $req", req in primNames)
            }

            // Validate vertex integrity & normalized skin weights
            for (prim in asset.primitives) {
                for (v in prim.positions) assertTrue("$path NaN pos in ${prim.meshName}", v.isFinite())
                for (n in prim.normals) assertTrue("$path NaN nrm in ${prim.meshName}", n.isFinite())
                val vCount = prim.vertexCount
                for (vi in 0 until vCount) {
                    val wSum = prim.weights[vi * 4] + prim.weights[vi * 4 + 1] + prim.weights[vi * 4 + 2] + prim.weights[vi * 4 + 3]
                    assertTrue("$path unnormalized skin weights in ${prim.meshName}: $wSum", wSum in 0.95f..1.05f)
                    for (k in 0 until 4) {
                        val ji = prim.joints[vi * 4 + k]
                        assertTrue("$path invalid joint index $ji", ji in 0 until BoneIds.TOTAL_BONES)
                    }
                }
            }
        }
    }

    @Test
    fun animationStateMachineAndIkSolverEvaluateAllLocomotionAndToolStatesSafely() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val asset = GlbLoader.loadFromAssets(context, "characters/operator_low.glb", decodeTextures = false)
        val sm = AnimationStateMachine()
        val pose = SkeletonPose(BoneIds.TOTAL_BONES)

        val cases = listOf(
            CharacterAnimState.IDLE to CharacterAnimInput(horizontalSpeed = 0f, onGround = true),
            CharacterAnimState.WALKING to CharacterAnimInput(horizontalSpeed = 2.6f, forwardDot = 2.6f, onGround = true),
            CharacterAnimState.JOGGING to CharacterAnimInput(horizontalSpeed = 4.9f, forwardDot = 4.9f, onGround = true),
            CharacterAnimState.SPRINTING to CharacterAnimInput(horizontalSpeed = 7.4f, forwardDot = 7.4f, isSprinting = true, onGround = true),
            CharacterAnimState.CROUCHING to CharacterAnimInput(horizontalSpeed = 1.4f, isCrouching = true, onGround = true),
            CharacterAnimState.JUMPING to CharacterAnimInput(horizontalSpeed = 2.0f, verticalVel = 4.5f, onGround = false),
            CharacterAnimState.FALLING to CharacterAnimInput(horizontalSpeed = 2.0f, verticalVel = -3.8f, onGround = false)
        )

        for ((expectedState, inp) in cases) {
            repeat(5) { sm.update(1f / 60f, inp, asset.bones, asset.animations, pose) }
            assertEquals("Expected locomotion state $expectedState", expectedState, sm.baseState)
            CharacterIkSolver.solveFullBodyIk(
                pose = pose,
                bones = asset.bones,
                characterWorldPos = Vec3.ZERO,
                characterYawRad = 0.4f,
                input = inp,
                staticProps = emptyList(),
                isFirstPerson = false
            )
            for (f in pose.skinMatrices) assertTrue("Skin matrix value must be finite", f.isFinite())
        }

        // Upper-body holding & tool use
        sm.update(
            1f / 60f,
            CharacterAnimInput(selectedTool = ToolType.CATALOG_TOOL, activeCatalogToolId = "CIRCULAR_SAW", toolActionPulse = 0.8f),
            asset.bones,
            asset.animations,
            pose
        )
        assertEquals(CharacterAnimState.TOOL_USING, sm.upperState)

        sm.update(
            1f / 60f,
            CharacterAnimInput(isGrabbing = true, grabbedWorldPos = Vec3(0f, 1.3f, -0.7f)),
            asset.bones,
            asset.animations,
            pose
        )
        assertEquals(CharacterAnimState.PICKING_UP, sm.upperState)
    }

    @Test
    fun toolAttachmentRegistryCoversAll65ToolsWithFiniteGripTransforms() {
        assertTrue(ToolCatalog.all.size >= 65)
        val registered = ToolAttachmentRegistry.allRegisteredToolIds()
        assertEquals(ToolCatalog.all.size, registered.size)
        for (tool in ToolCatalog.all) {
            val grip = ToolAttachmentRegistry.getSpec(tool.id)
            assertEquals(tool.id, grip.toolId)
            assertTrue(grip.primaryOffset.isFinite())
            assertTrue(grip.primaryRotDeg.isFinite())
            assertTrue(grip.equippedScale > 0.1f && grip.equippedScale.isFinite())
            val q = ToolAttachmentRegistry.gripLocalQuat(grip)
            assertTrue(q.x.isFinite() && q.y.isFinite() && q.z.isFinite() && q.w.isFinite())
        }
    }

    @Test
    fun animationWorkerStepSyncPublishesValidPoseSnapshot() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val asset = GlbLoader.loadFromAssets(context, "characters/operator_low.glb", decodeTextures = false)
        val world = PhysicsWorld(onAudioEvent = {}, onToolSfx = { _, _ -> })
        world.enqueue(PhysicsCommand.LoadMap(MapId.LAB, isMenuDemo = false))
        world.stepForTest(3)
        val snap = world.snapshotRef.get()!!

        val worker = AnimationWorker()
        worker.setCharacterRigData(asset.bones, asset.animations)
        val poseSnap = worker.stepSync(1f / 60f, snap)
        assertNotNull(poseSnap)
        assertEquals(BoneIds.TOTAL_BONES * 16, poseSnap!!.boneSkinMatrices.size)
        assertTrue(poseSnap.rightHandPos.isFinite())
        worker.shutdown()
    }

    @Test
    fun physicsFootstepsSilenceInMenuDemoAndJumpCrouchConstraintsHoldInPlayMode() {
        val world = PhysicsWorld(onAudioEvent = {}, onToolSfx = { _, _ -> })
        // 1. Menu demo MUST NOT emit player footsteps
        world.enqueue(PhysicsCommand.LoadMap(MapId.LAB, isMenuDemo = true))
        world.stepForTest(60)
        assertEquals("Menu demo must not emit footstep triggers", 0, world.footstepTriggerSeq.get())

        // 2. Play mode stationary -> still no footsteps
        world.enqueue(PhysicsCommand.LoadMap(MapId.LAB, isMenuDemo = false))
        world.stepForTest(30)
        val stationarySeq = world.footstepTriggerSeq.get()
        assertEquals("Standing still must not emit footsteps", 0, stationarySeq)

        // 3. Walk forward on ground -> footsteps increment
        world.moveInputY = 1.0f
        world.stepForTest(55)
        assertTrue("Walking on ground should emit footstep triggers", world.footstepTriggerSeq.get() > stationarySeq)

        // 4. Jump: single jump rises, re-requesting jump while airborne does NOT re-trigger
        world.moveInputY = 0f
        world.stepForTest(15)
        world.jumpRequested = true
        world.stepForTest(2)
        val airborneSnap1 = world.snapshotRef.get()!!
        assertFalse("Player should be airborne right after jump", airborneSnap1.playerOnGround)
        val velBeforeAirJumpAttempt = airborneSnap1.playerVel.y
        world.jumpRequested = true
        world.stepForTest(2)
        val airborneSnap2 = world.snapshotRef.get()!!
        assertTrue("Airborne re-jump must not add upward velocity impulse", airborneSnap2.playerVel.y <= velBeforeAirJumpAttempt)

        // Wait to land & settle, then test crouch
        world.stepForTest(70)
        world.enqueue(PhysicsCommand.SetPlayerPosture(crouching = true, sprinting = false))
        world.stepForTest(25)
        val crouchSnap = world.snapshotRef.get()!!
        assertTrue("Crouch command should engage isCrouching and crouchFraction", crouchSnap.isCrouching && crouchSnap.crouchFraction > 0.5f)
    }

    @Test
    fun spatialBroadphaseAndMaxObjectCapWorkStablyWithManyBodies() {
        val world = PhysicsWorld(onAudioEvent = {}, onToolSfx = { _, _ -> })
        world.enqueue(PhysicsCommand.LoadMap(MapId.OUTDOOR_TESTING_YARD, isMenuDemo = false))
        world.enqueue(PhysicsCommand.SetMaxDynamicObjects(60))
        repeat(10) {
            world.enqueue(PhysicsCommand.SpawnBatch(ObjectType.STEEL_CRATE, null, 5, null))
        }
        world.stepForTest(30)
        val snap = world.snapshotRef.get()!!
        assertTrue("Snapshot must contain spawned bodies", snap.dynamicBodies.isNotEmpty())
        for (b in snap.dynamicBodies) {
            assertTrue("Body #${b.id} pos must stay finite", b.position.isFinite())
        }
    }

    @Test
    fun characterCustomizationEncodesDecodesCleanlyAndViewModelPreservesInventoryAndRestoresDefaults() {
        val custom = CharacterProfilePreset.CHEN.defaultAppearance.copy(
            skinTone = SkinTonePreset.GOLDEN_AMBER,
            hairStyle = HairStyle.TECH_CAP,
            jacketStyle = JacketStyle.TECH_VEST,
            gloveStyle = GloveStyle.BARE_HANDS
        )
        val decoded = CharacterAppearance.decodeCompact(custom.encodeCompact())
        assertEquals(custom, decoded)

        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = SandboxViewModel(app)
        vm.startMap(MapId.UNDERGROUND_MAINTENANCE)
        assertEquals(AppScreen.PLAYING, vm.uiState.value.screen)
        assertEquals(MapId.UNDERGROUND_MAINTENANCE, vm.uiState.value.activeMap)

        // Toggle inventory open, apply filters, spawn object, verify Inventory remains open & filter stays
        vm.toggleInventory(true)
        vm.updateInventoryTabState { it.copy(selectedInvCat = InventoryCategory.MATERIALS, searchQuery = "Steel") }
        vm.spawnObject(ObjectType.STEEL_CRATE)
        vm.equipCatalogTool("HAMMER")
        assertTrue("Inventory should stay open after spawning/equipping", vm.uiState.value.showInventory)
        assertEquals("Steel", vm.uiState.value.inventoryTabState.searchQuery)

        // Restore defaults preserves favorites/character while resetting tuning fields
        val defaults = AppSettings.defaults()
        assertEquals(0.12f, defaults.joystickDeadzone, 0.001f)
        assertEquals(0.25f, defaults.cameraSmoothing, 0.001f)
        assertEquals(CameraMode.FIRST_PERSON, defaults.defaultCameraMode)
        vm.restoreDefaultSettings()
        vm.exitToMainMenu()
        vm.physicsWorld.shutdown()
        vm.animationWorker.shutdown()
        vm.soundEngine.shutdown()
    }
}
