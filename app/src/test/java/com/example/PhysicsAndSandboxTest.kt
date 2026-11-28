package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SaveRepository
import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.CollisionDetector
import com.example.physics.ConstraintType
import com.example.physics.MapId
import com.example.physics.MeshId
import com.example.physics.ObjectType
import com.example.physics.PhysicsCommand
import com.example.physics.PhysicsConstraint
import com.example.physics.PhysicsMaterial
import com.example.physics.PhysicsWorld
import com.example.physics.RigidBody
import com.example.physics.SavedBodyData
import com.example.physics.SessionExportData
import com.example.render.ProceduralMeshGenerator
import com.example.render.ProceduralTextureGenerator
import com.example.world.MapBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PhysicsAndSandboxTest {

    @Test
    fun allFiveMapsBuildWithInteractiveObjects() {
        for (mapId in MapId.entries) {
            val built = MapBuilder.buildMap(mapId)
            assertTrue("Map $mapId should have static geometry", built.statics.isNotEmpty())
            assertTrue("Map $mapId should have dynamic objects", built.dynamics.isNotEmpty())
        }
    }

    @Test
    fun allProceduralMeshesAndTexturesGenerateValidBuffers() {
        val meshes = ProceduralMeshGenerator.buildAllMeshes()
        assertEquals(MeshId.entries.size, meshes.size)
        for ((id, asset) in meshes) {
            assertTrue("$id LOD0 vertices non-empty", asset.lod0.vertexCount > 0)
            assertTrue("$id LOD0 indices non-empty", asset.lod0.indexCount > 0)
            assertTrue("$id LOD2 <= LOD0", asset.lod2.indexCount <= asset.lod0.indexCount)
        }

        val textures = ProceduralTextureGenerator.generateAllMaterials()
        assertEquals(PhysicsMaterial.entries.size, textures.size)
        for ((mat, tex) in textures) {
            assertEquals("$mat albedo bytes", tex.size * tex.size * 4, tex.albedoRgba.remaining())
            assertEquals("$mat normal bytes", tex.size * tex.size * 4, tex.normalRgba.remaining())
            assertEquals("$mat mra bytes", tex.size * tex.size * 4, tex.mraRgba.remaining())
        }
    }

    @Test
    fun metalAndRubberMaterialsHaveDistinctMassAndRestitution() {
        val steel = RigidBody(1, ObjectType.STEEL_CRATE, PhysicsMaterial.STEEL, Vec3(0f, 3f, 0f))
        val rubber = RigidBody(2, ObjectType.STEEL_CRATE, PhysicsMaterial.RUBBER, Vec3(2f, 3f, 0f))
        assertTrue("Steel must be much heavier than rubber", steel.mass > rubber.mass * 5f)
        assertTrue("Rubber must be bouncier than steel", PhysicsMaterial.RUBBER.restitution > PhysicsMaterial.STEEL.restitution * 3f)
    }

    @Test
    fun physicsWorldLoadsMapsSpawnsAndStepsSafely() {
        val world = PhysicsWorld(onAudioEvent = {}, onToolSfx = { _, _ -> })
        for (mapId in MapId.entries) {
            world.enqueue(PhysicsCommand.LoadMap(mapId, isMenuDemo = false))
        }
        world.enqueue(PhysicsCommand.SpawnObject(ObjectType.STEEL_CRATE))
        world.enqueue(PhysicsCommand.SpawnObject(ObjectType.BOUNCY_SPHERE))
        world.start()
        Thread.sleep(450)
        val snap = world.snapshotRef.get()
        assertNotNull(snap)
        assertEquals(MapId.entries.last(), snap!!.mapId)
        assertTrue(snap.dynamicBodies.size >= 4)
        world.shutdown()
    }

    @Test
    fun obbSatCollisionAndRaycastWorkAccurately() {
        val a = RigidBody(1, ObjectType.STEEL_CRATE, PhysicsMaterial.STEEL, Vec3(0f, 0f, 0f))
        val b = RigidBody(2, ObjectType.STEEL_CRATE, PhysicsMaterial.STEEL, Vec3(0.8f, 0f, 0f))
        val manifold = CollisionDetector.collideDynamicPair(a, b)
        assertNotNull("Overlapping OBBs should produce contact manifold", manifold)
        assertTrue("Manifold must have contact points", manifold!!.contacts.isNotEmpty())

        val hit = CollisionDetector.raycastBodiesAndStatics(
            origin = Vec3(0f, 0f, 5f),
            dir = Vec3(0f, 0f, -1f),
            maxDistance = 10f,
            bodies = listOf(a),
            statics = emptyList()
        )
        assertNotNull(hit)
        assertEquals(1, hit!!.bodyId)
    }

    @Test
    fun saveRepositoryRoundTripsSessionWithConstraints() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SaveRepository(context)
        val export = SessionExportData(
            mapId = MapId.FACTORY,
            playerPos = Vec3(3.5f, 1.2f, -4.0f),
            playerYaw = 1.25f,
            playerPitch = -0.15f,
            bodies = listOf(
                SavedBodyData(
                    id = 101,
                    type = ObjectType.COPPER_GYRO,
                    material = PhysicsMaterial.COPPER,
                    position = Vec3(1f, 2f, 3f),
                    orientation = Quat.fromEuler(0.2f, 0.5f, 0.1f),
                    linearVelocity = Vec3(0.5f, -0.2f, 0f),
                    angularVelocity = Vec3(0f, 1.4f, 0f),
                    scale = 1.4f,
                    isFrozen = true
                )
            ),
            constraints = listOf(
                PhysicsConstraint(
                    id = 201,
                    type = ConstraintType.SPRING,
                    bodyAId = 101,
                    bodyBId = -1,
                    localAnchorA = Vec3(0f, 0.5f, 0f),
                    localAnchorB = Vec3(1f, 5f, 3f),
                    restLength = 2.5f
                )
            )
        )
        repo.saveSession("slot_1", "Factory Test", export)
        val loaded = repo.loadSession("slot_1")
        assertNotNull(loaded)
        assertEquals(MapId.FACTORY, loaded!!.mapId)
        assertEquals(1, loaded.bodies.size)
        assertEquals(1.4f, loaded.bodies[0].scale, 0.001f)
        assertTrue(loaded.bodies[0].isFrozen)
        assertEquals(1, loaded.constraints.size)
        assertEquals(ConstraintType.SPRING, loaded.constraints[0].type)
    }
}
