package com.example.world

import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.MapId
import com.example.physics.MountType
import com.example.physics.RigidBody
import com.example.physics.StaticProp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class SceneQaReport(
    val mapId: MapId,
    val staticCount: Int,
    val dynamicCount: Int,
    val issues: List<String>,
    val reachableRatio: Float
) {
    val passed: Boolean get() = issues.isEmpty()
}

/**
 * Automated geometry, grounding, overlap, mounting, and walkability QA validator
 * for all 5 reconstructed environments.
 */
object EnvironmentValidator {

    fun validate(mapId: MapId, navCheckpoints: List<Vec3> = defaultWaypoints(mapId)): SceneQaReport {
        val built = MapBuilder.buildMap(mapId)
        val issues = ArrayList<String>()

        // 1. Finite bounds & normalized rotation check on all statics & dynamics
        for (s in built.statics) {
            if (!s.position.isFinite() || !s.halfExtents.isFinite() ||
                s.halfExtents.x <= 0.01f || s.halfExtents.y <= 0.01f || s.halfExtents.z <= 0.01f
            ) {
                issues.add("Static #${s.id} (${s.meshId}) has invalid extents/position")
            }
            val qLen = kotlin.math.sqrt(
                s.orientation.x * s.orientation.x + s.orientation.y * s.orientation.y +
                    s.orientation.z * s.orientation.z + s.orientation.w * s.orientation.w
            )
            if (abs(qLen - 1f) > 0.02f) {
                issues.add("Static #${s.id} (${s.meshId}) has unnormalized rotation")
            }
        }
        for (d in built.dynamics) {
            if (!d.position.isFinite() || d.scale <= 0.05f) {
                issues.add("Dynamic #${d.id} (${d.type}) has invalid position/scale")
            }
        }

        // 2. Vertical grounding & mounting checks for StaticProps
        val collidableStatics = built.statics.filter { it.isCollider }
        for (s in built.statics) {
            val sHe = EnvironmentPlacer.rotatedHalfExtents(s.halfExtents, s.orientation)
            val sMinY = s.position.y - sHe.y
            val sMaxY = s.position.y + sHe.y
            when (s.mountType) {
                MountType.FLOOR -> {
                    if (abs(sMinY - 0f) > 0.04f) {
                        issues.add("FLOOR static #${s.id} (${s.meshId} '${s.label}') minY=$sMinY != 0")
                    }
                }
                MountType.SURFACE -> {
                    val hasSupport = collidableStatics.any { sup ->
                        if (sup.id == s.id) return@any false
                        val supHe = EnvironmentPlacer.rotatedHalfExtents(sup.halfExtents, sup.orientation)
                        val supMaxY = sup.position.y + supHe.y
                        abs(sMinY - supMaxY) <= 0.045f &&
                            abs(s.position.x - sup.position.x) <= supHe.x + sHe.x * 0.5f &&
                            abs(s.position.z - sup.position.z) <= supHe.z + sHe.z * 0.5f
                    }
                    if (!hasSupport) {
                        issues.add("SURFACE static #${s.id} (${s.meshId}) floating at minY=$sMinY")
                    }
                }
                MountType.WALL -> {
                    val nearWall = collidableStatics.any { wall ->
                        if (wall.id == s.id || wall.mountType != MountType.STRUCTURAL) return@any false
                        val wHe = EnvironmentPlacer.rotatedHalfExtents(wall.halfExtents, wall.orientation)
                        val yOverlap = min(sMaxY, wall.position.y + wHe.y) - max(sMinY, wall.position.y - wHe.y)
                        if (yOverlap < 0.1f) return@any false
                        val dx = abs(s.position.x - wall.position.x) - (sHe.x + wHe.x)
                        val dz = abs(s.position.z - wall.position.z) - (sHe.z + wHe.z)
                        val touchX = abs(dx) <= 0.06f && abs(s.position.z - wall.position.z) <= wHe.z + sHe.z
                        val touchZ = abs(dz) <= 0.06f && abs(s.position.x - wall.position.x) <= wHe.x + sHe.x
                        touchX || touchZ
                    }
                    if (!nearWall) {
                        issues.add("WALL static #${s.id} (${s.meshId}) is detached from wall")
                    }
                }
                MountType.CEILING -> {
                    val hasCeiling = built.statics.any { c ->
                        if (c.id == s.id || c.mountType != MountType.STRUCTURAL) return@any false
                        val cHe = EnvironmentPlacer.rotatedHalfExtents(c.halfExtents, c.orientation)
                        val cMinY = c.position.y - cHe.y
                        abs(sMaxY - cMinY) <= 0.06f &&
                            abs(s.position.x - c.position.x) <= cHe.x + 0.1f &&
                            abs(s.position.z - c.position.z) <= cHe.z + 0.1f
                    }
                    if (!hasCeiling) {
                        issues.add("CEILING static #${s.id} (${s.meshId}) detached at maxY=$sMaxY")
                    }
                }
                MountType.STRUCTURAL -> Unit
            }
        }

        // 3. Static furniture/equipment overlap checks (exclude structural framing/walls/ramps)
        val equipmentStatics = built.statics.filter {
            it.isCollider && it.isVisible && (it.mountType == MountType.FLOOR || it.mountType == MountType.SURFACE || it.mountType == MountType.WALL)
        }
        for (i in equipmentStatics.indices) {
            val a = equipmentStatics[i]
            val aHe = EnvironmentPlacer.rotatedHalfExtents(a.halfExtents, a.orientation)
            for (j in i + 1 until equipmentStatics.size) {
                val b = equipmentStatics[j]
                val bHe = EnvironmentPlacer.rotatedHalfExtents(b.halfExtents, b.orientation)
                if (aabbOverlap(a.position, aHe, b.position, bHe, margin = -0.04f)) {
                    issues.add("Equipment static overlap: #${a.id} (${a.meshId}) & #${b.id} (${b.meshId})")
                }
            }
            // Check against structural walls/pillars (excluding floor slabs below)
            for (w in collidableStatics) {
                if (w.mountType != MountType.STRUCTURAL) continue
                val wHe = EnvironmentPlacer.rotatedHalfExtents(w.halfExtents, w.orientation)
                if (w.position.y + wHe.y <= 0.05f) continue // ground floor slab
                if (aabbOverlap(a.position, aHe, w.position, wHe, margin = -0.05f)) {
                    issues.add("Equipment #${a.id} (${a.meshId}) penetrates structural #${w.id} (${w.meshId})")
                }
            }
        }

        // 4. Dynamic body grounding & overlap check (skip rope/spring/hinge constrained bodies)
        val constrainedIds = built.constraints.map { it.bodyAId }.toSet()
        for (i in built.dynamics.indices) {
            val d = built.dynamics[i]
            val dHe = EnvironmentPlacer.rotatedHalfExtents(d.effectiveHalfExtents, d.orientation)
            val dMinY = d.position.y - dHe.y
            val dMaxY = d.position.y + dHe.y

            if (d.id !in constrainedIds && !d.isFrozen) {
                val supportedByStatic = collidableStatics.any { s ->
                    val sHe = EnvironmentPlacer.rotatedHalfExtents(s.halfExtents, s.orientation)
                    val sTop = s.position.y + sHe.y
                    abs(dMinY - sTop) <= 0.05f &&
                        abs(d.position.x - s.position.x) <= sHe.x + dHe.x * 0.4f &&
                        abs(d.position.z - s.position.z) <= sHe.z + dHe.z * 0.4f
                }
                val supportedByDynamic = built.dynamics.any { other ->
                    if (other.id == d.id) return@any false
                    val oHe = EnvironmentPlacer.rotatedHalfExtents(other.effectiveHalfExtents, other.orientation)
                    val oTop = other.position.y + oHe.y
                    abs(dMinY - oTop) <= 0.05f &&
                        abs(d.position.x - other.position.x) <= oHe.x + dHe.x * 0.4f &&
                        abs(d.position.z - other.position.z) <= oHe.z + dHe.z * 0.4f
                }
                if (!supportedByStatic && !supportedByDynamic) {
                    issues.add("Dynamic #${d.id} (${d.type}) floating at minY=$dMinY")
                }
            }

            // Penetration with non-floor collidable statics
            for (s in collidableStatics) {
                val sHe = EnvironmentPlacer.rotatedHalfExtents(s.halfExtents, s.orientation)
                if (s.position.y + sHe.y <= 0.05f) {
                    if (dMinY < -0.05f) {
                        issues.add("Dynamic #${d.id} (${d.type}) sinks into floor minY=$dMinY")
                    }
                    continue
                }
                // Skip tilted ramps since AABB overestimates rotated ramp box
                if (abs(s.orientation.x) > 0.05f || abs(s.orientation.z) > 0.05f) continue
                if (aabbOverlap(d.position, dHe, s.position, sHe, margin = -0.05f)) {
                    issues.add("Dynamic #${d.id} (${d.type}) penetrates static #${s.id} (${s.meshId})")
                }
            }

            // Penetration with other dynamics
            for (j in i + 1 until built.dynamics.size) {
                val o = built.dynamics[j]
                val oHe = EnvironmentPlacer.rotatedHalfExtents(o.effectiveHalfExtents, o.orientation)
                if (aabbOverlap(d.position, dHe, o.position, oHe, margin = -0.05f)) {
                    issues.add("Dynamic overlap: #${d.id} (${d.type}) & #${o.id} (${o.type})")
                }
            }
            if (dMaxY < 0f) {
                issues.add("Dynamic #${d.id} below ground")
            }
        }

        // 5. Grid BFS room/zone walkability check at pedestrian waist height (0.30..1.75m)
        val reachableRatio = checkWalkableReachability(built, navCheckpoints, issues)

        return SceneQaReport(
            mapId = mapId,
            staticCount = built.statics.size,
            dynamicCount = built.dynamics.size,
            issues = issues,
            reachableRatio = reachableRatio
        )
    }

