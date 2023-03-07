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
import org.niatahl.tahlan.utils.TahlanIDs.DAEMONIC_HEART
import java.awt.Color
import java.io.IOException
import kotlin.collections.ArrayList
import kotlin.math.ceil

class HelAltarEffectScript : EveryFrameWeaponEffectPlugin {
    private var loaded = false
    private var rotation = 0f
    private var sprite: SpriteAPI? = null
    private val targetList = ArrayList<ShipAPI>()
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        val ship = weapon.ship ?: return
        if (!ship.isAlive || ship.isHulk || ship.isPiece) {
            return
        }

        //Glows off in refit screen
        if (ship.originalOwner == -1) {
            return
        }
        if (sprite == null) {
            // Load sprite if it hasn't been loaded yet - not needed if you add it to settings.json
            if (!loaded) {
                try {
                    Global.getSettings().loadTexture(SPRITE_PATH)
                } catch (ex: IOException) {
                    throw RuntimeException("Failed to load sprite '" + SPRITE_PATH + "'!", ex)
                }
                loaded = true
            }
            sprite = Global.getSettings().getSprite(SPRITE_PATH)
        }

        val loc = ship.location
        val view = Global.getCombatEngine().viewport
        if (view.isNearViewport(loc, EFFECT_RANGE)) {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT)
            GL11.glMatrixMode(GL11.GL_PROJECTION)
            GL11.glPushMatrix()
            GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight())
            GL11.glOrtho(0.0, Display.getWidth().toDouble(), 0.0, Display.getHeight().toDouble(), -1.0, 1.0)
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glEnable(GL11.GL_BLEND)
            val scale = Global.getSettings().screenScaleMult
            val radius = EFFECT_RANGE * 2f * scale / view.viewMult
            sprite!!.setSize(radius, radius)
            sprite!!.color = COLOR
            sprite!!.alphaMult = (0.25f / ceil(ship.mutableStats.timeMult.modifiedValue))
            sprite!!.renderAtCenter(
                view.convertWorldXtoScreenX(loc.x) * scale,
                view.convertWorldYtoScreenY(loc.y) * scale
            )
            sprite!!.angle = rotation
            GL11.glPopMatrix()
            GL11.glPopAttrib()
        }

        // Spin it
        rotation += ROTATION_SPEED * amount
        if (rotation > 360f) {
            rotation -= 360f
        }
        for (target in CombatUtils.getShipsWithinRange(ship.location, EFFECT_RANGE)) {
            if (target.owner == ship.owner) {
                if (target.owner == ship.owner && !targetList.contains(target) && target.variant.hullMods.contains(DAEMONIC_HEART)) {
                    targetList.add(target)
                }
            }
        }
        val purgeList = ArrayList<ShipAPI>()
        for (target in targetList) {
            if (MathUtils.getDistance(target.location, ship.location) <= EFFECT_RANGE) {
                target.mutableStats.apply {
                    shieldDamageTakenMult.modifyMult(ALTAR_ID, DAMAGE_MULT)
                    armorDamageTakenMult.modifyMult(ALTAR_ID, DAMAGE_MULT)
                    hullDamageTakenMult.modifyMult(ALTAR_ID, DAMAGE_MULT)
                    damageToMissiles.modifyMult(ALTAR_ID, PDDMG_MULT)
                    damageToFighters.modifyMult(ALTAR_ID, PDDMG_MULT)
                }
            } else {
                target.mutableStats.apply {
                    shieldDamageTakenMult.unmodify(ALTAR_ID)
                    armorDamageTakenMult.unmodify(ALTAR_ID)
                    hullDamageTakenMult.unmodify(ALTAR_ID)
                    damageToMissiles.unmodify(ALTAR_ID)
                    damageToFighters.unmodify(ALTAR_ID)
                }
                purgeList.add(target)
            }
        }
        for (purge in purgeList) {
            targetList.remove(purge)
        }
    }

    companion object {
        private const val ALTAR_ID = "HelAltar_ID"
        const val EFFECT_RANGE = 2000f
        const val DAMAGE_MULT = 0.9f
        const val PDDMG_MULT = 1.5f

        // sprite path - necessary if loaded here and not in settings.json
        const val SPRITE_PATH = "graphics/tahlan/fx/tahlan_tempshield_ring_b.png"
        val COLOR = Color(186, 47, 52)
        const val ROTATION_SPEED = 20f
    }
}