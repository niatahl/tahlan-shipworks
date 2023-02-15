package org.niatahl.tahlan.utils

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.events.CampaignEventTarget
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Misc.FleetFilter
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*

/**
 * Original by Wisp
 * Additions by Nia Tahl
 */

fun ClosedFloatingPointRange<Float>.random(): Float =
    (this.start + (this.endInclusive - this.start) * kotlin.random.Random.nextFloat())

fun Color.modify(red: Int = this.red, green: Int = this.green, blue: Int = this.blue, alpha: Int = this.alpha) =
    Color(red, green, blue, alpha)

fun Float.adjustToward(target: Float, byAmount: Float) =
    if (target > this) (this + byAmount).coerceAtMost(target) else (this - byAmount).coerceAtLeast(target)