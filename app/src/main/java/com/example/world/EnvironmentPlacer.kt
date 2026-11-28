package com.example.world

import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.MeshId
import com.example.physics.MountType
import com.example.physics.ObjectType
import com.example.physics.PhysicsMaterial
import com.example.physics.RigidBody
import com.example.physics.StaticProp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Deterministic architectural & prop placement helpers that compute exact contact planes,
 * rotated AABB half-extents, flush wall/ceiling mounts, and walkable doorways/windows/stairs.
 */
object EnvironmentPlacer {

    fun rotatedHalfExtents(he: Vec3, rot: Quat): Vec3 {
        val rx = rot.rotate(Vec3(he.x, 0f, 0f))
        val ry = rot.rotate(Vec3(0f, he.y, 0f))
        val rz = rot.rotate(Vec3(0f, 0f, he.z))
        return Vec3(
            abs(rx.x) + abs(ry.x) + abs(rz.x),
            abs(rx.y) + abs(ry.y) + abs(rz.y),
            abs(rx.z) + abs(ry.z) + abs(rz.z)
        )
    }

    fun yawQuat(yawRad: Float): Quat = Quat.fromAxisAngle(Vec3.UP, yawRad)
}

fun MapConstructionContext.placeStaticOnFloor(
    meshId: MeshId,
    x: Float,
    z: Float,
    halfExtents: Vec3,
    material: PhysicsMaterial,
    rot: Quat = Quat.IDENTITY,
    floorY: Float = 0f,
    isCollider: Boolean = true,
    conveyorVel: Vec3 = Vec3.ZERO,
    colorOverride: Vec3? = null,
    alphaOverride: Float? = null,
    label: String = "",
    mountType: MountType = MountType.FLOOR
): Int {
    val worldHe = EnvironmentPlacer.rotatedHalfExtents(halfExtents, rot)
    return addStatic(
        meshId = meshId,
        pos = Vec3(x, floorY + worldHe.y, z),
        halfExtents = halfExtents,
        material = material,
        rot = rot,
        isCollider = isCollider,
        conveyorVel = conveyorVel,
        colorOverride = colorOverride,
        alphaOverride = alphaOverride,
        label = label,
        mountType = mountType
    )
}

fun MapConstructionContext.placeStaticOnSupport(
    meshId: MeshId,
    x: Float,
    z: Float,
    supportPropId: Int,
    halfExtents: Vec3,
    material: PhysicsMaterial,
    rot: Quat = Quat.IDENTITY,
    isCollider: Boolean = true,
    colorOverride: Vec3? = null,
    alphaOverride: Float? = null,
    label: String = ""
): Int {
    val support = statics.first { it.id == supportPropId }
    val supportTopY = support.position.y + EnvironmentPlacer.rotatedHalfExtents(support.halfExtents, support.orientation).y
    val worldHe = EnvironmentPlacer.rotatedHalfExtents(halfExtents, rot)
    return addStatic(
        meshId = meshId,
        pos = Vec3(x, supportTopY + worldHe.y, z),
        halfExtents = halfExtents,
        material = material,
        rot = rot,
        isCollider = isCollider,
        colorOverride = colorOverride,
        alphaOverride = alphaOverride,
        label = label,
        mountType = MountType.SURFACE
    )
}

fun MapConstructionContext.placeDynamicOnFloor(
    type: ObjectType,
    x: Float,
    z: Float,
    rot: Quat = Quat.IDENTITY,
    material: PhysicsMaterial = type.defaultMaterial,
    scale: Float = 1.0f,
    frozen: Boolean = false,
    floorY: Float = 0f
): RigidBody {
    val worldHe = EnvironmentPlacer.rotatedHalfExtents(type.halfExtents * scale, rot)
    return addDynamic(
        type = type,
        pos = Vec3(x, floorY + worldHe.y, z),
        rot = rot,
        material = material,
        scale = scale,
        frozen = frozen
    )
}

fun MapConstructionContext.placeDynamicOnSupport(
    type: ObjectType,
    x: Float,
    z: Float,
    supportPropId: Int,
    rot: Quat = Quat.IDENTITY,
    material: PhysicsMaterial = type.defaultMaterial,
    scale: Float = 1.0f,
    frozen: Boolean = false
): RigidBody {
    val support = statics.first { it.id == supportPropId }
    val supportTopY = support.position.y + EnvironmentPlacer.rotatedHalfExtents(support.halfExtents, support.orientation).y
    val worldHe = EnvironmentPlacer.rotatedHalfExtents(type.halfExtents * scale, rot)
    return addDynamic(
        type = type,
        pos = Vec3(x, supportTopY + worldHe.y, z),
        rot = rot,
        material = material,
        scale = scale,
        frozen = frozen
    )
}

