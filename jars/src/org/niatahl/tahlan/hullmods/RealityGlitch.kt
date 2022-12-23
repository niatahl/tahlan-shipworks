package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.utils.TahlanPeople
import org.niatahl.tahlan.utils.TahlanPeople.CHILD
import java.awt.Color

/**
 * Causes a ship to randomly "glitch" out of reality
 *
 * @author Nicke535
 */
class RealityGlitch : BaseHullMod() {
    //Handles all in-combat effects
    override fun advanceInCombat(ship: ShipAPI, amount: Float) {
        //Nothing should happen if we are paused, or our ship is destroyed
        if (Global.getCombatEngine().isPaused || !ship.isAlive) {
            return
        }

        //Gets the custom data for our specific ship
        var data = Global.getCombatEngine().customData["SPECIAL_REALITY_GLITCH_DATA_KEY" + ship.id] as ShipSpecificData?
        if (data == null) {
            data = ShipSpecificData()
        }

        //Checks our current armor and hull level
        val thisFrameArmor = getTotalArmor(ship)
        val thisFrameHull = ship.hitpoints
        if (!ship.system.isActive && !ship.fluxTracker.isOverloadedOrVenting) {

            //Tick down cooldown
            data.glitchCooldown -= amount

            //Don't check for activation if the system is on cooldown   lastFrameDestroyedGridPieces
            if (data.glitchCooldown <= 0f) {
                ship.isJitterShields = false
                ship.setJitterUnder(ship, SHIMMER_COLOR, 0.5f, 20, 1f, 5f)

                //If the armor and hull loss is big enough, or we lost a new armor grid this frame, activate a new glitch
                var shouldActivate = data.lastFrameArmor - thisFrameArmor + (data.lastFrameHull - thisFrameHull) > DAMAGE_ACTIVATION_THRESHHOLD

                //Armor grid check
                val maxX = ship.armorGrid.leftOf + ship.armorGrid.rightOf
                val maxY = ship.armorGrid.above + ship.armorGrid.below
                for (ix in 0 until maxX) {
                    for (iy in 0 until maxY) {
                        if (ship.armorGrid.getArmorFraction(ix, iy) > 0f) {
                            data.lastFrameDestroyedGridPieces.remove(ix + iy * maxX)
                        } else {
                            //If the grid piece wasn't destroyed last frame, it was lost this frame
                            if (!data.lastFrameDestroyedGridPieces.contains(ix + iy * maxX)) {
                                shouldActivate = true
                                data.lastFrameDestroyedGridPieces.add(ix + iy * maxX)
                            }
                        }
                    }
                }
                if (shouldActivate) {
                    data.hasExitedGlitch = false
                    val disappearTime = MathUtils.getRandomNumberInRange(MIN_DISAPPEAR_TIME, MAX_DISAPPEAR_TIME)
                    data.glitchCooldown = MathUtils.getRandomNumberInRange(MIN_DISAPPEAR_COOLDOWN, MAX_DISAPPEAR_COOLDOWN) + disappearTime
                    data.glitchDurationRemaining = disappearTime
                    if (SOUND_PITCH_ADJUSTMENT) {
                        Global.getSoundPlayer().playSound(GLITCH_SOUND, MIN_DISAPPEAR_TIME / disappearTime, 1f, ship.location, Vector2f(0f, 0f))
                    } else {
                        Global.getSoundPlayer().playSound(GLITCH_SOUND, 1f, 1f, ship.location, Vector2f(0f, 0f))
                    }
                }
            }

            //If we're currently in a glitch period, phase us out and affect opacity
            if (data.glitchDurationRemaining > 0f) {
                ship.isPhased = true
                ship.collisionClass = CollisionClass.NONE
                ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM)
                ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX)
                if (data.glitchDurationRemaining > OPACITY_FADE_TIME) {
                    ship.extraAlphaMult = GLITCH_OPACITY
                    ship.setApplyExtraAlphaToEngines(true)
                    ship.setJitter(ship, FLICKER_COLOR, 0.7f, 10, 25f, 50f)
                } else {
                    ship.extraAlphaMult = Misc.interpolate(GLITCH_OPACITY, 1f, data.glitchDurationRemaining / OPACITY_FADE_TIME)
                }
                data.glitchDurationRemaining -= amount
            } else {
                ship.isPhased = false
                ship.collisionClass = CollisionClass.SHIP
                ship.extraAlphaMult = 1f
                //Regen armor if we haven't yet
                if (!data.hasExitedGlitch) {
                    data.hasExitedGlitch = true
                    regenArmor(ship)
                }
            }
        }

        //Finally, write the custom data back to the engine, and update last-frame variables
        data.lastFrameArmor = thisFrameArmor
        data.lastFrameHull = thisFrameHull
        Global.getCombatEngine().customData["SPECIAL_REALITY_GLITCH_DATA_KEY" + ship.id] = data
    }

    override fun advanceInCampaign(member: FleetMemberAPI, amount: Float) {
        if (member.captain == null || member.captain.isDefault) {
            member.captain = TahlanPeople.getPerson(CHILD)
        }
    }

    //Handles applicability
    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        return false
    }

    //Calculates total armor of a ship
    private fun getTotalArmor(ship: ShipAPI): Float {
        val maxX = ship.armorGrid.leftOf + ship.armorGrid.rightOf
        val maxY = ship.armorGrid.above + ship.armorGrid.below
        var armor = 0f
        for (ix in 0 until maxX) {
            for (iy in 0 until maxY) {
                armor += ship.armorGrid.getArmorValue(ix, iy)
            }
        }
        return armor
    }

    //Handles regenerating armor of the ship
    private fun regenArmor(ship: ShipAPI) {
        //First, calculates average armor
        val maxX = ship.armorGrid.leftOf + ship.armorGrid.rightOf
        val maxY = ship.armorGrid.above + ship.armorGrid.below
        var averageArmor = getTotalArmor(ship) / (maxX * maxY).toFloat()
        if (averageArmor < ship.armorGrid.maxArmorInCell * 0.4f) {
            averageArmor = ship.armorGrid.maxArmorInCell * 0.4f
        }

        //Then we check all armor grid pieces again to set them to the average
        for (ix in 0 until maxX) {
            for (iy in 0 until maxY) {
                //if (ship.getArmorGrid().getArmorValue(ix, iy) < averageArmor) {
                ship.armorGrid.setArmorValue(ix, iy, averageArmor)
                //}
            }
        }
    }

    /**
     * Class for managing the data we need to track on a per-ship basis
     */
    private inner class ShipSpecificData {
        var glitchCooldown = 0f
        var glitchDurationRemaining = 0f
        var hasExitedGlitch = true
        var lastFrameArmor = 0f
        var lastFrameHull = 0f
        val lastFrameDestroyedGridPieces = HashSet<Int>()
    }

    companion object {
        // Threshold of damage to trigger a glitch
        // Both armor and hull damage counts
        private const val DAMAGE_ACTIVATION_THRESHHOLD = 500f

        // Disappearance cooldown, minimum and maximum
        // DOES NOT include the disappearance itself
        private const val MAX_DISAPPEAR_COOLDOWN = 30f
        private const val MIN_DISAPPEAR_COOLDOWN = 10f

        // Time to disappear, minimum and maximum
        private const val MAX_DISAPPEAR_TIME = 2.5f
        private const val MIN_DISAPPEAR_TIME = 0.5f

        // Alpha when "glitched out"
        private const val GLITCH_OPACITY = 0.3f

        // Duration to smoothly fade-in the ship after a glitch is over, in seconds
        private const val OPACITY_FADE_TIME = 0.2f

        // Sound to play when "glitching out"
        private const val GLITCH_SOUND = "system_phase_cloak_activate"

        // Whether the glitch sound should scale in pitch to match the disappearance time
        private const val SOUND_PITCH_ADJUSTMENT = true
        private val FLICKER_COLOR = Color(113, 129, 97, 131)
        private val SHIMMER_COLOR = Color(146, 226, 50, 57)
    }
}