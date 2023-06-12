package org.niatahl.tahlan.listeners

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetInflater
import com.fs.starfarer.api.campaign.listeners.FleetInflationListener
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.util.Misc
import org.apache.log4j.Priority
import org.lazywizard.lazylib.MathUtils
import org.niatahl.tahlan.hullmods.DaemonHeart
import org.niatahl.tahlan.plugins.DaemonOfficerPlugin
import org.niatahl.tahlan.plugins.TahlanModPlugin
import org.niatahl.tahlan.plugins.TahlanModPlugin.Companion.ENABLE_ADAPTIVEMODE
import org.niatahl.tahlan.utils.TahlanIDs
import org.niatahl.tahlan.utils.TahlanIDs.DAEMONIC_HEART
import org.niatahl.tahlan.utils.fixVariant
import org.niatahl.tahlan.utils.logger
import kotlin.math.roundToInt

class LegioFleetInflationListener : FleetInflationListener {
    override fun reportFleetInflated(fleet: CampaignFleetAPI, inflater: FleetInflater) {
        if (fleet.isPlayerFleet) return

        if (!fleet.faction.id.contains("tahlan_legio")) return

        fleet.membersWithFightersCopy
            .filter { !it.isFighterWing && it.variant.hasHullMod(DAEMONIC_HEART) }
            .forEach { member ->
                addSMods(member)
            }
    }

    companion object {

        private val SMOD_OPTIONS = listOf(
            HullMods.HEAVYARMOR,
            HullMods.HARDENED_SHIELDS,
            HullMods.MISSLERACKS,
            HullMods.UNSTABLE_INJECTOR,
            HullMods.EXTENDED_SHIELDS,
            HullMods.ACCELERATED_SHIELDS,
            HullMods.ECCM,
            HullMods.ARMOREDWEAPONS,
            HullMods.HARDENED_SUBSYSTEMS,
            HullMods.FLUXBREAKERS,
            HullMods.STABILIZEDSHIELDEMITTER,
            HullMods.AUTOREPAIR
        )

        fun addSMods(member: FleetMemberAPI) {

            val sMods = Global.getSector().playerFleet.membersWithFightersCopy
                .filter { !it.isFighterWing && !it.isCivilian }
                .sumOf { it.variant.sMods.count() }

            val numShips = Global.getSector().playerFleet.membersWithFightersCopy.count { !it.isFighterWing && !it.isCivilian }
            val avgSMods = (sMods.toFloat() / numShips.toFloat()).roundToInt()

            member.stats.dynamic.getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(DaemonHeart.dc_id, avgSMods.toFloat())

            member.fixVariant()

            for (i in 0 until avgSMods) {
                val pick = SMOD_OPTIONS
                    .filter { hm -> !member.variant.hasHullMod(hm) }
                    .filter { hm ->
                        !(member.variant.hullSpec.shieldType in setOf(ShieldAPI.ShieldType.NONE, ShieldAPI.ShieldType.PHASE)
                                && Global.getSettings().getHullModSpec(hm).hasTag(Tags.HULLMOD_REQ_SHIELDS))
                    }
                    .randomOrNull()
                if (pick != null) {
                    member.variant.addPermaMod(pick, true)
                }
            }

            member.updateStats()
        }

    }

}