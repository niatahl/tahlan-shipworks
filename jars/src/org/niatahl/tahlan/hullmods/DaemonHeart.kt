package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.combat.CombatUtils
import org.niatahl.tahlan.listeners.LegioFleetInflationListener.Companion.addDaemonCore
import org.niatahl.tahlan.listeners.LegioFleetInflationListener.Companion.addSMods
import org.niatahl.tahlan.plugins.TahlanModPlugin.Companion.ENABLE_ADAPTIVEMODE
import org.niatahl.tahlan.utils.TahlanIDs.SOTF_BARROW
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
        if (ship.originalOwner == -1) return
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
                    if (counterPresent) {
                        engine.addFloatingText(ship.location, "EWAR ATTACK INTERCEPTED", 40f, Color.RED, ship, 0.5f, 3f)
                        ship.fluxTracker.forceOverload(3f)
                        return
                    }

                    if (ship.captain.id in immuneCaptains) {
                        engine.addFloatingText(ship.location, "EWAR ATTACK RESISTED", 40f, Color.RED, ship, 0.5f, 3f)
                        ship.fluxTracker.forceOverload(6f)
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
                        ship.fluxTracker.forceOverload(12f)
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
        if (member.fleetCommander.isPlayer || member.fleetCommander.isDefault || member.fleetCommander.faction.id.contains("player")) return

        // Another check, I guess
        if (Global.getSector() != null && Global.getSector().playerFleet != null) {
            if (member.fleetData.fleet != null && member.fleetData.fleet == Global.getSector().playerFleet) return
        }

        // Daemons are self-repairing so...
        // basically just making sure they never spawn with D-mods
        member.variant.hullMods
            .filter { hm -> Global.getSettings().getHullModSpec(hm).hasTag(Tags.HULLMOD_DMOD) }
            .forEach { hm ->
                member.variant.removePermaMod(hm)
                member.variant.removeMod(hm)
            }
        restoreToNonDHull(member.variant)

//        if (!member.captain.isAICore) {
//            addDaemonCore(member)
//        }
//        if (ENABLE_ADAPTIVEMODE && member.variant.sMods.isEmpty()) {
//            addSMods(member)
//        }
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

    private fun restoreToNonDHull(v: ShipVariantAPI) {
        var base = v.hullSpec.dParentHull

        // so that a skin with dmods can be "restored" - i.e. just dmods suppressed w/o changing to
        // actual base skin
        if (!v.hullSpec.isDefaultDHull && !v.hullSpec.isRestoreToBase) base = v.hullSpec
        if (base == null && v.hullSpec.isRestoreToBase) {
            base = v.hullSpec.baseHull
        }
        if (base != null) {
            v.setHullSpecAPI(base)
        }
    }

    companion object {
        private val immuneCaptains = listOf(
            CIEVE,
            SOTF_SIERRA,
            SOTF_BARROW
        )

        private const val SUPPLIES_PERCENT = 100f
        private const val SPEED_BUFF = 50f
        private const val SPEED_CAP = 0.5f
        private const val PLAYER_NERF = 0.9f
        private val JITTER_COLOR = Color(255, 0, 0, 30)
        private val JITTER_UNDER_COLOR = Color(255, 0, 0, 80)
        const val dc_id = "tahlan_daemoncore"
        private val yoinkTimer = IntervalUtil(10f, 30f)
    }
}