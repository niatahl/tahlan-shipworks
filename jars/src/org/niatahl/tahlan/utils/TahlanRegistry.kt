package org.niatahl.tahlan.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import org.niatahl.tahlan.utils.TahlanIDs.TAG_DAEMON

/**
 * Runtime-collected id registries: shield-providing hullmods and the Legio daemon content
 * (ships / wings / weapons). The daemon lists seed a hardcoded core and are then topped up
 * from tagged specs by [collectFromSpecs] at application load.
 *
 * `@JvmField` so Java call sites read these as plain static fields
 * (e.g. `TahlanRegistry.SHIELD_HULLMODS`), matching the old TahlanModPlugin access.
 */
object TahlanRegistry {
    /** Every hullmod that provides (or bypasses) shields - used by lostech/override mods. */
    @JvmField
    val SHIELD_HULLMODS: MutableList<String> = ArrayList()

    @JvmField
    val DAEMON_SHIPS = mutableListOf(
        "tahlan_dominator_dmn",
        "tahlan_champion_dmn",
        "tahlan_manticore_dmn",
        "tahlan_hammerhead_dmn",
        "tahlan_centurion_dmn",
        "tahlan_vanguard_dmn",
        "tahlan_DunScaith_dmn",
        "tahlan_hound_dmn",
        "tahlan_sunder_dmn",
        "tahlan_kodai_dmn",
        "tahlan_retribution_dmn",
        "tahlan_mudskipper_dmn"
    )

    // to be added to blackwatch only
    @JvmField
    val BLACKWATCH_DAEMONS = mutableListOf(
        "tahlan_doom_dmn",
        "tahlan_afflictor_dmn"
    )

    @JvmField
    val DAEMON_WINGS = mutableListOf(
        "tahlan_miasma_drone_wing",
        "tahlan_flash_dmn_wing",
        "tahlan_spark_dmn_wing",
        "tahlan_lux_dmn_wing",
        "tahlan_thunder_dmn_wing",
        "tahlan_gaze_dmn_wing"
    )

    @JvmField
    val DAEMON_WEAPONS = mutableListOf(
        "kineticblaster",
        "gigacannon"
    )

    /** Collect shield-providing hullmods and tagged daemon ships/wings from the loaded specs. */
    fun collectFromSpecs() {
        for (hullModSpecAPI in Global.getSettings().allHullModSpecs) {
            if (hullModSpecAPI.hasTag("shields") && !SHIELD_HULLMODS.contains(hullModSpecAPI.id)) {
                SHIELD_HULLMODS.add(hullModSpecAPI.id)
            } else if (hullModSpecAPI.id.contains("swp_shieldbypass") && !SHIELD_HULLMODS.contains(hullModSpecAPI.id)) {
                SHIELD_HULLMODS.add("swp_shieldbypass") //Dirty fix for Shield Bypass, since that one is actually not tagged as a Shield mod, apparently
            }
        }

        Global.getSettings().allShipHullSpecs
            .filter { it.hasTag(TAG_DAEMON) && it.hullSize != ShipAPI.HullSize.FIGHTER }
            .filter { !DAEMON_SHIPS.contains(it.baseHullId) }
            .forEach { DAEMON_SHIPS.add(it.baseHullId) }

        Global.getSettings().allFighterWingSpecs
            .filter { it.hasTag(TAG_DAEMON) }
            .filter { !DAEMON_WINGS.contains(it.id) }
            .forEach { DAEMON_WINGS.add(it.id) }
    }
}
