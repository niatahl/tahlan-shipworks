package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import org.niatahl.tahlan.plugins.CustomRender
import org.niatahl.tahlan.utils.Afterimage.renderCustomAfterimage
import org.niatahl.tahlan.utils.Utils
import java.awt.Color
import kotlin.math.roundToInt

class KnightRefit : BaseHullMod() {
    private var fadeOut = 1f
    private val INNERLARGE = "graphics/tahlan/fx/tahlan_tempshield.png"
    private val OUTERLARGE = "graphics/tahlan/fx/tahlan_tempshield_ring.png"
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        when (hullSize) {
            HullSize.FRIGATE -> stats.armorBonus.modifyFlat(ke_id, -ARMOR_MALUS_FRIGATE)
            HullSize.DESTROYER -> stats.armorBonus.modifyFlat(ke_id, -ARMOR_MALUS_DESTROYER)
            HullSize.CRUISER -> stats.armorBonus.modifyFlat(ke_id, -ARMOR_MALUS_CRUISER)
            HullSize.CAPITAL_SHIP -> stats.armorBonus.modifyFlat(ke_id, -ARMOR_MALUS_CAPITAL)
            else -> stats.armorBonus.modifyFlat(ke_id, -ARMOR_MALUS_FRIGATE)
        }
        stats.suppliesPerMonth.modifyPercent(ke_id, SUPPLIES_MULT)
        stats.crLossPerSecondPercent.modifyMult(ke_id, 2f)
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (ship.variant.hasHullMod("safetyoverrides")) {
            ship.mutableStats.weaponMalfunctionChance.modifyFlat(id, SO_MALFUNCTION_PROB)
            ship.mutableStats.engineMalfunctionChance.modifyFlat(id, SO_MALFUNCTION_PROB)
            ship.mutableStats.criticalMalfunctionChance.modifyFlat(id, SO_MALFUNCTION_PROB / 2)
        }
        ship.shield.setRadius(ship.shieldRadiusEvenIfNoShield, INNERLARGE, OUTERLARGE)
    }

    override fun advanceInCombat(ship: ShipAPI, amount: Float) {

        //don't run while paused because duh
        if (Global.getCombatEngine().isPaused) {
            return
        }

        //The Great Houses are actually timelords
        val player = ship === Global.getCombatEngine().playerShip
        if (!ship.isAlive || ship.isPiece) {
            return
        }
        if (ship.hullSpec.baseHullId !in GH_HULLS) {
            ship.mutableStats.peakCRDuration.modifyMult("lol_lmao_even", 0.1f)
            return
        }
        ship.isJitterShields = false
        if (ship.system != null) {
            if (!ship.system.isActive && !ship.fluxTracker.isOverloadedOrVenting) {
                fadeOut = if (ship.hitpoints <= ship.maxHitpoints * OVERDRIVE_TRIGGER_PERCENTAGE || ship.variant.hullMods.contains("tahlan_forcedoverdrive")) {
                    if (player) {
                        ship.mutableStats.timeMult.modifyPercent(ke_id, OVERDRIVE_TIME_MULT)
                        Global.getCombatEngine().timeMult.modifyPercent(ke_id, 1f / OVERDRIVE_TIME_MULT)
                    } else {
                        ship.mutableStats.timeMult.modifyPercent(ke_id, OVERDRIVE_TIME_MULT)
                        Global.getCombatEngine().timeMult.unmodify(ke_id)
                    }
                    if (ship.originalOwner != -1) {
                        ship.engineController.fadeToOtherColor(this, OVERDRIVE_ENGINE_COLOR, null, 1f, 0.7f)
                        ship.setJitter(ke_id, OVERDRIVE_JITTER_COLOR, 0.5f, 3, 5f)
                        ship.setJitterUnder(ke_id, OVERDRIVE_JITTER_UNDER_COLOR, 0.5f, 20, 10f)
                    }
                    if (player) {
                        Global.getCombatEngine()
                            .maintainStatusForPlayerShip(ke_id, "graphics/icons/hullsys/temporal_shell.png", "Temporal Overdrive", "Timeflow at 130%", false)
                    }
                    2f
                } else {
                    if (player) {
                        ship.mutableStats.timeMult.modifyPercent(ke_id, TIME_MULT)
                        Global.getCombatEngine().timeMult.modifyPercent(ke_id, 1f / TIME_MULT)
                        Global.getCombatEngine()
                            .maintainStatusForPlayerShip(ke_id, "graphics/icons/hullsys/temporal_shell.png", "Temporal Field", "Timeflow at 110%", false)
                    } else {
                        ship.mutableStats.timeMult.modifyPercent(ke_id, TIME_MULT)
                        Global.getCombatEngine().timeMult.unmodify(ke_id)
                    }
                    1f
                }
                ship.mutableStats.dynamic.getStat("tahlan_KRAfterimageTracker").modifyFlat("tahlan_KRAfterimageTrackerNullerID", -1f)
                ship.mutableStats.dynamic.getStat("tahlan_KRAfterimageTracker").modifyFlat(
                    "tahlan_KRAfterimageTrackerID",
                    ship.mutableStats.dynamic.getStat("tahlan_KRAfterimageTracker").modifiedValue + amount
                )
                if (ship.mutableStats.dynamic.getStat("tahlan_KRAfterimageTracker").modifiedValue > AFTERIMAGE_THRESHOLD) {
                    CustomRender.addAfterimage(
                        ship = ship,
                        colorIn = AFTERIMAGE_COLOR,
                        duration = fadeOut
                    )
                    ship.mutableStats.dynamic.getStat("tahlan_KRAfterimageTracker").modifyFlat(
                        "tahlan_KRAfterimageTrackerID",
                        ship.mutableStats.dynamic.getStat("tahlan_KRAfterimageTracker").modifiedValue - AFTERIMAGE_THRESHOLD
                    )
                }
            } else {
                ship.mutableStats.timeMult.unmodify(ke_id)
                Global.getCombatEngine().timeMult.unmodify(ke_id)
            }
        }
    }

    //Built-in only
    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        return false
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        if (index == 0) return Utils.txt("hmd_KassEng1")
        if (index == 1) return "" + TIME_MULT.toInt() + Utils.txt("%")
        if (index == 2) return "" + ARMOR_MALUS_FRIGATE.toInt() + Utils.txt("/") + ARMOR_MALUS_DESTROYER.toInt() + Utils.txt("/") + ARMOR_MALUS_CRUISER.toInt() + Utils.txt("/") + ARMOR_MALUS_CAPITAL.toInt()
        if (index == 3) return "" + SUPPLIES_MULT.toInt() + Utils.txt("%")
        if (index == 4) return "" + (OVERDRIVE_TRIGGER_PERCENTAGE * 100f).roundToInt() + Utils.txt("%")
        if (index == 5) return "" + OVERDRIVE_TIME_MULT.toInt() + Utils.txt("%")
        if (index == 6) return Utils.txt("hmd_KassEng2")
        return if (index == 7) Utils.txt("hmd_KassEng3") else null
    }

    companion object {
        const val ARMOR_MALUS_FRIGATE = 50f
        const val ARMOR_MALUS_DESTROYER = 100f
        const val ARMOR_MALUS_CRUISER = 200f
        const val ARMOR_MALUS_CAPITAL = 300f
        const val SUPPLIES_MULT = 50f
        const val OVERDRIVE_TRIGGER_PERCENTAGE = 0.3f
        const val OVERDRIVE_TIME_MULT = 30f
        const val TIME_MULT = 10f
        private val AFTERIMAGE_COLOR = Color(133, 126, 116, 90)
        private const val AFTERIMAGE_THRESHOLD = 0.4f
        private val OVERDRIVE_ENGINE_COLOR = Color(255, 44, 0)
        private val OVERDRIVE_GLOW_COLOR = Color(255, 120, 16)
        private val OVERDRIVE_JITTER_COLOR = Color(255, 63, 0, 20)
        private val OVERDRIVE_JITTER_UNDER_COLOR = Color(255, 63, 0, 80)
        private const val SO_MALFUNCTION_PROB = 0.03f
        private const val ke_id = "tahlan_KnightRefitID"
        val GH_HULLS = listOf(
            "tahlan_dominator_gh",
            "tahlan_enforcer_gh",
            "tahlan_onslaught_gh",
            "tahlan_monitor_gh",
            "tahlan_vendetta_gh",
            "tahlan_legion_gh",
            "tahlan_Ristreza_b",
            "tahlan_Ristreza",
            "tahlan_Exa_Pico",
            "tahlan_Vale",
            "tahlan_Izanami",
            "tahlan_Metafalica",
            "tahlan_Ristreza_g"
        )
    }
}