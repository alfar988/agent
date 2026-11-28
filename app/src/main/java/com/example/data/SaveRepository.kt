package com.example.data

import android.content.Context
import com.example.electrical.WireConnection
import com.example.math.Quat
import com.example.math.Vec3
import com.example.physics.CollisionMode
import com.example.physics.ConstraintType
import com.example.physics.MapId
import com.example.physics.ObjectType
import com.example.physics.PhysicsConstraint
import com.example.physics.PhysicsMaterial
import com.example.physics.SavedBodyData
import com.example.physics.SessionExportData
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@JsonClass(generateAdapter = true)
data class Vec3Json(val x: Float, val y: Float, val z: Float) {
    fun toVec3() = Vec3(x, y, z)
    companion object {
        fun from(v: Vec3) = Vec3Json(v.x, v.y, v.z)
    }
}

@JsonClass(generateAdapter = true)
data class QuatJson(val x: Float, val y: Float, val z: Float, val w: Float) {
    fun toQuat() = Quat(x, y, z, w)
    companion object {
        fun from(q: Quat) = QuatJson(q.x, q.y, q.z, q.w)
    }
}

@JsonClass(generateAdapter = true)
data class SavedBodyJson(
    val id: Int,
    val type: String,
    val material: String,
    val pos: Vec3Json,
    val rot: QuatJson,
    val vel: Vec3Json,
    val angVel: Vec3Json,
    val scale: Float,
    val isFrozen: Boolean,
    val materialId: String? = null,
    val gravityScale: Float = 1.0f,
    val collisionMode: String = "SOLID",
    val massOverride: Float? = null,
    val frictionOverride: Float? = null,
    val restitutionOverride: Float? = null,
    val toolId: String? = null,
    val customLabel: String? = null,
    val groupId: Int? = null,
    val ammoVariantId: String? = null,
    val ammoAmount: Int = 0
)

@JsonClass(generateAdapter = true)
data class SavedConstraintJson(
    val id: Int,
    val type: String,
    val bodyAId: Int,
    val bodyBId: Int,
    val anchorA: Vec3Json,
    val anchorB: Vec3Json,
    val restLength: Float,
    val stiffness: Float,
    val damping: Float,
    val hingeAxis: Vec3Json,
    val relRot: QuatJson
)

@JsonClass(generateAdapter = true)
data class SavedWireJson(
    val id: Int,
    val bodyAId: Int,
    val bodyBId: Int
)

@JsonClass(generateAdapter = true)
data class SavedSessionJson(
    val slotId: String,
    val label: String,
    val mapId: String,
    val savedAtMs: Long,
    val playerPos: Vec3Json,
    val playerYaw: Float,
    val playerPitch: Float,
    val bodies: List<SavedBodyJson>,
    val constraints: List<SavedConstraintJson>,
    val wires: List<SavedWireJson> = emptyList(),
    val weaponMagAmmo: Map<String, Int> = emptyMap(),
    val weaponReserveAmmo: Map<String, Int> = emptyMap(),
    val isWeaponHolstered: Boolean = false
)

data class SaveSlotSummary(
    val slotId: String,
    val label: String,
    val mapId: MapId?,
    val objectCount: Int,
    val constraintCount: Int,
    val savedAtMs: Long,
    val isAutosave: Boolean
)

class SaveRepository(private val context: Context) {

    companion object {
        val SLOT_IDS = listOf("autosave", "slot_1", "slot_2", "slot_3")
    }

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(SavedSessionJson::class.java)
    private val saveDir: File
        get() = File(context.filesDir, "sandbox_saves").apply { mkdirs() }

    fun exportToJsonString(slotId: String, label: String, export: SessionExportData): String {
        val jsonModel = SavedSessionJson(
            slotId = slotId,
            label = label,
            mapId = export.mapId.name,
            savedAtMs = System.currentTimeMillis(),
            playerPos = Vec3Json.from(export.playerPos),
            playerYaw = export.playerYaw,
            playerPitch = export.playerPitch,
            bodies = export.bodies.map { b ->
                SavedBodyJson(
                    id = b.id,
                    type = b.type.name,
                    material = b.material.name,
                    pos = Vec3Json.from(b.position),
                    rot = QuatJson.from(b.orientation),
                    vel = Vec3Json.from(b.linearVelocity),
                    angVel = Vec3Json.from(b.angularVelocity),
                    scale = b.scale,
                    isFrozen = b.isFrozen,
                    materialId = b.materialId,
                    gravityScale = b.gravityScale,
                    collisionMode = b.collisionMode.name,
                    massOverride = b.massOverride,
                    frictionOverride = b.frictionOverride,
                    restitutionOverride = b.restitutionOverride,
                    toolId = b.toolId,
                    customLabel = b.customLabel,
                    groupId = b.groupId,
                    ammoVariantId = b.ammoVariantId,
                    ammoAmount = b.ammoAmount
                )
            },
            constraints = export.constraints.map { c ->
                SavedConstraintJson(
                    id = c.id,
                    type = c.type.name,
                    bodyAId = c.bodyAId,
                    bodyBId = c.bodyBId,
                    anchorA = Vec3Json.from(c.localAnchorA),
                    anchorB = Vec3Json.from(c.localAnchorB),
                    restLength = c.restLength,
                    stiffness = c.stiffness,
                    damping = c.damping,
                    hingeAxis = Vec3Json.from(c.hingeAxisWorld),
                    relRot = QuatJson.from(c.relativeOrientation)
                )
            },
            wires = export.wires.map { w -> SavedWireJson(w.id, w.bodyAId, w.bodyBId) },
            weaponMagAmmo = export.weaponMagAmmo,
            weaponReserveAmmo = export.weaponReserveAmmo,
            isWeaponHolstered = export.isWeaponHolstered
        )
        return adapter.toJson(jsonModel)
    }

