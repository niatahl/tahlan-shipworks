package org.niatahl.tahlan.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.CollisionUtils
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.plugins.CustomRender
import org.niatahl.tahlan.utils.Utils.txt
import org.niatahl.tahlan.utils.modify
import java.awt.Color

class PhaseBreakerV2Stats : BaseShipSystemScript() {
    private var activeTime = 0f
    private var runOnce = false
    private var levelForAlpha = 1f
    private var statuskey = Any()
    private val fogTimer = IntervalUtil(0.05f, 0.08f)
    private val projTracker = ArrayList<DamagingProjectileAPI>()
    private val beamTracker = ArrayList<BeamAPI>()

    fun maintainStatus(playerShip: ShipAPI, effectLevel: Float) {
        val cloak = playerShip.phaseCloak ?: playerShip.system ?: return
        if (effectLevel > VULNERABLE_FRACTION) {
            Global.getCombatEngine().maintainStatusForPlayerShip(
                statuskey,
                cloak.specAPI.iconSpriteName, cloak.displayName, txt("timeflow"), false
            )
        }
    }

    fun handleWeapons(ship: ShipAPI) {
        CombatUtils.getProjectilesWithinRange(ship.location, 500f)
            .filter { proj -> proj.source == ship && !projTracker.contains(proj) }
            .forEach { proj ->
                projTracker.add(proj)
                if (proj.weapon != null && proj.weapon.slot.isHardpoint && proj.weapon.slot.weaponType == WeaponAPI.WeaponType.ENERGY) {
                    proj.damage.damage *= 1.5f
                }
            }
        ship.allWeapons
            .filter { weapon -> weapon.isBeam && weapon.slot.isHardpoint && weapon.slot.weaponType == WeaponAPI.WeaponType.ENERGY }
            .forEach { weapon ->
                if (weapon.isFiring) {
                    weapon.beams.forEach { beam ->
                        if (!beamTracker.contains(beam)) {
                            beamTracker.add(beam)
                            beam.damage.damage *= 1.5f
                        }
                    }
                }
            }
    }

    fun fogMeUp(ship: ShipAPI) {
        if (fogTimer.intervalElapsed()) {
            val ce = Global.getCombatEngine()
            var point = Vector2f(MathUtils.getRandomPointInCircle(ship.location, ship.collisionRadius))
            while (!CollisionUtils.isPointWithinBounds(point, ship)) {
                point = MathUtils.getRandomPointInCircle(ship.location, ship.collisionRadius)
            }

            ce.addNebulaParticle(
                point,
                MathUtils.getRandomPointInCircle(Misc.ZERO, 50f),
                MathUtils.getRandomNumberInRange(150f, 300f),
                0.3f,
                0.3f,
                0.5f,
                MathUtils.getRandomNumberInRange(1f, 2f),
                Color(19, 25, 59, 80)
            )

            while (!CollisionUtils.isPointWithinBounds(point, ship)) {
                point = MathUtils.getRandomPointInCircle(ship.location, ship.collisionRadius * 0.75f)
            }

            ce.addNegativeNebulaParticle(
                point,
                MathUtils.getRandomPointInCircle(Misc.ZERO, 50f),
                MathUtils.getRandomNumberInRange(150f, 300f),
                0.3f,
                0.3f,
                0.5f,
                MathUtils.getRandomNumberInRange(1f, 2f),
                Color(200, 100, 80, 60)
            )

            CustomRender.addAfterimage(
                ship,
                EFFECT_COLOR.modify(alpha = 80),
                EFFECT_COLOR.modify(blue = 255),
                0.3f,
                10f,
                MathUtils.getRandomPointInCircle(ship.location, 10f)
            )

            ship.engineController.shipEngines.forEach { engine ->
                ce.addNegativeNebulaParticle(
                    engine.location,
                    MathUtils.getRandomPointInCircle(Misc.ZERO, 50f),
                    MathUtils.getRandomNumberInRange(40f, 80f),
                    0.3f,
                    0.5f,
                    0.5f,
                    MathUtils.getRandomNumberInRange(1f, 3f),
                    Color(200, 100, 80, 30)
                )
            }

        }
    }

    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        if (Global.getCombatEngine().isPaused) return
        val ship = if (stats.entity is ShipAPI) stats.entity as ShipAPI else return
        val player = ship === Global.getCombatEngine().playerShip
        val cloak = ship.phaseCloak ?: ship.system ?: return

