package org.niatahl.tahlan.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.utils.Afterimage.renderCustomAfterimage
import java.awt.Color

class TemporalDriftStats : BaseShipSystemScript() {
    var HAS_FIRED_LIGHTNING = false
    private var runOnce = false
    private val interval = IntervalUtil(0.2f, 0.2f)
    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        val ship: ShipAPI?
        val player: Boolean
        if (stats.entity is ShipAPI) {
            ship = stats.entity as ShipAPI
            player = ship === Global.getCombatEngine().playerShip
        } else {
            return
        }

        //Sound
        if (!runOnce) {
            runOnce = true
            val loc = ship.location
            if (player) {
                Global.getSoundPlayer().playSound("tahlan_zawarudo", 1f, 1f, loc, ship.velocity)
            }
        }

        //Fires lightning at full charge, once
        var actualElectricSize = ELECTRIC_SIZE
        if (ship.hullSpec.hullId.contains("tahlan_Izanami")) {
            actualElectricSize = ELECTRIC_SIZE_IZANAMI
        }
        if (effectLevel >= 0.8f) {
            if (!HAS_FIRED_LIGHTNING) {
                HAS_FIRED_LIGHTNING = true
                /*Lightning based code...*/
                var tempCounter = 0f
                while (tempCounter <= 5.0f / ELECTRIC_SIZE * actualElectricSize) {
                    Global.getCombatEngine().spawnEmpArc(
                        ship,
                        Vector2f(
                            ship.location.x + MathUtils.getRandomNumberInRange(-actualElectricSize, actualElectricSize),
                            ship.location.y + MathUtils.getRandomNumberInRange(-actualElectricSize, actualElectricSize)
                        ),
                        null,
                        ship,
                        DamageType.ENERGY,  //Damage type
                        0f,  //Damage
                        0f,  //Emp
                        100000f,  //Max range
                        "tachyon_lance_emp_impact",
                        10f / ELECTRIC_SIZE * actualElectricSize,  // thickness
                        JITTER_COLOR,  //Central color
                        JITTER_UNDER_COLOR //Fringe Color
                    )
                    tempCounter++
                }
            }
        } else {
            HAS_FIRED_LIGHTNING = false
        }

        //time acceleration
        if (!ship.fluxTracker.isVenting) {
            val TimeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel
            stats.timeMult.modifyMult(id, TimeMult)
            if (player) {
                Global.getCombatEngine().timeMult.modifyMult(id, 1f / TimeMult)
            } else {
                Global.getCombatEngine().timeMult.unmodify(id)
            }
        } else {
            stats.timeMult.unmodify(id)
            Global.getCombatEngine().timeMult.unmodify(id)
        }

        //damage taken debuff
        val actualDamageMult = 1f + (1f - DAMAGE_MULT) * effectLevel
        stats.shieldDamageTakenMult.modifyMult(id, actualDamageMult)
        stats.armorDamageTakenMult.modifyMult(id, actualDamageMult)

        //dps debuff
        val actualDPSMult = 1f - DPS_MULT * effectLevel
        stats.energyWeaponDamageMult.modifyMult(id, actualDPSMult)
        stats.energyWeaponFluxCostMod.modifyMult(id, actualDPSMult)
        stats.ballisticRoFMult.modifyMult(id, actualDPSMult)

        //For Afterimages
        if (!Global.getCombatEngine().isPaused) {
            interval.advance(Global.getCombatEngine().elapsedInLastFrame)
            if (interval.intervalElapsed()) {
                renderCustomAfterimage(ship, AFTERIMAGE_COLOR, 1f)
            }
        }
    }

    override fun unapply(stats: MutableShipStatsAPI, id: String) {
        runOnce = false
        Global.getCombatEngine().timeMult.unmodify(id)
        stats.timeMult.unmodify(id)
        stats.armorDamageTakenMult.unmodify(id)
        stats.shieldDamageTakenMult.unmodify(id)
        stats.hullDamageTakenMult.unmodify(id)
        stats.energyRoFMult.unmodify(id)
        stats.ballisticRoFMult.unmodify(id)
        stats.beamWeaponDamageMult.unmodify(id)
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State, effectLevel: Float): StatusData? {
        return if (index == 0) {
            StatusData("ZA WARUDO", false)
        } else null
    }

    companion object {
        private val AFTERIMAGE_COLOR = Color(255, 63, 0, 60)
        private const val AFTERIMAGE_THRESHOLD = 4f
        const val DAMAGE_MULT = 2f
        const val DPS_MULT = 0.5f
        const val MAX_TIME_MULT = 20f
        val JITTER_COLOR = Color(255, 106, 32, 55)
        val JITTER_UNDER_COLOR = Color(255, 54, 0, 125)
        const val ELECTRIC_SIZE = 80.0f
        const val ELECTRIC_SIZE_IZANAMI = 300.0f
        private val ZERO = Vector2f()
    }
}