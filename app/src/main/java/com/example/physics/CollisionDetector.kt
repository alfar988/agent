package com.example.physics

import com.example.math.Quat
import com.example.math.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

data class ContactPoint(
    val position: Vec3,
    val normal: Vec3, // Points from A toward B
    val penetration: Float
)

data class ContactManifold(
    val bodyA: RigidBody,
    val bodyB: RigidBody?, // null if static collider
    val staticProp: StaticProp?,
    val normal: Vec3, // Points from A toward B (or toward static prop)
    val contacts: List<ContactPoint>
)

data class RayHit(
    val bodyId: Int, // -1 if static
    val staticPropId: Int,
    val point: Vec3,
    val normal: Vec3,
    val distance: Float
)

object CollisionDetector {

    fun collideDynamicPair(a: RigidBody, b: RigidBody): ContactManifold? {
        if (a.collisionMode == CollisionMode.TRIGGER || b.collisionMode == CollisionMode.TRIGGER) return null
        if (a.isFrozen && b.isFrozen) return null
        if (a.isSleeping && b.isSleeping) return null

        // Broadphase bounding sphere reject
        val diff = b.position - a.position
        val rSum = a.effectiveRadius + b.effectiveRadius + 0.05f
        if (diff.lengthSq() > rSum * rSum) return null

        val shapeA = a.type.colliderType
        val shapeB = b.type.colliderType

        return when {
            shapeA == ColliderShapeType.SPHERE && shapeB == ColliderShapeType.SPHERE ->
                collideSphereSphere(a, b)
            shapeA == ColliderShapeType.SPHERE && shapeB == ColliderShapeType.BOX ->
                collideSphereObb(a, b.position, b.orientation, b.effectiveHalfExtents, b, null)
            shapeA == ColliderShapeType.BOX && shapeB == ColliderShapeType.SPHERE -> {
                val m = collideSphereObb(b, a.position, a.orientation, a.effectiveHalfExtents, a, null) ?: return null
                // Flip normal so it points from A (box) to B (sphere)
                ContactManifold(
                    bodyA = a,
                    bodyB = b,
                    staticProp = null,
                    normal = -m.normal,
                    contacts = m.contacts.map { ContactPoint(it.position, -it.normal, it.penetration) }
                )
            }
            else -> collideObbObb(a, b.position, b.orientation, b.effectiveHalfExtents, b, null)
        }
    }

    fun collideDynamicStatic(a: RigidBody, prop: StaticProp): ContactManifold? {
        if (a.collisionMode == CollisionMode.TRIGGER) return null
        if (a.isFrozen || a.isSleeping || !prop.isCollider) return null
        // Broadphase AABB/sphere check
        val maxStaticRadius = prop.halfExtents.length()
        val rSum = a.effectiveRadius + maxStaticRadius + 0.1f
        if ((prop.position - a.position).lengthSq() > rSum * rSum) return null

        return if (a.type.colliderType == ColliderShapeType.SPHERE) {
            collideSphereObb(a, prop.position, prop.orientation, prop.halfExtents, null, prop)
        } else {
            collideObbObb(a, prop.position, prop.orientation, prop.halfExtents, null, prop)
        }
    }

    private fun collideSphereSphere(a: RigidBody, b: RigidBody): ContactManifold? {
        val d = b.position - a.position
        val distSq = d.lengthSq()
        val rSum = a.effectiveRadius + b.effectiveRadius
        if (distSq >= rSum * rSum) return null
        val dist = sqrt(distSq)
        val n = if (dist > 1e-5f) d / dist else Vec3.UP
        val pen = rSum - dist
        val contactPos = a.position + n * (a.effectiveRadius - pen * 0.5f)
        return ContactManifold(a, b, null, n, listOf(ContactPoint(contactPos, n, pen)))
    }