        if (player) maintainStatus(ship, effectLevel)

        fogTimer.advance(Global.getCombatEngine().elapsedInLastFrame)
        fogMeUp(ship)

        if (state == ShipSystemStatsScript.State.COOLDOWN || state == ShipSystemStatsScript.State.IDLE) {
            unapply(stats, id)
            return
        }

        val engine = Global.getCombatEngine()
        val amount = engine.elapsedInLastFrame
        activeTime += amount

        var level = effectLevel

        when (state) {
            ShipSystemStatsScript.State.IN -> {
                levelForAlpha = level
            }

            ShipSystemStatsScript.State.ACTIVE -> {
                ship.isPhased = true
                levelForAlpha = level
                regenerateArmor(ship, amount)
            }

            ShipSystemStatsScript.State.OUT -> {
                Global.getSoundPlayer().playLoop("system_temporalshell_loop", ship, 1f, 1f, ship.location, ship.velocity)
                ship.isPhased = false
                levelForAlpha = (levelForAlpha - 2f * engine.elapsedInLastFrame).coerceAtLeast(0.5f)
                ship.setJitterUnder(id, EFFECT_COLOR, 1f - levelForAlpha, 10, 8f)
                level = if (ship.fluxTracker.isVenting || ship.fluxTracker.isOverloaded) 0f else 1f
                if (level > 0f) handleWeapons(ship)
            }

            else -> {}
        }

        Global.getCombatEngine().maintainStatusForPlayerShip("tahlan_debug", cloak.specAPI.iconSpriteName, "cloak", "active: " + levelForAlpha, false)
        ship.extraAlphaMult = 1f - levelForAlpha
        ship.setApplyExtraAlphaToEngines(true)
        val shipTimeMult = 1f + (getMaxTimeMult(stats) - 1f) * level
        stats.timeMult.modifyMult(id, shipTimeMult)
        if (player) {
            Global.getCombatEngine().timeMult.modifyMult(id, (1f / (shipTimeMult / 2f)).coerceAtMost(1f))
        } else {
            Global.getCombatEngine().timeMult.unmodify(id)
        }
        val speedPercentMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(0f)
        stats.maxSpeed.modifyPercent(id + "_skillmod", speedPercentMod * level)
    }

    private fun regenerateArmor(ship: ShipAPI, amount: Float) {
        val armorGrid = ship.armorGrid
        val grid = armorGrid.grid
        val maxArmor = armorGrid.maxArmorInCell
        val baseCell = maxArmor * ship.hullSpec.armorRating / armorGrid.armorRating
        val repairAmount = baseCell * (REGEN_PER_SEC_PERCENT / 100f) * amount

        for (x in grid.indices) {
            for (y in grid[0].indices) {
                if (grid[x][y] < maxArmor) {
                    val regen = (grid[x][y] + repairAmount).coerceAtMost(maxArmor)
                    armorGrid.setArmorValue(x, y, regen)
                }
            }
        }
        ship.syncWithArmorGridState()
    }


    override fun unapply(stats: MutableShipStatsAPI, id: String) {
        val ship: ShipAPI = if (stats.entity is ShipAPI) {
            stats.entity as ShipAPI
        } else {
            return
        }
        var cloak = ship.phaseCloak
        if (cloak == null) cloak = ship.system
        if (cloak == null) return
        runOnce = false
        ship.setJitterUnder(id, cloak.specAPI.effectColor2, 0f, 10, 2f)
        Global.getCombatEngine().timeMult.unmodify(id)
        stats.timeMult.unmodify(id)
        ship.isPhased = false
        ship.extraAlphaMult = 1f
        activeTime = 0f

        // clear lists
        projTracker.clear()
        beamTracker.clear()
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State, effectLevel: Float): StatusData? {
        return null
    }

    companion object {
        const val SHIP_ALPHA_MULT = 0.25f
        const val VULNERABLE_FRACTION = 0f
        const val MAX_TIME_MULT = 3f
        const val REGEN_PER_SEC_PERCENT = 10F
        val EFFECT_COLOR = Color(59, 121, 184, 80)
        fun getMaxTimeMult(stats: MutableShipStatsAPI): Float {
            return 1f + (MAX_TIME_MULT - 1f) * stats.dynamic.getValue(Stats.PHASE_TIME_BONUS_MULT)
        }
    }
}