fun MapConstructionContext.placeDynamicOnBody(
    type: ObjectType,
    x: Float,
    z: Float,
    supportBody: RigidBody,
    rot: Quat = Quat.IDENTITY,
    material: PhysicsMaterial = type.defaultMaterial,
    scale: Float = 1.0f,
    frozen: Boolean = false
): RigidBody {
    val supportTopY = supportBody.position.y + EnvironmentPlacer.rotatedHalfExtents(
        supportBody.effectiveHalfExtents, supportBody.orientation
    ).y
    val worldHe = EnvironmentPlacer.rotatedHalfExtents(type.halfExtents * scale, rot)
    return addDynamic(
        type = type,
        pos = Vec3(x, supportTopY + worldHe.y, z),
        rot = rot,
        material = material,
        scale = scale,
        frozen = frozen
    )
}

fun MapConstructionContext.placeWallMounted(
    meshId: MeshId,
    wallCenterX: Float,
    wallCenterZ: Float,
    wallHalfThickness: Float,
    mountBottomY: Float,
    inwardDirX: Float,
    inwardDirZ: Float,
    halfExtents: Vec3,
    material: PhysicsMaterial,
    colorOverride: Vec3? = null,
    label: String = ""
): Int {
    val yaw = atan2(inwardDirX, inwardDirZ)
    val rot = EnvironmentPlacer.yawQuat(yaw)
    val px = wallCenterX + inwardDirX * (wallHalfThickness + halfExtents.z)
    val pz = wallCenterZ + inwardDirZ * (wallHalfThickness + halfExtents.z)
    val py = mountBottomY + halfExtents.y
    return addStatic(
        meshId = meshId,
        pos = Vec3(px, py, pz),
        halfExtents = halfExtents,
        material = material,
        rot = rot,
        isCollider = true,
        colorOverride = colorOverride,
        label = label,
        mountType = MountType.WALL
    )
}

fun MapConstructionContext.placeCeilingLight(
    x: Float,
    z: Float,
    ceilingBottomY: Float,
    halfX: Float = 1.10f,
    halfY: Float = 0.14f,
    halfZ: Float = 0.30f,
    yaw: Float = 0f,
    emissiveColor: Vec3 = Vec3(0.95f, 0.97f, 1.0f),
    emissiveIntensity: Float = 0.55f
): Int {
    return addStatic(
        meshId = MeshId.CEILING_LIGHT,
        pos = Vec3(x, ceilingBottomY - halfY, z),
        halfExtents = Vec3(halfX, halfY, halfZ),
        material = PhysicsMaterial.STEEL,
        rot = EnvironmentPlacer.yawQuat(yaw),
        isCollider = false,
        colorOverride = Vec3(0.84f, 0.87f, 0.90f),
        mountType = MountType.CEILING,
        emissiveColor = emissiveColor,
        emissiveIntensity = emissiveIntensity
    )
}

/**
 * Builds a clean architectural interior/exterior wall along X or Z with a real walkable doorway opening
 * and flush DoorFrame trim.
 */
fun MapConstructionContext.addWallWithDoorway(
    isAlongX: Boolean,
    centerX: Float,
    centerZ: Float,
    halfLength: Float,
    halfThickness: Float,
    baseY: Float,
    wallHeight: Float,
    doorCenterOffset: Float,
    doorHalfWidth: Float,
    doorHeight: Float,
    material: PhysicsMaterial,
    wallColor: Vec3,
    frameColor: Vec3 = Vec3(0.24f, 0.27f, 0.31f)
) {
    val start = -halfLength
    val end = halfLength
    val dStart = (doorCenterOffset - doorHalfWidth).coerceAtLeast(start)
    val dEnd = (doorCenterOffset + doorHalfWidth).coerceAtMost(end)

    fun emitWallSeg(segStart: Float, segEnd: Float, y0: Float, y1: Float) {
        val len = segEnd - segStart
        val h = y1 - y0
        if (len < 0.08f || h < 0.08f) return
        val mid = (segStart + segEnd) * 0.5f
        val cy = (y0 + y1) * 0.5f
        val wx = if (isAlongX) centerX + mid else centerX
        val wz = if (isAlongX) centerZ else centerZ + mid
        val he = if (isAlongX) Vec3(len * 0.5f, h * 0.5f, halfThickness) else Vec3(halfThickness, h * 0.5f, len * 0.5f)
        addStatic(
            meshId = MeshId.WALL_PANEL,
            pos = Vec3(wx, cy, wz),
            halfExtents = he,
            material = material,
            colorOverride = wallColor,
            mountType = MountType.STRUCTURAL
        )
    }

    // Left jamb wall, right jamb wall, top lintel above doorway
    emitWallSeg(start, dStart, baseY, baseY + wallHeight)
    emitWallSeg(dEnd, end, baseY, baseY + wallHeight)
    emitWallSeg(dStart, dEnd, baseY + doorHeight, baseY + wallHeight)

    // Door frame trim around opening
    val frameMid = (dStart + dEnd) * 0.5f
    val fx = if (isAlongX) centerX + frameMid else centerX
    val fz = if (isAlongX) centerZ else centerZ + frameMid
    val yaw = if (isAlongX) 0f else PI.toFloat() * 0.5f
    addStatic(
        meshId = MeshId.DOOR_FRAME,
        pos = Vec3(fx, baseY + doorHeight * 0.5f, fz),
        halfExtents = Vec3((dEnd - dStart) * 0.5f, doorHeight * 0.5f, halfThickness + 0.04f),
        material = PhysicsMaterial.STEEL,
        rot = EnvironmentPlacer.yawQuat(yaw),
        isCollider = false,
        colorOverride = frameColor,
        mountType = MountType.STRUCTURAL
    )
}