    private fun collideSphereObb(
        sphere: RigidBody,
        boxPos: Vec3,
        boxRot: Quat,
        boxHe: Vec3,
        boxBody: RigidBody?,
        staticProp: StaticProp?
    ): ContactManifold? {
        val invRot = boxRot.conjugate()
        val localCenter = invRot.rotate(sphere.position - boxPos)
        val clamped = Vec3(
            localCenter.x.coerceIn(-boxHe.x, boxHe.x),
            localCenter.y.coerceIn(-boxHe.y, boxHe.y),
            localCenter.z.coerceIn(-boxHe.z, boxHe.z)
        )
        val deltaLocal = localCenter - clamped
        val distSq = deltaLocal.lengthSq()
        val r = sphere.effectiveRadius
        if (distSq > r * r) return null

        val normalFromBoxToSphereLocal: Vec3
        val pen: Float
        val contactLocal: Vec3

        if (distSq > 1e-7f) {
            val dist = sqrt(distSq)
            normalFromBoxToSphereLocal = deltaLocal / dist
            pen = r - dist
            contactLocal = clamped
        } else {
            // Sphere center is inside box: push out along shallowest axis
            val dx = boxHe.x - abs(localCenter.x)
            val dy = boxHe.y - abs(localCenter.y)
            val dz = boxHe.z - abs(localCenter.z)
            when {
                dx <= dy && dx <= dz -> {
                    val sx = if (localCenter.x >= 0f) 1f else -1f
                    normalFromBoxToSphereLocal = Vec3(sx, 0f, 0f)
                    pen = r + dx
                    contactLocal = Vec3(sx * boxHe.x, localCenter.y, localCenter.z)
                }
                dy <= dz -> {
                    val sy = if (localCenter.y >= 0f) 1f else -1f
                    normalFromBoxToSphereLocal = Vec3(0f, sy, 0f)
                    pen = r + dy
                    contactLocal = Vec3(localCenter.x, sy * boxHe.y, localCenter.z)
                }
                else -> {
                    val sz = if (localCenter.z >= 0f) 1f else -1f
                    normalFromBoxToSphereLocal = Vec3(0f, 0f, sz)
                    pen = r + dz
                    contactLocal = Vec3(localCenter.x, localCenter.y, sz * boxHe.z)
                }
            }
        }

        // Normal in ContactManifold must point from A (sphere) toward B (box)
        val normalAtoB = -(boxRot.rotate(normalFromBoxToSphereLocal)).normalized()
        val contactWorld = boxPos + boxRot.rotate(contactLocal)
        return ContactManifold(
            bodyA = sphere,
            bodyB = boxBody,
            staticProp = staticProp,
            normal = normalAtoB,
            contacts = listOf(ContactPoint(contactWorld, normalAtoB, pen))
        )
    }

