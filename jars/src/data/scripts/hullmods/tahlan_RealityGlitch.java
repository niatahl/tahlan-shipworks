package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashSet;

/**
 * Causes a ship to randomly "glitch" out of reality
 *
 * @author Nicke535
 */
public class tahlan_RealityGlitch extends BaseHullMod {
    //Threshold of damage to trigger a glitch
    //  Both armor and hull damage counts
    private static final float DAMAGE_ACTIVATION_THRESHHOLD = 500f;

    //Disappearance cooldown, minimum and maximum
    //  DOES NOT include the disappearance itself
    private static final float MAX_DISAPPEAR_COOLDOWN = 20f;
    private static final float MIN_DISAPPEAR_COOLDOWN = 10f;

    //Time to disappear, minimum and maximum
    private static final float MAX_DISAPPEAR_TIME = 2.5f;
    private static final float MIN_DISAPPEAR_TIME = 0.5f;

    //Alpha when "glitched out"
    private static final float GLITCH_OPACITY = 0.3f;

    //Duration to smoothly fade-in the ship after a glitch is over, in seconds
    private static final float OPACITY_FADE_TIME = 0.2f;

    //Sound to play when "glitching out"
    private static final String GLITCH_SOUND = "system_phase_cloak_activate";

    //Whether the glitch sound should scale in pitch to match the disappearance time
    private static final boolean SOUND_PITCH_ADJUSTMENT = true;

    private static final Color FLICKER_COLOR = new Color(113, 129, 97, 131);
    private static final Color SHIMMER_COLOR = new Color(146, 226, 50, 57);


    //Handles all in-combat effects
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Nothing should happen if we are paused, or our ship is destroyed
        if (Global.getCombatEngine().isPaused() || !ship.isAlive()) {
            return;
        }

        //Gets the custom data for our specific ship
        ShipSpecificData data = (ShipSpecificData) Global.getCombatEngine().getCustomData().get("SPECIAL_REALITY_GLITCH_DATA_KEY" + ship.getId());
        if (data == null) {
            data = new ShipSpecificData();
        }


        //Checks our current armor and hull level
        float thisFrameArmor = getTotalArmor(ship);
        float thisFrameHull = ship.getHitpoints();


        if (!ship.getSystem().isActive() && !ship.getFluxTracker().isOverloadedOrVenting()) {

            //Tick down cooldown
            data.glitchCooldown -= amount;


            //Don't check for activation if the system is on cooldown   lastFrameDestroyedGridPieces
            if (data.glitchCooldown <= 0f) {
                ship.setJitterShields(false);
                ship.setJitterUnder(ship, SHIMMER_COLOR, 0.5f, 20, 1f, 5f);

                //If the armor and hull loss is big enough, or we lost a new armor grid this frame, activate a new glitch
                boolean shouldActivate = (data.lastFrameArmor - thisFrameArmor) + (data.lastFrameHull - thisFrameHull) > DAMAGE_ACTIVATION_THRESHHOLD;

                //Armor grid check
                int maxX = ship.getArmorGrid().getLeftOf() + ship.getArmorGrid().getRightOf();
                int maxY = ship.getArmorGrid().getAbove() + ship.getArmorGrid().getBelow();
                for (int ix = 0; ix < maxX; ix++) {
                    for (int iy = 0; iy < maxY; iy++) {
                        if (ship.getArmorGrid().getArmorFraction(ix, iy) > 0f) {
                            data.lastFrameDestroyedGridPieces.remove(ix + (iy * maxX));
                        } else {
                            //If the grid piece wasn't destroyed last frame, it was lost this frame
                            if (!data.lastFrameDestroyedGridPieces.contains(ix + (iy * maxX))) {
                                shouldActivate = true;
                                data.lastFrameDestroyedGridPieces.add(ix + (iy * maxX));
                            }
                        }
                    }
                }

                if (shouldActivate) {
                    data.hasExitedGlitch = false;
                    float disappearTime = MathUtils.getRandomNumberInRange(MIN_DISAPPEAR_TIME, MAX_DISAPPEAR_TIME);
                    data.glitchCooldown = MathUtils.getRandomNumberInRange(MIN_DISAPPEAR_COOLDOWN, MAX_DISAPPEAR_COOLDOWN) + disappearTime;
                    data.glitchDurationRemaining = disappearTime;
                    if (SOUND_PITCH_ADJUSTMENT) {
                        Global.getSoundPlayer().playSound(GLITCH_SOUND, MIN_DISAPPEAR_TIME / disappearTime, 1f, ship.getLocation(), new Vector2f(0f, 0f));
                    } else {
                        Global.getSoundPlayer().playSound(GLITCH_SOUND, 1f, 1f, ship.getLocation(), new Vector2f(0f, 0f));
                    }
                }
            }

            //If we're currently in a glitch period, phase us out and affect opacity
            if (data.glitchDurationRemaining > 0f) {
                ship.setPhased(true);
                ship.setCollisionClass(CollisionClass.NONE);
                ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
                if (data.glitchDurationRemaining > OPACITY_FADE_TIME) {
                    ship.setExtraAlphaMult(GLITCH_OPACITY);
                    ship.setApplyExtraAlphaToEngines(true);

                    ship.setJitter(ship, FLICKER_COLOR, 0.7f, 10, 25f, 50f);
                } else {
                    ship.setExtraAlphaMult(Misc.interpolate(GLITCH_OPACITY, 1f, data.glitchDurationRemaining / OPACITY_FADE_TIME));
                }

                data.glitchDurationRemaining -= amount;
            } else {
                ship.setPhased(false);
                ship.setCollisionClass(CollisionClass.SHIP);
                ship.setExtraAlphaMult(1f);
                //Regen armor if we haven't yet
                if (!data.hasExitedGlitch) {
                    data.hasExitedGlitch = true;
                    regenArmor(ship);
                }
            }

        }

