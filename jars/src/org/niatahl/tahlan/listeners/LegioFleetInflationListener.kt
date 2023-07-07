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
import org.lazywizard.lazylib.MathUtils
import org.niatahl.tahlan.hullmods.DaemonHeart
import org.niatahl.tahlan.plugins.DaemonOfficerPlugin
import org.niatahl.tahlan.plugins.TahlanModPlugin
import org.niatahl.tahlan.plugins.TahlanModPlugin.Companion.ENABLE_ADAPTIVEMODE
import org.niatahl.tahlan.utils.TahlanIDs
import org.niatahl.tahlan.utils.TahlanIDs.DAEMONIC_HEART
import org.niatahl.tahlan.utils.fixVariant
import kotlin.math.roundToInt

class LegioFleetInflationListener : FleetInflationListener {
    override fun reportFleetInflated(fleet: CampaignFleetAPI, inflater: FleetInflater) {
        if (fleet.isPlayerFleet) return

        if (!fleet.faction.id.contains("tahlan_legio")) return

        fleet.membersWithFightersCopy
            .filter { !it.isFighterWing && it.variant.hasHullMod(DAEMONIC_HEART) }
            .forEach {
                addDaemonCore(it)
                if (ENABLE_ADAPTIVEMODE) {
                    clearSMods(it)
                    addSMods(it)
                }
            }
    }

    companion object {

        private val MAG = mapOf(
            ShipAPI.HullSize.FRIGATE to 2,
            ShipAPI.HullSize.DESTROYER to 1,
            ShipAPI.HullSize.CRUISER to 0,
            ShipAPI.HullSize.CAPITAL_SHIP to 0,
            ShipAPI.HullSize.FIGHTER to 0
        ).withDefault { 0 }

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

            if (Global.getSector() == null || Global.getSector().playerFleet == null) return

            val sMods = Global.getSector().playerFleet.membersWithFightersCopy
                .filter { !it.isFighterWing && !it.isCivilian }
                .sumOf {
                    if (it.variant != null) it.variant.sMods.count() else 0
                }

            if (sMods == 0) return

            val numShips = Global.getSector().playerFleet.membersWithFightersCopy.count { !it.isFighterWing && !it.isCivilian }.coerceAtLeast(1)
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

        fun clearSMods(member: FleetMemberAPI) {
            member.fixVariant()
            member.variant.sMods.toList().forEach{
                member.variant.removePermaMod(it)
                member.variant.removeMod(it)
            }
        }

        fun addDaemonCore(member: FleetMemberAPI) {
            // Now we make a new captain if we don't have an AI captain already
            if (member.captain != null && member.captain.isAICore) return

            // Apparently this can be the case
            if (Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE) == null) {
                return
            }

            val min = if (member.hullSpec.hullId.contains("tahlan_DunScaith_dmn")) {
                3
            } else if (TahlanModPlugin.ENABLE_HARDMODE) {
                Global.getSector().playerPerson.stats.level.div(3).coerceAtLeast(1)
            } else {
                1
            }

            val die = (MathUtils.getRandomNumberInRange(1, 5) - MAG[member.hullSpec.hullSize]!!).coerceAtLeast(min)

            if (member.fleetCommander.faction.id.contains("tahlan_legio")) { // Should catch all legio subfactions
                member.captain = when (die) {
                    1 -> Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE).createPerson(Commodities.GAMMA_CORE, "tahlan_legioinfernalis", Misc.random)
                    2 -> DaemonOfficerPlugin().createPerson(TahlanIDs.CORE_DAEMON, "tahlan_legioinfernalis", Misc.random)!!
                    else -> DaemonOfficerPlugin().createPerson(TahlanIDs.CORE_ARCHDAEMON, "tahlan_legioinfernalis", Misc.random)!!
                }
            } else {
                member.captain = when (die) {
                    1 -> Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE).createPerson(Commodities.GAMMA_CORE, member.fleetCommander.faction.id, Misc.random)
                    2 -> Misc.getAICoreOfficerPlugin(Commodities.BETA_CORE).createPerson(Commodities.BETA_CORE, member.fleetCommander.faction.id, Misc.random)
                    else -> Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE).createPerson(Commodities.ALPHA_CORE, member.fleetCommander.faction.id, Misc.random)
                }
            }

            if (member.variant.hasHullMod(HullMods.SAFETYOVERRIDES)) {
                if (!member.captain.stats.hasSkill(Skills.POINT_DEFENSE)) {
                    member.captain.stats.setSkillLevel(member.captain.stats.skillsCopy.filter { it.skill.id != Skills.COMBAT_ENDURANCE }.random().skill.id, 0f)
                    member.captain.stats.setSkillLevel(Skills.POINT_DEFENSE, 2f)
                }
                if (!member.captain.stats.hasSkill(Skills.COMBAT_ENDURANCE)) {
                    member.captain.stats.setSkillLevel(member.captain.stats.skillsCopy.filter { it.skill.id != Skills.POINT_DEFENSE }.random().skill.id, 0f)
                    member.captain.stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2f)
                }
            }
        }
    }

}