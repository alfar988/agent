package com.example.physics

import com.example.electrical.ElectricalRole
import com.example.electrical.ElectricalSolver
import com.example.electrical.WireConnection
import com.example.math.Quat
import com.example.math.Vec3
import com.example.weapons.AmmoBoxVariant
import com.example.weapons.FirearmCatalog
import com.example.weapons.FirearmHudState
import com.example.weapons.FirearmSpec
import com.example.weapons.WeaponController
import com.example.world.MapBuilder
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

sealed interface PhysicsCommand {
    data class LoadMap(val mapId: MapId, val isMenuDemo: Boolean) : PhysicsCommand
    data class RestoreState(
        val mapId: MapId,
        val playerPos: Vec3,
        val playerYaw: Float,
        val playerPitch: Float,
        val bodies: List<SavedBodyData>,
        val constraints: List<PhysicsConstraint>,
        val wires: List<WireConnection> = emptyList(),
        val weaponMagAmmo: Map<String, Int> = emptyMap(),
        val weaponReserveAmmo: Map<String, Int> = emptyMap(),
        val isWeaponHolstered: Boolean = false
    ) : PhysicsCommand
    data class SpawnObject(val type: ObjectType, val material: PhysicsMaterial? = null) : PhysicsCommand
    data class SpawnBatch(
        val type: ObjectType,
        val materialSpec: MaterialSpec? = null,
        val quantity: Int = 1,
        val toolId: String? = null
    ) : PhysicsCommand
    data class EquipCatalogTool(val toolId: String) : PhysicsCommand
    data class SpawnCatalogToolObject(val toolId: String, val quantity: Int = 1) : PhysicsCommand
    data class GrabOrUsePrimary(val tool: ToolType, val attachMode: ConstraintType) : PhysicsCommand
    data object ReleaseGrab : PhysicsCommand
    data class ThrowOrSecondary(val tool: ToolType, val impulsePower: Float) : PhysicsCommand
    data class RotateHeld(val deltaYaw: Float, val deltaPitch: Float) : PhysicsCommand
    data class AdjustGrabDistance(val delta: Float) : PhysicsCommand
    data object ToggleFreezeTargeted : PhysicsCommand
    data object DuplicateTargeted : PhysicsCommand
    data object DeleteTargeted : PhysicsCommand
    data object DetachTargeted : PhysicsCommand
    data class ResizeTargeted(val newScale: Float) : PhysicsCommand
    data class SetMaterialTargeted(val material: PhysicsMaterial) : PhysicsCommand
    data class SetMaterialSpecTargeted(val spec: MaterialSpec) : PhysicsCommand
    data class UpdateBodyProperties(
        val bodyId: Int,
        val massOverride: Float? = null,
        val clearMassOverride: Boolean = false,
        val scale: Float? = null,
        val frictionOverride: Float? = null,
        val restitutionOverride: Float? = null,
        val gravityScale: Float? = null,
        val collisionMode: CollisionMode? = null,
        val materialSpecId: String? = null,
        val posOffset: Vec3? = null,
        val rotEulerDeg: Vec3? = null,
        val isFrozen: Boolean? = null
    ) : PhysicsCommand
    data object GroupTargetedWithLook : PhysicsCommand
    data object UngroupTargeted : PhysicsCommand
    data object ToggleElectricalSwitchTargeted : PhysicsCommand
    data object ResetFuseTargeted : PhysicsCommand
    data class SetCameraMode(val mode: CameraMode) : PhysicsCommand
    data class SetPaused(val paused: Boolean) : PhysicsCommand
    data object ClearDynamicObjects : PhysicsCommand
    data object Undo : PhysicsCommand
    data object Redo : PhysicsCommand
    data object ResetScene : PhysicsCommand
    data class SetEnvironment(
        val timeOfDayHours: Float? = null,
        val weatherType: WeatherType? = null,
        val windStrength: Float? = null,
        val fluidBasinEnabled: Boolean? = null,
        val fluidLevelY: Float? = null,
        val timeScale: Float? = null
    ) : PhysicsCommand
    data class SetPhysicsQuality(val solverIterations: Int) : PhysicsCommand
    data class SetParticleBudget(val maxParticles: Int) : PhysicsCommand
    data class SetMaxDynamicObjects(val maxObjects: Int) : PhysicsCommand
    data class SetCameraBehavior(
        val thirdPersonDistance: Float,
        val thirdPersonHeight: Float,
        val headBobEnabled: Boolean,
        val cameraShakeEnabled: Boolean,
        val movementSpeedScale: Float
    ) : PhysicsCommand
    data class SetPlayerPosture(val crouching: Boolean? = null, val sprinting: Boolean? = null) : PhysicsCommand
    data class SetToolSelected(val tool: ToolType) : PhysicsCommand
    data class SetMagnetMode(val rangeMeters: Float? = null, val repel: Boolean? = null) : PhysicsCommand
    data object ToggleFlashlight : PhysicsCommand
    data object ToggleStopwatch : PhysicsCommand
    data object ResetStopwatch : PhysicsCommand
    data class SetFireInput(val pressed: Boolean) : PhysicsCommand
    data object TriggerSingleFire : PhysicsCommand
    data class SetAimState(val aiming: Boolean? = null) : PhysicsCommand
    data object ReloadFirearm : PhysicsCommand
    data object ToggleHolster : PhysicsCommand
    data object InspectWeapon : PhysicsCommand
    data object PickupNearbyAmmo : PhysicsCommand
    data class SetFirearmSettings(val autoReload: Boolean) : PhysicsCommand
}

data class SavedBodyData(
    val id: Int,
    val type: ObjectType,
    val material: PhysicsMaterial,
    val position: Vec3,
    val orientation: Quat,
    val linearVelocity: Vec3,
    val angularVelocity: Vec3,
    val scale: Float,
    val isFrozen: Boolean,
    val materialId: String? = null,
    val gravityScale: Float = 1.0f,
    val collisionMode: CollisionMode = CollisionMode.SOLID,
    val massOverride: Float? = null,
    val frictionOverride: Float? = null,
    val restitutionOverride: Float? = null,
    val toolId: String? = null,
    val customLabel: String? = null,
    val groupId: Int? = null,
    val ammoVariantId: String? = null,
    val ammoAmount: Int = 0
)

data class SessionExportData(
    val mapId: MapId,
    val playerPos: Vec3,
    val playerYaw: Float,
    val playerPitch: Float,
    val bodies: List<SavedBodyData>,
    val constraints: List<PhysicsConstraint>,
    val wires: List<WireConnection> = emptyList(),
    val weaponMagAmmo: Map<String, Int> = emptyMap(),
    val weaponReserveAmmo: Map<String, Int> = emptyMap(),
    val isWeaponHolstered: Boolean = false
)

private data class WorldUndoSnapshot(
    val bodies: List<SavedBodyData>,
    val constraints: List<PhysicsConstraint>,
    val wires: List<WireConnection>,
    val nextId: Int
)