    private fun collideObbObb(
        a: RigidBody,
        bPos: Vec3,
        bRot: Quat,
        bHe: Vec3,
        bBody: RigidBody?,
        staticProp: StaticProp?
    ): ContactManifold? {
        val aPos = a.position
        val aHe = a.effectiveHalfExtents
        val rotMatA = a.orientation.toMat3()
        val rotMatB = bRot.toMat3()

        val axesA = arrayOf(rotMatA.axisX(), rotMatA.axisY(), rotMatA.axisZ())
        val axesB = arrayOf(rotMatB.axisX(), rotMatB.axisY(), rotMatB.axisZ())
        val heA = floatArrayOf(aHe.x, aHe.y, aHe.z)
        val heB = floatArrayOf(bHe.x, bHe.y, bHe.z)

        val t = bPos - aPos

        var minOverlap = Float.POSITIVE_INFINITY
        var bestAxisIndex = -1
        var bestAxis = Vec3.UP

        // 1. Test 3 face axes of A (0..2)
        for (i in 0 until 3) {
            val axis = axesA[i]
            val rA = heA[i]
            val rB = heB[0] * abs(axis.dot(axesB[0])) +
                heB[1] * abs(axis.dot(axesB[1])) +
                heB[2] * abs(axis.dot(axesB[2]))
            val dist = abs(t.dot(axis))
            val overlap = rA + rB - dist
            if (overlap <= 0f) return null
            if (overlap < minOverlap) {
                minOverlap = overlap
                bestAxisIndex = i
                bestAxis = axis
            }
        }

        // 2. Test 3 face axes of B (3..5)
        for (j in 0 until 3) {
            val axis = axesB[j]
            val rA = heA[0] * abs(axis.dot(axesA[0])) +
                heA[1] * abs(axis.dot(axesA[1])) +
                heA[2] * abs(axis.dot(axesA[2]))
            val rB = heB[j]
            val dist = abs(t.dot(axis))
            val overlap = rA + rB - dist
            if (overlap <= 0f) return null
            // Slight bias toward face contacts over near-ties
            if (overlap < minOverlap * 0.99f) {
                minOverlap = overlap
                bestAxisIndex = 3 + j
                bestAxis = axis
            }
        }

        // 3. Test 9 edge-edge cross axes (6..14)
        var bestEdgeA = 0
        var bestEdgeB = 0
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                val cross = axesA[i].cross(axesB[j])
                val lenSq = cross.lengthSq()
                if (lenSq < 1e-6f) continue
                val axis = cross / sqrt(lenSq)
                val rA = heA[0] * abs(axis.dot(axesA[0])) +
                    heA[1] * abs(axis.dot(axesA[1])) +
                    heA[2] * abs(axis.dot(axesA[2]))
                val rB = heB[0] * abs(axis.dot(axesB[0])) +
                    heB[1] * abs(axis.dot(axesB[1])) +
                    heB[2] * abs(axis.dot(axesB[2]))
                val dist = abs(t.dot(axis))
                val overlap = rA + rB - dist
                if (overlap <= 0f) return null
                // Prefer face-face axes unless edge penetration is significantly smaller
                if (overlap < minOverlap * 0.95f) {
                    minOverlap = overlap
                    bestAxisIndex = 6 + i * 3 + j
                    bestAxis = axis
                    bestEdgeA = i
                    bestEdgeB = j
                }
            }
        }

        if (bestAxisIndex < 0) return null

        // Ensure normal points from A toward B
        var normal = if (bestAxis.dot(t) < 0f) -bestAxis else bestAxis
        normal = normal.normalized()

        val contacts: List<ContactPoint> = if (bestAxisIndex < 6) {
            generateFaceManifold(
                aPos, axesA, heA,
                bPos, axesB, heB,
                bestAxisIndex, normal, minOverlap
            )
        } else {
            val pt = closestEdgeContact(
                aPos, axesA, heA, bestEdgeA,
                bPos, axesB, heB, bestEdgeB,
                normal
            )
            listOf(ContactPoint(pt, normal, minOverlap))
        }

        if (contacts.isEmpty()) return null
        return ContactManifold(a, bBody, staticProp, normal, contacts)
    }

    private fun generateFaceManifold(
        aPos: Vec3, axesA: Array<Vec3>, heA: FloatArray,
        bPos: Vec3, axesB: Array<Vec3>, heB: FloatArray,
        bestAxisIndex: Int,
        normalAtoB: Vec3,
        fallbackPen: Float
    ): List<ContactPoint> {
        // Reference box has the separating face; incident box provides the clipped polygon
        val refIsA = bestAxisIndex < 3
        val refPos = if (refIsA) aPos else bPos
        val refAxes = if (refIsA) axesA else axesB
        val refHe = if (refIsA) heA else heB
        val incPos = if (refIsA) bPos else aPos
        val incAxes = if (refIsA) axesB else axesA
        val incHe = if (refIsA) heB else heA

        // Reference normal points outward from reference box toward incident box
        val refOutwardNormal = if (refIsA) normalAtoB else -normalAtoB

        // Find reference face axis index (0, 1, or 2)
        val refAxisIdx = if (refIsA) bestAxisIndex else bestAxisIndex - 3
        val refFaceSign = if (refAxes[refAxisIdx].dot(refOutwardNormal) >= 0f) 1f else -1f
        val refFaceCenter = refPos + refAxes[refAxisIdx] * (refHe[refAxisIdx] * refFaceSign)

        val uIdx = (refAxisIdx + 1) % 3
        val vIdx = (refAxisIdx + 2) % 3
        val uAxis = refAxes[uIdx]
        val vAxis = refAxes[vIdx]
        val uExt = refHe[uIdx]
        val vExt = refHe[vIdx]

        // Find incident face on incident box most anti-parallel to refOutwardNormal
        var incAxisIdx = 0
        var minDot = Float.POSITIVE_INFINITY
        for (k in 0 until 3) {
            val d = incAxes[k].dot(refOutwardNormal)
            if (d < minDot) {
                minDot = d
                incAxisIdx = k
            }
            if (-d < minDot) {
                minDot = -d
                incAxisIdx = k
            }
        }
        val incFaceSign = if (incAxes[incAxisIdx].dot(refOutwardNormal) > 0f) -1f else 1f
        val incCenter = incPos + incAxes[incAxisIdx] * (incHe[incAxisIdx] * incFaceSign)
        val iu = (incAxisIdx + 1) % 3
        val iv = (incAxisIdx + 2) % 3
        val du = incAxes[iu] * incHe[iu]
        val dv = incAxes[iv] * incHe[iv]

        var poly = mutableListOf(
            incCenter + du + dv,
            incCenter - du + dv,
            incCenter - du - dv,
            incCenter + du - dv
        )

        // Sutherland-Hodgman clip against the 4 side planes of the reference face
        poly = clipPolyAgainstPlane(poly, refFaceCenter + uAxis * uExt, -uAxis)
        poly = clipPolyAgainstPlane(poly, refFaceCenter - uAxis * uExt, uAxis)
        poly = clipPolyAgainstPlane(poly, refFaceCenter + vAxis * vExt, -vAxis)
        poly = clipPolyAgainstPlane(poly, refFaceCenter - vAxis * vExt, vAxis)

        val refPlaneNormal = refAxes[refAxisIdx] * refFaceSign
        val out = ArrayList<ContactPoint>(4)
        for (p in poly) {
            val dist = (p - refFaceCenter).dot(refPlaneNormal)
            if (dist <= 0.02f) {
                val pen = (-dist).coerceAtLeast(0.001f)
                out.add(ContactPoint(p, normalAtoB, pen))
            }
        }

        if (out.isEmpty()) {
            // Fallback center point
            out.add(ContactPoint(incCenter, normalAtoB, fallbackPen))
        } else if (out.size > 4) {
            return out.sortedByDescending { it.penetration }.take(4)
        }
        return out
    }

    private fun clipPolyAgainstPlane(
        poly: List<Vec3>,
        planePoint: Vec3,
        planeInsideNormal: Vec3
    ): MutableList<Vec3> {
        val result = ArrayList<Vec3>(6)
        if (poly.isEmpty()) return result
        var prev = poly.last()
        var prevDist = (prev - planePoint).dot(planeInsideNormal)
        for (curr in poly) {
            val currDist = (curr - planePoint).dot(planeInsideNormal)
            if (currDist >= 0f) {
                if (prevDist < 0f) {
                    val t = prevDist / (prevDist - currDist)
                    result.add(prev + (curr - prev) * t)
                }
                result.add(curr)
            } else if (prevDist >= 0f) {
                val t = prevDist / (prevDist - currDist)
                result.add(prev + (curr - prev) * t)
            }
            prev = curr
            prevDist = currDist
        }
        return result
    }

    private fun closestEdgeContact(
        aPos: Vec3, axesA: Array<Vec3>, heA: FloatArray, edgeA: Int,
        bPos: Vec3, axesB: Array<Vec3>, heB: FloatArray, edgeB: Int,
        normalAtoB: Vec3
    ): Vec3 {
        var ptA = aPos
        var ptB = bPos
        for (k in 0 until 3) {
            if (k != edgeA) {
                val s = if (axesA[k].dot(normalAtoB) > 0f) 1f else -1f
                ptA += axesA[k] * (heA[k] * s)
            }
            if (k != edgeB) {
                val s = if (axesB[k].dot(normalAtoB) < 0f) 1f else -1f
                ptB += axesB[k] * (heB[k] * s)
            }
        }
        val dA = axesA[edgeA]
        val dB = axesB[edgeB]
        val r = ptA - ptB
        val aDotA = dA.dot(dA)
        val eDotE = dB.dot(dB)
        val aDotB = dA.dot(dB)
        val c = dA.dot(r)
        val f = dB.dot(r)
        val denom = aDotA * eDotE - aDotB * aDotB
        var s = 0f
        var t = 0f
        if (denom > 1e-6f) {
            s = ((aDotB * f - c * eDotE) / denom).coerceIn(-heA[edgeA], heA[edgeA])
            t = ((aDotB * s + f) / eDotE).coerceIn(-heB[edgeB], heB[edgeB])
        }
        val closestA = ptA + dA * s
        val closestB = ptB + dB * t
        return (closestA + closestB) * 0.5f
    }

    fun raycastBodiesAndStatics(
        origin: Vec3,
        dir: Vec3,
        maxDistance: Float,
        bodies: List<RigidBody>,
        statics: List<StaticProp>
    ): RayHit? {
        var closestHit: RayHit? = null
        var minDist = maxDistance

        for (body in bodies) {
            if (body.isDestroyed) continue
            val hit = if (body.type.colliderType == ColliderShapeType.SPHERE) {
                raySphere(origin, dir, body.position, body.effectiveRadius, minDist)
            } else {
                rayObb(origin, dir, body.position, body.orientation, body.effectiveHalfExtents, minDist)
            }
            if (hit != null && hit.first < minDist) {
                minDist = hit.first
                closestHit = RayHit(
                    bodyId = body.id,
                    staticPropId = -1,
                    point = origin + dir * minDist,
                    normal = hit.second,
                    distance = minDist
                )
            }
        }

        for (prop in statics) {
            if (!prop.isCollider) continue
            val hit = rayObb(origin, dir, prop.position, prop.orientation, prop.halfExtents, minDist)
            if (hit != null && hit.first < minDist) {
                minDist = hit.first
                closestHit = RayHit(
                    bodyId = -1,
                    staticPropId = prop.id,
                    point = origin + dir * minDist,
                    normal = hit.second,
                    distance = minDist
                )
            }
        }
        return closestHit
    }

    private fun raySphere(
        origin: Vec3,
        dir: Vec3,
        center: Vec3,
        radius: Float,
        maxDist: Float
    ): Pair<Float, Vec3>? {
        val oc = origin - center
        val b = oc.dot(dir)
        val c = oc.dot(oc) - radius * radius
        if (c > 0f && b > 0f) return null
        val disc = b * b - c
        if (disc < 0f) return null
        val t = (-b - sqrt(disc)).coerceAtLeast(0f)
        if (t > maxDist) return null
        val p = origin + dir * t
        return t to (p - center).normalized()
    }

    private fun rayObb(
        origin: Vec3,
        dir: Vec3,
        boxPos: Vec3,
        boxRot: Quat,
        he: Vec3,
        maxDist: Float
    ): Pair<Float, Vec3>? {
        val invRot = boxRot.conjugate()
        val localOrigin = invRot.rotate(origin - boxPos)
        val localDir = invRot.rotate(dir)

        var tMin = 0f
        var tMax = maxDist
        var hitNormalLocal = Vec3.UP

        val oArr = floatArrayOf(localOrigin.x, localOrigin.y, localOrigin.z)
        val dArr = floatArrayOf(localDir.x, localDir.y, localDir.z)
        val heArr = floatArrayOf(he.x, he.y, he.z)

        for (i in 0 until 3) {
            if (abs(dArr[i]) < 1e-6f) {
                if (oArr[i] < -heArr[i] || oArr[i] > heArr[i]) return null
            } else {
                val invD = 1f / dArr[i]
                var t1 = (-heArr[i] - oArr[i]) * invD
                var t2 = (heArr[i] - oArr[i]) * invD
                var nSign = -1f
                if (t1 > t2) {
                    val tmp = t1; t1 = t2; t2 = tmp
                    nSign = 1f
                }
                if (t1 > tMin) {
                    tMin = t1
                    hitNormalLocal = when (i) {
                        0 -> Vec3(nSign, 0f, 0f)
                        1 -> Vec3(0f, nSign, 0f)
                        else -> Vec3(0f, 0f, nSign)
                    }
                }
                if (t2 < tMax) tMax = t2
                if (tMin > tMax) return null
            }
        }
        return tMin to boxRot.rotate(hitNormalLocal).normalized()
    }
}