        //Finally, write the custom data back to the engine, and update last-frame variables
        data.lastFrameArmor = thisFrameArmor;
        data.lastFrameHull = thisFrameHull;
        Global.getCombatEngine().getCustomData().put("SPECIAL_REALITY_GLITCH_DATA_KEY" + ship.getId(), data);
    }


    //Handles applicability
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    //Calculates total armor of a ship
    private float getTotalArmor(ShipAPI ship) {
        int maxX = ship.getArmorGrid().getLeftOf() + ship.getArmorGrid().getRightOf();
        int maxY = ship.getArmorGrid().getAbove() + ship.getArmorGrid().getBelow();
        float armor = 0f;
        for (int ix = 0; ix < maxX; ix++) {
            for (int iy = 0; iy < maxY; iy++) {
                armor += ship.getArmorGrid().getArmorValue(ix, iy);
            }
        }
        return armor;
    }

    //Handles regenerating armor of the ship
    private void regenArmor(ShipAPI ship) {
        //First, calculates average armor
        int maxX = ship.getArmorGrid().getLeftOf() + ship.getArmorGrid().getRightOf();
        int maxY = ship.getArmorGrid().getAbove() + ship.getArmorGrid().getBelow();
        float averageArmor = getTotalArmor(ship) / (float)(maxX*maxY);

        if (averageArmor < ship.getArmorGrid().getMaxArmorInCell() * 0.5f) {
            averageArmor = ship.getArmorGrid().getMaxArmorInCell() * 0.5f;
        }

        //Then we check all armor grid pieces again to set them to the average
        for (int ix = 0; ix < maxX; ix++) {
            for (int iy = 0; iy < maxY; iy++) {
                //if (ship.getArmorGrid().getArmorValue(ix, iy) < averageArmor) {
                ship.getArmorGrid().setArmorValue(ix, iy, averageArmor);
                //}
            }
        }
    }

    /**
     * Class for managing the data we need to track on a per-ship basis
     */
    private class ShipSpecificData {
        private float glitchCooldown = 0f;
        private float glitchDurationRemaining = 0f;
        private boolean hasExitedGlitch = true;
        private float lastFrameArmor = 0f;
        private float lastFrameHull = 0f;
        private HashSet<Integer> lastFrameDestroyedGridPieces = new HashSet<>();
    }
}