    private fun checkWalkableReachability(
        built: BuiltMap,
        checkpoints: List<Vec3>,
        issues: MutableList<String>
    ): Float {
        if (checkpoints.isEmpty()) return 1f
        val step = 0.5f
        val gridHalf = 25f
        val gridSize = ((gridHalf * 2f) / step).toInt() + 1

        fun toCell(x: Float, z: Float): Pair<Int, Int> {
            val ix = ((x + gridHalf) / step).toInt().coerceIn(0, gridSize - 1)
            val iz = ((z + gridHalf) / step).toInt().coerceIn(0, gridSize - 1)
            return ix to iz
        }

        // Precompute blocked cells for vertical obstacles intersecting [0.35m .. 1.75m]
        val blocked = BooleanArray(gridSize * gridSize)
        val obstacles = built.statics.filter {
            it.isCollider && it.isVisible && abs(it.orientation.x) < 0.05f && abs(it.orientation.z) < 0.05f
        }
        val pad = 0.25f
        for (obs in obstacles) {
            val he = EnvironmentPlacer.rotatedHalfExtents(obs.halfExtents, obs.orientation)
            val minY = obs.position.y - he.y
            val maxY = obs.position.y + he.y
            if (maxY < 0.35f || minY > 1.75f) continue
            val ix0 = (((obs.position.x - he.x - pad) + gridHalf) / step).toInt().coerceIn(0, gridSize - 1)
            val ix1 = (((obs.position.x + he.x + pad) + gridHalf) / step).toInt().coerceIn(0, gridSize - 1)
            val iz0 = (((obs.position.z - he.z - pad) + gridHalf) / step).toInt().coerceIn(0, gridSize - 1)
            val iz1 = (((obs.position.z + he.z + pad) + gridHalf) / step).toInt().coerceIn(0, gridSize - 1)
            for (ix in ix0..ix1) for (iz in iz0..iz1) {
                blocked[iz * gridSize + ix] = true
            }
        }

        val (sx, sz) = toCell(built.spawnPos.x, built.spawnPos.z)
        if (blocked[sz * gridSize + sx]) {
            issues.add("Player spawn (${built.spawnPos.x}, ${built.spawnPos.z}) is blocked")
            return 0f
        }

        val visited = BooleanArray(gridSize * gridSize)
        val qx = ArrayDeque<Int>()
        val qz = ArrayDeque<Int>()
        visited[sz * gridSize + sx] = true
        qx.addLast(sx); qz.addLast(sz)

        val dirs = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        while (qx.isNotEmpty()) {
            val cx = qx.removeFirst()
            val cz = qz.removeFirst()
            for ((dx, dz) in dirs) {
                val nx = cx + dx
                val nz = cz + dz
                if (nx in 0 until gridSize && nz in 0 until gridSize) {
                    val idx = nz * gridSize + nx
                    if (!visited[idx] && !blocked[idx]) {
                        visited[idx] = true
                        qx.addLast(nx); qz.addLast(nz)
                    }
                }
            }
        }

        var reached = 0
        for (cp in checkpoints) {
            val (cx, cz) = toCell(cp.x, cp.z)
            if (visited[cz * gridSize + cx]) {
                reached++
            } else {
                issues.add("Room/zone waypoint (${cp.x}, ${cp.z}) is unreachable from spawn")
            }
        }
        return reached.toFloat() / checkpoints.size
    }

