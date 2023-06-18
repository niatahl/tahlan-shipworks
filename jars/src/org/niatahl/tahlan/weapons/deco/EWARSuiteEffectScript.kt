package org.niatahl.tahlan.weapons.deco

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicRender
import org.niatahl.tahlan.utils.TahlanIDs.DAEMONIC_HEART
import java.awt.Color
import kotlin.collections.ArrayList
import kotlin.math.ceil

class EWARSuiteEffectScript : EveryFrameWeaponEffectPlugin {
    private var rotation = 0f
    private var sprite: SpriteAPI? = null
    private val targetList = ArrayList<ShipAPI>()
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        if (engine.isPaused) return
        val ship = weapon.ship ?: return
        if (!ship.isAlive || ship.isHulk || ship.isPiece) {
            return
        }

        //Glows off in refit screen
        if (ship.originalOwner == -1) {
            return
        }

        sprite = Global.getSettings().getSprite("fx", "tahlan_ewar_aura")

        val loc = ship.location
        MagicRender.singleframe(
            sprite,
            loc,
            Vector2f(EFFECT_RANGE*2f, EFFECT_RANGE*2f),
            rotation,
            Color(255,0,0,30),
            true
        )

        // Spin it
        rotation += ROTATION_SPEED * amount
        if (rotation > 360f) {
            rotation -= 360f
        }

        for (target in CombatUtils.getShipsWithinRange(ship.location, EFFECT_RANGE)) {
            if (target.owner != ship.owner && !targetList.contains(target)) {
                targetList.add(target)
            }
        }
        val purgeList = ArrayList<ShipAPI>()
        for (target in targetList) {
            if (MathUtils.getDistance(target.location, ship.location) <= EFFECT_RANGE) {
                target.mutableStats.apply {
                    shieldDamageTakenMult.modifyMult(EWAR_ID, DAMAGE_MULT)
                    armorDamageTakenMult.modifyMult(EWAR_ID, DAMAGE_MULT)
                    hullDamageTakenMult.modifyMult(EWAR_ID, DAMAGE_MULT)
                    damageToMissiles.modifyMult(EWAR_ID, PDDMG_MULT)
                    damageToFighters.modifyMult(EWAR_ID, PDDMG_MULT)
                }
            } else {
                target.mutableStats.apply {
                    shieldDamageTakenMult.unmodify(EWAR_ID)
                    armorDamageTakenMult.unmodify(EWAR_ID)
                    hullDamageTakenMult.unmodify(EWAR_ID)
                    damageToMissiles.unmodify(EWAR_ID)
                    damageToFighters.unmodify(EWAR_ID)
                }
                purgeList.add(target)
            }
        }
        for (purge in purgeList) {
            targetList.remove(purge)
        }
    }

    companion object {
        private const val EWAR_ID = "tahlan_ewar_ID"
        const val EFFECT_RANGE = 2000f
        const val DAMAGE_MULT = 1.1f
        const val PDDMG_MULT = 0.67f
        const val ROTATION_SPEED = 2f
    }
}