/**
 * Builds an architectural wall with a recessed framed window and translucent glass pane.
 */
fun MapConstructionContext.addWallWithWindow(
    isAlongX: Boolean,
    centerX: Float,
    centerZ: Float,
    halfLength: Float,
    halfThickness: Float,
    baseY: Float,
    wallHeight: Float,
    winCenterOffset: Float,
    winHalfWidth: Float,
    sillHeight: Float,
    winTopHeight: Float,
    material: PhysicsMaterial,
    wallColor: Vec3,
    frameColor: Vec3 = Vec3(0.24f, 0.27f, 0.31f)
) {
    val start = -halfLength
    val end = halfLength
    val wStart = (winCenterOffset - winHalfWidth).coerceAtLeast(start)
    val wEnd = (winCenterOffset + winHalfWidth).coerceAtMost(end)

    fun emitWallSeg(segStart: Float, segEnd: Float, y0: Float, y1: Float) {
        val len = segEnd - segStart
        val h = y1 - y0
        if (len < 0.08f || h < 0.08f) return
        val mid = (segStart + segEnd) * 0.5f
        val cy = (y0 + y1) * 0.5f
        val wx = if (isAlongX) centerX + mid else centerX
        val wz = if (isAlongX) centerZ else centerZ + mid
        val he = if (isAlongX) Vec3(len * 0.5f, h * 0.5f, halfThickness) else Vec3(halfThickness, h * 0.5f, len * 0.5f)
        addStatic(
            meshId = MeshId.WALL_PANEL,
            pos = Vec3(wx, cy, wz),
            halfExtents = he,
            material = material,
            colorOverride = wallColor,
            mountType = MountType.STRUCTURAL
        )
    }

    emitWallSeg(start, wStart, baseY, baseY + wallHeight)
    emitWallSeg(wEnd, end, baseY, baseY + wallHeight)
    emitWallSeg(wStart, wEnd, baseY, baseY + sillHeight)
    emitWallSeg(wStart, wEnd, baseY + winTopHeight, baseY + wallHeight)

    val mid = (wStart + wEnd) * 0.5f
    val wx = if (isAlongX) centerX + mid else centerX
    val wz = if (isAlongX) centerZ else centerZ + mid
    val winHeight = winTopHeight - sillHeight
    val winCy = baseY + sillHeight + winHeight * 0.5f
    val yaw = if (isAlongX) 0f else PI.toFloat() * 0.5f
    val wHalfW = (wEnd - wStart) * 0.5f

    // Steel window frame
    addStatic(
        meshId = MeshId.WINDOW_FRAME,
        pos = Vec3(wx, winCy, wz),
        halfExtents = Vec3(wHalfW, winHeight * 0.5f, halfThickness + 0.03f),
        material = PhysicsMaterial.STEEL,
        rot = EnvironmentPlacer.yawQuat(yaw),
        isCollider = false,
        colorOverride = frameColor,
        mountType = MountType.STRUCTURAL
    )

    // Glazing inside window opening
    val glassHe = if (isAlongX) Vec3(wHalfW * 0.94f, winHeight * 0.45f, 0.05f) else Vec3(0.05f, winHeight * 0.45f, wHalfW * 0.94f)
    addStatic(
        meshId = MeshId.GLASS_PARTITION,
        pos = Vec3(wx, winCy, wz),
        halfExtents = glassHe,
        material = PhysicsMaterial.GLASS,
        alphaOverride = 0.36f,
        colorOverride = Vec3(0.76f, 0.88f, 0.94f),
        mountType = MountType.STRUCTURAL
    )
}

