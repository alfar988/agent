package com.example.render.gltf

import android.content.Context
import com.example.physics.MeshId
import com.example.physics.ObjectType
import com.example.physics.ToolCatalog
import com.example.physics.ToolDefinition
import com.example.physics.ToolMeshStyle
import com.example.weapons.AmmoBoxVariant
import com.example.weapons.FirearmCatalog
import com.example.weapons.FirearmId
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class GlbManifestEntry(
    val key: String,
    val assetPath: String,
    val vertices: Int,
    val triangles: Int
)

object GlbModelRegistry {

    const val MANIFEST_PATH = "models/manifest.json"
    const val CREDITS_PATH = "models/ASSET_CREDITS.json"
    const val KARAKTER_COMPANION_PATH = "characters/karakter_companion.glb"

    private val cache = ConcurrentHashMap<String, GlbStaticAsset>()

    fun assetPathFor(meshId: MeshId): String = when (meshId) {
        // Containers
        MeshId.REINFORCED_CRATE -> "models/containers/reinforced_crate.glb"
        MeshId.WOOD_PALLET_CRATE -> "models/containers/wood_pallet_crate.glb"
        MeshId.GLASS_CONTAINER -> "models/containers/glass_container.glb"
        MeshId.BARREL_DRUM -> "models/containers/barrel_drum.glb"
        // Mechanical
        MeshId.TREADED_TIRE -> "models/mechanical/treaded_tire.glb"
        MeshId.GYRO_FLYWHEEL -> "models/mechanical/gyro_flywheel.glb"
        MeshId.SPRING_PISTON -> "models/mechanical/spring_piston.glb"
        MeshId.GEAR_WHEEL -> "models/mechanical/gear_wheel.glb"
        MeshId.AXLE_ROD -> "models/mechanical/axle_rod.glb"
        MeshId.SUSPENSION_BOGIE -> "models/mechanical/suspension_bogie.glb"
        // Construction
        MeshId.STEEL_IBEAM -> "models/construction/steel_ibeam.glb"
        MeshId.CONCRETE_MASONRY -> "models/construction/concrete_masonry.glb"
        MeshId.RAMP_WEDGE -> "models/construction/ramp_wedge.glb"
        // Laboratory
        MeshId.CENTRIFUGE_POD -> "models/laboratory/centrifuge_pod.glb"
        MeshId.LAB_CONSOLE -> "models/laboratory/lab_console.glb"
        MeshId.LAB_TABLE -> "models/laboratory/lab_table.glb"
        // Experimental
        MeshId.GEODESIC_SPHERE -> "models/experimental/geodesic_sphere.glb"
        MeshId.DOMINO_MONOLITH -> "models/experimental/domino_monolith.glb"
        MeshId.SOFT_FOAM_BLOCK -> "models/experimental/soft_foam_block.glb"
        MeshId.BUOYANCY_FLOAT -> "models/experimental/buoyancy_float.glb"
        // Furniture
        MeshId.LAB_WORKBENCH -> "models/furniture/lab_workbench.glb"
        MeshId.OFFICE_CHAIR -> "models/furniture/office_chair.glb"
        MeshId.METAL_CABINET -> "models/furniture/metal_cabinet.glb"
        MeshId.WALL_CABINET -> "models/furniture/wall_cabinet.glb"
        // Electrical
        MeshId.BATTERY_UNIT -> "models/electrical/battery_unit.glb"
        MeshId.SWITCH_BOX -> "models/electrical/switch_box.glb"
        MeshId.LIGHT_BULB_LAMP -> "models/electrical/light_bulb_lamp.glb"
        MeshId.LED_INDICATOR -> "models/electrical/led_indicator.glb"
        MeshId.DC_MOTOR -> "models/electrical/dc_motor.glb"
        MeshId.KINETIC_GENERATOR -> "models/electrical/kinetic_generator.glb"
        MeshId.FUSE_BLOCK -> "models/electrical/fuse_block.glb"
        MeshId.CIRCUIT_CONNECTOR -> "models/electrical/circuit_connector.glb"
        MeshId.ELECTRICAL_PANEL -> "models/electrical/electrical_panel.glb"
        // Environment / Architectural
        MeshId.SHELF_RACK -> "models/environment/shelf_rack.glb"
        MeshId.CONVEYOR_UNIT -> "models/environment/conveyor_unit.glb"
        MeshId.SHIPPING_CONTAINER -> "models/environment/shipping_container.glb"
        MeshId.SCAFFOLD_FRAME -> "models/environment/scaffold_frame.glb"
        MeshId.FLOOR_SLAB -> "models/environment/floor_slab.glb"
        MeshId.WALL_PANEL -> "models/environment/wall_panel.glb"
        MeshId.PILLAR_COLUMN -> "models/environment/pillar_column.glb"
        MeshId.GLASS_PARTITION -> "models/environment/glass_partition.glb"
        MeshId.CRANE_BOOM -> "models/environment/crane_boom.glb"
        MeshId.CEILING_LIGHT -> "models/environment/ceiling_light.glb"
        MeshId.DOOR_FRAME -> "models/environment/door_frame.glb"
        MeshId.WINDOW_FRAME -> "models/environment/window_frame.glb"
        MeshId.CNC_MACHINE -> "models/environment/cnc_machine.glb"
        MeshId.HYDRAULIC_PRESS -> "models/environment/hydraulic_press.glb"
        MeshId.ROOF_TRUSS -> "models/environment/roof_truss.glb"
        MeshId.STAIR_FLIGHT -> "models/environment/stair_flight.glb"
        MeshId.PIPE_CONDUIT -> "models/environment/pipe_conduit.glb"
        MeshId.SAFETY_RAIL -> "models/environment/safety_rail.glb"
        MeshId.WARNING_STRIPE_CURB -> "models/environment/warning_stripe_curb.glb"
        MeshId.CEILING_VENT -> "models/environment/ceiling_vent.glb"
        // Ammunition Crates & Boxes
        MeshId.AMMO_BOX_9MM_AUTO -> AmmoBoxVariant.AMMO_9MM_AUTO.modelAssetPath
        MeshId.AMMO_BOX_45_SERVICE -> AmmoBoxVariant.AMMO_45_SERVICE.modelAssetPath
        MeshId.AMMO_BOX_SMG_9MM -> AmmoBoxVariant.AMMO_SMG_9MM.modelAssetPath
        MeshId.AMMO_BOX_556_CARBINE -> AmmoBoxVariant.AMMO_556_CARBINE.modelAssetPath
        MeshId.AMMO_BOX_12G_BUCKSHOT -> AmmoBoxVariant.AMMO_12G_BUCKSHOT.modelAssetPath
        MeshId.AMMO_BOX_762_MATCH -> AmmoBoxVariant.AMMO_762_MATCH.modelAssetPath
        MeshId.AMMO_BOX_762_AK47 -> AmmoBoxVariant.AMMO_762_AK47.modelAssetPath
        // Ordnance & Tactical Equipment
        MeshId.GRENADE_FRAG -> "models/equipment/grenade_frag.glb"
        MeshId.GRENADE_SMOKE -> "models/equipment/grenade_smoke.glb"
    }

