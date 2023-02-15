package org.niatahl.tahlan.utils

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