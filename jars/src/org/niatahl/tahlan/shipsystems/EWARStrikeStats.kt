package org.niatahl.tahlan.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicRender
import java.awt.Color


open class EWARStrikeStats : BaseShipSystemScript() {
    class TargetData(var ship: ShipAPI, var target: ShipAPI) {
        var targetEffectPlugin: EveryFrameCombatPlugin? = null
        var currDamMult = 0f
        var elaspedAfterInState = 0f
    }

    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        val ship: ShipAPI = if (stats.entity is ShipAPI) {
            stats.entity as ShipAPI
        } else {
            return
        }
        val targetDataKey = ship.id + "_ewar_target_data"
        var targetDataObj = Global.getCombatEngine().customData[targetDataKey]
        if (state == ShipSystemStatsScript.State.IN && targetDataObj == null) {
            val target = findTarget(ship)
            Global.getCombatEngine().customData[targetDataKey] = target?.let { TargetData(ship, it) }
            if (target != null) {
                if (target.fluxTracker.showFloaty() || ship === Global.getCombatEngine().playerShip || target === Global.getCombatEngine().playerShip) {
                    target.fluxTracker.showOverloadFloatyIfNeeded("EWAR Strike!", TEXT_COLOR, 4f, true)
                }
            }
        } else if (state == ShipSystemStatsScript.State.IDLE && targetDataObj != null) {
            Global.getCombatEngine().customData.remove(targetDataKey)
            (targetDataObj as TargetData).currDamMult = 1f
            targetDataObj = null
        }

