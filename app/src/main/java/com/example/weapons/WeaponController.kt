package com.example.weapons

import com.example.math.Vec3
import com.example.physics.CollisionDetector
import com.example.physics.RigidBody
import kotlin.math.min

class WeaponController {

    private val magAmmo = HashMap<FirearmId, Int>()
    private val reserveAmmo = HashMap<FirearmId, Int>()

    var activeFirearmId: FirearmId? = null
        private set
    val activeSpec: FirearmSpec?
        get() = activeFirearmId?.let { FirearmCatalog.get(it) }
    var actionState: WeaponActionState = WeaponActionState.HOLSTERED
        private set
    var isAiming: Boolean = false
        private set
    var isHolstered: Boolean = false
        private set
    var autoReloadEnabled: Boolean = true

    private var firePressed: Boolean = false
    private var semiShotQueued: Boolean = false
    private var semiTriggerConsumed: Boolean = false
    private var dryFirePlayedForPull: Boolean = false
    private var fireCooldownTimer: Float = 0f
    private var stateTimer: Float = 0f
    private var stateDuration: Float = 0.01f
    var recoilVisualOffset: Float = 0f
        private set
    var muzzleFlashIntensity: Float = 0f
        private set

    init {
        resetAmmoToDefaults()
    }

    fun resetAmmoToDefaults() {
        for (spec in FirearmCatalog.all) {
            magAmmo[spec.id] = spec.magazineCapacity
            reserveAmmo[spec.id] = spec.initialReserve
        }
        activeFirearmId = null
        actionState = WeaponActionState.HOLSTERED
        isAiming = false
        isHolstered = false
        firePressed = false
        semiShotQueued = false
        semiTriggerConsumed = false
        fireCooldownTimer = 0f
        stateTimer = 0f
        recoilVisualOffset = 0f
        muzzleFlashIntensity = 0f
    }

    fun getMagAmmo(id: FirearmId): Int = magAmmo[id] ?: FirearmCatalog.get(id).magazineCapacity
    fun getReserveAmmo(id: FirearmId): Int = reserveAmmo[id] ?: FirearmCatalog.get(id).initialReserve

    fun setMagAndReserve(id: FirearmId, mag: Int, reserve: Int) {
        val spec = FirearmCatalog.get(id)
        magAmmo[id] = mag.coerceIn(0, spec.magazineCapacity)
        reserveAmmo[id] = reserve.coerceIn(0, spec.maxReserve)
    }

    fun equipFirearmByToolId(toolId: String): Boolean {
        val spec = FirearmCatalog.byToolId(toolId) ?: return false
        val wasSame = activeFirearmId == spec.id
        activeFirearmId = spec.id
        firePressed = false
        semiShotQueued = false
        semiTriggerConsumed = false
        isAiming = false
        if (isHolstered) {
            actionState = WeaponActionState.HOLSTERED
        } else if (!wasSame || actionState == WeaponActionState.HOLSTERED) {
            actionState = if (wasSame) WeaponActionState.DRAWING else WeaponActionState.SWITCHING
            stateDuration = 0.28f
            stateTimer = stateDuration
        }
        return true
    }

    fun clearActiveFirearm() {
        activeFirearmId = null
        isAiming = false
        firePressed = false
        semiShotQueued = false
        semiTriggerConsumed = false
        actionState = WeaponActionState.HOLSTERED
        stateTimer = 0f
    }

    fun toggleHolster(): Boolean {
        if (activeFirearmId == null) return false
        isHolstered = !isHolstered
        firePressed = false
        semiShotQueued = false
        isAiming = false
        if (isHolstered) {
            actionState = WeaponActionState.HOLSTERING
            stateDuration = 0.22f
            stateTimer = stateDuration
        } else {
            actionState = WeaponActionState.DRAWING
            stateDuration = 0.25f
            stateTimer = stateDuration
        }
        return true
    }

    fun setAiming(aiming: Boolean?) {
        val spec = activeFirearmId?.let { FirearmCatalog.get(it) } ?: return
        if (isHolstered || actionState == WeaponActionState.HOLSTERED ||
            actionState == WeaponActionState.HOLSTERING || actionState == WeaponActionState.RELOADING
        ) {
            isAiming = false
            return
        }
        val next = aiming ?: !isAiming
        if (isAiming == next) return
        isAiming = next
        if (actionState == WeaponActionState.EQUIPPED_IDLE ||
            actionState == WeaponActionState.AIMING ||
            actionState == WeaponActionState.INSPECTING
        ) {
            actionState = if (next) WeaponActionState.AIMING else WeaponActionState.EQUIPPED_IDLE
        }
        // Keep spec referenced for future per-weapon ADS settle tuning
        if (spec.adsFovDeg <= 0f) isAiming = false
    }