/**
 * Adds a visible architectural staircase paired with a smooth invisible ramp collider
 * so the player and rolling objects traverse cleanly between levels.
 * Ascend direction yaw: 0f = ascends toward -Z, PI = toward +Z, -PI/2 = toward +X, +PI/2 = toward -X.
 */
fun MapConstructionContext.addStaircaseToDeck(
    centerX: Float,
    centerZ: Float,
    floorY: Float,
    topY: Float,
    halfWidth: Float,
    halfRun: Float,
    ascendYaw: Float,
    material: PhysicsMaterial = PhysicsMaterial.STEEL,
    colorOverride: Vec3 = Vec3(0.42f, 0.46f, 0.51f)
) {
    val rise = topY - floorY
    val run = halfRun * 2f
    val midY = (floorY + topY) * 0.5f
    val visualRot = EnvironmentPlacer.yawQuat(ascendYaw)

    // Visual stair flight with steps, stringers & handrails
    addStatic(
        meshId = MeshId.STAIR_FLIGHT,
        pos = Vec3(centerX, midY, centerZ),
        halfExtents = Vec3(halfWidth, rise * 0.5f, halfRun),
        material = material,
        rot = visualRot,
        isCollider = false,
        colorOverride = colorOverride,
        mountType = MountType.STRUCTURAL,
        isVisible = true
    )

    // Smooth invisible collider slab aligned from (bottom at +localZ, floorY) to (top at -localZ, topY)
    val slopeAngle = atan2(rise, run)
    val diagLen = hypot(run.toDouble(), rise.toDouble()).toFloat()
    val rampRot = visualRot * Quat.fromAxisAngle(Vec3.RIGHT, slopeAngle)
    val normalOffset = Vec3(0f, -0.14f * cos(slopeAngle), 0f)
    addStatic(
        meshId = MeshId.RAMP_WEDGE,
        pos = Vec3(centerX, midY, centerZ) + normalOffset,
        halfExtents = Vec3(halfWidth * 0.90f, 0.14f, diagLen * 0.50f),
        material = material,
        rot = rampRot,
        isCollider = true,
        mountType = MountType.STRUCTURAL,
        isVisible = false
    )
}

/**
 * Places a structural sloped ramp whose lower edge rests at lowY and higher edge reaches highY,
 * with steel support columns anchoring the ramp underside down to floorY.
 */
fun MapConstructionContext.addSlopedRampWithPiers(
    centerX: Float,
    centerZ: Float,
    lowY: Float,
    highY: Float,
    halfWidth: Float,
    halfRun: Float,
    ascendYaw: Float,
    floorY: Float = 0f,
    material: PhysicsMaterial = PhysicsMaterial.STEEL,
    colorOverride: Vec3 = Vec3(0.46f, 0.50f, 0.55f)
) {
    val rise = highY - lowY
    val run = halfRun * 2f
    val slopeAngle = atan2(rise, run)
    val diagLen = hypot(run.toDouble(), rise.toDouble()).toFloat()
    val yawRot = EnvironmentPlacer.yawQuat(ascendYaw)
    val rampRot = yawRot * Quat.fromAxisAngle(Vec3.RIGHT, slopeAngle)
    val deckHalfThick = 0.16f
    val midY = (lowY + highY) * 0.5f - deckHalfThick * cos(slopeAngle)

    addStatic(
        meshId = MeshId.RAMP_WEDGE,
        pos = Vec3(centerX, midY, centerZ),
        halfExtents = Vec3(halfWidth, deckHalfThick, diagLen * 0.5f),
        material = material,
        rot = rampRot,
        colorOverride = colorOverride,
        mountType = MountType.STRUCTURAL
    )

    // Support piers under the high end of the ramp (reaching strictly from floorY to highY - 2*deckThick)
    val topPierY = (highY - deckHalfThick * 2.1f).coerceAtLeast(floorY + 0.3f)
    val pierH = topPierY - floorY
    if (pierH > 0.35f) {
        val dirToTop = yawRot.rotate(Vec3(0f, 0f, -halfRun * 0.85f))
        val sideRight = yawRot.rotate(Vec3(halfWidth * 0.80f, 0f, 0f))
        for (sign in listOf(-1f, 1f)) {
            val px = centerX + dirToTop.x + sideRight.x * sign
            val pz = centerZ + dirToTop.z + sideRight.z * sign
            placeStaticOnFloor(
                MeshId.PILLAR_COLUMN, px, pz,
                Vec3(0.18f, pierH * 0.5f, 0.18f),
                PhysicsMaterial.STEEL,
                floorY = floorY,
                colorOverride = Vec3(0.30f, 0.34f, 0.38f),
                mountType = MountType.STRUCTURAL
            )
        }
    }
}