class PhysicsWorld(
    private val onAudioEvent: (CollisionAudioEvent) -> Unit,
    private val onToolSfx: (String, Vec3) -> Unit
) {
    companion object {
        const val FIXED_DT = 1f / 60f
        const val GRAVITY = -14.5f
        const val DEFAULT_SOLVER_ITERATIONS = 10
        const val PLAYER_EYE_HEIGHT = 1.62f
        const val CROUCH_EYE_HEIGHT = 1.04f
        const val PLAYER_RADIUS = 0.42f
        const val PLAYER_HEIGHT = 1.85f
        const val CROUCH_HEIGHT = 1.18f
        const val JUMP_COOLDOWN_SEC = 0.18f
        const val MAX_UNDO_STEPS = 25
    }

    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "KineticLab-PhysicsThread").apply { priority = Thread.NORM_PRIORITY + 1 }
    }

    private val commandQueue = ConcurrentLinkedQueue<PhysicsCommand>()
    val snapshotRef = AtomicReference<WorldRenderSnapshot>()
    val targetedBodyRef = AtomicReference<InspectedBodyInfo?>(null)
    val grabbedBodyIdRef = AtomicReference<Int?>(null)
    val pendingAttachBodyIdRef = AtomicReference<Int?>(null)
    val bodyCountRef = AtomicReference(0)
    val physicsStepMsRef = AtomicReference(0f)
    val toolTelemetryRef = AtomicReference(ToolTelemetrySnapshot())
    val firearmHudRef = AtomicReference(FirearmHudState())
    val canUndoRef = AtomicReference(false)
    val canRedoRef = AtomicReference(false)
    val screenshotTriggerSeq = AtomicInteger(0)
    val footstepTriggerSeq = AtomicInteger(0)

    private val running = AtomicBoolean(false)
    private var paused = false
    private var isMenuDemo = true
    private var currentMapId = MapId.LAB
    private var solverIterations = DEFAULT_SOLVER_ITERATIONS
    private var maxDynamicObjects = 160

    private val bodies = ArrayList<RigidBody>(128)
    private val staticProps = ArrayList<StaticProp>(128)
    private val constraints = ArrayList<PhysicsConstraint>(64)
    private val electricalSolver = ElectricalSolver()
    private val particleSystem = ParticleSystem()
    private val broadphaseBuckets = HashMap<Long, ArrayList<Int>>(256)
    private val broadphasePairs = HashSet<Long>(512)
    private var nextId = 100
    private var nextGroupId = 1

    private val undoStack = ArrayDeque<WorldUndoSnapshot>()
    private val redoStack = ArrayDeque<WorldUndoSnapshot>()

    // Player & Camera state
    @Volatile var moveInputX = 0f
    @Volatile var moveInputY = 0f
    @Volatile var jumpRequested = false
    @Volatile var isCrouching = false
    @Volatile var isSprinting = false
    @Volatile private var playerPos = Vec3(0f, 1.2f, 8f)
    private var playerVel = Vec3.ZERO
    private var playerOnGround = false
    private var wasPlayerOnGround = true
    private var playerGroundAcoustic = AcousticClass.CONCRETE
    private var jumpCooldownTimer = 0f
    private var effectiveCrouching = false
    private var crouchFraction = 0f
    private var stridePhase = 0f
    private var interactPulse = 0f
    private var releasePulse = 0f
    private var landingImpulse = 0f
    private var cameraShakeAmp = 0f

    @Volatile private var cameraYaw = 0f
    @Volatile private var cameraPitch = -0.08f
    @Volatile private var cameraMode = CameraMode.FIRST_PERSON
    private var thirdPersonDistance = 3.6f
    private var thirdPersonHeight = 0.45f
    private var cameraBobEnabled = true
    private var cameraShakeEnabled = false
    private var movementSpeedScale = 1.0f

    // Phase 2 Environment & Time
    private var timeScale = 1.0f
    private var timeOfDayHours = 13.5f
    private var weatherType = WeatherType.CLEAR
    private var windStrength = 0f
    private var fluidBasinEnabled = false
    private var fluidLevelY = 0.65f

    // Tool & Interaction state
    private var grabbedBodyId: Int? = null
    private var grabDistance = 3.6f
    private var grabTargetRotOffset = Quat.IDENTITY
    private var pendingAttachBodyId: Int? = null
    private var pendingAttachLocalAnchor = Vec3.ZERO
    private var pendingGroupBodyId: Int? = null
    private var targetedBodyId: Int? = null
    private var targetedWorldPos: Vec3? = null
    private var selectedToolType: ToolType = ToolType.GRAB
    private var menuDemoTime = 0f

    @Volatile
    var listenerTransform: Pair<Vec3, Float> = Pair(Vec3.ZERO, 0f)
        private set

    // Catalog tool & firearm state
    private var activeCatalogToolId: String = "HAMMER"
    private var toolSpinAngle = 0f
    private var toolActionPulse = 0f
    private var toolBatteryPercent = 100f
    private var magnetRangeMeters = 8f
    private var magnetRepelMode = false
    private var stopwatchSeconds = 0f
    private var stopwatchRunning = false
    private var flashlightActive = false
    private var readoutTitle = "HAMMER READY"
    private var readoutDetail = "Tap action to strike or inspect"
    private val weaponController = WeaponController()
    private var adsBlend = 0f
    private var firearmShotCounter = 0

    init {
        loadMapInternal(MapId.LAB, isDemo = true)
        publishSnapshot(1f)
    }

    fun start() {
        if (running.compareAndSet(false, true)) {
            executor.scheduleAtFixedRate({
                try {
                    stepTick()
                } catch (_: Throwable) {
                    // Keep physics loop resilient
                }
            }, 0L, 16666L, TimeUnit.MICROSECONDS)
        }
    }

    fun shutdown() {
        running.set(false)
        executor.shutdownNow()
    }

    fun enqueue(cmd: PhysicsCommand) {
        commandQueue.offer(cmd)
    }

    /**
     * Deterministic synchronous stepping helper for JVM / Robolectric unit testing.
     */
    fun stepForTest(steps: Int = 1) {
        synchronized(this) {
            processCommands()
            repeat(steps.coerceAtLeast(0)) {
                val scaledDt = FIXED_DT * timeScale
                if (!paused) {
                    if (isMenuDemo) {
                        menuDemoTime += FIXED_DT
                        updateMenuDemoCamera(FIXED_DT)
                    } else {
                        updatePlayer(FIXED_DT)
                    }
                    stepPhysicsSimulation(scaledDt)
                    updateRaycastTarget()
                }
            }
            publishSnapshot(1.0f)
        }
    }

    fun addLookDelta(deltaYawRad: Float, deltaPitchRad: Float) {
        if (isMenuDemo || paused) return
        cameraYaw = (cameraYaw + deltaYawRad) % (2f * PI.toFloat())
        cameraPitch = (cameraPitch + deltaPitchRad).coerceIn(-1.42f, 1.42f)
    }

    fun exportSessionSync(): SessionExportData? {
        synchronized(this) {
            return SessionExportData(
                mapId = currentMapId,
                playerPos = playerPos,
                playerYaw = cameraYaw,
                playerPitch = cameraPitch,
                bodies = snapshotBodiesList(),
                constraints = constraints.toList(),
                wires = electricalSolver.wires.toList(),
                weaponMagAmmo = weaponController.exportMagSnapshot(),
                weaponReserveAmmo = weaponController.exportReserveSnapshot(),
                isWeaponHolstered = weaponController.isHolstered
            )
        }
    }

    private fun snapshotBodiesList(): List<SavedBodyData> =
        bodies.filter { !it.isDestroyed && !it.isDebris }.map {
            SavedBodyData(
                id = it.id,
                type = it.type,
                material = it.material,
                position = it.position,
                orientation = it.orientation,
                linearVelocity = it.linearVelocity,
                angularVelocity = it.angularVelocity,
                scale = it.scale,
                isFrozen = it.isFrozen,
                materialId = it.materialSpec.id,
                gravityScale = it.gravityScale,
                collisionMode = it.collisionMode,
                massOverride = it.massOverride,
                frictionOverride = it.frictionOverride,
                restitutionOverride = it.restitutionOverride,
                toolId = it.toolId,
                customLabel = it.customLabel,
                groupId = it.groupId,
                ammoVariantId = it.ammoVariantId,
                ammoAmount = it.ammoAmount
            )
        }

    private fun pushUndoSnapshot() {
        undoStack.addLast(
            WorldUndoSnapshot(
                bodies = snapshotBodiesList(),
                constraints = constraints.toList(),
                wires = electricalSolver.wires.toList(),
                nextId = nextId
            )
        )
        while (undoStack.size > MAX_UNDO_STEPS) undoStack.removeFirst()
        redoStack.clear()
        canUndoRef.set(undoStack.isNotEmpty())
        canRedoRef.set(false)
    }

    private fun applyUndoRedoSnapshot(snap: WorldUndoSnapshot) {
        grabbedBodyId = null
        pendingAttachBodyId = null
        pendingGroupBodyId = null
        targetedBodyId = null
        bodies.clear()
        constraints.clear()
        electricalSolver.clear()
        for (sb in snap.bodies) {
            val rb = buildBodyFromSaved(sb)
            bodies.add(rb)
            electricalSolver.registerBody(rb, rb.electricalRole)
        }
        constraints.addAll(snap.constraints)
        for (w in snap.wires) electricalSolver.addWire(w.id, w.bodyAId, w.bodyBId)
        nextId = max(nextId, snap.nextId)
        canUndoRef.set(undoStack.isNotEmpty())
        canRedoRef.set(redoStack.isNotEmpty())
    }

    private fun undoInternal() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(
            WorldUndoSnapshot(snapshotBodiesList(), constraints.toList(), electricalSolver.wires.toList(), nextId)
        )
        val prev = undoStack.removeLast()
        applyUndoRedoSnapshot(prev)
        onToolSfx("detach", playerPos)
    }

    private fun redoInternal() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(
            WorldUndoSnapshot(snapshotBodiesList(), constraints.toList(), electricalSolver.wires.toList(), nextId)
        )
        val nxt = redoStack.removeLast()
        applyUndoRedoSnapshot(nxt)
        onToolSfx("attach", playerPos)
    }

    private fun buildBodyFromSaved(sb: SavedBodyData): RigidBody {
        val spec = sb.materialId?.let { MaterialLibrary.get(it) }
        val rb = RigidBody(
            id = sb.id,
            type = sb.type,
            material = sb.material,
            position = sb.position,
            orientation = sb.orientation,
            scale = sb.scale,
            isFrozen = sb.isFrozen,
            customLabel = sb.customLabel ?: sb.type.displayName,
            materialSpec = spec
        )
        rb.linearVelocity = sb.linearVelocity
        rb.angularVelocity = sb.angularVelocity
        rb.gravityScale = sb.gravityScale
        rb.collisionMode = sb.collisionMode
        rb.massOverride = sb.massOverride
        rb.frictionOverride = sb.frictionOverride
        rb.restitutionOverride = sb.restitutionOverride
        rb.toolId = sb.toolId
        rb.groupId = sb.groupId
        if (sb.ammoVariantId != null || sb.ammoAmount > 0) {
            rb.ammoVariantId = sb.ammoVariantId ?: sb.type.defaultAmmoVariantId()
            rb.ammoAmount = if (sb.ammoAmount > 0) sb.ammoAmount else (AmmoBoxVariant.fromId(rb.ammoVariantId)?.ammoAmount ?: 0)
        }
        rb.recomputeMassProperties()
        return rb
    }

    private fun stepTick() {
        val startNs = System.nanoTime()
        synchronized(this) {
            processCommands()

            if (!paused) {
                if (isMenuDemo) {
                    menuDemoTime += FIXED_DT
                    updateMenuDemoCamera(FIXED_DT)
                } else {
                    updatePlayer(FIXED_DT)
                }
                val scaledDt = FIXED_DT * timeScale
                stepPhysicsSimulation(scaledDt)
                updateRaycastTarget()
            }
            publishSnapshot(1.0f)
        }
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000f
        val prevMs = physicsStepMsRef.get()
        physicsStepMsRef.set(prevMs * 0.8f + elapsedMs * 0.2f)
    }

    private fun processCommands() {
        while (true) {
            val cmd = commandQueue.poll() ?: break
            when (cmd) {
                is PhysicsCommand.LoadMap -> loadMapInternal(cmd.mapId, cmd.isMenuDemo)
                is PhysicsCommand.RestoreState -> restoreStateInternal(cmd)
                is PhysicsCommand.SpawnObject -> {
                    pushUndoSnapshot()
                    spawnBatchInternal(cmd.type, cmd.material?.let { MaterialLibrary.fromLegacy(it) }, 1, null)
                }
                is PhysicsCommand.SpawnBatch -> {
                    pushUndoSnapshot()
                    spawnBatchInternal(cmd.type, cmd.materialSpec, cmd.quantity, cmd.toolId)
                }
                is PhysicsCommand.EquipCatalogTool -> equipCatalogToolInternal(cmd.toolId)
                is PhysicsCommand.SpawnCatalogToolObject -> {
                    val tool = ToolCatalog.get(cmd.toolId)
                    if (tool != null) {
                        pushUndoSnapshot()
                        spawnBatchInternal(
                            type = ObjectType.TOOL_ITEM,
                            matSpec = MaterialLibrary.get(tool.materialId),
                            quantity = cmd.quantity,
                            toolId = tool.id
                        )
                    }
                }
                is PhysicsCommand.GrabOrUsePrimary -> handlePrimaryAction(cmd.tool, cmd.attachMode)
                PhysicsCommand.ReleaseGrab -> releaseGrabInternal()
                is PhysicsCommand.ThrowOrSecondary -> handleSecondaryAction(cmd.tool, cmd.impulsePower)
                is PhysicsCommand.RotateHeld -> rotateHeldInternal(cmd.deltaYaw, cmd.deltaPitch)
                is PhysicsCommand.AdjustGrabDistance -> {
                    grabDistance = (grabDistance + cmd.delta).coerceIn(1.8f, 10.5f)
                }
                PhysicsCommand.ToggleFreezeTargeted -> toggleFreezeTargetedInternal()
                PhysicsCommand.DuplicateTargeted -> {
                    pushUndoSnapshot()
                    duplicateTargetedInternal()
                }
                PhysicsCommand.DeleteTargeted -> {
                    pushUndoSnapshot()
                    deleteTargetedInternal()
                }
                PhysicsCommand.DetachTargeted -> {
                    pushUndoSnapshot()
                    detachTargetedInternal()
                }
                is PhysicsCommand.ResizeTargeted -> resizeTargetedInternal(cmd.newScale)
                is PhysicsCommand.SetMaterialTargeted -> setMaterialTargetedInternal(MaterialLibrary.fromLegacy(cmd.material))
                is PhysicsCommand.SetMaterialSpecTargeted -> setMaterialTargetedInternal(cmd.spec)
                is PhysicsCommand.UpdateBodyProperties -> updateBodyPropertiesInternal(cmd)
                PhysicsCommand.GroupTargetedWithLook -> groupTargetedInternal()
                PhysicsCommand.UngroupTargeted -> ungroupTargetedInternal()
                PhysicsCommand.ToggleElectricalSwitchTargeted -> toggleElectricalSwitchInternal()
                PhysicsCommand.ResetFuseTargeted -> resetFuseInternal()
                is PhysicsCommand.SetCameraMode -> cameraMode = cmd.mode
                is PhysicsCommand.SetPaused -> {
                    paused = cmd.paused
                    if (cmd.paused) weaponController.setFirePressed(false)
                }
                PhysicsCommand.ClearDynamicObjects -> {
                    pushUndoSnapshot()
                    bodies.clear()
                    constraints.clear()
                    electricalSolver.clear()
                    particleSystem.clear()
                    grabbedBodyId = null
                    pendingAttachBodyId = null
                    pendingGroupBodyId = null
                }
                PhysicsCommand.Undo -> undoInternal()
                PhysicsCommand.Redo -> redoInternal()
                PhysicsCommand.ResetScene -> loadMapInternal(currentMapId, isDemo = false)
                is PhysicsCommand.SetEnvironment -> {
                    cmd.timeOfDayHours?.let { timeOfDayHours = it.coerceIn(0f, 24f) }
                    cmd.weatherType?.let { weatherType = it }
                    cmd.windStrength?.let { windStrength = it.coerceIn(0f, 20f) }
                    cmd.fluidBasinEnabled?.let { fluidBasinEnabled = it }
                    cmd.fluidLevelY?.let { fluidLevelY = it.coerceIn(-0.2f, 2.5f) }
                    cmd.timeScale?.let { timeScale = it.coerceIn(0.1f, 2.0f) }
                }
                is PhysicsCommand.SetPhysicsQuality -> solverIterations = cmd.solverIterations.coerceIn(4, 18)
                is PhysicsCommand.SetParticleBudget -> particleSystem.maxActiveBudget = cmd.maxParticles.coerceIn(40, ParticleSystem.HARD_POOL_LIMIT)
                is PhysicsCommand.SetMaxDynamicObjects -> {
                    maxDynamicObjects = cmd.maxObjects.coerceIn(40, 260)
                    trimExcessDebrisIfNeeded()
                }
                is PhysicsCommand.SetCameraBehavior -> {
                    thirdPersonDistance = cmd.thirdPersonDistance.coerceIn(2.0f, 6.0f)
                    thirdPersonHeight = cmd.thirdPersonHeight.coerceIn(0.15f, 1.15f)
                    cameraBobEnabled = cmd.headBobEnabled
                    cameraShakeEnabled = cmd.cameraShakeEnabled
                    movementSpeedScale = cmd.movementSpeedScale.coerceIn(0.6f, 1.6f)
                }
                is PhysicsCommand.SetPlayerPosture -> {
                    cmd.crouching?.let { isCrouching = it }
                    cmd.sprinting?.let { isSprinting = it }
                }
                is PhysicsCommand.SetToolSelected -> {
                    selectedToolType = cmd.tool
                    if (cmd.tool != ToolType.CATALOG_TOOL) {
                        weaponController.clearActiveFirearm()
                        adsBlend = 0f
                    } else if (FirearmCatalog.isFirearmTool(activeCatalogToolId)) {
                        weaponController.equipFirearmByToolId(activeCatalogToolId)
                    }
                    syncFirearmHudRef()
                }
                is PhysicsCommand.SetMagnetMode -> {
                    cmd.rangeMeters?.let { magnetRangeMeters = it.coerceIn(2f, 18f) }
                    cmd.repel?.let { magnetRepelMode = it }
                }
                PhysicsCommand.ToggleFlashlight -> {
                    flashlightActive = !flashlightActive
                    onToolSfx("switch", playerPos)
                }
                PhysicsCommand.ToggleStopwatch -> {
                    stopwatchRunning = !stopwatchRunning
                    onToolSfx("switch", playerPos)
                }
                PhysicsCommand.ResetStopwatch -> {
                    stopwatchSeconds = 0f
                    stopwatchRunning = false
                }
                is PhysicsCommand.SetFireInput -> {
                    if (cmd.pressed && selectedToolType == ToolType.CATALOG_TOOL && FirearmCatalog.isFirearmTool(activeCatalogToolId)) {
                        weaponController.setFirePressed(true)
                    } else {
                        weaponController.setFirePressed(false)
                    }
                }
                PhysicsCommand.TriggerSingleFire -> {
                    if (selectedToolType == ToolType.CATALOG_TOOL && FirearmCatalog.isFirearmTool(activeCatalogToolId)) {
                        weaponController.queueSingleTriggerTap()
                    }
                }
                is PhysicsCommand.SetAimState -> {
                    val targetAim = cmd.aiming ?: !weaponController.isAiming
                    weaponController.setAiming(targetAim)
                    syncFirearmHudRef()
                }
                PhysicsCommand.ReloadFirearm -> {
                    if (weaponController.startReload()) {
                        onToolSfx("gun_reload", playerPos)
                    }
                    syncFirearmHudRef()
                }
                PhysicsCommand.ToggleHolster -> {
                    weaponController.toggleHolster()
                    onToolSfx("switch", playerPos)
                    syncFirearmHudRef()
                }
                PhysicsCommand.InspectWeapon -> {
                    weaponController.triggerInspect()
                    onToolSfx("switch", playerPos)
                    syncFirearmHudRef()
                }
                PhysicsCommand.PickupNearbyAmmo -> {
                    pickupNearbyAmmoInternal()
                    syncFirearmHudRef()
                }
                is PhysicsCommand.SetFirearmSettings -> {
                    weaponController.autoReloadEnabled = cmd.autoReload
                }
            }
        }
    }

    private fun loadMapInternal(mapId: MapId, isDemo: Boolean) {
        currentMapId = mapId
        isMenuDemo = isDemo
        paused = false
        grabbedBodyId = null
        pendingAttachBodyId = null
        pendingGroupBodyId = null
        targetedBodyId = null
        bodies.clear()
        staticProps.clear()
        constraints.clear()
        electricalSolver.clear()
        particleSystem.clear()
        undoStack.clear()
        redoStack.clear()
        canUndoRef.set(false)
        canRedoRef.set(false)

        val built = if (isDemo) MapBuilder.buildMenuDemoScene() else MapBuilder.buildMap(mapId)
        staticProps.addAll(built.statics)
        bodies.addAll(built.dynamics)
        for (b in bodies) {
            electricalSolver.registerBody(b, b.electricalRole)
        }
        constraints.addAll(built.constraints)
        for (w in built.wires) {
            electricalSolver.addWire(w.id, w.bodyAId, w.bodyBId)
        }
        nextId = built.nextId
        playerPos = built.spawnPos
        playerVel = Vec3.ZERO
        cameraYaw = built.spawnYaw
        cameraPitch = -0.06f
        isCrouching = false
        effectiveCrouching = false
        crouchFraction = 0f
        isSprinting = false
        jumpCooldownTimer = 0f
        wasPlayerOnGround = true
        stridePhase = 0f
        menuDemoTime = 0f
        adsBlend = 0f
        weaponController.resetAmmoToDefaults()
        if (!isDemo && selectedToolType == ToolType.CATALOG_TOOL && FirearmCatalog.isFirearmTool(activeCatalogToolId)) {
            weaponController.equipFirearmByToolId(activeCatalogToolId)
        } else {
            weaponController.clearActiveFirearm()
        }
        syncFirearmHudRef()
        if (mapId == MapId.CONSTRUCTION || mapId == MapId.OUTDOOR_TESTING_YARD) {
            timeOfDayHours = 14.5f
        } else {
            timeOfDayHours = 13.5f
        }
    }

    private fun restoreStateInternal(cmd: PhysicsCommand.RestoreState) {
        loadMapInternal(cmd.mapId, isDemo = false)
        bodies.clear()
        constraints.clear()
        electricalSolver.clear()
        var maxId = nextId
        for (sb in cmd.bodies) {
            val rb = buildBodyFromSaved(sb)
            bodies.add(rb)
            electricalSolver.registerBody(rb, rb.electricalRole)
            maxId = max(maxId, sb.id + 1)
        }
        constraints.addAll(cmd.constraints)
        for (w in cmd.wires) {
            electricalSolver.addWire(w.id, w.bodyAId, w.bodyBId)
            maxId = max(maxId, w.id + 1)
        }
        nextId = max(maxId, (cmd.constraints.maxOfOrNull { it.id } ?: maxId) + 1)
        playerPos = cmd.playerPos
        playerVel = Vec3.ZERO
        cameraYaw = cmd.playerYaw
        cameraPitch = cmd.playerPitch
        if (cmd.weaponMagAmmo.isNotEmpty() || cmd.weaponReserveAmmo.isNotEmpty()) {
            weaponController.restoreAmmoSnapshot(cmd.weaponMagAmmo, cmd.weaponReserveAmmo)
        }
        if (cmd.isWeaponHolstered && weaponController.activeSpec != null && !weaponController.isHolstered) {
            weaponController.toggleHolster()
        }
        syncFirearmHudRef()
    }

    private fun currentEyeHeight(): Float =
        PLAYER_EYE_HEIGHT + (CROUCH_EYE_HEIGHT - PLAYER_EYE_HEIGHT) * crouchFraction

    private fun currentColliderHeight(): Float =
        PLAYER_HEIGHT + (CROUCH_HEIGHT - PLAYER_HEIGHT) * crouchFraction

    private fun getCameraBasis(): Triple<Vec3, Vec3, Vec3> {
        val eyeHeight = currentEyeHeight()
        val horizSpeed = hypot(playerVel.x, playerVel.z)
        val bobY = if (!isMenuDemo && cameraMode == CameraMode.FIRST_PERSON && cameraBobEnabled && playerOnGround && horizSpeed > 0.45f) {
            sin(stridePhase) * 0.028f * (horizSpeed / 6f).coerceIn(0f, 1.2f)
        } else 0f
        val shakeOffset = if (!isMenuDemo && cameraShakeEnabled && cameraShakeAmp > 0.002f) {
            Vec3(
                sin(menuDemoTime * 53f) * cameraShakeAmp,
                cos(menuDemoTime * 67f) * cameraShakeAmp,
                0f
            )
        } else Vec3.ZERO

        val forward = if (isMenuDemo) {
            val angle = menuDemoTime * 0.22f
            val camP = Vec3(sin(angle) * 9.2f, 4.6f + sin(menuDemoTime * 0.4f) * 0.6f, cos(angle) * 9.2f)
            (Vec3(0f, 1.8f, 0f) - camP).normalized()
        } else lookDirection()

        val eyePos = if (isMenuDemo) {
            val angle = menuDemoTime * 0.22f
            Vec3(sin(angle) * 9.2f, 4.6f + sin(menuDemoTime * 0.4f) * 0.6f, cos(angle) * 9.2f)
        } else when (cameraMode) {
            CameraMode.FIRST_PERSON, CameraMode.FREE_CAMERA ->
                playerPos + Vec3(0f, eyeHeight + bobY, 0f) + shakeOffset
            CameraMode.THIRD_PERSON -> {
                val shoulderOrigin = playerPos + Vec3(0f, eyeHeight + thirdPersonHeight, 0f)
                val backDir = -forward
                val clipHit = CollisionDetector.raycastBodiesAndStatics(
                    origin = shoulderOrigin,
                    dir = backDir,
                    maxDistance = thirdPersonDistance + 0.35f,
                    bodies = emptyList(),
                    statics = staticProps
                )
                val safeDist = if (clipHit != null) {
                    (clipHit.distance - 0.32f).coerceIn(0.65f, thirdPersonDistance)
                } else {
                    thirdPersonDistance
                }
                val desired = shoulderOrigin + backDir * safeDist + shakeOffset
                desired.copy(y = max(desired.y, playerPos.y + 0.35f))
            }
        }
        val right = forward.cross(Vec3.UP).normalized()
        return Triple(eyePos, forward, right)
    }

    private fun lookDirection(): Vec3 {
        val cp = cos(cameraPitch)
        return Vec3(sin(cameraYaw) * cp, sin(cameraPitch), -cos(cameraYaw) * cp).normalized()
    }

    private fun updateMenuDemoCamera(dt: Float) {
        val angle = menuDemoTime * 0.22f
        val camPos = Vec3(sin(angle) * 9.2f, 4.6f, cos(angle) * 9.2f)
        val toCenter = (Vec3(0f, 1.8f, 0f) - camPos).normalized()
        cameraYaw = atan2(toCenter.x, -toCenter.z)
        cameraPitch = asin(toCenter.y.coerceIn(-1f, 1f))
        if ((menuDemoTime / 7.5f).toInt() != ((menuDemoTime - dt) / 7.5f).toInt()) {
            bodies.firstOrNull { it.type == ObjectType.STEEL_SPHERE }?.let { ball ->
                ball.wake()
                ball.linearVelocity = Vec3(4.5f, 1.2f, -1.5f)
            }
        }
    }

    fun canStandUp(): Boolean {
        val standHalfH = PLAYER_HEIGHT * 0.5f
        val standCenter = playerPos + Vec3(0f, standHalfH, 0f)
        val standRadius = PLAYER_RADIUS * 0.88f
        for (prop in staticProps) {
            if (!prop.isCollider) continue
            val localP = prop.orientation.conjugate().rotate(standCenter - prop.position)
            val he = prop.halfExtents
            if (abs(localP.x) < he.x + standRadius &&
                abs(localP.y) < he.y + standHalfH - 0.04f &&
                abs(localP.z) < he.z + standRadius
            ) {
                return false
            }
        }
        return true
    }

    private fun updatePlayer(dt: Float) {
        if (jumpCooldownTimer > 0f) jumpCooldownTimer = (jumpCooldownTimer - dt).coerceAtLeast(0f)
        if (interactPulse > 0f) interactPulse = (interactPulse - dt * 2.8f).coerceAtLeast(0f)
        if (releasePulse > 0f) releasePulse = (releasePulse - dt * 3.2f).coerceAtLeast(0f)
        if (landingImpulse > 0f) landingImpulse = (landingImpulse - dt * 3.5f).coerceAtLeast(0f)
        if (cameraShakeAmp > 0f) cameraShakeAmp = (cameraShakeAmp - dt * 0.12f).coerceAtLeast(0f)

        if (cameraMode == CameraMode.FREE_CAMERA) {
            val forward = lookDirection()
            val right = forward.cross(Vec3.UP).normalized()
            var flyVec = forward * moveInputY + right * moveInputX
            if (jumpRequested) {
                flyVec += Vec3(0f, 1f, 0f)
                jumpRequested = false
            }
            val flySpeed = (if (isSprinting) 14.5f else 9.5f) * movementSpeedScale
            playerPos += flyVec * (flySpeed * dt)
            playerVel = Vec3.ZERO
            listenerTransform = Pair(playerPos + Vec3(0f, currentEyeHeight(), 0f), cameraYaw)
            return
        }

        // Resolve crouch posture with automatic ceiling clearance check
        effectiveCrouching = if (isCrouching) true else !canStandUp()
        val targetCrouchFrac = if (effectiveCrouching) 1f else 0f
        crouchFraction += (targetCrouchFrac - crouchFraction) * min(1f, dt * 11f)

        val forwardFlat = Vec3(sin(cameraYaw), 0f, -cos(cameraYaw)).normalized()
        val rightFlat = Vec3(cos(cameraYaw), 0f, sin(cameraYaw)).normalized()
        val moveDir = (forwardFlat * moveInputY + rightFlat * moveInputX)
        val clampedMove = if (moveDir.lengthSq() > 1f) moveDir.normalized() else moveDir

        val sprintActive = isSprinting && !effectiveCrouching && moveInputY > 0.1f && hypot(clampedMove.x, clampedMove.z) > 0.3f
        val baseMoveSpeed = when {
            effectiveCrouching -> 2.85f
            sprintActive -> 8.6f
            else -> 5.4f
        } * movementSpeedScale

        val targetVx = clampedMove.x * baseMoveSpeed
        val targetVz = clampedMove.z * baseMoveSpeed
        val accel = if (playerOnGround) 16f else 5.5f

        playerVel = Vec3(
            playerVel.x + (targetVx - playerVel.x) * min(1f, accel * dt),
            playerVel.y + GRAVITY * dt,
            playerVel.z + (targetVz - playerVel.z) * min(1f, accel * dt)
        )

        // Single jump with strict grounded check & cooldown (prevents mid-air jumping)
        if (jumpRequested) {
            if (playerOnGround && jumpCooldownTimer <= 0f) {
                playerVel = playerVel.copy(y = if (effectiveCrouching) 5.2f else 6.45f)
                playerOnGround = false
                jumpCooldownTimer = JUMP_COOLDOWN_SEC
                onToolSfx("jump", playerPos)
            }
            jumpRequested = false
        }

        val prevFallSpeed = -playerVel.y
        playerPos += playerVel * dt
        wasPlayerOnGround = playerOnGround
        playerOnGround = false
        var detectedGroundAcoustic = AcousticClass.CONCRETE

        val colHeight = currentColliderHeight()
        val halfH = colHeight * 0.5f
        val playerCenter = playerPos + Vec3(0f, halfH, 0f)
        var resolvedCenter = playerCenter

        for (prop in staticProps) {
            if (!prop.isCollider) continue
            val invRot = prop.orientation.conjugate()
            val localP = invRot.rotate(resolvedCenter - prop.position)
            val he = prop.halfExtents
            val extX = he.x + PLAYER_RADIUS
            val extY = he.y + halfH
            val extZ = he.z + PLAYER_RADIUS
            if (abs(localP.x) < extX && abs(localP.y) < extY && abs(localP.z) < extZ) {
                val ox = extX - abs(localP.x)
                val oy = extY - abs(localP.y)
                val oz = extZ - abs(localP.z)
                val pushLocal = when {
                    oy <= ox && oy <= oz -> Vec3(0f, if (localP.y >= 0f) oy else -oy, 0f)
                    ox <= oz -> Vec3(if (localP.x >= 0f) ox else -ox, 0f, 0f)
                    else -> Vec3(0f, 0f, if (localP.z >= 0f) oz else -oz)
                }
                val pushWorld = prop.orientation.rotate(pushLocal)
                resolvedCenter += pushWorld
                if (pushWorld.y > 0.35f * pushWorld.length()) {
                    playerOnGround = true
                    detectedGroundAcoustic = prop.material.acousticClass
                    if (playerVel.y < 0f) playerVel = playerVel.copy(y = 0f)
                    if (prop.conveyorVelocity.lengthSq() > 0.01f) {
                        resolvedCenter += prop.conveyorVelocity * dt * 0.7f
                    }
                } else if (pushWorld.y < -0.5f * pushWorld.length() && playerVel.y > 0f) {
                    playerVel = playerVel.copy(y = 0f)
                }
            }
        }

        for (body in bodies) {
            if (body.isDestroyed || body.id == grabbedBodyId || body.collisionMode == CollisionMode.TRIGGER) continue
            val invRot = body.orientation.conjugate()
            val localP = invRot.rotate(resolvedCenter - body.position)
            val he = body.effectiveHalfExtents
            val extX = he.x + PLAYER_RADIUS
            val extY = he.y + halfH
            val extZ = he.z + PLAYER_RADIUS
            if (abs(localP.x) < extX && abs(localP.y) < extY && abs(localP.z) < extZ) {
                val ox = extX - abs(localP.x)
                val oy = extY - abs(localP.y)
                val oz = extZ - abs(localP.z)
                val pushLocal = when {
                    oy <= ox && oy <= oz -> Vec3(0f, if (localP.y >= 0f) oy else -oy, 0f)
                    ox <= oz -> Vec3(if (localP.x >= 0f) ox else -ox, 0f, 0f)
                    else -> Vec3(0f, 0f, if (localP.z >= 0f) oz else -oz)
                }
                val pushWorld = body.orientation.rotate(pushLocal)
                if (pushWorld.y > 0.5f * pushWorld.length()) {
                    resolvedCenter += pushWorld
                    playerOnGround = true
                    detectedGroundAcoustic = body.materialSpec.acousticClass
                    if (playerVel.y < 0f) playerVel = playerVel.copy(y = 0f)
                } else {
                    val pushFactor = if (body.isFrozen) 1f else 0.55f
                    resolvedCenter += pushWorld * pushFactor
                    if (!body.isFrozen) {
                        body.applyImpulseAtPoint(-pushWorld.normalized() * 14f, resolvedCenter)
                    }
                }
            }
        }

        if (playerOnGround) {
            playerGroundAcoustic = detectedGroundAcoustic
            if (!wasPlayerOnGround && prevFallSpeed > 3.2f) {
                landingImpulse = (prevFallSpeed / 10f).coerceIn(0.35f, 1.4f)
                onToolSfx("land", playerPos)
                if (cameraShakeEnabled) {
                    cameraShakeAmp = (prevFallSpeed * 0.0045f).coerceAtMost(0.035f)
                }
            }
        }

        // Gait-phase synced surface footsteps (never fires while idle or airborne)
        val horizSpeed = hypot(playerVel.x, playerVel.z)
        if (!isMenuDemo && playerOnGround && horizSpeed > 0.55f) {
            val prevHalfCycle = floor(stridePhase / PI.toFloat()).toInt()
            val cadenceRadPerSec = (horizSpeed * 1.95f).coerceIn(3.5f, 14.0f)
            stridePhase += cadenceRadPerSec * dt
            val nextHalfCycle = floor(stridePhase / PI.toFloat()).toInt()
            if (nextHalfCycle != prevHalfCycle) {
                footstepTriggerSeq.incrementAndGet()
                val stepTag = when (playerGroundAcoustic) {
                    AcousticClass.METAL -> "step_metal"
                    AcousticClass.WOOD -> "step_wood"
                    AcousticClass.GLASS -> "step_metal"
                    AcousticClass.RUBBER -> "step_concrete"
                    AcousticClass.CONCRETE -> "step_concrete"
                }
                onToolSfx(stepTag, playerPos)
            }
        }

        playerPos = resolvedCenter - Vec3(0f, halfH, 0f)
        if (playerPos.y < -12f) {
            playerPos = Vec3(0f, 2f, 6f)
            playerVel = Vec3.ZERO
        }
        listenerTransform = Pair(playerPos + Vec3(0f, currentEyeHeight(), 0f), cameraYaw)
    }

    private fun trimExcessDebrisIfNeeded() {
        var excess = bodies.size - maxDynamicObjects
        if (excess <= 0) return
        val iter = bodies.iterator()
        while (iter.hasNext() && excess > 0) {
            val b = iter.next()
            if (b.isDebris && b.id != grabbedBodyId && b.id != targetedBodyId) {
                electricalSolver.removeBody(b.id)
                constraints.removeAll { it.bodyAId == b.id || it.bodyBId == b.id }
                iter.remove()
                excess--
            }
        }
    }

    private fun stepPhysicsSimulation(dt: Float) {
        if (stopwatchRunning) stopwatchSeconds += dt
        if (toolActionPulse > 0f) toolActionPulse = (toolActionPulse - dt * 4f).coerceAtLeast(0f)
        val activeTool = ToolCatalog.get(activeCatalogToolId)
        if (activeTool?.animatedSpin == true && toolActionPulse > 0.05f) {
            toolSpinAngle = (toolSpinAngle + dt * 22f) % (2f * PI.toFloat())
        } else if (activeTool?.animatedSpin == true) {
            toolSpinAngle = (toolSpinAngle + dt * 1.4f) % (2f * PI.toFloat())
        }

        if (!isMenuDemo && selectedToolType == ToolType.CATALOG_TOOL && FirearmCatalog.isFirearmTool(activeCatalogToolId)) {
            weaponController.step(
                dt = dt,
                onFireShot = ::handleFirearmShot,
                onSfx = { tag -> onToolSfx(tag, playerPos) }
            )
            val targetAds = if (weaponController.isAiming) 1f else 0f
            adsBlend += (targetAds - adsBlend) * min(1f, dt * 13f)
        } else {
            adsBlend = (adsBlend - dt * 12f).coerceAtLeast(0f)
        }

        val (camEye, _, _) = getCameraBasis()
        particleSystem.emitWeatherStep(camEye, weatherType, windStrength)
        particleSystem.step(dt, GRAVITY)

        for (b in bodies) {
            b.prevPosition = b.position
            b.prevOrientation = b.orientation
            if (b.squashAmount > 0.001f) {
                b.squashAmount = (b.squashAmount * (1f - dt * 5.5f)).coerceAtLeast(0f)
            }
            if (b.collisionMode == CollisionMode.NO_SLEEP && b.isSleeping && !b.isFrozen) {
                b.wake()
            }
        }

        val heldBody = grabbedBodyId?.let { id -> bodies.firstOrNull { it.id == id && !it.isDestroyed } }
        if (heldBody != null && !heldBody.isFrozen) {
            val (eye, forward, _) = getCameraBasis()
            val targetPos = eye + forward * grabDistance
            val posErr = targetPos - heldBody.position
            val desiredVel = posErr * 12f
            val clampedVel = if (desiredVel.length() > 24f) desiredVel.normalized() * 24f else desiredVel
            heldBody.wake()
            heldBody.linearVelocity = heldBody.linearVelocity.lerp(clampedVel, 0.45f)
            val rotErr = grabTargetRotOffset * heldBody.orientation.conjugate()
            val angErr = Vec3(rotErr.x, rotErr.y, rotErr.z) * (if (rotErr.w < 0f) -2f else 2f)
            heldBody.angularVelocity = heldBody.angularVelocity.lerp(angErr * 8f, 0.35f)
        }

        val activeWind = when {
            windStrength > 0.05f -> windStrength
            weatherType == WeatherType.WINDY -> 7.5f
            else -> 0f
        }

        for (b in bodies) {
            if (b.isDestroyed) continue

            // Thermal relaxation & melting/softening for low-melting materials (Wax, Ice, Snow)
            if (abs(b.temperatureC - 22f) > 0.2f) {
                b.temperatureC += (22f - b.temperatureC) * dt * 0.08f
            }
            if (b.materialSpec.destructionProfile == DestructionProfile.MELT_WAX &&
                b.temperatureC > b.materialSpec.meltingPointC && !b.isFrozen
            ) {
                val excess = ((b.temperatureC - b.materialSpec.meltingPointC) / 60f).coerceIn(0f, 1f)
                b.squashAmount = max(b.squashAmount, excess * 0.42f)
                b.health -= excess * 18f * dt
                if (b.health <= 0f) {
                    b.isDestroyed = true
                    particleSystem.emitBurst(b.position, ParticleType.FOAM_SPRAY, 10, 2.2f)
                    continue
                }
            }

            if (b.isFrozen || b.isSleeping) continue

            b.linearVelocity += Vec3(0f, GRAVITY * b.gravityScale * dt, 0f)

            // Fluid Buoyancy tank (Archimedes upward force + hydrodynamic drag)
            if (fluidBasinEnabled) {
                val he = b.effectiveHalfExtents
                val bottomY = b.position.y - he.y
                if (bottomY < fluidLevelY) {
                    b.wake()
                    val height = (he.y * 2f).coerceAtLeast(0.1f)
                    val submergedFrac = ((fluidLevelY - bottomY) / height).coerceIn(0f, 1f)
                    val volume = (he.x * 2f) * (he.y * 2f) * (he.z * 2f)
                    val displacedWaterMass = volume * 1000f * 0.02f * submergedFrac
                    val buoyantAcc = (displacedWaterMass * abs(GRAVITY)) * b.invMass
                    b.linearVelocity += Vec3(0f, buoyantAcc * dt, 0f)
                    val waterDrag = (1f - 2.6f * submergedFrac * dt).coerceIn(0.4f, 1f)
                    b.linearVelocity *= waterDrag
                    b.angularVelocity *= waterDrag
                }
            }

            // Directional wind force
            if (activeWind > 0.05f) {
                val areaFactor = (b.effectiveRadius * b.effectiveRadius).coerceIn(0.15f, 4f)
                val windForce = Vec3(activeWind * areaFactor * 2.2f, 0f, activeWind * areaFactor * 0.4f)
                b.linearVelocity += windForce * (b.invMass * dt)
            }

            b.linearVelocity += b.forceAccum * (b.invMass * dt)
            b.angularVelocity += b.invInertiaWorld.multiplyVec(b.torqueAccum) * dt
            b.forceAccum = Vec3.ZERO
            b.torqueAccum = Vec3.ZERO

            val linDamp = (1f - b.material.linearDamping * dt).coerceIn(0f, 1f)
            val angDamp = (1f - b.material.angularDamping * dt).coerceIn(0f, 1f)
            b.linearVelocity *= linDamp
            b.angularVelocity *= angDamp
        }

        // Electrical Circuit Simulation Step
        val bodiesById = HashMap<Int, RigidBody>(bodies.size)
        for (b in bodies) {
            if (!b.isDestroyed) bodiesById[b.id] = b
        }
        electricalSolver.step(bodiesById, dt)
        for (b in bodies) {
            val node = electricalSolver.nodes[b.id] ?: continue
            if (node.overloaded && !b.isDestroyed && (menuDemoTime * 10f).toInt() % 6 == 0) {
                particleSystem.emitBurst(b.position, ParticleType.SPARK, 5, 3.8f)
            }
            b.emissiveIntensity = when (node.role) {
                ElectricalRole.LIGHT_BULB -> if (node.energized) (node.voltage / 12f).coerceIn(0.5f, 2.2f) else 0f
                ElectricalRole.LED -> if (node.energized) 1.6f else 0f
                ElectricalRole.ELECTRICAL_PANEL -> if (node.switchClosed) 0.55f else 0.05f
                ElectricalRole.BATTERY -> 0.25f
                ElectricalRole.FUSE -> if (node.isBlown) 0f else if (node.energized) 0.45f else 0f
                else -> if (node.energized) 0.35f else 0f
            }
            b.emissiveColor = when {
                node.overloaded || node.isBlown -> Vec3(1f, 0.22f, 0.15f)
                node.role == ElectricalRole.LED -> Vec3(0.20f, 0.95f, 0.55f)
                node.role == ElectricalRole.LIGHT_BULB -> Vec3(1.0f, 0.88f, 0.55f)
                else -> Vec3(0.25f, 0.78f, 1.0f)
            }
        }

        applyForceConstraints(dt)

        // Spatial Hash Broadphase for Dynamic-Dynamic + Dynamic-Static pairs
        val manifolds = ArrayList<ContactManifold>(96)
        val count = bodies.size
        broadphaseBuckets.clear()
        broadphasePairs.clear()
        val cellSize = 2.6f
        for (i in 0 until count) {
            val a = bodies[i]
            if (a.isDestroyed || a.collisionMode == CollisionMode.TRIGGER) continue
            val r = a.effectiveRadius + 0.1f
            val minX = floor((a.position.x - r) / cellSize).toInt()
            val maxX = floor((a.position.x + r) / cellSize).toInt()
            val minY = floor((a.position.y - r) / cellSize).toInt()
            val maxY = floor((a.position.y + r) / cellSize).toInt()
            val minZ = floor((a.position.z - r) / cellSize).toInt()
            val maxZ = floor((a.position.z + r) / cellSize).toInt()
            for (gx in minX..maxX) {
                for (gy in minY..maxY) {
                    for (gz in minZ..maxZ) {
                        val key = ((gx.toLong() and 0x1FFFFFL) shl 42) or
                            ((gy.toLong() and 0x1FFFFFL) shl 21) or
                            (gz.toLong() and 0x1FFFFFL)
                        val bucket = broadphaseBuckets.getOrPut(key) { ArrayList(6) }
                        for (j in bucket) {
                            val lo = min(i, j)
                            val hi = max(i, j)
                            val pairKey = (lo.toLong() shl 32) or hi.toLong()
                            if (broadphasePairs.add(pairKey)) {
                                val b = bodies[j]
                                if (!b.isDestroyed) {
                                    CollisionDetector.collideDynamicPair(a, b)?.let { manifolds.add(it) }
                                }
                            }
                        }
                        bucket.add(i)
                    }
                }
            }
            if (!a.isFrozen && !a.isSleeping) {
                for (prop in staticProps) {
                    CollisionDetector.collideDynamicStatic(a, prop)?.let { manifolds.add(it) }
                }
            }
        }

        for (m in manifolds) {
            val b = m.bodyB
            if (b != null) {
                if (!m.bodyA.isSleeping && b.isSleeping && !b.isFrozen) b.wake()
                if (!b.isSleeping && m.bodyA.isSleeping && !m.bodyA.isFrozen) m.bodyA.wake()
            }
        }

        val spawnedFragments = ArrayList<RigidBody>()
        for (m in manifolds) {
            val a = m.bodyA
            val b = m.bodyB
            val contact = m.contacts.firstOrNull() ?: continue
            val rA = contact.position - a.position
            val vA = a.linearVelocity + a.angularVelocity.cross(rA)
            val vB = if (b != null) {
                val rB = contact.position - b.position
                b.linearVelocity + b.angularVelocity.cross(rB)
            } else {
                m.staticProp?.conveyorVelocity ?: Vec3.ZERO
            }
            val relNormalSpeed = -(vB - vA).dot(m.normal)
            if (relNormalSpeed > 1.6f) {
                val matB = b?.materialSpec?.acousticClass ?: m.staticProp?.material?.acousticClass ?: AcousticClass.CONCRETE
                onAudioEvent(CollisionAudioEvent(a.materialSpec.acousticClass, matB, relNormalSpeed, contact.position))

                // Soft-body squish impulse on foam/rubber/fabric materials
                if (a.materialSpec.softnessSquish > 0.05f) {
                    a.squashAmount = max(a.squashAmount, (relNormalSpeed * 0.045f).coerceAtMost(a.materialSpec.softnessSquish))
                }
                if (b != null && b.materialSpec.softnessSquish > 0.05f) {
                    b.squashAmount = max(b.squashAmount, (relNormalSpeed * 0.045f).coerceAtMost(b.materialSpec.softnessSquish))
                }

                // Material-specific impact sparks or dust
                if (relNormalSpeed > 7.5f) {
                    if (a.materialSpec.category == MaterialCategory.METALS) {
                        particleSystem.emitBurst(contact.position, ParticleType.SPARK, 5, 3.6f)
                    } else if (a.materialSpec.category == MaterialCategory.CONSTRUCTION) {
                        particleSystem.emitBurst(contact.position, ParticleType.CONCRETE_DUST, 4, 2.0f)
                    }
                }
                if (relNormalSpeed > 11.5f) {
                    spawnedFragments.addAll(applyImpactDamage(a, relNormalSpeed))
                    if (b != null) spawnedFragments.addAll(applyImpactDamage(b, relNormalSpeed))
                }
            }
        }

        for (iter in 0 until solverIterations) {
            for (m in manifolds) solveManifold(m, dt)
            solveImpulseConstraints(dt)
        }

        for (b in bodies) {
            if (b.isDestroyed || b.isFrozen || b.isSleeping) continue
            if (b.linearVelocity.lengthSq() > 2500f) b.linearVelocity = b.linearVelocity.normalized() * 50f
            if (b.angularVelocity.lengthSq() > 400f) b.angularVelocity = b.angularVelocity.normalized() * 20f

            b.position += b.linearVelocity * dt
            if (b.angularVelocity.lengthSq() > 1e-6f) {
                b.orientation = b.orientation.integrateAngularVelocity(b.angularVelocity, dt)
                b.updateWorldInertia()
            }

            if (b.position.y < -25f) {
                b.position = Vec3(0f, 3f, 0f)
                b.linearVelocity = Vec3.ZERO
                b.angularVelocity = Vec3.ZERO
            }

            if (b.id != grabbedBodyId &&
                b.collisionMode != CollisionMode.NO_SLEEP &&
                b.linearVelocity.lengthSq() < 0.015f &&
                b.angularVelocity.lengthSq() < 0.025f
            ) {
                b.sleepTimer += dt
                if (b.sleepTimer > 0.65f) {
                    b.isSleeping = true
                    b.linearVelocity = Vec3.ZERO
                    b.angularVelocity = Vec3.ZERO
                }
            } else {
                b.sleepTimer = 0f
            }
        }

        if (spawnedFragments.isNotEmpty()) {
            bodies.addAll(spawnedFragments)
            trimExcessDebrisIfNeeded()
        }

        val grenadeBlastFragments = ArrayList<RigidBody>()
        for (b in bodies) {
            if (b.isDestroyed || !b.isGrenade) continue
            if (b.isArmed) {
                b.fuseTimerSec -= dt
                val blink = 0.45f + 0.85f * abs(sin(b.fuseTimerSec * 14f))
                b.emissiveColor = if (b.type == ObjectType.GRENADE_FRAG) Vec3(1.0f, 0.28f, 0.08f) else Vec3(0.30f, 0.86f, 0.96f)
                b.emissiveIntensity = blink
                if (b.fuseTimerSec <= 0f) {
                    b.isArmed = false
                    b.fuseTimerSec = 0f
                    if (b.type == ObjectType.GRENADE_FRAG) {
                        detonateFragGrenade(b, grenadeBlastFragments)
                    } else {
                        b.smokeActiveTimerSec = 7.5f
                        b.emissiveColor = Vec3(0.45f, 0.82f, 0.92f)
                        b.emissiveIntensity = 0.65f
                        particleSystem.emitBurst(b.position + Vec3(0f, 0.18f, 0f), ParticleType.FOAM_SPRAY, 18, 3.6f, Vec3.UP)
                        particleSystem.emitBurst(b.position + Vec3(0f, 0.12f, 0f), ParticleType.CONCRETE_DUST, 14, 2.8f, Vec3.UP)
                        onToolSfx("impulse", b.position)
                    }
                }
            } else if (b.smokeActiveTimerSec > 0f) {
                b.smokeActiveTimerSec -= dt
                b.emissiveColor = Vec3(0.45f, 0.82f, 0.92f)
                b.emissiveIntensity = 0.35f
                particleSystem.emitBurst(b.position + Vec3(0f, 0.16f, 0f), ParticleType.FOAM_SPRAY, 3, 2.6f, Vec3.UP)
                particleSystem.emitBurst(b.position + Vec3(0f, 0.22f, 0f), ParticleType.CONCRETE_DUST, 2, 2.2f, Vec3.UP)
                if (b.smokeActiveTimerSec <= 0f) {
                    b.isDestroyed = true
                }
            }
        }
        if (grenadeBlastFragments.isNotEmpty()) {
            bodies.addAll(grenadeBlastFragments)
            trimExcessDebrisIfNeeded()
        }

        if (bodies.any { it.isDestroyed }) {
            val deadIds = bodies.filter { it.isDestroyed }.map { it.id }.toSet()
            bodies.removeAll { it.isDestroyed }
            constraints.removeAll { it.bodyAId in deadIds || it.bodyBId in deadIds }
            for (id in deadIds) electricalSolver.removeBody(id)
            if (grabbedBodyId in deadIds) grabbedBodyId = null
        }
    }

    private fun detonateFragGrenade(grenade: RigidBody, spawnedFragments: MutableList<RigidBody>) {
        grenade.isDestroyed = true
        val center = grenade.position
        onToolSfx("explosion", center)
        particleSystem.emitBurst(center, ParticleType.SPARK, 34, 9.5f, Vec3.UP)
        particleSystem.emitBurst(center, ParticleType.CONCRETE_DUST, 24, 6.2f, Vec3.UP)
        if (cameraShakeEnabled && (center - playerPos).length() < 18f) {
            cameraShakeAmp = max(cameraShakeAmp, 0.048f)
        }

        val blastRadius = 6.5f
        for (other in bodies) {
            if (other.id == grenade.id || other.isDestroyed) continue
            val delta = other.position - center
            val dist = delta.length()
            if (dist >= blastRadius) continue
            val falloff = (1f - dist / blastRadius).coerceIn(0f, 1f)
            val dir = if (dist > 0.02f) delta / dist else Vec3.UP
            if (!other.isFrozen) {
                other.wake()
                val blastImpulse = (dir + Vec3(0f, 0.35f, 0f)).normalized() * (165f * falloff)
                other.applyImpulseAtPoint(blastImpulse, other.position)
            }
            if (other.isGrenade && !other.isArmed && dist < 4.5f) {
                other.armGrenade(0.25f)
            }
            if (!other.isFrozen && (other.type.destructible || other.materialSpec.destructionProfile != DestructionProfile.NONE)) {
                val dmg = 140f * falloff
                other.health -= dmg
                other.crackLevel = ((100f - other.health) / 100f).coerceIn(0f, 1f)
                if (other.health <= 0f && !other.isDestroyed) {
                    other.isDestroyed = true
                    spawnedFragments.addAll(
                        DestructionHelper.spawnDebrisForBody(
                            other,
                            { nextId++ },
                            bodies.size + spawnedFragments.size,
                            particleSystem
                        )
                    )
                }
            }
        }
    }

    fun armBodyIfGrenade(bodyId: Int, fuseSec: Float = 2.6f): Boolean {
        val body = bodies.firstOrNull { it.id == bodyId && !it.isDestroyed } ?: return false
        if (!body.isGrenade) return false
        body.armGrenade(fuseSec)
        return true
    }

    private fun applyImpactDamage(body: RigidBody, speed: Float): List<RigidBody> {
        val profile = body.materialSpec.destructionProfile
        if ((!body.type.destructible && profile == DestructionProfile.NONE) || body.isFrozen) return emptyList()
        val threshold = when (profile) {
            DestructionProfile.SHATTER_GLASS -> 8.5f
            DestructionProfile.SPLINTER_WOOD, DestructionProfile.SNAP_PLASTIC -> 11.5f
            DestructionProfile.FRACTURE_CONCRETE -> 13.0f
            else -> 16.0f
        }
        if (speed < threshold) return emptyList()
        val dmg = (speed - threshold + 2f) * (10f / body.materialSpec.hardness.coerceAtLeast(1.2f)) * 4f
        body.health -= dmg
        body.crackLevel = ((100f - body.health) / 100f).coerceIn(0f, 1f)
        if (body.health <= 0f) {
            body.isDestroyed = true
            onToolSfx("shatter", body.position)
            return DestructionHelper.spawnDebrisForBody(body, { nextId++ }, bodies.size, particleSystem)
        }
        return emptyList()
    }

    private fun solveManifold(m: ContactManifold, dt: Float) {
        val a = m.bodyA
        val b = m.bodyB
        val invMassA = a.invMass
        val invMassB = b?.invMass ?: 0f
        val totalInvMass = invMassA + invMassB
        if (totalInvMass <= 0f) return

        val restitution = max(a.effectiveRestitution, b?.effectiveRestitution ?: m.staticProp?.material?.restitution ?: 0.1f)
        val fricB = b?.effectiveDynamicFriction ?: m.staticProp?.material?.dynamicFriction ?: 0.4f
        val friction = sqrt(a.effectiveDynamicFriction * fricB)
        val conveyorVel = m.staticProp?.conveyorVelocity ?: Vec3.ZERO

        val contactCount = m.contacts.size.toFloat()
        for (c in m.contacts) {
            val n = m.normal
            val rA = c.position - a.position
            val rB = if (b != null) c.position - b.position else Vec3.ZERO

            val vA = a.linearVelocity + a.angularVelocity.cross(rA)
            val vB = if (b != null) b.linearVelocity + b.angularVelocity.cross(rB) else conveyorVel
            val relVel = vB - vA
            val vn = relVel.dot(n)

            val rnA = rA.cross(n)
            val rnB = rB.cross(n)
            val angA = a.invInertiaWorld.multiplyVec(rnA).cross(rA).dot(n)
            val angB = if (b != null) b.invInertiaWorld.multiplyVec(rnB).cross(rB).dot(n) else 0f
            val effMass = totalInvMass + angA + angB
            if (effMass <= 1e-6f) continue

            val slop = 0.004f
            val baumgarte = (0.24f / dt) * max(0f, c.penetration - slop)
            val bounce = if (vn < -1.1f) restitution * (-vn) else 0f
            val jn = max(0f, (-vn + bounce + baumgarte) / effMass) / contactCount

            val impulseN = n * jn
            if (!a.isFrozen) {
                a.linearVelocity -= impulseN * invMassA
                a.angularVelocity -= a.invInertiaWorld.multiplyVec(rA.cross(impulseN))
            }
            if (b != null && !b.isFrozen) {
                b.linearVelocity += impulseN * invMassB
                b.angularVelocity += b.invInertiaWorld.multiplyVec(rB.cross(impulseN))
            }

            val vA2 = a.linearVelocity + a.angularVelocity.cross(rA)
            val vB2 = if (b != null) b.linearVelocity + b.angularVelocity.cross(rB) else conveyorVel
            val relVel2 = vB2 - vA2
            val tangent = relVel2 - n * relVel2.dot(n)
            val tLenSq = tangent.lengthSq()
            if (tLenSq > 1e-6f) {
                val tDir = tangent / sqrt(tLenSq)
                val rtA = rA.cross(tDir)
                val rtB = rB.cross(tDir)
                val tAngA = a.invInertiaWorld.multiplyVec(rtA).cross(rA).dot(tDir)
                val tAngB = if (b != null) b.invInertiaWorld.multiplyVec(rtB).cross(rB).dot(tDir) else 0f
                val tEffMass = totalInvMass + tAngA + tAngB
                if (tEffMass > 1e-6f) {
                    val vt = relVel2.dot(tDir)
                    val jt = (vt / tEffMass).coerceIn(-jn * friction, jn * friction)
                    val impulseT = tDir * jt
                    if (!a.isFrozen) {
                        a.linearVelocity += impulseT * invMassA
                        a.angularVelocity += a.invInertiaWorld.multiplyVec(rA.cross(impulseT))
                    }
                    if (b != null && !b.isFrozen) {
                        b.linearVelocity -= impulseT * invMassB
                        b.angularVelocity -= b.invInertiaWorld.multiplyVec(rB.cross(impulseT))
                    }
                }
            }
        }
    }

    private fun applyForceConstraints(dt: Float) {
        for (c in constraints) {
            if (c.type != ConstraintType.SPRING) continue
            val a = bodies.firstOrNull { it.id == c.bodyAId } ?: continue
            val b = if (c.bodyBId >= 0) bodies.firstOrNull { it.id == c.bodyBId } else null
            val pA = a.position + a.orientation.rotate(c.localAnchorA)
            val pB = if (b != null) b.position + b.orientation.rotate(c.localAnchorB) else c.localAnchorB
            val delta = pB - pA
            val dist = delta.length()
            if (dist < 1e-5f) continue
            val dir = delta / dist
            val stretch = dist - c.restLength
            val vA = a.linearVelocity + a.angularVelocity.cross(pA - a.position)
            val vB = if (b != null) b.linearVelocity + b.angularVelocity.cross(pB - b.position) else Vec3.ZERO
            val relVelAlong = (vB - vA).dot(dir)
            val forceMag = c.stiffness * stretch + c.damping * relVelAlong
            val impulse = dir * (forceMag * dt)
            if (!a.isFrozen) a.applyImpulseAtPoint(impulse, pA)
            if (b != null && !b.isFrozen) b.applyImpulseAtPoint(-impulse, pB)
        }
    }

    private fun solveImpulseConstraints(dt: Float) {
        for (c in constraints) {
            if (c.type == ConstraintType.SPRING) continue
            val a = bodies.firstOrNull { it.id == c.bodyAId } ?: continue
            val b = if (c.bodyBId >= 0) bodies.firstOrNull { it.id == c.bodyBId } else null
            val pA = a.position + a.orientation.rotate(c.localAnchorA)
            val pB = if (b != null) b.position + b.orientation.rotate(c.localAnchorB) else c.localAnchorB
            val rA = pA - a.position
            val rB = if (b != null) pB - b.position else Vec3.ZERO
            val delta = pB - pA
            val dist = delta.length()

            when (c.type) {
                ConstraintType.ROPE -> {
                    if (dist <= c.restLength || dist < 1e-5f) continue
                    val dir = delta / dist
                    val invMassSum = effectivePointMass(a, b, rA, rB, dir)
                    if (invMassSum <= 1e-6f) continue
                    val vA = a.linearVelocity + a.angularVelocity.cross(rA)
                    val vB = if (b != null) b.linearVelocity + b.angularVelocity.cross(rB) else Vec3.ZERO
                    val relVn = (vB - vA).dot(dir)
                    val bias = ((dist - c.restLength) * 0.25f / dt)
                    val j = (relVn + bias) / invMassSum
                    if (j > 0f) {
                        val imp = dir * j
                        if (!a.isFrozen) a.applyImpulseAtPoint(imp, pA)
                        if (b != null && !b.isFrozen) b.applyImpulseAtPoint(-imp, pB)
                    }
                }
                ConstraintType.HINGE, ConstraintType.WELD -> {
                    if (dist > 1e-5f) {
                        val dir = delta / dist
                        val invMassSum = effectivePointMass(a, b, rA, rB, dir)
                        if (invMassSum > 1e-6f) {
                            val vA = a.linearVelocity + a.angularVelocity.cross(rA)
                            val vB = if (b != null) b.linearVelocity + b.angularVelocity.cross(rB) else Vec3.ZERO
                            val relVn = (vB - vA).dot(dir)
                            val bias = (dist * 0.30f / dt)
                            val j = (relVn + bias) / invMassSum
                            val imp = dir * j
                            if (!a.isFrozen) a.applyImpulseAtPoint(imp, pA)
                            if (b != null && !b.isFrozen) b.applyImpulseAtPoint(-imp, pB)
                        }
                    }
                    if (c.type == ConstraintType.WELD) {
                        val targetA = if (b != null) b.orientation * c.relativeOrientation else c.relativeOrientation
                        val qErr = targetA * a.orientation.conjugate()
                        val axisErr = Vec3(qErr.x, qErr.y, qErr.z) * (if (qErr.w < 0f) -2f else 2f)
                        val relAng = (b?.angularVelocity ?: Vec3.ZERO) - a.angularVelocity
                        val angImp = (axisErr * 0.35f / dt - relAng) * 0.08f
                        if (!a.isFrozen) a.angularVelocity += angImp
                        if (b != null && !b.isFrozen) b.angularVelocity -= angImp
                    } else {
                        val axis = c.hingeAxisWorld.normalized()
                        if (!a.isFrozen) {
                            val perpA = a.angularVelocity - axis * a.angularVelocity.dot(axis)
                            a.angularVelocity -= perpA * 0.35f
                        }
                        if (b != null && !b.isFrozen) {
                            val perpB = b.angularVelocity - axis * b.angularVelocity.dot(axis)
                            b.angularVelocity -= perpB * 0.35f
                        }
                    }
                }
                ConstraintType.SPRING -> Unit
            }
        }
    }

    private fun effectivePointMass(a: RigidBody, b: RigidBody?, rA: Vec3, rB: Vec3, dir: Vec3): Float {
        val rnA = rA.cross(dir)
        val angA = a.invInertiaWorld.multiplyVec(rnA).cross(rA).dot(dir)
        val invA = if (a.isFrozen) 0f else a.invMass + angA
        val invB = if (b == null || b.isFrozen) 0f else {
            val rnB = rB.cross(dir)
            b.invMass + b.invInertiaWorld.multiplyVec(rnB).cross(rB).dot(dir)
        }
        return invA + invB
    }

    private fun updateRaycastTarget() {
        if (isMenuDemo) {
            targetedBodyId = null
            targetedBodyRef.set(null)
            return
        }
        val (eye, dir, _) = getCameraBasis()
        val hit = CollisionDetector.raycastBodiesAndStatics(eye, dir, 16f, bodies, staticProps)
        val body = if (hit != null && hit.bodyId >= 0) bodies.firstOrNull { it.id == hit.bodyId } else null

        targetedBodyId = body?.id
        targetedWorldPos = hit?.point
        targetedBodyRef.set(
            body?.let { b ->
                val eNode = electricalSolver.nodes[b.id]
                InspectedBodyInfo(
                    id = b.id,
                    name = b.customLabel,
                    type = b.type,
                    material = b.material,
                    materialSpec = b.materialSpec,
                    massKg = b.mass,
                    speedMs = b.linearVelocity.length(),
                    scale = b.scale,
                    isFrozen = b.isFrozen,
                    constraintCount = constraints.count { it.bodyAId == b.id || it.bodyBId == b.id } +
                        electricalSolver.wires.count { it.bodyAId == b.id || it.bodyBId == b.id },
                    resizable = b.type.resizable,
                    gravityScale = b.gravityScale,
                    collisionMode = b.collisionMode,
                    friction = b.effectiveDynamicFriction,
                    restitution = b.effectiveRestitution,
                    temperatureC = b.temperatureC,
                    crackLevel = b.crackLevel,
                    groupId = b.groupId,
                    position = b.position,
                    eulerDeg = b.orientation.toEulerDegrees(),
                    electricalRole = b.electricalRole,
                    electricalVoltage = eNode?.voltage ?: 0f,
                    electricalCurrent = eNode?.currentAmps ?: 0f,
                    electricalSwitchClosed = eNode?.switchClosed ?: true,
                    electricalBlown = eNode?.isBlown ?: false,
                    toolId = b.toolId,
                    ammoVariantId = b.ammoVariantId,
                    ammoAmount = b.ammoAmount
                )
            }
        )
        grabbedBodyIdRef.set(grabbedBodyId)
        pendingAttachBodyIdRef.set(pendingAttachBodyId)
        bodyCountRef.set(bodies.size)

        val toolDef = ToolCatalog.get(activeCatalogToolId)
        toolTelemetryRef.set(
            ToolTelemetrySnapshot(
                activeToolId = activeCatalogToolId,
                activeToolName = toolDef?.displayName ?: activeCatalogToolId,
                readoutTitle = readoutTitle,
                readoutDetail = readoutDetail,
                batteryPercent = toolBatteryPercent,
                magnetRangeMeters = magnetRangeMeters,
                magnetRepelMode = magnetRepelMode,
                stopwatchSeconds = stopwatchSeconds,
                stopwatchRunning = stopwatchRunning,
                flashlightOn = flashlightActive
            )
        )
        syncFirearmHudRef()
    }

    private fun syncFirearmHudRef() {
        val (eye, dir, _) = getCameraBasis()
        val nearbyAmmo = if (isMenuDemo) null else weaponController.findNearbyAmmoBox(eye, dir, bodies)
        firearmHudRef.set(weaponController.toHudState(nearbyAmmo))
    }

    private fun spawnBatchInternal(
        type: ObjectType,
        matSpec: MaterialSpec?,
        quantity: Int,
        toolId: String?
    ) {
        val (eye, dir, right) = getCameraBasis()
        val count = quantity.coerceIn(1, 5)
        val rot = Quat.fromEuler(0f, cameraYaw, 0f)
        for (i in 0 until count) {
            val lateral = (i - (count - 1) * 0.5f) * 1.15f
            val spawnPos = eye + dir * 3.5f + right * lateral + Vec3(0f, (i / 3) * 0.6f, 0f)
            val toolDef = toolId?.let { ToolCatalog.get(it) }
            val spec = matSpec ?: toolDef?.let { MaterialLibrary.get(it.materialId) } ?: MaterialLibrary.get(type.defaultMaterialId)
            val label = toolDef?.displayName ?: if (matSpec != null && matSpec.id != type.defaultMaterialId) {
                "${matSpec.displayName} ${type.displayName}"
            } else {
                type.displayName
            }
            val rb = RigidBody(
                id = nextId++,
                type = type,
                position = spawnPos,
                orientation = rot,
                customLabel = label,
                materialSpec = spec
            )
            rb.toolId = toolId
            bodies.add(rb)
            electricalSolver.registerBody(rb, rb.electricalRole)
            onToolSfx("spawn", spawnPos)
        }
        trimExcessDebrisIfNeeded()
    }

    private fun equipCatalogToolInternal(toolId: String) {
        val tool = ToolCatalog.get(toolId) ?: return
        activeCatalogToolId = tool.id
        if (FirearmCatalog.isFirearmTool(tool.id)) {
            selectedToolType = ToolType.CATALOG_TOOL
            weaponController.equipFirearmByToolId(tool.id)
        } else {
            weaponController.clearActiveFirearm()
            adsBlend = 0f
        }
        readoutTitle = tool.displayName.uppercase()
        readoutDetail = tool.description.take(54)
        onToolSfx("switch", playerPos)
        syncFirearmHudRef()
    }

    private fun handlePrimaryAction(tool: ToolType, attachMode: ConstraintType) {
        selectedToolType = tool
        interactPulse = 1.0f
        if (tool == ToolType.CATALOG_TOOL) {
            handleCatalogToolPrimary()
            return
        }
        val (eye, dir, _) = getCameraBasis()
        when (tool) {
            ToolType.GRAB -> {
                if (grabbedBodyId != null) {
                    releaseGrabInternal()
                    return
                }
                val hit = CollisionDetector.raycastBodiesAndStatics(eye, dir, 10f, bodies, emptyList())
                if (hit != null && hit.bodyId >= 0) {
                    val body = bodies.firstOrNull { it.id == hit.bodyId } ?: return
                    if (body.isFrozen) body.setFrozenState(false)
                    grabbedBodyId = body.id
                    grabDistance = hit.distance.coerceIn(2.0f, 8.0f)
                    grabTargetRotOffset = body.orientation
                    body.wake()
                    onToolSfx("grab", body.position)
                }
            }
            ToolType.IMPULSE -> {
                val hit = CollisionDetector.raycastBodiesAndStatics(eye, dir, 22f, bodies, emptyList())
                if (hit != null && hit.bodyId >= 0) {
                    val body = bodies.firstOrNull { it.id == hit.bodyId } ?: return
                    if (body.isFrozen) body.setFrozenState(false)
                    val impulse = dir * (body.mass * 16f).coerceIn(45f, 1800f)
                    body.applyImpulseAtPoint(impulse, hit.point)
                    if (body.isGrenade) {
                        body.armGrenade(1.2f)
                        readoutTitle = "GRENADE ARMED"
                        readoutDetail = "${body.customLabel} fuse lit"
                    }
                    onToolSfx("impulse", hit.point)
                }
            }
            ToolType.CONSTRAINT -> {
                val hit = CollisionDetector.raycastBodiesAndStatics(eye, dir, 14f, bodies, staticProps) ?: return
                if (pendingAttachBodyId == null) {
                    if (hit.bodyId >= 0) {
                        val bodyA = bodies.firstOrNull { it.id == hit.bodyId } ?: return
                        pendingAttachBodyId = bodyA.id
                        pendingAttachLocalAnchor = bodyA.orientation.conjugate().rotate(hit.point - bodyA.position)
                        onToolSfx("attach", hit.point)
                    }
                } else {
                    val bodyA = bodies.firstOrNull { it.id == pendingAttachBodyId }
                    if (bodyA != null && hit.bodyId != bodyA.id) {
                        pushUndoSnapshot()
                        val worldA = bodyA.position + bodyA.orientation.rotate(pendingAttachLocalAnchor)
                        val bodyB = if (hit.bodyId >= 0) bodies.firstOrNull { it.id == hit.bodyId } else null
                        val localB = if (bodyB != null) bodyB.orientation.conjugate().rotate(hit.point - bodyB.position) else hit.point
                        val dist = (hit.point - worldA).length().coerceIn(0.4f, 12f)
                        val relRot = if (bodyB != null) bodyB.orientation.conjugate() * bodyA.orientation else bodyA.orientation
                        constraints.add(
                            PhysicsConstraint(
                                id = nextId++,
                                type = attachMode,
                                bodyAId = bodyA.id,
                                bodyBId = bodyB?.id ?: -1,
                                localAnchorA = pendingAttachLocalAnchor,
                                localAnchorB = localB,
                                restLength = dist,
                                hingeAxisWorld = hit.normal,
                                relativeOrientation = relRot
                            )
                        )
                        if (bodyB != null && bodyA.electricalRole != ElectricalRole.NONE && bodyB.electricalRole != ElectricalRole.NONE) {
                            electricalSolver.addWire(nextId++, bodyA.id, bodyB.id)
                        }
                        bodyA.wake()
                        bodyB?.wake()
                        onToolSfx("attach", hit.point)
                    }
                    pendingAttachBodyId = null
                }
            }
            ToolType.INSPECT -> {
                val id = targetedBodyId
                if (id != null) {
                    val msg = electricalSolver.toggleSwitchOrResetFuse(id)
                    if (msg != null) {
                        readoutTitle = "CIRCUIT"
                        readoutDetail = msg
                        onToolSfx("switch", playerPos)
                    } else {
                        toggleFreezeTargetedInternal()
                    }
                }
            }
            ToolType.CATALOG_TOOL -> Unit
        }
    }

    private fun handleCatalogToolPrimary() {
        val tool = ToolCatalog.get(activeCatalogToolId) ?: return
        toolActionPulse = 1.0f
        val (eye, dir, _) = getCameraBasis()
        val hit = CollisionDetector.raycastBodiesAndStatics(eye, dir, tool.rangeMeters, bodies, staticProps)
        val targetBody = if (hit != null && hit.bodyId >= 0) bodies.firstOrNull { it.id == hit.bodyId } else null

        if (tool.category == ToolCategory.ELECTRIC) {
            if (toolBatteryPercent <= 2f) {
                readoutTitle = "BATTERY LOW"
                readoutDetail = "Recharge tool battery in HUD"
                onToolSfx("switch", eye)
                return
            }
            toolBatteryPercent = (toolBatteryPercent - 1.5f).coerceAtLeast(0f)
        }

        when (tool.interactionKind) {
            ToolInteractionKind.STRIKE_HAMMER -> {
                onToolSfx("hammer", hit?.point ?: eye + dir * 2f)
                if (targetBody != null && hit != null) {
                    if (targetBody.isFrozen) targetBody.setFrozenState(false)
                    targetBody.applyImpulseAtPoint(dir * (targetBody.mass * 14f * tool.powerRating).coerceIn(30f, 1600f), hit.point)
                    if (targetBody.isGrenade) {
                        targetBody.armGrenade(1.0f)
                    }
                    val frags = applyImpactDamage(targetBody, 14f * tool.powerRating)
                    bodies.addAll(frags)
                    particleSystem.emitBurst(hit.point, ParticleType.SPARK, 10, 4.5f, hit.normal)
                    readoutTitle = "STRIKE ${targetBody.customLabel.uppercase()}"
                    readoutDetail = "Integrity: ${targetBody.health.toInt().coerceAtLeast(0)}%"
                }
            }
            ToolInteractionKind.STRIKE_MALLET -> {
                onToolSfx("hammer", hit?.point ?: eye + dir * 2f)
                if (targetBody != null && hit != null) {
                    if (targetBody.isFrozen) targetBody.setFrozenState(false)
                    targetBody.applyImpulseAtPoint(dir * (targetBody.mass * 9f).coerceIn(20f, 900f), hit.point)
                    targetBody.squashAmount = max(targetBody.squashAmount, targetBody.materialSpec.softnessSquish * 0.9f)
                    readoutTitle = "DAMPED STRIKE"
                    readoutDetail = "Non-marring rubber impulse applied"
                }
            }
            ToolInteractionKind.WRENCH_TORQUE, ToolInteractionKind.DRILL_SPIN -> {
                onToolSfx("drill", hit?.point ?: eye + dir * 2f)
                if (targetBody != null && hit != null) {
                    if (targetBody.isFrozen) targetBody.setFrozenState(false)
                    targetBody.wake()
                    targetBody.angularVelocity += dir * (8f * tool.powerRating)
                    electricalSolver.toggleSwitchOrResetFuse(targetBody.id)?.let { msg ->
                        readoutTitle = "FASTENED / TOGGLED"
                        readoutDetail = msg
                    } ?: run {
                        readoutTitle = "TORQUE APPLIED"
                        readoutDetail = "Spin: ${String.format("%.1f", targetBody.angularVelocity.length())} rad/s"
                    }
                    particleSystem.emitBurst(hit.point, ParticleType.SPARK, 4, 2.5f)
                }
            }
            ToolInteractionKind.PLIERS_GRIP, ToolInteractionKind.SUCTION_GRAB -> {
                if (grabbedBodyId != null) {
                    releaseGrabInternal()
                    readoutTitle = "RELEASED"
                    readoutDetail = "Object released"
                } else if (targetBody != null && hit != null) {
                    if (targetBody.isFrozen) targetBody.setFrozenState(false)
                    grabbedBodyId = targetBody.id
                    grabDistance = hit.distance.coerceIn(1.8f, tool.rangeMeters)
                    grabTargetRotOffset = targetBody.orientation
                    targetBody.wake()
                    onToolSfx("grab", targetBody.position)
                    readoutTitle = "GRIPPING ${targetBody.customLabel.uppercase()}"
                    readoutDetail = "Mass: ${String.format("%.1f", targetBody.mass)} kg"
                }
            }
            ToolInteractionKind.WIRE_CUTTER, ToolInteractionKind.SAW_CUT -> {
                onToolSfx("grinder", hit?.point ?: eye + dir * 2f)
                if (targetBody != null && hit != null) {
                    pushUndoSnapshot()
                    val cutC = constraints.removeAll { it.bodyAId == targetBody.id || it.bodyBId == targetBody.id }
                    val cutW = electricalSolver.wires.removeAll { it.bodyAId == targetBody.id || it.bodyBId == targetBody.id }
                    targetBody.wake()
                    val pType = if (targetBody.materialSpec.category == MaterialCategory.WOOD) ParticleType.WOOD_SPLINTER else ParticleType.SPARK
                    particleSystem.emitBurst(hit.point, pType, 14, 4.5f, hit.normal)
                    if (tool.interactionKind == ToolInteractionKind.SAW_CUT && targetBody.type.destructible) {
                        bodies.addAll(applyImpactDamage(targetBody, 16f))
                    }
                    readoutTitle = if (cutC || cutW) "CUT LINKS" else "SURFACE CUT"
                    readoutDetail = "Severed connections on ${targetBody.customLabel}"
                }
            }
            ToolInteractionKind.GRIND_SPARK -> {
                onToolSfx("grinder", hit?.point ?: eye + dir * 2f)
                if (hit != null) {
                    particleSystem.emitBurst(hit.point, ParticleType.SPARK, 18, 6.2f, hit.normal)
                    if (targetBody != null) {
                        pushUndoSnapshot()
                        constraints.removeAll { (it.bodyAId == targetBody.id || it.bodyBId == targetBody.id) && it.type == ConstraintType.WELD }
                        targetBody.temperatureC = (targetBody.temperatureC + 18f).coerceAtMost(450f)
                        targetBody.wake()
                        readoutTitle = "GRINDING ${targetBody.materialSpec.displayName.uppercase()}"
                        readoutDetail = "Temp: ${targetBody.temperatureC.toInt()} C • Welds ground"
                    }
                }
            }
            ToolInteractionKind.SAND_POLISH -> {
                onToolSfx("grinder", hit?.point ?: eye + dir * 2f)
                if (targetBody != null && hit != null) {
                    val currentFric = targetBody.effectiveDynamicFriction
                    targetBody.frictionOverride = (currentFric * 0.72f).coerceAtLeast(0.04f)
                    particleSystem.emitBurst(hit.point, ParticleType.CONCRETE_DUST, 8, 1.8f, hit.normal)
                    readoutTitle = "POLISHED SURFACE"
                    readoutDetail = "Friction reduced to ${String.format("%.2f", targetBody.effectiveDynamicFriction)}"
                }
            }
            ToolInteractionKind.WELD_ARC, ToolInteractionKind.SOLDER_JOIN -> {
                if (hit != null) {
                    particleSystem.emitBurst(hit.point, ParticleType.WELD_ARC, 16, 4.2f, hit.normal)
                    onToolSfx("weld", hit.point)
                }
                if (hit != null) {
                    if (pendingAttachBodyId == null && targetBody != null) {
                        pendingAttachBodyId = targetBody.id
                        pendingAttachLocalAnchor = targetBody.orientation.conjugate().rotate(hit.point - targetBody.position)
                        readoutTitle = "ARC POINT A SET"
                        readoutDetail = "Aim at second body or surface to weld"
                    } else if (pendingAttachBodyId != null) {
                        val bodyA = bodies.firstOrNull { it.id == pendingAttachBodyId }
                        if (bodyA != null && hit.bodyId != bodyA.id) {
                            pushUndoSnapshot()
                            val bodyB = targetBody
                            val localB = if (bodyB != null) bodyB.orientation.conjugate().rotate(hit.point - bodyB.position) else hit.point
                            val relRot = if (bodyB != null) bodyB.orientation.conjugate() * bodyA.orientation else bodyA.orientation
                            constraints.add(
                                PhysicsConstraint(
                                    id = nextId++,
                                    type = ConstraintType.WELD,
                                    bodyAId = bodyA.id,
                                    bodyBId = bodyB?.id ?: -1,
                                    localAnchorA = pendingAttachLocalAnchor,
                                    localAnchorB = localB,
                                    restLength = 0.2f,
                                    relativeOrientation = relRot
                                )
                            )
                            if (bodyB != null && (tool.interactionKind == ToolInteractionKind.SOLDER_JOIN ||
                                (bodyA.electricalRole != ElectricalRole.NONE && bodyB.electricalRole != ElectricalRole.NONE))
                            ) {
                                electricalSolver.registerBody(bodyA, if (bodyA.electricalRole == ElectricalRole.NONE) ElectricalRole.CONNECTOR else bodyA.electricalRole)
                                electricalSolver.registerBody(bodyB, if (bodyB.electricalRole == ElectricalRole.NONE) ElectricalRole.CONNECTOR else bodyB.electricalRole)
                                electricalSolver.addWire(nextId++, bodyA.id, bodyB.id)
                            }
                            bodyA.temperatureC += 45f
                            bodyB?.let { it.temperatureC += 45f }
                            readoutTitle = "WELD / SOLDER COMPLETE"
                            readoutDetail = "Joined ${bodyA.customLabel}"
                        }
                        pendingAttachBodyId = null
                    }
                }
            }
            ToolInteractionKind.HEAT_THERMAL -> {
                val pOrigin = hit?.point ?: (eye + dir * 2.5f)
                particleSystem.emitBurst(pOrigin, ParticleType.SPARK, 8, 3.2f, dir)
                onToolSfx("weld", pOrigin)
                if (targetBody != null) {
                    targetBody.temperatureC = (targetBody.temperatureC + 42f).coerceAtMost(850f)
                    targetBody.wake()
                    readoutTitle = "HEATING ${targetBody.materialSpec.displayName.uppercase()}"
                    readoutDetail = "${targetBody.temperatureC.toInt()} C (Melt: ${targetBody.materialSpec.meltingPointC.toInt()} C)"
                }
            }
            ToolInteractionKind.EXTINGUISHER_FOAM -> {
                val sprayPos = eye + dir * 1.4f
                particleSystem.emitBurst(sprayPos, ParticleType.FOAM_SPRAY, 20, 7.5f, dir)
                onToolSfx("impulse", sprayPos)
                if (targetBody != null && hit != null) {
                    targetBody.temperatureC = (targetBody.temperatureC - 35f).coerceAtLeast(-20f)
                    if (!targetBody.isFrozen) targetBody.applyImpulseAtPoint(dir * 28f, hit.point)
                    readoutTitle = "COOLING FOAM"
                    readoutDetail = "Temp cooled to ${targetBody.temperatureC.toInt()} C"
                }
            }
            ToolInteractionKind.AIR_BLOW -> {
                val pOrigin = eye + dir * 1.6f
                particleSystem.emitBurst(pOrigin, ParticleType.FOAM_SPRAY, 10, 9.5f, dir)
                onToolSfx("impulse", pOrigin)
                for (b in bodies) {
                    if (b.isDestroyed || b.isFrozen) continue
                    val toB = b.position - eye
                    val proj = toB.dot(dir)
                    if (proj in 0.5f..tool.rangeMeters) {
                        val perpDist = (toB - dir * proj).length()
                        if (perpDist < 2.2f) {
                            b.wake()
                            val force = (1f - proj / tool.rangeMeters) * 45f * tool.powerRating
                            b.applyImpulseAtPoint(dir * force, b.position)
                        }
                    }
                }
                readoutTitle = "AIR STREAM"
                readoutDetail = "Pneumatic thrust active"
            }
            ToolInteractionKind.JACK_LIFT -> {
                if (targetBody != null) {
                    if (targetBody.isFrozen) targetBody.setFrozenState(false)
                    targetBody.wake()
                    targetBody.linearVelocity += Vec3(0f, 5.2f * tool.powerRating, 0f)
                    onToolSfx("attach", targetBody.position)
                    readoutTitle = "HYDRAULIC LIFT"
                    readoutDetail = "Hoisted ${targetBody.customLabel}"
                }
            }
            ToolInteractionKind.CLAMP_LOCK -> {
                if (targetBody != null) {
                    targetBody.setFrozenState(!targetBody.isFrozen)
                    onToolSfx("freeze", targetBody.position)
                    readoutTitle = if (targetBody.isFrozen) "CLAMP LOCKED" else "CLAMP RELEASED"
                    readoutDetail = targetBody.customLabel
                }
            }
            ToolInteractionKind.ROPE_TETHER, ToolInteractionKind.CABLE_TETHER, ToolInteractionKind.SPRING_TETHER,
            ToolInteractionKind.WINCH_HOIST, ToolInteractionKind.PULLEY_HOIST -> {
                val cType = when (tool.interactionKind) {
                    ToolInteractionKind.SPRING_TETHER -> ConstraintType.SPRING
                    else -> ConstraintType.ROPE
                }
                if (hit != null) {
                    if (pendingAttachBodyId == null && targetBody != null) {
                        pendingAttachBodyId = targetBody.id
                        pendingAttachLocalAnchor = targetBody.orientation.conjugate().rotate(hit.point - targetBody.position)
                        onToolSfx("attach", hit.point)
                        readoutTitle = "TETHER ANCHOR A"
                        readoutDetail = "Aim at target B to link ${cType.name}"
                    } else if (pendingAttachBodyId != null) {
                        val bodyA = bodies.firstOrNull { it.id == pendingAttachBodyId }
                        if (bodyA != null && hit.bodyId != bodyA.id) {
                            pushUndoSnapshot()
                            val bodyB = targetBody
                            val worldA = bodyA.position + bodyA.orientation.rotate(pendingAttachLocalAnchor)
                            val localB = if (bodyB != null) bodyB.orientation.conjugate().rotate(hit.point - bodyB.position) else hit.point
                            val dist = (hit.point - worldA).length().coerceIn(0.4f, 12f)
                            val restLen = if (tool.interactionKind == ToolInteractionKind.WINCH_HOIST) dist * 0.72f else dist
                            constraints.add(
                                PhysicsConstraint(nextId++, cType, bodyA.id, bodyB?.id ?: -1, pendingAttachLocalAnchor, localB, restLen)
                            )
                            if (tool.interactionKind == ToolInteractionKind.CABLE_TETHER && bodyB != null) {
                                if (bodyA.electricalRole != ElectricalRole.NONE && bodyB.electricalRole != ElectricalRole.NONE) {
                                    electricalSolver.addWire(nextId++, bodyA.id, bodyB.id)
                                }
                            }
                            bodyA.wake()
                            bodyB?.wake()
                            onToolSfx("attach", hit.point)
                            readoutTitle = "${cType.name} LINKED"
                            readoutDetail = "Rest length: ${String.format("%.2f", restLen)} m"
                        }
                        pendingAttachBodyId = null
                    }
                }
            }
            ToolInteractionKind.MAGNET_FIELD -> {
                val focus = eye + dir * (magnetRangeMeters * 0.5f)
                particleSystem.emitBurst(focus, ParticleType.MAGNET_PULSE, 14, 3.2f, -dir)
                onToolSfx("switch", focus)
                var affected = 0
                for (b in bodies) {
                    if (b.isDestroyed || b.isFrozen || !b.materialSpec.isMagnetic) continue
                    val delta = (eye + dir * 2.2f) - b.position
                    val d = delta.length()
                    if (d in 0.25f..magnetRangeMeters) {
                        val sign = if (magnetRepelMode) -1f else 1f
                        val strength = (1f - d / magnetRangeMeters) * 42f * tool.powerRating
                        b.wake()
                        b.applyImpulseAtPoint((delta / d) * (strength * sign), b.position)
                        affected++
                    }
                }
                readoutTitle = if (magnetRepelMode) "MAGNET REPEL" else "MAGNET ATTRACT"
                readoutDetail = "$affected ferromagnetic bodies in ${magnetRangeMeters.toInt()}m"
            }
            ToolInteractionKind.MULTIMETER_PROBE -> {
                onToolSfx("switch", hit?.point ?: eye)
                if (targetBody != null) {
                    val node = electricalSolver.nodes[targetBody.id]
                    if (node != null) {
                        val toggled = electricalSolver.toggleSwitchOrResetFuse(targetBody.id)
                        readoutTitle = node.role.displayName.uppercase()
                        readoutDetail = toggled ?: "${String.format("%.1f", node.voltage)}V • ${String.format("%.2f", node.currentAmps)}A • ${if (node.energized) "ON" else "OFF"}"
                    } else {
                        readoutTitle = "CONDUCTIVITY CHECK"
                        readoutDetail = "${targetBody.materialSpec.displayName}: ${String.format("%.0f", targetBody.materialSpec.electricalConductivity * 100f)}% cond"
                    }
                }
            }
            ToolInteractionKind.GENERATOR_POWER -> {
                if (targetBody != null) {
                    electricalSolver.registerBody(targetBody, ElectricalRole.GENERATOR)
                    targetBody.electricalRole = ElectricalRole.GENERATOR
                    targetBody.wake()
                    targetBody.angularVelocity += Vec3(0f, 9f, 0f)
                    onToolSfx("drill", targetBody.position)
                    readoutTitle = "GENERATOR ENERGIZED"
                    readoutDetail = "Supplying rotational EMF"
                }
            }
            ToolInteractionKind.MEASURE_THERMOMETER -> {
                onToolSfx("ui_click", eye)
                if (targetBody != null) {
                    readoutTitle = "${String.format("%.1f", targetBody.temperatureC)} C"
                    readoutDetail = "${targetBody.materialSpec.displayName} (Melt: ${targetBody.materialSpec.meltingPointC.toInt()} C)"
                }
            }
            ToolInteractionKind.MEASURE_SCALE -> {
                onToolSfx("ui_click", eye)
                if (targetBody != null) {
                    val weightN = targetBody.mass * abs(GRAVITY)
                    readoutTitle = "${String.format("%.2f", targetBody.mass)} KG"
                    readoutDetail = "Weight: ${String.format("%.1f", weightN)} N • Density: ${targetBody.materialSpec.densityKgM3.toInt()} kg/m3"
                }
            }
            ToolInteractionKind.MEASURE_DISTANCE -> {
                onToolSfx("ui_click", eye)
                if (hit != null) {
                    readoutTitle = "${String.format("%.2f", hit.distance)} M"
                    readoutDetail = targetBody?.customLabel ?: "Static Surface Distance"
                }
            }
            ToolInteractionKind.MEASURE_CALIPER -> {
                onToolSfx("ui_click", eye)
                if (targetBody != null) {
                    val s = targetBody.effectiveHalfExtents * 2f
                    readoutTitle = "${String.format("%.2f", s.x)}x${String.format("%.2f", s.y)}x${String.format("%.2f", s.z)}m"
                    readoutDetail = "Scale: ${String.format("%.2f", targetBody.scale)}x"
                }
            }
            ToolInteractionKind.MEASURE_PRESSURE -> {
                onToolSfx("ui_click", eye)
                val wind = if (weatherType == WeatherType.WINDY) max(windStrength, 7.5f) else windStrength
                val kpa = 101.325f + wind * 0.45f
                readoutTitle = "${String.format("%.2f", kpa)} KPA"
                readoutDetail = "Wind: ${String.format("%.1f", wind)} m/s • Fluid: ${if (fluidBasinEnabled) "ON" else "OFF"}"
            }
            ToolInteractionKind.STOPWATCH_TIMER -> {
                stopwatchRunning = !stopwatchRunning
                onToolSfx("switch", eye)
                readoutTitle = if (stopwatchRunning) "TIMER RUNNING" else "TIMER PAUSED"
                readoutDetail = String.format("%.2f s", stopwatchSeconds)
            }
            ToolInteractionKind.OPTICAL_INSPECT -> {
                onToolSfx("ui_click", eye)
                if (targetBody != null) {
                    val sp = targetBody.materialSpec
                    readoutTitle = sp.displayName.uppercase()
                    readoutDetail = "Hard:${sp.hardness} Rest:${sp.restitution} Crack:${(targetBody.crackLevel * 100).toInt()}%"
                }
            }
            ToolInteractionKind.LAB_VESSEL -> {
                pushUndoSnapshot()
                spawnBatchInternal(ObjectType.GLASS_CUBE, MaterialLibrary.get("TEMPERED_GLASS"), 1, null)
                readoutTitle = "VESSEL DEPLOYED"
                readoutDetail = "Tempered borosilicate test cube"
            }
            ToolInteractionKind.LIGHT_BEAM -> {
                flashlightActive = !flashlightActive
                onToolSfx("switch", eye)
                readoutTitle = if (flashlightActive) "BEAM ON" else "BEAM OFF"
                readoutDetail = "Dynamic point light ${if (flashlightActive) "active" else "standby"}"
            }
            ToolInteractionKind.CAMERA_SNAP -> {
                screenshotTriggerSeq.incrementAndGet()
                onToolSfx("switch", eye)
                readoutTitle = "CAPTURE TRIGGERED"
                readoutDetail = "Viewport snapshot requested"
            }
            ToolInteractionKind.METAL_DETECTOR -> {
                var nearestDist = Float.POSITIVE_INFINITY
                var nearestName = "None"
                for (b in bodies) {
                    if (b.materialSpec.category == MaterialCategory.METALS || b.materialSpec.isMagnetic) {
                        val d = (b.position - eye).length()
                        if (d < tool.rangeMeters && d < nearestDist) {
                            nearestDist = d
                            nearestName = "${b.materialSpec.displayName} (${b.customLabel})"
                        }
                    }
                }
                if (nearestDist < Float.POSITIVE_INFINITY) {
                    onToolSfx("attach", eye)
                    readoutTitle = "METAL @ ${String.format("%.1f", nearestDist)}M"
                    readoutDetail = nearestName
                } else {
                    readoutTitle = "NO METAL IN RANGE"
                    readoutDetail = "Scanned ${tool.rangeMeters.toInt()}m radius"
                }
            }
            ToolInteractionKind.LEVEL_ALIGN -> {
                if (targetBody != null) {
                    val e = targetBody.orientation.toEulerDegrees()
                    if (abs(e.x) > 1f || abs(e.z) > 1f) {
                        targetBody.orientation = Quat.fromEuler(0f, e.y * (PI.toFloat() / 180f), 0f)
                        targetBody.angularVelocity = Vec3.ZERO
                        targetBody.updateWorldInertia()
                    }
                    onToolSfx("switch", targetBody.position)
                    readoutTitle = "LEVEL ALIGNED"
                    readoutDetail = "Pitch ${String.format("%.1f", e.x)}° -> 0°, Roll ${String.format("%.1f", e.z)}° -> 0°"
                }
            }
            ToolInteractionKind.FIREARM_SHOOT -> {
                weaponController.queueSingleTriggerTap()
            }
        }
    }

    private fun handleFirearmShot(spec: FirearmSpec, aiming: Boolean) {
        val (eye, forward, right) = getCameraBasis()
        val up = right.cross(forward).normalized()
        toolActionPulse = 1.0f
        interactPulse = 1.0f

        val spread = if (aiming) spec.adsSpreadRad else spec.hipSpreadRad
        val recoilFactor = if (aiming) 0.55f else 1.0f
        val pitchKick = spec.recoilPitchRad * recoilFactor
        val yawKick = if ((firearmShotCounter++ % 2) == 0) spec.recoilYawRad * 0.45f else -spec.recoilYawRad * 0.45f
        cameraPitch = (cameraPitch + pitchKick).coerceIn(-1.42f, 1.42f)
        cameraYaw = (cameraYaw + yawKick) % (2f * PI.toFloat())
        if (cameraShakeEnabled) {
            cameraShakeAmp = max(cameraShakeAmp, (spec.recoilPitchRad * 0.5f).coerceAtMost(0.032f))
        }

        val muzzleApprox = eye + forward * 0.55f + right * (if (aiming) 0.0f else 0.14f) - up * 0.09f
        particleSystem.emitBurst(muzzleApprox, ParticleType.SPARK, 2, 2.2f, forward)

        val spawnedDebris = ArrayList<RigidBody>()
        val pellets = spec.pellets.coerceAtLeast(1)
        val perPelletImpulse = spec.impulseNewtonSec / pellets.toFloat()
        val perPelletDamage = (spec.damageRatio * 115f) / pellets.toFloat()

        for (p in 0 until pellets) {
            val rayDir = if (p == 0 && pellets == 1 && spread < 0.005f) {
                forward
            } else {
                val angle = ((firearmShotCounter * 1.6180339f + p * 2.39996f) % (2f * PI.toFloat()))
                val radial = if (pellets == 1) spread * 0.4f else spread * sqrt((p + 0.6f) / pellets.toFloat())
                (forward + right * (cos(angle) * radial) + up * (sin(angle) * radial)).normalized()
            }
            val hit = CollisionDetector.raycastBodiesAndStatics(eye, rayDir, spec.rangeMeters, bodies, staticProps)
            if (hit != null) {
                val hitBody = if (hit.bodyId >= 0) bodies.firstOrNull { it.id == hit.bodyId && !it.isDestroyed } else null
                val hitAcoustic = hitBody?.materialSpec?.acousticClass ?: AcousticClass.CONCRETE
                if (hitBody != null) {
                    if (!hitBody.isFrozen) {
                        hitBody.wake()
                        hitBody.applyImpulseAtPoint(rayDir * perPelletImpulse, hit.point)
                    }
                    if (hitBody.isGrenade) {
                        hitBody.armGrenade(0.15f)
                    }
                    if (!hitBody.isFrozen && (hitBody.type.destructible || hitBody.materialSpec.destructionProfile != DestructionProfile.NONE)) {
                        hitBody.health -= perPelletDamage
                        hitBody.crackLevel = ((100f - hitBody.health) / 100f).coerceIn(0f, 1f)
                        if (hitBody.health <= 0f && !hitBody.isDestroyed) {
                            hitBody.isDestroyed = true
                            onToolSfx("shatter", hit.point)
                            spawnedDebris.addAll(DestructionHelper.spawnDebrisForBody(hitBody, { nextId++ }, bodies.size + spawnedDebris.size, particleSystem))
                        }
                    }
                    val pType = when (hitBody.materialSpec.category) {
                        MaterialCategory.WOOD -> ParticleType.WOOD_SPLINTER
                        MaterialCategory.CONSTRUCTION, MaterialCategory.GLASS_CERAMICS -> ParticleType.CONCRETE_DUST
                        else -> ParticleType.SPARK
                    }
                    particleSystem.emitBurst(hit.point, pType, if (pellets > 1) 2 else 6, 4.2f, hit.normal)
                } else {
                    particleSystem.emitBurst(hit.point, ParticleType.CONCRETE_DUST, if (pellets > 1) 2 else 4, 2.8f, hit.normal)
                }
                if (p == 0) {
                    onAudioEvent(CollisionAudioEvent(AcousticClass.METAL, hitAcoustic, (spec.impulseNewtonSec * 0.25f).coerceIn(3f, 14f), hit.point))
                }
            }
        }
        if (spawnedDebris.isNotEmpty()) {
            bodies.addAll(spawnedDebris)
            trimExcessDebrisIfNeeded()
        }
        if (bodies.any { it.isDestroyed }) {
            val deadIds = bodies.filter { it.isDestroyed }.map { it.id }.toSet()
            bodies.removeAll { it.isDestroyed }
            constraints.removeAll { it.bodyAId in deadIds || it.bodyBId in deadIds }
            for (id in deadIds) electricalSolver.removeBody(id)
            if (grabbedBodyId in deadIds) grabbedBodyId = null
        }
        syncFirearmHudRef()
    }

    private fun pickupNearbyAmmoInternal() {
        val (eye, dir, _) = getCameraBasis()
        val prompt = weaponController.findNearbyAmmoBox(eye, dir, bodies) ?: return
        val crate = bodies.firstOrNull { it.id == prompt.bodyId && !it.isDestroyed } ?: return
        val variant = AmmoBoxVariant.fromId(crate.ammoVariantId ?: crate.type.defaultAmmoVariantId()) ?: return
        val amount = if (crate.ammoAmount > 0) crate.ammoAmount else variant.ammoAmount
        val added = weaponController.addAmmoFromBox(variant, amount)
        if (added > 0) {
            crate.isDestroyed = true
            bodies.removeAll { it.isDestroyed }
            constraints.removeAll { it.bodyAId == crate.id || it.bodyBId == crate.id }
            electricalSolver.removeBody(crate.id)
            if (grabbedBodyId == crate.id) grabbedBodyId = null
            onToolSfx("ammo_pickup", crate.position)
        }
    }

    private fun releaseGrabInternal() {
        if (grabbedBodyId != null) releasePulse = 1.0f
        grabbedBodyId = null
    }

    private fun handleSecondaryAction(tool: ToolType, impulsePower: Float) {
        val heldId = grabbedBodyId
        if (heldId != null) {
            val body = bodies.firstOrNull { it.id == heldId }
            grabbedBodyId = null
            releasePulse = 1.0f
            if (body != null && !body.isFrozen) {
                val (_, dir, _) = getCameraBasis()
                body.wake()
                body.linearVelocity = dir * (16f * impulsePower)
                if (body.isGrenade) {
                    body.armGrenade(2.6f)
                    readoutTitle = "GRENADE ARMED"
                    readoutDetail = "${body.customLabel} thrown (2.6s fuse)"
                }
                onToolSfx("throw", body.position)
            }
            return
        }
        if (tool == ToolType.CONSTRAINT) {
            pendingAttachBodyId = null
            detachTargetedInternal()
        } else if (tool == ToolType.CATALOG_TOOL) {
            val t = ToolCatalog.get(activeCatalogToolId)
            if (t?.interactionKind == ToolInteractionKind.MAGNET_FIELD) {
                magnetRepelMode = !magnetRepelMode
                readoutTitle = if (magnetRepelMode) "MAGNET: REPEL" else "MAGNET: ATTRACT"
                onToolSfx("switch", playerPos)
            } else if (t?.category == ToolCategory.ELECTRIC) {
                toolBatteryPercent = 100f
                readoutTitle = "BATTERY CHARGED"
                readoutDetail = "100% power cell restored"
                onToolSfx("attach", playerPos)
            } else {
                pendingAttachBodyId = null
            }
        } else {
            val (eye, dir, _) = getCameraBasis()
            val hit = CollisionDetector.raycastBodiesAndStatics(eye, dir, 18f, bodies, emptyList())
            if (hit != null && hit.bodyId >= 0) {
                val body = bodies.firstOrNull { it.id == hit.bodyId } ?: return
                if (body.isFrozen) body.setFrozenState(false)
                body.applyImpulseAtPoint(-dir * (body.mass * 8f), hit.point)
                onToolSfx("impulse", hit.point)
            }
        }
    }

    private fun rotateHeldInternal(deltaYaw: Float, deltaPitch: Float) {
        val heldId = grabbedBodyId ?: return
        val dq = Quat.fromAxisAngle(Vec3.UP, deltaYaw) * Quat.fromAxisAngle(Vec3.RIGHT, deltaPitch)
        grabTargetRotOffset = (dq * grabTargetRotOffset).normalized()
        bodies.firstOrNull { it.id == heldId }?.wake()
    }

    private fun toggleFreezeTargetedInternal() {
        val id = grabbedBodyId ?: targetedBodyId ?: return
        val body = bodies.firstOrNull { it.id == id } ?: return
        val newFrozen = !body.isFrozen
        val gid = body.groupId
        if (gid != null) {
            bodies.filter { it.groupId == gid }.forEach { it.setFrozenState(newFrozen) }
        } else {
            body.setFrozenState(newFrozen)
        }
        if (newFrozen && grabbedBodyId == id) grabbedBodyId = null
        onToolSfx("freeze", body.position)
    }

    private fun duplicateTargetedInternal() {
        val id = targetedBodyId ?: return
        val src = bodies.firstOrNull { it.id == id } ?: return
        val clone = RigidBody(
            id = nextId++,
            type = src.type,
            material = src.material,
            position = src.position + Vec3(0f, src.effectiveHalfExtents.y * 2.1f + 0.15f, 0f),
            orientation = src.orientation,
            scale = src.scale,
            isFrozen = src.isFrozen,
            customLabel = src.customLabel,
            materialSpec = src.materialSpec
        )
        clone.gravityScale = src.gravityScale
        clone.collisionMode = src.collisionMode
        clone.massOverride = src.massOverride
        clone.frictionOverride = src.frictionOverride
        clone.restitutionOverride = src.restitutionOverride
        clone.toolId = src.toolId
        clone.ammoVariantId = src.ammoVariantId
        clone.ammoAmount = src.ammoAmount
        clone.recomputeMassProperties()
        bodies.add(clone)
        electricalSolver.registerBody(clone, clone.electricalRole)
        onToolSfx("spawn", clone.position)
    }

    private fun deleteTargetedInternal() {
        val id = grabbedBodyId ?: targetedBodyId ?: return
        bodies.removeAll { it.id == id }
        constraints.removeAll { it.bodyAId == id || it.bodyBId == id }
        electricalSolver.removeBody(id)
        if (grabbedBodyId == id) grabbedBodyId = null
        if (pendingAttachBodyId == id) pendingAttachBodyId = null
        targetedBodyId = null
        onToolSfx("delete", playerPos)
    }

    private fun detachTargetedInternal() {
        val id = targetedBodyId ?: return
        val removedC = constraints.removeAll { it.bodyAId == id || it.bodyBId == id }
        val removedW = electricalSolver.wires.removeAll { it.bodyAId == id || it.bodyBId == id }
        if (removedC || removedW) {
            bodies.firstOrNull { it.id == id }?.wake()
            onToolSfx("detach", playerPos)
        }
    }

    private fun resizeTargetedInternal(newScale: Float) {
        val id = grabbedBodyId ?: targetedBodyId ?: return
        val body = bodies.firstOrNull { it.id == id } ?: return
        if (!body.type.resizable) return
        body.scale = newScale.coerceIn(0.4f, 3.0f)
        body.recomputeMassProperties()
        body.wake()
    }

    private fun setMaterialTargetedInternal(spec: MaterialSpec) {
        val id = grabbedBodyId ?: targetedBodyId ?: return
        val body = bodies.firstOrNull { it.id == id } ?: return
        body.applyMaterialSpec(spec)
    }

    private fun updateBodyPropertiesInternal(cmd: PhysicsCommand.UpdateBodyProperties) {
        val body = bodies.firstOrNull { it.id == cmd.bodyId } ?: return
        pushUndoSnapshot()
        if (cmd.clearMassOverride) body.massOverride = null
        else if (cmd.massOverride != null) body.massOverride = cmd.massOverride.coerceIn(0.2f, 2500f)
        cmd.scale?.let { if (body.type.resizable) body.scale = it.coerceIn(0.4f, 3.0f) }
        cmd.frictionOverride?.let { body.frictionOverride = it.coerceIn(0.02f, 1.0f) }
        cmd.restitutionOverride?.let { body.restitutionOverride = it.coerceIn(0.0f, 0.98f) }
        cmd.gravityScale?.let { body.gravityScale = it.coerceIn(-1.0f, 2.5f) }
        cmd.collisionMode?.let { body.collisionMode = it }
        cmd.materialSpecId?.let { body.applyMaterialSpec(MaterialLibrary.get(it)) }
        cmd.posOffset?.let { body.position += it }
        cmd.rotEulerDeg?.let { deg ->
            val r = PI.toFloat() / 180f
            body.orientation = Quat.fromEuler(deg.x * r, deg.y * r, deg.z * r)
        }
        cmd.isFrozen?.let { body.setFrozenState(it) }
        body.recomputeMassProperties()
        body.wake()
    }

    private fun groupTargetedInternal() {
        val id = targetedBodyId ?: return
        val target = bodies.firstOrNull { it.id == id } ?: return
        val firstId = pendingGroupBodyId
        if (firstId == null || firstId == id) {
            pendingGroupBodyId = id
            readoutTitle = "GROUP ANCHOR SET"
            readoutDetail = "Inspect another object and tap Group"
        } else {
            val first = bodies.firstOrNull { it.id == firstId }
            if (first != null) {
                val gid = first.groupId ?: target.groupId ?: nextGroupId++
                first.groupId = gid
                target.groupId = gid
                onToolSfx("attach", target.position)
                readoutTitle = "GROUPED #$gid"
                readoutDetail = "${first.customLabel} + ${target.customLabel}"
            }
            pendingGroupBodyId = null
        }
    }

    private fun ungroupTargetedInternal() {
        val id = targetedBodyId ?: return
        bodies.firstOrNull { it.id == id }?.groupId = null
        pendingGroupBodyId = null
        onToolSfx("detach", playerPos)
    }

    private fun toggleElectricalSwitchInternal() {
        val id = targetedBodyId ?: return
        val msg = electricalSolver.toggleSwitchOrResetFuse(id)
        if (msg != null) {
            onToolSfx("switch", playerPos)
            readoutTitle = "CIRCUIT"
            readoutDetail = msg
        }
    }

    private fun resetFuseInternal() {
        val id = targetedBodyId ?: return
        val msg = electricalSolver.toggleSwitchOrResetFuse(id)
        if (msg != null) {
            onToolSfx("attach", playerPos)
            readoutTitle = "FUSE"
            readoutDetail = msg
        }
    }

    private fun publishSnapshot(alpha: Float) {
        val (eyePos, forward, _) = getCameraBasis()
        val renderBodies = ArrayList<RenderBodyInstance>(bodies.size)
        for (b in bodies) {
            if (b.isDestroyed) continue
            val squashScale = if (b.squashAmount > 0.005f) {
                val sy = (1f - b.squashAmount).coerceAtLeast(0.45f)
                val sxz = 1f + b.squashAmount * 0.32f
                Vec3(b.scale * sxz, b.scale * sy, b.scale * sxz)
            } else {
                Vec3(b.scale, b.scale, b.scale)
            }
            val toolStyle = b.toolId?.let { ToolCatalog.get(it)?.meshStyle }
            renderBodies.add(
                RenderBodyInstance(
                    id = b.id,
                    meshId = b.type.meshId,
                    material = b.material,
                    materialSpec = b.materialSpec,
                    position = b.position,
                    prevPosition = b.prevPosition,
                    orientation = b.orientation,
                    prevOrientation = b.prevOrientation,
                    scale = squashScale,
                    boundingRadius = b.effectiveRadius * 1.25f,
                    isFrozen = b.isFrozen,
                    isHighlighted = (b.id == targetedBodyId || b.id == grabbedBodyId || b.id == pendingAttachBodyId),
                    toolMeshStyle = toolStyle,
                    crackLevel = b.crackLevel,
                    squashAmount = b.squashAmount,
                    emissiveColor = b.emissiveColor,
                    emissiveIntensity = b.emissiveIntensity
                )
            )
        }

        val renderConstraints = ArrayList<RenderConstraintLine>(constraints.size)
        for (c in constraints) {
            val a = bodies.firstOrNull { it.id == c.bodyAId } ?: continue
            val b = if (c.bodyBId >= 0) bodies.firstOrNull { it.id == c.bodyBId } else null
            val start = a.position + a.orientation.rotate(c.localAnchorA)
            val end = if (b != null) b.position + b.orientation.rotate(c.localAnchorB) else c.localAnchorB
            renderConstraints.add(RenderConstraintLine(c.type, start, end))
        }

        val renderWires = ArrayList<WireRenderLine>(electricalSolver.wires.size)
        for (w in electricalSolver.wires) {
            val a = bodies.firstOrNull { it.id == w.bodyAId } ?: continue
            val b = bodies.firstOrNull { it.id == w.bodyBId } ?: continue
            val na = electricalSolver.nodes[a.id]
            val nb = electricalSolver.nodes[b.id]
            val energized = (na?.energized == true) && (nb?.energized == true)
            val overloaded = (na?.overloaded == true) || (nb?.overloaded == true)
            renderWires.add(WireRenderLine(a.position, b.position, energized, overloaded))
        }

        val grabbedBody = grabbedBodyId?.let { id -> bodies.firstOrNull { it.id == id } }
        val beamStart = if (grabbedBody != null) eyePos + forward * 0.7f - Vec3(0f, 0.2f, 0f) else null
        val beamEnd = grabbedBody?.position

        val attachBody = pendingAttachBodyId?.let { id -> bodies.firstOrNull { it.id == id } }
        val attachStart = if (attachBody != null) attachBody.position + attachBody.orientation.rotate(pendingAttachLocalAnchor) else null
        val attachEnd = if (attachStart != null) eyePos + forward * 3.5f else null

        val equippedTool = ToolCatalog.get(activeCatalogToolId)
        val equippedToolMat = equippedTool?.let { MaterialLibrary.get(it.materialId) }
        val hud = firearmHudRef.get() ?: weaponController.toHudState(null)

        snapshotRef.set(
            WorldRenderSnapshot(
                mapId = currentMapId,
                timestampNs = System.nanoTime(),
                alpha = alpha,
                cameraPos = eyePos,
                cameraYaw = cameraYaw,
                cameraPitch = cameraPitch,
                cameraMode = cameraMode,
                playerPos = playerPos,
                playerVel = playerVel,
                playerOnGround = playerOnGround,
                playerGroundAcoustic = playerGroundAcoustic,
                isCrouching = effectiveCrouching,
                isSprinting = isSprinting && !effectiveCrouching,
                crouchFraction = crouchFraction,
                isGrabbing = grabbedBody != null,
                grabbedObjectPos = grabbedBody?.position,
                targetedWorldPos = targetedWorldPos,
                selectedTool = selectedToolType,
                activeCatalogToolId = activeCatalogToolId,
                interactPulse = interactPulse,
                releasePulse = releasePulse,
                landingImpulse = landingImpulse,
                dynamicBodies = renderBodies,
                staticProps = staticProps.toList(),
                constraints = renderConstraints,
                wires = renderWires,
                particles = particleSystem.buildRenderList(),
                grabbedBeamStart = beamStart,
                grabbedBeamEnd = beamEnd,
                attachPreviewStart = attachStart,
                attachPreviewEnd = attachEnd,
                isMenuDemo = isMenuDemo,
                timeOfDayHours = timeOfDayHours,
                weatherType = weatherType,
                fluidBasinEnabled = fluidBasinEnabled,
                fluidLevelY = fluidLevelY,
                equippedToolStyle = if (!isMenuDemo) equippedTool?.meshStyle else null,
                equippedToolMaterialSpec = equippedToolMat,
                equippedToolSpinAngle = toolSpinAngle,
                equippedToolActionPulse = toolActionPulse,
                flashlightActive = flashlightActive,
                firearmState = hud,
                weaponActionState = weaponController.actionState,
                isAiming = weaponController.isAiming,
                adsFovDegrees = weaponController.activeSpec?.adsFovDeg ?: 0f,
                adsBlend = adsBlend,
                recoilVisualOffset = weaponController.recoilVisualOffset,
                muzzleFlashIntensity = weaponController.muzzleFlashIntensity
            )
        )
    }
}
