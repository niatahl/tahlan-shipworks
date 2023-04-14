package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.niatahl.tahlan.TahlanModPlugin.Companion.ENABLE_ADAPTIVEMODE
import org.niatahl.tahlan.TahlanModPlugin.Companion.ENABLE_HARDMODE
import org.niatahl.tahlan.plugins.DaemonOfficerPlugin
import org.niatahl.tahlan.utils.TahlanIDs.CORE_ARCHDAEMON
import org.niatahl.tahlan.utils.TahlanIDs.CORE_DAEMON
import org.niatahl.tahlan.utils.TahlanIDs.SOTF_CYWAR
import org.niatahl.tahlan.utils.TahlanIDs.SOTF_SIERRA
import org.niatahl.tahlan.utils.TahlanPeople.CIEVE
import org.niatahl.tahlan.utils.Utils.txt
import java.awt.Color
import kotlin.math.roundToInt

// There was some fun here. It was silly indeed.
class DaemonHeart : BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        stats.suppliesPerMonth.modifyPercent(id, SUPPLIES_PERCENT)
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (ship.shield != null) {
            val inner = "graphics/tahlan/fx/tahlan_shellshield.png"
            val outer = "graphics/tahlan/fx/tahlan_tempshield_ring.png"
            ship.shield.setRadius(ship.shieldRadiusEvenIfNoShield, inner, outer)
        }
        if (Global.getSector().playerFleet == null) {
            return
        }

        // Hackery to make the ships uniquely more potent while in Legio fleets in attempt to keep them more balanced in player hands
        var isPlayerFleet = false
        for (member in Global.getSector().playerFleet.fleetData.membersListCopy) {
            if (member.variant.hullVariantId == ship.variant.hullVariantId) {
                isPlayerFleet = true
            }
        }
        if (ship.variant.hasHullMod("tahlan_daemonboost")) {
            ship.variant.removeMod("tahlan_daemonboost")
        }
        if (isPlayerFleet) {
            ship.mutableStats.timeMult.modifyMult(id, PLAYER_NERF)
        } else {
            ship.variant.addMod("tahlan_daemonboost")
        }
    }

    override fun advanceInCombat(ship: ShipAPI, amount: Float) {
        val engine = Global.getCombatEngine() ?: return
        val speedBoost = 1f - (ship.fluxLevel / SPEED_CAP).coerceIn(0f, 1f)
        ship.mutableStats.maxSpeed.modifyFlat(dc_id, speedBoost * SPEED_BUFF)

        if (engine.getFleetManager(ship.owner) == engine.getFleetManager(FleetSide.PLAYER)) {
            //Only run this in campaign context, not missions
            if (!engine.isInCampaign) {
                return
            }
            yoinkTimer.advance(amount)
            if (yoinkTimer.intervalElapsed()) {
                // Legio-owned Hel Scaiths can hijack enemy Daemons

                var scaithPresent = false
                var counterPresent = false

                CombatUtils.getShipsWithinRange(ship.location, 2000f).forEach { bote ->
                    if (bote.isFighter || bote.captain == null) return@forEach
                    if (bote.hullSpec.hullId.contains("tahlan_DunScaith_dmn") && Math.random() > 0.75f
                        && bote.fleetMember.fleetCommander.faction.id.contains("legioinfernalis")
                        && bote.owner == 1
                    ) {
                        scaithPresent = true
                    }
                    if (bote.captain.stats.hasSkill(SOTF_CYWAR)) counterPresent = true
                }

                if (scaithPresent) {

                    // some captains can counter attempt if controlling the ship currently
                    if (counterPresent || ship.captain.id in immuneCaptains) {
                        engine.addFloatingText(ship.location, "EWAR ATTACK INTERCEPTED", 40f, Color.RED, ship, 0.5f, 3f)
                        ship.fluxTracker.forceOverload(5f)
                        return
                    }

                    // player gets an overload instead of being yoinked
                    if (ship.captain.isPlayer) {
                        engine.addFloatingText(
                            ship.location,
                            "DIRECT CONTROL ATTEMPT AVERTED",
                            40f,
                            Color.RED,
                            ship,
                            0.5f,
                            3f
                        )
                        ship.fluxTracker.forceOverload(15f)
                        return
                    }

                    engine.addFloatingText(ship.location, "ASSUMING DIRECT CONTROL", 40f, Color.RED, ship, 0.5f, 3f)
                    ship.owner = 1

                    // yoinked from Xhan
                    if (ship.shipAI != null) {
                        //cancel orders so the AI doesn't get confused
                        val memberA =
                            Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getDeployedFleetMember(ship)
                        if (memberA != null) Global.getCombatEngine().getFleetManager(FleetSide.PLAYER)
                            .getTaskManager(false).orderSearchAndDestroy(memberA, false)

                        val memberB =
                            Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getDeployedFleetMember(ship)
                        if (memberB != null) Global.getCombatEngine().getFleetManager(FleetSide.PLAYER)
                            .getTaskManager(true).orderSearchAndDestroy(memberB, false)

                        val memberC =
                            Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).getDeployedFleetMember(ship)
                        if (memberC != null) Global.getCombatEngine().getFleetManager(FleetSide.ENEMY)
                            .getTaskManager(false).orderSearchAndDestroy(memberC, false)

                        ship.shipAI.forceCircumstanceEvaluation()
                    }

                }

            }
        } else {
            if (!ship.isAlive || ship.isHulk || ship.isPiece) {
                ship.setJitter(dc_id, JITTER_COLOR, 0f, 0, 0f)
                ship.setJitterUnder(dc_id, JITTER_UNDER_COLOR, 0f, 0, 0f)
                return
            }

            // Enrage function
            val enrage = 1f - ship.hullLevel
            ship.mutableStats.timeMult.modifyMult(dc_id, 1f + enrage * 0.25f)
            ship.mutableStats.energyWeaponRangeBonus.modifyMult(dc_id, 1f - enrage * 0.25f)
            ship.mutableStats.ballisticWeaponRangeBonus.modifyMult(dc_id, 1f - enrage * 0.25f)
            ship.setJitter(dc_id, JITTER_COLOR, enrage, 3, 5f)
            ship.setJitterUnder(dc_id, JITTER_UNDER_COLOR, enrage, 20, 15f)
        }
    }

    override fun advanceInCampaign(member: FleetMemberAPI, amount: Float) {

        // Don't do this if we're in player fleet
        if (member.fleetCommander.isPlayer || member.fleetCommander.isDefault) {
            return
        }

        // Another check, I guess
        if (Global.getSector() != null && Global.getSector().playerFleet != null) {
            for (mem in Global.getSector().playerFleet.membersWithFightersCopy) {
                if (mem.id == member.id) {
                    return
                }
            }
        }

        // and another
        if (!member.fleetCommander.faction.id.contains("legioinfernalis") && !member.fleetCommander.faction.id.contains("unknown")) {
            return
        }

        // Daemons are self-repairing so...
        // basically just making sure they never spawn with D-mods
        member.variant.hullMods
            .filter { hm -> Global.getSettings().getHullModSpec(hm).hasTag(Tags.HULLMOD_DMOD) }
            .forEach { hm ->
                member.variant.removePermaMod(hm)
                member.variant.removeMod(hm)
            }


        // Now we make a new captain if we don't have an AI captain already
        if (member.captain != null) {
            if (member.captain.isAICore) {
                return
            }
        }

        // Also do Nightmare mode S-mod upgrades here, so we only run this once
        if (ENABLE_ADAPTIVEMODE) {
            val sMods = Global.getSector().playerFleet.membersWithFightersCopy
                .filter { !it.isFighterWing }
                .sumOf { it.variant.sMods.count() }

            val avgSMods = sMods.div(Global.getSector().playerFleet.fleetSizeCount)

            for (i in 0 until avgSMods) {
                val pick = SMOD_OPTIONS.filter { !member.variant.hasHullMod(it) }.randomOrNull()
                if (pick != null) {
                    member.variant.addMod(pick)
                    member.variant.addPermaMod(pick, true)
                }
            }
        }

        // Apparently this can be the case
        if (Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE) == null) {
            return
        }

        val min = if (member.hullSpec.hullId.contains("tahlan_DunScaith_dmn")) {
            3
        } else if (ENABLE_HARDMODE) {
            Global.getSector().playerPerson.stats.level.div(3).coerceAtLeast(1)
        } else {
            1
        }

        var die = (MathUtils.getRandomNumberInRange(1, 5) - MAG[member.hullSpec.hullSize]!!).coerceAtLeast(min)

        member.captain = when (die) {
            1 -> Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE).createPerson(Commodities.GAMMA_CORE, "tahlan_legioinfernalis", Misc.random)
            2 -> DaemonOfficerPlugin().createPerson(CORE_DAEMON, "tahlan_legioinfernalis", Misc.random)!!
            else -> DaemonOfficerPlugin().createPerson(CORE_ARCHDAEMON, "tahlan_legioinfernalis", Misc.random)!!
        }
    }

    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        return false
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        return when (index) {
            0 -> "" + (SPEED_CAP * 100f).roundToInt() + txt("%")
            1 -> "" + (SPEED_BUFF).roundToInt() + txt("su")
            2 -> "" + (PLAYER_NERF * 100f).roundToInt() + txt("%")
            3 -> "" + SUPPLIES_PERCENT.roundToInt() + txt("%")
            else -> null
        }
    }

    companion object {
        private val MAG = mapOf(
            HullSize.FRIGATE to 2,
            HullSize.DESTROYER to 1,
            HullSize.CRUISER to 0,
            HullSize.CAPITAL_SHIP to 0,
            HullSize.FIGHTER to 0
        )

        private val immuneCaptains = listOf(
            CIEVE,
            SOTF_SIERRA
        )

        private val SMOD_OPTIONS = listOf(
            HullMods.HEAVYARMOR,
            HullMods.HARDENED_SHIELDS,
            HullMods.MISSLERACKS,
            HullMods.UNSTABLE_INJECTOR,
            HullMods.EXTENDED_SHIELDS,
            HullMods.ACCELERATED_SHIELDS,
            HullMods.ECCM,
            HullMods.ARMOREDWEAPONS,
            HullMods.HARDENED_SUBSYSTEMS
        )

        private const val SUPPLIES_PERCENT = 100f
        private const val ACC_BUFF = 0.25f
        private const val MSSL_DAMAGE = 0.5f
        private const val SPEED_BUFF = 50f
        private const val SPEED_CAP = 0.75f
        private const val PLAYER_NERF = 0.9f
        private val JITTER_COLOR = Color(255, 0, 0, 30)
        private val JITTER_UNDER_COLOR = Color(255, 0, 0, 80)
        private const val dc_id = "tahlan_daemoncore"
        private val yoinkTimer = IntervalUtil(10f, 30f)
    }
}