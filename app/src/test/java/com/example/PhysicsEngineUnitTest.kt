package com.example

import com.example.data.QuatJson
import com.example.data.SavedBodyJson
import com.example.data.SavedConstraintJson
import com.example.data.SavedSessionJson
import com.example.data.Vec3Json
import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.CollisionDetector
import com.example.physics.ConstraintType
import com.example.physics.MapId
import com.example.physics.MeshId
import com.example.physics.ObjectType
import com.example.physics.PhysicsCommand
import com.example.physics.PhysicsMaterial
import com.example.physics.PhysicsWorld
import com.example.physics.RigidBody
import com.example.physics.ToolType
import com.example.render.ProceduralMeshGenerator
import com.example.render.ProceduralTextureGenerator
import com.example.world.MapBuilder
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicsEngineUnitTest {

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
    fun physicsWorldLoadsMapsSpawnsMovesAndInteracts() {
        val world = PhysicsWorld(onAudioEvent = {}, onToolSfx = { _, _ -> })
        for (mapId in MapId.entries) {
            world.enqueue(PhysicsCommand.LoadMap(mapId, isMenuDemo = false))
        }
        world.start()
        Thread.sleep(150)

        val initialSnap = world.snapshotRef.get()!!
        val initialPos = initialSnap.playerPos
        val initialBodies = initialSnap.dynamicBodies.size

        // Spawn objects & move player forward
        world.enqueue(PhysicsCommand.SpawnObject(ObjectType.STEEL_CRATE))
        world.enqueue(PhysicsCommand.SpawnObject(ObjectType.BOUNCY_SPHERE))
        world.moveInputY = 1f
        world.jumpRequested = true
        Thread.sleep(250)
        world.moveInputY = 0f

        val afterSnap = world.snapshotRef.get()!!
        assertEquals(MapId.entries.last(), afterSnap.mapId)
        assertEquals(initialBodies + 2, afterSnap.dynamicBodies.size)
        assertTrue("Player should have moved", (afterSnap.playerPos - initialPos).length() > 0.2f)
        assertTrue("All bodies must remain finite", afterSnap.dynamicBodies.all { it.position.isFinite() })

        // Test primary action (grab) & secondary (throw)
        world.enqueue(PhysicsCommand.GrabOrUsePrimary(ToolType.GRAB, ConstraintType.WELD))
        Thread.sleep(60)
        world.enqueue(PhysicsCommand.ThrowOrSecondary(ToolType.GRAB, 1.0f))
        Thread.sleep(80)

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
    fun moshiSaveSerializationRoundTripsSessionWithConstraints() {
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(SavedSessionJson::class.java)

        val session = SavedSessionJson(
            slotId = "slot_1",
            label = "Slot 1",
            mapId = MapId.FACTORY.name,
            savedAtMs = 1764000000000L,
            playerPos = Vec3Json(2.5f, 0.2f, -3.5f),
            playerYaw = 0.85f,
            playerPitch = -0.12f,
            bodies = listOf(
                SavedBodyJson(
                    id = 101,
                    type = ObjectType.COPPER_GYRO.name,
                    material = PhysicsMaterial.COPPER.name,
                    pos = Vec3Json(1f, 2f, 3f),
                    rot = QuatJson.from(Quat.fromEuler(0.2f, 0.4f, 0.1f)),
                    vel = Vec3Json(0.5f, 0f, -0.2f),
                    angVel = Vec3Json(0f, 1.5f, 0f),
                    scale = 1.6f,
                    isFrozen = true
                )
            ),
            constraints = listOf(
                SavedConstraintJson(
                    id = 201,
                    type = ConstraintType.SPRING.name,
                    bodyAId = 101,
                    bodyBId = -1,
                    anchorA = Vec3Json(0f, 0.5f, 0f),
                    anchorB = Vec3Json(1f, 5f, 3f),
                    restLength = 2.5f,
                    stiffness = 180f,
                    damping = 12f,
                    hingeAxis = Vec3Json(0f, 1f, 0f),
                    relRot = QuatJson.from(Quat.IDENTITY)
                )
            )
        )

        val json = adapter.toJson(session)
        val restored = adapter.fromJson(json)
        assertNotNull(restored)
        assertEquals(MapId.FACTORY.name, restored!!.mapId)
        assertEquals(1, restored.bodies.size)
        assertEquals(1.6f, restored.bodies[0].scale, 0.001f)
        assertTrue(restored.bodies[0].isFrozen)
        assertEquals(1, restored.constraints.size)
        assertEquals(ConstraintType.SPRING.name, restored.constraints[0].type)
    }
}
