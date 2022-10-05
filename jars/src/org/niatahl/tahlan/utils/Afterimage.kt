package org.niatahl.tahlan.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.ShipAPI
import data.scripts.util.MagicRender
import org.lazywizard.lazylib.FastTrig
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

object Afterimage {
    @JvmStatic
    fun renderCustomAfterimage(ship: ShipAPI, color: Color, duration: Float) {

        // Sprite offset fuckery - Don't you love trigonometry?
        val sprite = ship.spriteAPI
        val offsetX = sprite.width / 2 - sprite.centerX
        val offsetY = sprite.height / 2 - sprite.centerY
        val trueOffsetX =
            FastTrig.cos(Math.toRadians((ship.facing - 90f).toDouble())).toFloat() * offsetX - FastTrig.sin(
                Math.toRadians((ship.facing - 90f).toDouble())
            ).toFloat() * offsetY
        val trueOffsetY =
            FastTrig.sin(Math.toRadians((ship.facing - 90f).toDouble())).toFloat() * offsetX + FastTrig.cos(
                Math.toRadians((ship.facing - 90f).toDouble())
            ).toFloat() * offsetY
        MagicRender.battlespace(
            Global.getSettings().getSprite(ship.hullSpec.spriteName),
            Vector2f(ship.location.getX() + trueOffsetX, ship.location.getY() + trueOffsetY),
            Vector2f(0f, 0f),
            Vector2f(ship.spriteAPI.width, ship.spriteAPI.height),
            Vector2f(0f, 0f),
            ship.facing - 90f,
            0f,
            color,
            true,
            0f,
            0f,
            0f,
            0f,
            0f,
            0.1f,
            0.1f,
            duration,
            CombatEngineLayers.BELOW_SHIPS_LAYER
        )
    }
}