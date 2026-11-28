package com.example

import com.example.physics.MapId
import com.example.render.MapPreviewRenderer
import com.example.world.EnvironmentValidator
import com.example.world.MapBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EnvironmentQaTest {

    @Test
    fun allSevenMapsPassSpatialGroundingOverlapAndWalkabilityChecks() {
        assertEquals(7, MapId.entries.size)
        assertTrue(MapId.UNDERGROUND_MAINTENANCE in MapId.entries)
        assertTrue(MapId.OUTDOOR_TESTING_YARD in MapId.entries)
        val failures = ArrayList<String>()
        for (mapId in MapId.entries) {
            val report = EnvironmentValidator.validate(mapId)
            if (!report.passed || report.reachableRatio < 0.999f) {
                failures.add(
                    "Map ${mapId.name} (reachable=${report.reachableRatio}):\n" +
                        report.issues.joinToString("\n") { "  - $it" }
                )
            }
        }
        assertTrue(
            "Spatial QA failures:\n${failures.joinToString("\n")}",
            failures.isEmpty()
        )
    }

    @Test
    fun menuDemoSceneHasValidFiniteGeometry() {
        val built = MapBuilder.buildMenuDemoScene()
        assertTrue("Menu demo scene should contain architectural statics", built.statics.size >= 5)
        assertTrue("Menu demo scene should contain dynamic demo bodies", built.dynamics.size >= 4)
        for (s in built.statics) {
            assertTrue("Menu static #${s.id} position must be finite", s.position.isFinite())
            assertTrue("Menu static #${s.id} extents must be finite", s.halfExtents.isFinite())
        }
    }

    @Test
    fun mapPreviewRendererProducesDistinctAuthentic16x9BitmapsForAllMaps() {
        val hashes = mutableSetOf<Int>()
        for (mapId in MapId.entries) {
            val bmp = MapPreviewRenderer.renderMapBitmap(mapId, width = 320, height = 180)
            assertEquals(320, bmp.width)
            assertEquals(180, bmp.height)

            val pixels = IntArray(320 * 180)
            bmp.getPixels(pixels, 0, 320, 0, 0, 320, 180)
            val hash = pixels.contentHashCode()
            assertTrue("Map ${mapId.name} preview should have distinct rendered pixels", hashes.add(hash))

            // Verify preview isn't solid color (contains scene variation)
            val distinctSample = pixels.filterIndexed { idx, _ -> idx % 97 == 0 }.toSet()
            assertTrue("Map ${mapId.name} preview must depict varied 3D geometry", distinctSample.size > 15)
        }
    }
}
