package org.niatahl.tahlan.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.loading.VariantSource
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.lazywizard.lazylib.FastTrig
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.random.Random

/**
 * Toys Wisp forgot
 */

fun ClosedFloatingPointRange<Float>.random() =
    Random.nextDouble(this.start.toDouble(), this.endInclusive.toDouble()).toFloat()

fun Color.modify(red: Int = this.red, green: Int = this.green, blue: Int = this.blue, alpha: Int = this.alpha) =
    Color(red, green, blue, alpha)

fun Float.adjustToward(target: Float, byAmount: Float) =
    if (target > this) (this + byAmount).coerceAtMost(target) else (this - byAmount).coerceAtLeast(target)

fun Any.logger(): Logger {
    return Global.getLogger(this::class.java).apply { level = Level.ALL }
}

fun ShipVariantAPI.getRefitVariant(): ShipVariantAPI {
    var shipVariant = this
    if (shipVariant.isStockVariant || shipVariant.source != VariantSource.REFIT) {
        shipVariant = shipVariant.clone()
        shipVariant.originalVariant = null
        shipVariant.source = VariantSource.REFIT
    }
    return shipVariant
}

fun FleetMemberAPI.fixVariant() {
    val newVariant = this.variant.getRefitVariant()
    if (newVariant != this.variant) {
        this.setVariant(newVariant, false, false)
    }

    newVariant.fixModuleVariants()
}

fun ShipAPI.getSpriteOffset(): Vector2f {
    val sprite = this.spriteAPI
    val spriteX = sprite.width / 2 - sprite.centerX
    val spriteY = sprite.height / 2 - sprite.centerY
    val offset = Vector2f()
    offset.x = FastTrig.cos(Math.toRadians((this.facing - 90f).toDouble())).toFloat() * spriteX - FastTrig.sin(Math.toRadians((this.facing - 90f).toDouble())).toFloat() * spriteY
    offset.y = FastTrig.sin(Math.toRadians((this.facing - 90f).toDouble())).toFloat() * spriteX + FastTrig.cos(Math.toRadians((this.facing - 90f).toDouble())).toFloat() * spriteY
    return offset
}

fun ShipVariantAPI.fixModuleVariants() {
    this.stationModules.forEach { (slotId, _) ->
        val moduleVariant = this.getModuleVariant(slotId)
        val newModuleVariant = moduleVariant.getRefitVariant()
        if (newModuleVariant != moduleVariant) {
            this.setModuleVariant(slotId, newModuleVariant)
        }

        newModuleVariant.fixModuleVariants()
    }
}