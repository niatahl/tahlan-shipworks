package org.niatahl.tahlan.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.lazywizard.lazylib.combat.entities.SimpleEntity
import org.niatahl.tahlan.utils.Utils
import java.awt.Color

class DroneOverdriveStats : BaseShipSystemScript() {

    val interval = IntervalUtil(0.1f, 0.2f)

    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        val ship: ShipAPI = if (stats.entity is ShipAPI) {
            stats.entity as ShipAPI
        } else {
            return
        }

        interval.advance(Global.getCombatEngine().elapsedInLastFrame)

        if (effectLevel > 0) {
            getFighters(ship).forEach {
                if (interval.intervalElapsed() && Math.random() > 0.5f) zap(it)
                it.mutableStats.apply {
                    energyRoFMult.modifyMult(id, EFFECT_MULT)
                    energyWeaponFluxCostMod.modifyMult(id, 1f / EFFECT_MULT)
                }
            }
        }
    }

    override fun unapply(stats: MutableShipStatsAPI, id: String) {
        val ship: ShipAPI = if (stats.entity is ShipAPI) {
            stats.entity as ShipAPI
        } else {
            return
        }

        interval.advance(Global.getCombatEngine().elapsedInLastFrame)

        getFighters(ship).forEach {
            it.mutableStats.apply {
                energyRoFMult.unmodify(id)
                energyWeaponFluxCostMod.unmodify(id)
            }
        }
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State, effectLevel: Float): StatusData? {
        return if (index == 0) {
            StatusData(Utils.txt("droneOverdrive"), false)
        } else null
    }

    private fun getFighters(carrier: ShipAPI): List<ShipAPI> {
        val result = ArrayList<ShipAPI>()
        for (ship in Global.getCombatEngine().ships) {
            if (!ship.isFighter) continue
            if (ship.wing == null) continue
            if (ship.wing.sourceShip === carrier) {
                result.add(ship)
            }
        }
        return result
    }

    private fun zap(fighter: ShipAPI) {
        //Finds a target, in case we are going to overkill our current one

        //Finds a target, in case we are going to overkill our current one
        val targetList = CombatUtils.getEntitiesWithinRange(fighter.location, 500f)
        var target: CombatEntityAPI? = null

        for (potentialTarget in targetList) {
            //Checks for dissallowed targets, and ignores them
            if (potentialTarget !is ShipAPI && potentialTarget !is MissileAPI) continue

            if (potentialTarget.owner == fighter.owner) continue

            if (potentialTarget is ShipAPI && potentialTarget.isPhased) continue

            //If we found any applicable targets, pick the closest one

            if (target == null) {
                target = potentialTarget
            } else if (MathUtils.getDistance(target, fighter) > MathUtils.getDistance(potentialTarget, fighter)) {
                target = potentialTarget
            }
        }

        if (target != null) {
            Global.getCombatEngine().spawnEmpArc(
                fighter, fighter.location, fighter, target,
                DamageType.FRAGMENTATION,  //Damage type
                50f,  //Damage
                100f,  //Emp
                100000f,  //Max range
                null,  //Impact sound
                6f,  // thickness of the lightning bolt
                LIGHTNING_CORE_COLOR,  //Central color
                LIGHTNING_FRINGE_COLOR //Fringe Color
            )
        } else {
            Global.getCombatEngine().spawnEmpArc(
                fighter, fighter.location, fighter, SimpleEntity(MathUtils.getRandomPointInCircle(fighter.location, 100f)),
                DamageType.FRAGMENTATION,  //Damage type
                50f,  //Damage
                100f,  //Emp
                100000f,  //Max range
                null,  //Impact sound
                6f,  // thickness of the lightning bolt
                LIGHTNING_CORE_COLOR,  //Central color
                LIGHTNING_FRINGE_COLOR //Fringe Color
            )
        }
    }

    companion object {
        const val EFFECT_MULT = 2f
        val LIGHTNING_CORE_COLOR = Color(239, 255, 212, 150)
        val LIGHTNING_FRINGE_COLOR = Color(126, 195, 0, 200)
    }
}