    fun importFromJsonString(json: String): SessionExportData? {
        val parsed = runCatching { adapter.fromJson(json) }.getOrNull() ?: return null
        val mapId = runCatching { MapId.valueOf(parsed.mapId) }.getOrDefault(MapId.LAB)
        return SessionExportData(
            mapId = mapId,
            playerPos = parsed.playerPos.toVec3(),
            playerYaw = parsed.playerYaw,
            playerPitch = parsed.playerPitch,
            bodies = parsed.bodies.mapNotNull { b ->
                val type = runCatching { ObjectType.valueOf(b.type) }.getOrNull() ?: return@mapNotNull null
                val mat = runCatching { PhysicsMaterial.valueOf(b.material) }.getOrDefault(type.defaultMaterial)
                val colMode = runCatching { CollisionMode.valueOf(b.collisionMode) }.getOrDefault(CollisionMode.SOLID)
                SavedBodyData(
                    id = b.id,
                    type = type,
                    material = mat,
                    position = b.pos.toVec3(),
                    orientation = b.rot.toQuat(),
                    linearVelocity = b.vel.toVec3(),
                    angularVelocity = b.angVel.toVec3(),
                    scale = b.scale,
                    isFrozen = b.isFrozen,
                    materialId = b.materialId,
                    gravityScale = b.gravityScale,
                    collisionMode = colMode,
                    massOverride = b.massOverride,
                    frictionOverride = b.frictionOverride,
                    restitutionOverride = b.restitutionOverride,
                    toolId = b.toolId,
                    customLabel = b.customLabel,
                    groupId = b.groupId,
                    ammoVariantId = b.ammoVariantId,
                    ammoAmount = b.ammoAmount
                )
            },
            constraints = parsed.constraints.mapNotNull { c ->
                val type = runCatching { ConstraintType.valueOf(c.type) }.getOrNull() ?: return@mapNotNull null
                PhysicsConstraint(
                    id = c.id,
                    type = type,
                    bodyAId = c.bodyAId,
                    bodyBId = c.bodyBId,
                    localAnchorA = c.anchorA.toVec3(),
                    localAnchorB = c.anchorB.toVec3(),
                    restLength = c.restLength,
                    stiffness = c.stiffness,
                    damping = c.damping,
                    hingeAxisWorld = c.hingeAxis.toVec3(),
                    relativeOrientation = c.relRot.toQuat()
                )
            },
            wires = parsed.wires.map { w -> WireConnection(w.id, w.bodyAId, w.bodyBId) },
            weaponMagAmmo = parsed.weaponMagAmmo,
            weaponReserveAmmo = parsed.weaponReserveAmmo,
            isWeaponHolstered = parsed.isWeaponHolstered
        )
    }

    suspend fun saveSession(slotId: String, label: String, export: SessionExportData) = withContext(Dispatchers.IO) {
        val file = File(saveDir, "$slotId.json")
        file.writeText(exportToJsonString(slotId, label, export))
    }

    suspend fun loadSession(slotId: String): SessionExportData? = withContext(Dispatchers.IO) {
        val file = File(saveDir, "$slotId.json")
        if (!file.exists()) return@withContext null
        importFromJsonString(file.readText())
    }

    suspend fun listSlotSummaries(): List<SaveSlotSummary> = withContext(Dispatchers.IO) {
        SLOT_IDS.map { slotId ->
            val file = File(saveDir, "$slotId.json")
            val isAuto = slotId == "autosave"
            val defaultLabel = if (isAuto) "Autosave" else "Slot ${slotId.removePrefix("slot_")}"
            if (!file.exists()) {
                SaveSlotSummary(slotId, defaultLabel, null, 0, 0, 0L, isAuto)
            } else {
                val parsed = runCatching { adapter.fromJson(file.readText()) }.getOrNull()
                if (parsed == null) {
                    SaveSlotSummary(slotId, defaultLabel, null, 0, 0, 0L, isAuto)
                } else {
                    SaveSlotSummary(
                        slotId = slotId,
                        label = parsed.label.ifBlank { defaultLabel },
                        mapId = runCatching { MapId.valueOf(parsed.mapId) }.getOrNull(),
                        objectCount = parsed.bodies.size,
                        constraintCount = parsed.constraints.size,
                        savedAtMs = parsed.savedAtMs,
                        isAutosave = isAuto
                    )
                }
            }
        }
    }

    suspend fun deleteSlot(slotId: String) = withContext(Dispatchers.IO) {
        File(saveDir, "$slotId.json").delete()
    }
}