    fun assetPathFor(style: ToolMeshStyle): String = when (style) {
        ToolMeshStyle.HAMMER_CLAW -> "models/tools/hammer_claw.glb"
        ToolMeshStyle.MALLET_HEAD -> "models/tools/mallet_head.glb"
        ToolMeshStyle.WRENCH_OPEN -> "models/tools/wrench_open.glb"
        ToolMeshStyle.RATCHET_SOCKET -> "models/tools/ratchet_socket.glb"
        ToolMeshStyle.SCREWDRIVER_DRIVER -> "models/tools/screwdriver_driver.glb"
        ToolMeshStyle.PLIERS_JAWS -> "models/tools/pliers_jaws.glb"
        ToolMeshStyle.HAND_SAW_BLADE -> "models/tools/hand_saw_blade.glb"
        ToolMeshStyle.POWER_DRILL -> "models/tools/power_drill.glb"
        ToolMeshStyle.ANGLE_GRINDER -> "models/tools/angle_grinder.glb"
        ToolMeshStyle.CIRCULAR_SAW -> "models/tools/circular_saw.glb"
        ToolMeshStyle.HEAT_GUN_NOZZLE -> "models/tools/heat_gun_nozzle.glb"
        ToolMeshStyle.WELDING_UNIT -> "models/tools/welding_unit.glb"
        ToolMeshStyle.WELDING_TORCH -> "models/tools/welding_torch.glb"
        ToolMeshStyle.CLAMP_C_FRAME -> "models/tools/clamp_c_frame.glb"
        ToolMeshStyle.HYDRAULIC_JACK -> "models/tools/hydraulic_jack.glb"
        ToolMeshStyle.HOIST_WINCH -> "models/tools/hoist_winch.glb"
        ToolMeshStyle.COMPRESSOR_TANK -> "models/tools/compressor_tank.glb"
        ToolMeshStyle.FAN_SHROUD -> "models/tools/fan_shroud.glb"
        ToolMeshStyle.GENERATOR_FRAME -> "models/tools/generator_frame.glb"
        ToolMeshStyle.DIGITAL_METER -> "models/tools/digital_meter.glb"
        ToolMeshStyle.THERMOMETER_PROBE -> "models/tools/thermometer_probe.glb"
        ToolMeshStyle.SCALE_PLATFORM -> "models/tools/scale_platform.glb"
        ToolMeshStyle.CALIPER_SLIDE -> "models/tools/caliper_slide.glb"
        ToolMeshStyle.PRESSURE_DIAL -> "models/tools/pressure_dial.glb"
        ToolMeshStyle.OPTICAL_MICROSCOPE -> "models/tools/optical_microscope.glb"
        ToolMeshStyle.LAB_BEAKER -> "models/tools/lab_beaker.glb"
        ToolMeshStyle.EXTINGUISHER_CYL -> "models/tools/extinguisher_cyl.glb"
        ToolMeshStyle.FLASHLIGHT_TORCH -> "models/tools/flashlight_torch.glb"
        ToolMeshStyle.WORKLIGHT_TRIPOD -> "models/tools/worklight_tripod.glb"
        ToolMeshStyle.DETECTOR_WAND -> "models/tools/detector_wand.glb"
        ToolMeshStyle.MAGNET_HORSESHOE -> "models/tools/magnet_horseshoe.glb"
        ToolMeshStyle.SUCTION_CUP_HANDLE -> "models/tools/suction_cup_handle.glb"
        ToolMeshStyle.PULLEY_BLOCK -> "models/tools/pulley_block.glb"
        ToolMeshStyle.SPIRIT_LEVEL -> "models/tools/spirit_level.glb"
        ToolMeshStyle.FIREARM_GLOCK18 -> FirearmCatalog.get(FirearmId.GLOCK18_STYLE).modelAssetPath
        ToolMeshStyle.FIREARM_SERVICE_PISTOL -> FirearmCatalog.get(FirearmId.SERVICE_PISTOL).modelAssetPath
        ToolMeshStyle.FIREARM_MP5 -> FirearmCatalog.get(FirearmId.MP5_STYLE).modelAssetPath
        ToolMeshStyle.FIREARM_M4A1 -> FirearmCatalog.get(FirearmId.M4A1_STYLE).modelAssetPath
        ToolMeshStyle.FIREARM_SHOTGUN -> FirearmCatalog.get(FirearmId.PUMP_SHOTGUN).modelAssetPath
        ToolMeshStyle.FIREARM_SNIPER -> FirearmCatalog.get(FirearmId.BOLT_SNIPER).modelAssetPath
        ToolMeshStyle.FIREARM_AK47 -> FirearmCatalog.get(FirearmId.AK47_STYLE).modelAssetPath
    }