        if (targetDataObj == null) return
        val targetData = targetDataObj as TargetData
        targetData.currDamMult = 1f + (DAM_MULT - 1f) * effectLevel
        if (targetData.targetEffectPlugin == null) {
            targetData.targetEffectPlugin = object : BaseEveryFrameCombatPlugin() {
                val interval = IntervalUtil(0.5f, 0.5f)
                override fun advance(amount: Float, events: List<InputEventAPI>) {
                    if (Global.getCombatEngine().isPaused) return
                    if (targetData.target === Global.getCombatEngine().playerShip) {
                        Global.getCombatEngine().maintainStatusForPlayerShip(
                            KEY_TARGET,
                            targetData.ship.system.specAPI.iconSpriteName,
                            targetData.ship.system.displayName,
                            "" + ((targetData.currDamMult - 1f) * 100f).toInt() + "% more damage taken", true
                        )
                    }

                    interval.advance(amount)
                    val fxSprite = Global.getSettings().getSprite("fx", "tahlan_ewar_target")
                    if (fxSprite != null) {
                        val radius = targetData.target.collisionRadius * 2.5f
                        if (interval.intervalElapsed()) MagicRender.objectspace(
                            fxSprite,
                            targetData.target,
                            Vector2f(),
                            Vector2f(),
                            Vector2f(radius, radius),
                            Vector2f(),
                            90f - targetData.target.facing,
                            0f,
                            true,
                            Color(215, 0, 0, 100),
                            true,
                            .1f,
                            .3f,
                            .2f,
                            true
                        )
                    }


                    if (targetData.currDamMult <= 1f || !targetData.ship.isAlive) {
                        targetData.target.mutableStats.apply {
                            hullDamageTakenMult.unmodify(id)
                            armorDamageTakenMult.unmodify(id)
                            shieldDamageTakenMult.unmodify(id)
                            empDamageTakenMult.unmodify(id)
                            maxSpeed.unmodify(id)
                            energyWeaponRangeBonus.unmodify(id)
                            ballisticWeaponRangeBonus.unmodify(id)
                        }
                        Global.getCombatEngine().removePlugin(targetData.targetEffectPlugin)
                    } else {
                        targetData.target.mutableStats.apply {
                            val currMult = targetData.currDamMult
                            hullDamageTakenMult.modifyMult(id, currMult)
                            armorDamageTakenMult.modifyMult(id, currMult)
                            shieldDamageTakenMult.modifyMult(id, currMult)
                            empDamageTakenMult.modifyMult(id, currMult)
                            maxSpeed.modifyMult(id, 2f - currMult)
                            energyWeaponRangeBonus.modifyMult(id, 2f - currMult)
                            ballisticWeaponRangeBonus.modifyMult(id, 2f -currMult)
                        }
                    }
                }
            }
            Global.getCombatEngine().addPlugin(targetData.targetEffectPlugin)
        }
        if (effectLevel > 0) {
            if (state != ShipSystemStatsScript.State.IN) {
                targetData.elaspedAfterInState += Global.getCombatEngine().elapsedInLastFrame
            }
            var shipJitterLevel = 0f
            shipJitterLevel = if (state == ShipSystemStatsScript.State.IN) {
                effectLevel
            } else {
                val durOut = 0.5f
                (durOut - targetData.elaspedAfterInState).coerceAtLeast(0f) / durOut
            }
            val maxRangeBonus = 50f
            val jitterRangeBonus = shipJitterLevel * maxRangeBonus
            val color = JITTER_COLOR
            if (shipJitterLevel > 0) {
                ship.setJitter(KEY_SHIP, color, shipJitterLevel, 4, 0f, 0 + jitterRangeBonus * 1f)
            }
            targetData.target.setJitter(KEY_TARGET, color, effectLevel, 3, 0f, 5f)
        }
    }

    override fun unapply(stats: MutableShipStatsAPI, id: String) {}

    private fun findTarget(ship: ShipAPI): ShipAPI? {
        val range = getMaxRange(ship)
        val player = ship === Global.getCombatEngine().playerShip
        var target = ship.shipTarget
        if (ship.shipAI != null && ship.aiFlags.hasFlag(AIFlags.TARGET_FOR_SHIP_SYSTEM)) {
            target = ship.aiFlags.getCustom(AIFlags.TARGET_FOR_SHIP_SYSTEM) as ShipAPI
        }
        if (target != null) {
            val dist = Misc.getDistance(ship.location, target.location)
            val radSum = ship.collisionRadius + target.collisionRadius
            if (dist > range + radSum) target = null
        } else {
            if (player) {
                target = Misc.findClosestShipEnemyOf(ship, ship.mouseTarget, HullSize.FIGHTER, range, true)
            } else {
                val test = ship.aiFlags.getCustom(AIFlags.MANEUVER_TARGET)
                if (test is ShipAPI) {
                    target = test
                    val dist = Misc.getDistance(ship.location, target.location)
                    val radSum = ship.collisionRadius + target.collisionRadius
                    if (dist > range + radSum) target = null
                }
            }
            if (target == null) {
                target = Misc.findClosestShipEnemyOf(ship, ship.location, HullSize.FIGHTER, range, true)
            }
        }
        return target
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State, effectLevel: Float): StatusData? {
        if (effectLevel > 0) {
            if (index == 0) {
                return StatusData("Target crippled", false)
            }
        }
        return null
    }

    override fun getInfoText(system: ShipSystemAPI, ship: ShipAPI): String? {
        if (system.isOutOfAmmo) return null
        if (system.state != SystemState.IDLE) return null
        val target = findTarget(ship)
        if (target != null && target !== ship) {
            return "READY"
        }
        return if (target == null && ship.shipTarget != null) {
            "OUT OF RANGE"
        } else "NO TARGET"
    }

    override fun isUsable(system: ShipSystemAPI, ship: ShipAPI): Boolean {
        //if (true) return true;
        val target = findTarget(ship)
        return target != null && target !== ship
    }

    companion object {
        var KEY_SHIP = Any()
        var KEY_TARGET = Any()
        var DAM_MULT = 1.25f
        protected var RANGE = 2000f
        var TEXT_COLOR = Color(255, 55, 55, 255)
        var JITTER_COLOR = Color(255, 50, 50, 75)
        var JITTER_UNDER_COLOR = Color(255, 100, 100, 155)
        fun getMaxRange(ship: ShipAPI): Float {
            return ship.mutableStats.systemRangeBonus.computeEffective(RANGE)
        }
    }
}