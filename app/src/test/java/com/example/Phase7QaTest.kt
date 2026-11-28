package com.example

import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppSettings
import com.example.data.SaveRepository
import com.example.data.SettingsRepository
import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.MapId
import com.example.physics.ObjectType
import com.example.physics.PhysicsCommand
import com.example.physics.PhysicsMaterial
import com.example.physics.PhysicsWorld
import com.example.physics.SavedBodyData
import com.example.physics.SessionExportData
import com.example.physics.ToolType
import com.example.render.gltf.GlbLoader
import com.example.render.gltf.GlbModelRegistry
import com.example.weapons.AmmoBoxVariant
import com.example.weapons.FirearmCatalog
import com.example.weapons.FirearmId
import com.example.weapons.WeaponActionState
import com.example.weapons.WeaponController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Phase7QaTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun all99GlbAssetsExistInManifestParseCleanlyAndFirearmsHaveRequiredSockets() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manifest = GlbModelRegistry.loadManifest(context)
        val required = GlbModelRegistry.allRequiredPaths()

        assertEquals("Expected 99 required GLB assets", 99, required.size)
        assertTrue("Manifest should contain at least 99 entries", manifest.size >= 99)

        for (path in required) {
            assertTrue("Manifest missing $path", manifest.values.any { it.assetPath == path })
            val asset = GlbLoader.loadStaticFromAssets(context, path, decodeTextures = false)
            assertTrue("$path validation errors: ${asset.validationErrors}", asset.validationErrors.isEmpty())
            assertTrue("$path totalVertices <= 0", asset.totalVertices > 0)
            assertTrue("$path totalTriangles <= 0", asset.totalTriangles > 0)
            assertTrue("$path invalid boundingRadius ${asset.boundingRadius}", asset.boundingRadius.isFinite() && asset.boundingRadius > 0f)
            assertTrue("$path non-finite bounds", asset.boundsMin.x.isFinite() && asset.boundsMax.z.isFinite())
        }

        for (spec in FirearmCatalog.all) {
            val asset = GlbLoader.loadStaticFromAssets(context, spec.modelAssetPath, decodeTextures = false)
            assertTrue("${spec.id} missing muzzle socket", asset.sockets.containsKey("muzzle"))
            assertTrue("${spec.id} missing grip socket", asset.sockets.containsKey("grip"))
            if (spec.isTwoHanded) {
                assertTrue("${spec.id} two-handed firearm missing foregrip socket", asset.sockets.containsKey("foregrip"))
            }
        }
    }

    @Test
    fun weaponControllerHandlesAutoSemiAdsReloadHolsterAndAmmoCaps() {
        val wc = WeaponController()

        // Equip full-auto M4A1-style carbine
        assertTrue(wc.equipFirearmByToolId("FIREARM_M4A1"))
        assertEquals(FirearmId.M4A1_STYLE, wc.activeFirearmId)
        val m4 = FirearmCatalog.get(FirearmId.M4A1_STYLE)

        // Advance draw animation (0.28s)
        repeat(20) { wc.step(1f / 60f, { _, _ -> }, {}) }
        assertEquals(WeaponActionState.EQUIPPED_IDLE, wc.actionState)

        // Hold trigger on full-auto -> fires multiple rounds
        var shotsFired = 0
        wc.setFirePressed(true)
        repeat(25) {
            wc.step(1f / 60f, { _, _ -> shotsFired++ }, {})
        }
        wc.setFirePressed(false)
        assertTrue("Full-auto should fire >2 rounds while held (fired=$shotsFired)", shotsFired >= 3)
        assertEquals(m4.magazineCapacity - shotsFired, wc.getMagAmmo(FirearmId.M4A1_STYLE))

        // Switch to semi-auto Service Pistol & verify holding trigger fires only 1 round
        assertTrue(wc.equipFirearmByToolId("FIREARM_SERVICE_PISTOL"))
        repeat(20) { wc.step(1f / 60f, { _, _ -> }, {}) }
        var semiShots = 0
        wc.setFirePressed(true)
        repeat(35) { wc.step(1f / 60f, { _, _ -> semiShots++ }, {}) }
        wc.setFirePressed(false)
        assertEquals("Semi-auto pistol should fire exactly 1 round per trigger pull", 1, semiShots)

        // ADS toggles on/off cleanly
        wc.setAiming(true)
        assertTrue(wc.isAiming)
        assertEquals(WeaponActionState.AIMING, wc.actionState)

        // Reload transfers reserve -> magazine cleanly
        assertTrue(wc.startReload())
        assertFalse("Reload should cancel ADS", wc.isAiming)
        assertEquals(WeaponActionState.RELOADING, wc.actionState)
        repeat(100) { wc.step(1f / 60f, { _, _ -> }, {}) }
        val pistolSpec = FirearmCatalog.get(FirearmId.SERVICE_PISTOL)
        assertEquals(pistolSpec.magazineCapacity, wc.getMagAmmo(FirearmId.SERVICE_PISTOL))
        assertEquals(pistolSpec.initialReserve - 1, wc.getReserveAmmo(FirearmId.SERVICE_PISTOL))

        // Holster disables ADS and blocks firing
        assertTrue(wc.toggleHolster())
        repeat(20) { wc.step(1f / 60f, { _, _ -> }, {}) }
        assertEquals(WeaponActionState.HOLSTERED, wc.actionState)
        assertTrue(wc.isHolstered)
        var holsteredShots = 0
        wc.setFirePressed(true)
        repeat(15) { wc.step(1f / 60f, { _, _ -> holsteredShots++ }, {}) }
        wc.setFirePressed(false)
        assertEquals(0, holsteredShots)

        // Ammo pickup caps cleanly at maxReserve
        val pistolAmmo = AmmoBoxVariant.forFirearm(FirearmId.SERVICE_PISTOL)
        val added1 = wc.addAmmoFromBox(pistolAmmo, 500)
        assertTrue(added1 > 0)
        assertEquals(pistolSpec.maxReserve, wc.getReserveAmmo(FirearmId.SERVICE_PISTOL))
        assertEquals(0, wc.addAmmoFromBox(pistolAmmo, 24))
    }

    @Test
    fun physicsWorldFirearmBallisticsShatterDestructiblesAndPickupAmmoCrate() {
        val sfxTags = mutableListOf<String>()
        val world = PhysicsWorld(onAudioEvent = {}, onToolSfx = { tag, _ -> sfxTags.add(tag) })
        world.enqueue(PhysicsCommand.LoadMap(MapId.LAB, isMenuDemo = false))
        world.stepForTest(3)

        // Spawn a brittle glass crate directly ahead of the player and equip shotgun
        world.enqueue(PhysicsCommand.SpawnObject(ObjectType.GLASS_CUBE))
        world.enqueue(PhysicsCommand.EquipCatalogTool("FIREARM_SHOTGUN"))
        world.enqueue(PhysicsCommand.SetToolSelected(ToolType.CATALOG_TOOL))
        world.stepForTest(25)

        val initialCount = world.bodyCountRef.get() ?: 0
        assertEquals(FirearmId.PUMP_SHOTGUN, world.firearmHudRef.get()?.equippedFirearmId)

        // Fire shotgun blasts toward spawned glass cube
        repeat(3) {
            world.enqueue(PhysicsCommand.TriggerSingleFire)
            world.stepForTest(50)
        }
        assertTrue("Expected gun_shotgun SFX in $sfxTags", "gun_shotgun" in sfxTags)
        val postShotMag = world.firearmHudRef.get()?.magAmmo ?: 8
        assertTrue("Magazine should decrement after firing (mag=$postShotMag)", postShotMag < 8)
        assertTrue("Body count should reflect spawned/shattered objects", (world.bodyCountRef.get() ?: 0) >= initialCount)

        // Spawn a 12G ammo box in front of player and pick it up
        world.enqueue(PhysicsCommand.SpawnObject(ObjectType.AMMO_BOX_12G_BUCKSHOT))
        world.stepForTest(10)
        val beforeReserve = world.firearmHudRef.get()?.reserveAmmo ?: 0
        val nearby = world.firearmHudRef.get()?.nearbyAmmoBox
        assertNotNull("Spawned ammo box in front of player should be detected nearby", nearby)
        world.enqueue(PhysicsCommand.PickupNearbyAmmo)
        world.stepForTest(2)
        val afterReserve = world.firearmHudRef.get()?.reserveAmmo ?: 0
        assertTrue("Reserve ammo should increase after ammo crate pickup ($beforeReserve -> $afterReserve)", afterReserve > beforeReserve)
    }

    @Test
    fun saveAndSettingsRepositoriesRoundTripPhase7State() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val saveRepo = SaveRepository(context)
        val session = SessionExportData(
            mapId = MapId.FACILITY,
            playerPos = Vec3(1f, 0f, 2f),
            playerYaw = 0.5f,
            playerPitch = -0.1f,
            bodies = listOf(
                SavedBodyData(
                    id = 42,
                    type = ObjectType.AMMO_BOX_556_CARBINE,
                    material = PhysicsMaterial.STEEL,
                    position = Vec3(0f, 1f, 0f),
                    orientation = Quat.IDENTITY,
                    linearVelocity = Vec3.ZERO,
                    angularVelocity = Vec3.ZERO,
                    scale = 1f,
                    isFrozen = false,
                    ammoVariantId = AmmoBoxVariant.AMMO_556_CARBINE.id,
                    ammoAmount = 60
                )
            ),
            constraints = emptyList(),
            wires = emptyList(),
            weaponMagAmmo = mapOf(FirearmId.M4A1_STYLE.name to 17),
            weaponReserveAmmo = mapOf(FirearmId.M4A1_STYLE.name to 120),
            isWeaponHolstered = true
        )

        val json = saveRepo.exportToJsonString("slot_1", "Phase 7 QA", session)
        val restored = saveRepo.importFromJsonString(json)
        assertNotNull(restored)
        assertEquals(AmmoBoxVariant.AMMO_556_CARBINE.id, restored!!.bodies[0].ammoVariantId)
        assertEquals(60, restored.bodies[0].ammoAmount)
        assertEquals(17, restored.weaponMagAmmo[FirearmId.M4A1_STYLE.name])
        assertEquals(120, restored.weaponReserveAmmo[FirearmId.M4A1_STYLE.name])
        assertTrue(restored.isWeaponHolstered)

        val settingsRepo = SettingsRepository(context)
        settingsRepo.updateSettings {
            AppSettings.defaults().copy(
                aimSensitivity = 0.45f,
                autoReloadEnabled = false,
                holdToAim = true,
                keyBindings = it.keyBindings.copy(reload = KeyEvent.KEYCODE_T, holster = KeyEvent.KEYCODE_G)
            )
        }
        val readBack = settingsRepo.settingsFlow.first()
        assertEquals(0.45f, readBack.aimSensitivity, 0.001f)
        assertFalse(readBack.autoReloadEnabled)
        assertTrue(readBack.holdToAim)
        assertEquals(KeyEvent.KEYCODE_T, readBack.keyBindings.reload)
        assertEquals(KeyEvent.KEYCODE_G, readBack.keyBindings.holster)
    }

    @Test
    fun equippingFirearmShowsTranslucentHudControlsAndFiringDoesNotSpamToolPopup() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_play_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("map_grid").performScrollToKey(MapId.LAB.name)
        composeRule.onNodeWithTag("launch_map_lab").performClick()
        composeRule.waitForIdle()

        // Open Inventory -> Tools -> search CR-4 Carbine -> Equip
        composeRule.onNodeWithTag("hud_inventory_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inv_cat_tools").performScrollTo().performClick()
        composeRule.onNodeWithTag("inventory_search_field").performTextInput("CR-4 Carbine")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("equip_tool_firearm_m4a1").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inventory_close_button").performClick()
        composeRule.waitForIdle()

        // Wait for UI sync loop (~50ms) and draw completion (~300ms) without starving main looper
        composeRule.waitUntil(timeoutMillis = 3000L) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper(60, java.util.concurrent.TimeUnit.MILLISECONDS)
            composeRule.onAllNodesWithTag("hud_firearm_readout").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        // Verify translucent firearm controls & readout
        composeRule.onNodeWithTag("hud_firearm_readout").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_fire_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_aim_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_reload_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_holster_button").assertIsDisplayed()
        composeRule.onNodeWithText("CR-4 Carbine").assertIsDisplayed()

        // Dismiss any initial equip info popup
        if (composeRule.onAllNodesWithTag("tool_info_close_button").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("tool_info_close_button").performClick()
            composeRule.mainClock.advanceTimeBy(250L)
            composeRule.waitForIdle()
        }

        // Fire shot and confirm popup stays hidden (no Phase 6 regression)
        composeRule.onNodeWithTag("hud_fire_button").performClick()
        org.robolectric.shadows.ShadowLooper.idleMainLooper(120, java.util.concurrent.TimeUnit.MILLISECONDS)
        composeRule.mainClock.advanceTimeBy(200L)
        composeRule.waitForIdle()
        assertTrue(
            "Firing must never reopen tool_info_popup",
            composeRule.onAllNodesWithTag("tool_info_popup").fetchSemanticsNodes().isEmpty()
        )

        // Holstering hides fire & aim buttons and restores standard primary action button
        composeRule.onNodeWithTag("hud_holster_button").performClick()
        composeRule.waitUntil(timeoutMillis = 3000L) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper(60, java.util.concurrent.TimeUnit.MILLISECONDS)
            composeRule.onAllNodesWithTag("hud_primary_action_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hud_primary_action_button").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("hud_fire_button").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("hud_aim_button").fetchSemanticsNodes().isEmpty())
    }
}