    private fun aabbOverlap(pA: Vec3, heA: Vec3, pB: Vec3, heB: Vec3, margin: Float): Boolean {
        val ox = (heA.x + heB.x + margin) - abs(pA.x - pB.x)
        val oy = (heA.y + heB.y + margin) - abs(pA.y - pB.y)
        val oz = (heA.z + heB.z + margin) - abs(pA.z - pB.z)
        return ox > 0f && oy > 0f && oz > 0f
    }

    fun defaultWaypoints(mapId: MapId): List<Vec3> = when (mapId) {
        MapId.LAB -> listOf(
            Vec3(0f, 0f, 13f),    // Reception / entrance
            Vec3(0f, 0f, 0f),     // Central testing bay
            Vec3(-13f, 0f, -6f),  // Prep room
            Vec3(-13f, 0f, 6f),   // Storage room
            Vec3(12f, 0f, -9f),   // Observation room
            Vec3(10.8f, 0f, 6f)   // Electrical control room
        )
        MapId.FACTORY -> listOf(
            Vec3(0f, 0f, 14f),    // Central entry
            Vec3(0f, 0f, 0f),     // Main conveyor floor
            Vec3(-13f, 0f, -6f),  // CNC & press machinery bay
            Vec3(-11.8f, 0f, 9f), // Maintenance bay
            Vec3(13f, 0f, 9f),    // Storage area
            Vec3(13f, 0f, -11f)   // Control room
        )
        MapId.WAREHOUSE -> listOf(
            Vec3(0f, 0f, 14f),    // Loading bay
            Vec3(0f, 0f, 0f),     // Main storage aisle
            Vec3(-13f, 0f, -12f), // Corner office
            Vec3(13f, 0f, 8f),    // East shipping bay
            Vec3(0f, 0f, -10f)    // North racking aisle
        )
        MapId.CONSTRUCTION -> listOf(
            Vec3(0f, 0f, 16f),    // Entry yard
            Vec3(-8f, 0f, -8f),   // Concrete building ground bay
            Vec3(12f, 0f, -2.5f), // Scaffolding zone
            Vec3(-12f, 0f, 9f),   // Material staging
            Vec3(5f, 0f, 1f)      // Wrecking crane / masonry target
        )
        MapId.FACILITY -> listOf(
            Vec3(0f, 0f, 15f),    // Facility entry
            Vec3(0f, 0f, 0f),     // Central proving hall
            Vec3(-14f, 0f, -13f), // Under mezzanine
            Vec3(13f, 0f, -12f),  // East observation gallery
            Vec3(11.5f, 0f, 10f)  // Equipment storage bay
        )
        MapId.UNDERGROUND_MAINTENANCE -> listOf(
            Vec3(0f, 0f, 12f),     // Hall spawn
            Vec3(0f, 0f, 1.5f),    // Central maintenance floor
            Vec3(-14.5f, 0f, -13f),// NW Equipment storage bay
            Vec3(-15.5f, 0f, 10.5f),// SW Electrical control room
            Vec3(14f, 0f, -12.5f), // NE Mechanical workshop
            Vec3(14f, 0f, 0f),     // East utility corridor
            Vec3(14.5f, 0f, 12.5f),// SE Pump room
            Vec3(-4.5f, 0f, -13.5f),// Service elevator approach
            Vec3(4.8f, 0f, -15f)   // Observation booth
        )
        MapId.OUTDOOR_TESTING_YARD -> listOf(
            Vec3(0f, 0f, 15.5f),   // Entry apron
            Vec3(0f, 0f, 0f),      // Open central test yard
            Vec3(-11.5f, 0f, -11.5f),// Covered workshop canopy
            Vec3(12.5f, 0f, -12f), // Storage container yard
            Vec3(-9.5f, 0f, 3f),   // Mechanical test rig & ramp
            Vec3(11f, 0f, 14f),    // South-East loading bay
            Vec3(15.5f, 0f, 8.5f), // Watch platform stair approach
            Vec3(-11.5f, 0f, 14f)  // Equipment parking zone
        )
    }
}