    fun assetPathFor(obj: ObjectType): String = assetPathFor(obj.meshId)

    fun assetPathFor(tool: ToolDefinition): String = assetPathFor(tool.meshStyle)

    fun assetPathFor(firearmId: FirearmId): String = FirearmCatalog.get(firearmId).modelAssetPath

    fun assetPathFor(ammoVariant: AmmoBoxVariant): String = ammoVariant.modelAssetPath

    fun allRequiredPaths(): List<String> {
        val set = LinkedHashSet<String>()
        MeshId.entries.forEach { set.add(assetPathFor(it)) }
        ToolMeshStyle.entries.forEach { set.add(assetPathFor(it)) }
        FirearmId.entries.forEach { set.add(assetPathFor(it)) }
        AmmoBoxVariant.entries.forEach { set.add(assetPathFor(it)) }
        return set.toList()
    }

    fun loadOrGet(context: Context, assetPath: String, decodeTextures: Boolean = true): GlbStaticAsset? {
        cache[assetPath]?.let { return it }
        return try {
            val loaded = GlbLoader.loadStaticFromAssets(context, assetPath, decodeTextures)
            cache.putIfAbsent(assetPath, loaded) ?: loaded
        } catch (_: Throwable) {
            null
        }
    }

    fun loadManifest(context: Context): Map<String, GlbManifestEntry> {
        return try {
            val raw = context.assets.open(MANIFEST_PATH).bufferedReader().use { it.readText() }
            val root = JSONObject(raw)
            val assetsObj = root.optJSONObject("assets") ?: return emptyMap()
            val result = LinkedHashMap<String, GlbManifestEntry>()
            val keys = assetsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val item = assetsObj.getJSONObject(key)
                result[key] = GlbManifestEntry(
                    key = key,
                    assetPath = item.optString("path", key),
                    vertices = item.optInt("vertices", 0),
                    triangles = item.optInt("triangles", 0)
                )
            }
            result
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    fun clearCache() {
        cache.clear()
    }

    fun cachedAssetCount(): Int = cache.size
}
