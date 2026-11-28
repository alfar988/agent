package com.example.electrical

import com.example.physics.RigidBody
import kotlin.math.abs

enum class ElectricalRole(
    val displayName: String,
    val baseResistanceOhms: Float,
    val sourceVoltage: Float,
    val maxCurrentAmps: Float
) {
    NONE("Passive", 0f, 0f, 0f),
    BATTERY("12V DC Battery", 0.5f, 12.0f, 25.0f),
    GENERATOR("Kinetic Generator", 0.8f, 0.0f, 30.0f),
    SWITCH("Toggle Switch", 0.05f, 0f, 40.0f),
    LIGHT_BULB("Incandescent Bulb", 6.0f, 0f, 4.0f),
    LED("Indicator LED", 12.0f, 0f, 2.0f),
    MOTOR("DC Drive Motor", 3.0f, 0f, 10.0f),
    FUSE("Safety Fuse (3A)", 0.1f, 0f, 3.0f),
    CONNECTOR("Circuit Connector", 0.05f, 0f, 40.0f),
    ELECTRICAL_PANEL("Distribution Panel", 0.2f, 24.0f, 35.0f)
}

data class WireConnection(
    val id: Int,
    val bodyAId: Int,
    val bodyBId: Int
)

data class ElectricalNodeState(
    val bodyId: Int,
    val role: ElectricalRole,
    var switchClosed: Boolean = true,
    var isBlown: Boolean = false,
    var energized: Boolean = false,
    var voltage: Float = 0f,
    var currentAmps: Float = 0f,
    var overloaded: Boolean = false
)

class ElectricalSolver {

    val nodes = HashMap<Int, ElectricalNodeState>()
    val wires = ArrayList<WireConnection>()

    fun clear() {
        nodes.clear()
        wires.clear()
    }

    fun registerBody(body: RigidBody, role: ElectricalRole) {
        if (role != ElectricalRole.NONE && !nodes.containsKey(body.id)) {
            nodes[body.id] = ElectricalNodeState(body.id, role)
        }
    }

    fun removeBody(bodyId: Int) {
        nodes.remove(bodyId)
        wires.removeAll { it.bodyAId == bodyId || it.bodyBId == bodyId }
    }

    fun addWire(id: Int, bodyAId: Int, bodyBId: Int) {
        if (bodyAId == bodyBId) return
        if (wires.any { (it.bodyAId == bodyAId && it.bodyBId == bodyBId) || (it.bodyAId == bodyBId && it.bodyBId == bodyAId) }) return
        wires.add(WireConnection(id, bodyAId, bodyBId))
    }

    fun toggleSwitchOrResetFuse(bodyId: Int): String? {
        val node = nodes[bodyId] ?: return null
        return when (node.role) {
            ElectricalRole.SWITCH -> {
                node.switchClosed = !node.switchClosed
                if (node.switchClosed) "Switch CLOSED (ON)" else "Switch OPEN (OFF)"
            }
            ElectricalRole.FUSE -> {
                node.isBlown = false
                node.overloaded = false
                "Safety Fuse RESET"
            }
            ElectricalRole.ELECTRICAL_PANEL -> {
                node.switchClosed = !node.switchClosed
                if (node.switchClosed) "Panel Breaker ON" else "Panel Breaker OFF"
            }
            else -> null
        }
    }

    /**
     * Evaluates DC circuit connected components, computes voltage & current, drives Motors & Lights,
     * generates power from spinning Generators, and trips Fuses on overload.
     */
    fun step(bodiesById: Map<Int, RigidBody>, dt: Float) {
        // Reset outputs before solve
        for (n in nodes.values) {
            n.energized = false
            n.voltage = 0f
            n.currentAmps = 0f
            n.overloaded = n.isBlown
        }

        // Build adjacency of conducting nodes
        val adj = HashMap<Int, MutableList<Int>>()
        for (w in wires) {
            val na = nodes[w.bodyAId]
            val nb = nodes[w.bodyBId]
            if (na == null || nb == null) continue
            if (!isConducting(na) || !isConducting(nb)) continue
            adj.getOrPut(w.bodyAId) { ArrayList(4) }.add(w.bodyBId)
            adj.getOrPut(w.bodyBId) { ArrayList(4) }.add(w.bodyAId)
        }

        val visited = HashSet<Int>()
        for (startId in nodes.keys) {
            if (startId in visited) continue
            val startNode = nodes[startId] ?: continue
            if (!isConducting(startNode)) {
                visited.add(startId)
                continue
            }

            // BFS connected conducting island
            val island = ArrayList<ElectricalNodeState>()
            val queue = ArrayDeque<Int>()
            queue.add(startId)
            visited.add(startId)
            while (queue.isNotEmpty()) {
                val cur = queue.removeFirst()
                nodes[cur]?.let { island.add(it) }
                for (nxt in adj[cur].orEmpty()) {
                    if (nxt !in visited) {
                        visited.add(nxt)
                        queue.add(nxt)
                    }
                }
            }

            // Sum available voltage sources in island (Batteries, Panels, spinning Generators)
            var totalVoltage = 0f
            var totalResistance = 0.25f
            for (n in island) {
                totalResistance += n.role.baseResistanceOhms
                when (n.role) {
                    ElectricalRole.BATTERY -> totalVoltage += n.role.sourceVoltage
                    ElectricalRole.ELECTRICAL_PANEL -> if (n.switchClosed) totalVoltage += n.role.sourceVoltage
                    ElectricalRole.GENERATOR -> {
                        val rb = bodiesById[n.bodyId]
                        if (rb != null) {
                            totalVoltage += (rb.angularVelocity.length() * 4.5f).coerceAtMost(36f)
                        }
                    }
                    else -> Unit
                }
            }

            if (totalVoltage > 0.2f) {
                val current = totalVoltage / totalResistance.coerceAtLeast(0.15f)
                var tripped = false
                for (n in island) {
                    if (n.role == ElectricalRole.FUSE && current > n.role.maxCurrentAmps) {
                        n.isBlown = true
                        n.overloaded = true
                        tripped = true
                    }
                }
                if (!tripped) {
                    for (n in island) {
                        n.energized = true
                        n.voltage = totalVoltage
                        n.currentAmps = current
                        n.overloaded = current > n.role.maxCurrentAmps
                        if (n.role == ElectricalRole.MOTOR) {
                            val rb = bodiesById[n.bodyId]
                            if (rb != null && !rb.isFrozen) {
                                rb.wake()
                                val torque = (current * 14f).coerceAtMost(95f)
                                val axis = rb.orientation.rotate(com.example.math.Vec3.UP)
                                rb.angularVelocity += axis * (torque * dt * 0.25f)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isConducting(n: ElectricalNodeState): Boolean {
        if (n.isBlown) return false
        if ((n.role == ElectricalRole.SWITCH || n.role == ElectricalRole.ELECTRICAL_PANEL) && !n.switchClosed) {
            return false
        }
        return true
    }
}
