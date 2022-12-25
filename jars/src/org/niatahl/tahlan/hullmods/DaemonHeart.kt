package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.niatahl.tahlan.plugins.DaemonOfficerPlugin
import org.niatahl.tahlan.utils.TahlanPeople
import org.niatahl.tahlan.utils.Utils
import java.awt.Color
import java.util.*
import kotlin.math.roundToInt

// There was some fun here. It was silly indeed.
class DaemonHeart : BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        stats.projectileSpeedMult.modifyMult(id, 1f + ACC_BUFF)
        stats.maxRecoilMult.modifyMult(id, 1f - ACC_BUFF)
        stats.recoilDecayMult.modifyMult(id, 1f + ACC_BUFF)
        stats.recoilPerShotMult.modifyMult(id, 1f - ACC_BUFF)
        stats.damageToMissiles.modifyMult(id, 1f + MSSL_DAMAGE)
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
        if (!ship.variant.hasTag(Tags.SHIP_UNIQUE_SIGNATURE)) {
            ship.variant.addTag(Tags.SHIP_UNIQUE_SIGNATURE)
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
        val speedBoost = 1f - Math.min(1f, ship.fluxLevel / SPEED_CAP)
        ship.mutableStats.maxSpeed.modifyMult(dc_id, 1f + speedBoost * SPEED_BUFF)
        if (engine.getFleetManager(ship.owner) === engine.getFleetManager(FleetSide.PLAYER)) {
            //Only run this in campaign context, not missions
            if (!engine.isInCampaign) {
                return
            }
            yoinkTimer.advance(amount)
            if (yoinkTimer.intervalElapsed()) {
                // Legio-owned Hel Scaiths can hijack enemy Daemons
                for (bote in CombatUtils.getShipsWithinRange(ship.location, 2000f)) {
                    if (bote.hullSpec.hullId.contains("tahlan_DunScaith_dmn") && Math.random() > 0.75f
                        && bote.fleetMember.fleetCommander.faction.id.contains("legioinfernalis")
                    ) {
                        engine.addFloatingText(ship.location, "ASSUMING DIRECT CONTROL", 40f, Color.RED, ship, 0.5f, 3f)
                        ship.owner = bote.owner

                        // yoinked from Xhan
                        if (ship.shipAI != null) {

                            //cancel orders so the AI doesn't get confused
                            val member_a = Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getDeployedFleetMember(ship)
                            if (member_a != null) Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getTaskManager(false).orderSearchAndDestroy(member_a, false)
                            val member_aa = Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getDeployedFleetMember(ship)
                            if (member_aa != null) Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getTaskManager(true).orderSearchAndDestroy(member_aa, false)
                            val member_b = Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).getDeployedFleetMember(ship)
                            if (member_b != null) Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).getTaskManager(false).orderSearchAndDestroy(member_b, false)
                            ship.shipAI.forceCircumstanceEvaluation()
                        }
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
        if (!member.fleetCommander.faction.id.contains("legioinfernalis")) {
            return
        }

        // Daemons are self-repairing so...
        // basically just making sure they never spawn with D-mods
        member.variant.hullMods.forEach { hm ->
            if (Global.getSettings().getHullModSpec(hm).hasTag(Tags.HULLMOD_DMOD)) {
                member.variant.removePermaMod(hm)
                member.variant.removeMod(hm)
            }
        }

        // Now we make a new captain if we don't have an AI captain already
        if (member.captain != null) {
            if (member.captain.isAICore) {
                return
            }
        }

        // Apparently this can be the case
        if (Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE) == null) {
            return
        }
        var die = MathUtils.getRandomNumberInRange(1, 5) - MAG[member.hullSpec.hullSize]!!
        if (member.hullSpec.hullId.contains("tahlan_DunScaith_dmn")) {
            die = 3 // Hel Scaith always gets an alpha
        }
        val person: PersonAPI // yes, a "person"
        if (die <= 1) {
            person = Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE).createPerson(Commodities.GAMMA_CORE, "tahlan_legioinfernalis", Misc.random)
        } else if (die == 2) {
            person = DaemonOfficerPlugin().createPerson(TahlanPeople.CORE_DAEMON, "tahlan_legioinfernalis", Misc.random)!!
            member.stats.dynamic.getMod("individual_ship_recovery_mod").modifyFlat("tahlan_daemoncore", -100f)
        } else {
            person = DaemonOfficerPlugin().createPerson(TahlanPeople.CORE_ARCHDAEMON, "tahlan_legioinfernalis", Misc.random)!!
            member.stats.dynamic.getMod("individual_ship_recovery_mod").modifyFlat("tahlan_archdaemoncore", -1000f)
        }
        member.captain = person
    }

    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        return false
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        if (index == 0) return "" + (ACC_BUFF * 100f).roundToInt() + Utils.txt("%")
        if (index == 1) return "" + (MSSL_DAMAGE * 100f).roundToInt() + Utils.txt("%")
        if (index == 2) return "" + (SPEED_CAP * 100f).roundToInt() + Utils.txt("%")
        if (index == 3) return "" + (SPEED_BUFF * 100f).roundToInt() + Utils.txt("%")
        if (index == 4) return "" + (PLAYER_NERF * 100f).roundToInt() + Utils.txt("%")
        return if (index == 5) "" + SUPPLIES_PERCENT.roundToInt() + Utils.txt("%") else null
    }

    companion object {
        private val MAG: MutableMap<HullSize, Int> = EnumMap(HullSize::class.java)

        init {
            MAG[HullSize.FRIGATE] = 2
            MAG[HullSize.DESTROYER] = 1
            MAG[HullSize.CRUISER] = 0
            MAG[HullSize.CAPITAL_SHIP] = 0
        }

        private const val SUPPLIES_PERCENT = 100f
        private const val ACC_BUFF = 0.25f
        private const val MSSL_DAMAGE = 0.5f
        private const val SPEED_BUFF = 0.2f
        private const val SPEED_CAP = 0.6f
        private const val PLAYER_NERF = 0.9f
        private val JITTER_COLOR = Color(255, 0, 0, 30)
        private val JITTER_UNDER_COLOR = Color(255, 0, 0, 80)
        private const val dc_id = "tahlan_daemoncore"
        private val yoinkTimer = IntervalUtil(10f, 30f)
    }
}