    fun setFirePressed(pressed: Boolean) {
        if (pressed && !firePressed) {
            dryFirePlayedForPull = false
            semiShotQueued = true
        }
        if (!pressed) {
            semiTriggerConsumed = false
            dryFirePlayedForPull = false
        }
        firePressed = pressed
    }

    fun queueSingleTriggerTap() {
        if (activeFirearmId == null || isHolstered) return
        dryFirePlayedForPull = false
        semiShotQueued = true
        semiTriggerConsumed = false
    }

    fun startReload(): Boolean {
        val id = activeFirearmId ?: return false
        if (isHolstered || actionState == WeaponActionState.RELOADING ||
            actionState == WeaponActionState.HOLSTERED || actionState == WeaponActionState.HOLSTERING
        ) {
            return false
        }
        val spec = FirearmCatalog.get(id)
        val curMag = getMagAmmo(id)
        val curRes = getReserveAmmo(id)
        if (curMag >= spec.magazineCapacity || curRes <= 0) return false
        isAiming = false
        actionState = WeaponActionState.RELOADING
        stateDuration = spec.reloadDurationSec
        stateTimer = stateDuration
        return true
    }

    fun triggerInspect(): Boolean {
        if (activeFirearmId == null || isHolstered) return false
        if (actionState == WeaponActionState.EQUIPPED_IDLE) {
            actionState = WeaponActionState.INSPECTING
            stateDuration = 1.4f
            stateTimer = stateDuration
            return true
        }
        return false
    }

    fun addAmmoFromBox(variant: AmmoBoxVariant, boxAmount: Int): Int {
        val spec = FirearmCatalog.get(variant.firearmId)
        val cur = getReserveAmmo(variant.firearmId)
        val added = min(boxAmount.coerceAtLeast(0), (spec.maxReserve - cur).coerceAtLeast(0))
        reserveAmmo[variant.firearmId] = (cur + added).coerceIn(0, spec.maxReserve)
        return added
    }

    fun findNearbyAmmoBox(eyePos: Vec3, lookDir: Vec3, bodies: List<RigidBody>): NearbyAmmoBoxPrompt? {
        val ammoBodies = bodies.filter { !it.isDestroyed && it.type.isAmmoBox && it.ammoAmount > 0 }
        if (ammoBodies.isEmpty()) return null

        val rayHit = CollisionDetector.raycastBodiesAndStatics(eyePos, lookDir, 4.4f, ammoBodies, emptyList())
        val target = rayHit?.let { h -> ammoBodies.firstOrNull { it.id == h.bodyId } }
            ?: ammoBodies.minByOrNull { (it.position - eyePos).lengthSq() }?.takeIf { body ->
                val toBody = body.position - eyePos
                val distSq = toBody.lengthSq()
                if (distSq <= 2.5f * 2.5f) {
                    true
                } else if (distSq <= 4.35f * 4.35f) {
                    val len = kotlin.math.sqrt(distSq).coerceAtLeast(1e-4f)
                    val dot = (toBody.x * lookDir.x + toBody.y * lookDir.y + toBody.z * lookDir.z) / len
                    dot >= 0.55f
                } else {
                    false
                }
            } ?: return null

        val variant = AmmoBoxVariant.fromId(target.ammoVariantId ?: target.type.defaultAmmoVariantId()) ?: return null
        return NearbyAmmoBoxPrompt(
            bodyId = target.id,
            variantId = variant.id,
            displayName = variant.displayName,
            caliberLabel = variant.caliberLabel,
            amount = target.ammoAmount
        )
    }

    fun step(
        dt: Float,
        onFireShot: (spec: FirearmSpec, isAiming: Boolean) -> Unit,
        onSfx: (tag: String) -> Unit
    ) {
        recoilVisualOffset = (recoilVisualOffset - dt * 6.5f).coerceAtLeast(0f)
        muzzleFlashIntensity = (muzzleFlashIntensity - dt * 18.0f).coerceAtLeast(0f)
        if (fireCooldownTimer > 0f) {
            fireCooldownTimer = (fireCooldownTimer - dt).coerceAtLeast(0f)
        }

        val id = activeFirearmId ?: return
        val spec = FirearmCatalog.get(id)

        if (stateTimer > 0f) {
            stateTimer = (stateTimer - dt).coerceAtLeast(0f)
            if (stateTimer <= 0f) {
                when (actionState) {
                    WeaponActionState.HOLSTERING -> {
                        actionState = WeaponActionState.HOLSTERED
                    }
                    WeaponActionState.DRAWING,
                    WeaponActionState.SWITCHING,
                    WeaponActionState.INSPECTING -> {
                        actionState = if (isAiming) WeaponActionState.AIMING else WeaponActionState.EQUIPPED_IDLE
                    }
                    WeaponActionState.RELOADING -> {
                        val curMag = getMagAmmo(id)
                        val curRes = getReserveAmmo(id)
                        val needed = (spec.magazineCapacity - curMag).coerceAtLeast(0)
                        val load = min(needed, curRes)
                        magAmmo[id] = curMag + load
                        reserveAmmo[id] = curRes - load
                        actionState = if (isAiming) WeaponActionState.AIMING else WeaponActionState.EQUIPPED_IDLE
                    }
                    else -> Unit
                }
            }
        }

        if (isHolstered ||
            actionState == WeaponActionState.HOLSTERED ||
            actionState == WeaponActionState.HOLSTERING ||
            actionState == WeaponActionState.DRAWING ||
            actionState == WeaponActionState.SWITCHING ||
            actionState == WeaponActionState.RELOADING
        ) {
            semiShotQueued = false
            return
        }

        val wantsAuto = spec.automatic && firePressed
        val wantsSemi = !spec.automatic && (semiShotQueued || (firePressed && !semiTriggerConsumed))
        if ((wantsAuto || wantsSemi) && fireCooldownTimer <= 0f) {
            semiShotQueued = false
            val mag = getMagAmmo(id)
            if (mag > 0) {
                magAmmo[id] = mag - 1
                fireCooldownTimer = spec.fireIntervalSec
                if (!spec.automatic) semiTriggerConsumed = true
                actionState = WeaponActionState.FIRING
                recoilVisualOffset = 1.0f
                muzzleFlashIntensity = 1.0f
                onSfx(spec.shotSfxKey)
                onFireShot(spec, isAiming)
            } else {
                if (!spec.automatic) semiTriggerConsumed = true
                if (!dryFirePlayedForPull) {
                    dryFirePlayedForPull = true
                    onSfx("gun_dryfire")
                }
                if (autoReloadEnabled && getReserveAmmo(id) > 0) {
                    if (startReload()) {
                        onSfx("gun_reload")
                    }
                }
            }
        } else if (actionState == WeaponActionState.FIRING && fireCooldownTimer <= spec.fireIntervalSec * 0.35f) {
            actionState = if (isAiming) WeaponActionState.AIMING else WeaponActionState.EQUIPPED_IDLE
        }
    }

    fun exportMagSnapshot(): Map<String, Int> = magAmmo.entries.associate { it.key.name to it.value }
    fun exportReserveSnapshot(): Map<String, Int> = reserveAmmo.entries.associate { it.key.name to it.value }

    fun restoreAmmoSnapshot(mags: Map<String, Int>, reserves: Map<String, Int>) {
        if (mags.isEmpty() && reserves.isEmpty()) return
        for (spec in FirearmCatalog.all) {
            mags[spec.id.name]?.let { magAmmo[spec.id] = it.coerceIn(0, spec.magazineCapacity) }
            reserves[spec.id.name]?.let { reserveAmmo[spec.id] = it.coerceIn(0, spec.maxReserve) }
        }
    }

    fun toHudState(nearby: NearbyAmmoBoxPrompt?): FirearmHudState {
        val id = activeFirearmId
        val spec = id?.let { FirearmCatalog.get(it) }
        val reloadFrac = if (actionState == WeaponActionState.RELOADING && stateDuration > 0.01f) {
            (1f - stateTimer / stateDuration).coerceIn(0f, 1f)
        } else 0f
        return FirearmHudState(
            equippedFirearmId = id,
            displayName = spec?.displayName ?: "",
            caliberLabel = spec?.caliberLabel ?: "",
            actionState = actionState,
            isAiming = isAiming && !isHolstered,
            isHolstered = isHolstered || actionState == WeaponActionState.HOLSTERED,
            magAmmo = id?.let { getMagAmmo(it) } ?: 0,
            magCapacity = spec?.magazineCapacity ?: 0,
            reserveAmmo = id?.let { getReserveAmmo(it) } ?: 0,
            reloadProgress = reloadFrac,
            recoilVisualOffset = recoilVisualOffset,
            muzzleFlashIntensity = muzzleFlashIntensity,
            nearbyAmmoBox = nearby,
            allAmmoReserves = FirearmId.entries.associateWith { getReserveAmmo(it) }
        )
    }
}
