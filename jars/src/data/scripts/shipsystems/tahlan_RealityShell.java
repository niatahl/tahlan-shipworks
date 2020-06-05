package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.tahlan_ModPlugin;
import data.scripts.util.MagicRender;
import org.dark.shaders.post.PostProcessShader;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Arrays;

/**
 * First comes the time-mult, then comes the reversal.
 *
 * @author Nicke535
 */
public class tahlan_RealityShell extends BaseShipSystemScript {
    private static final float CRITICAL_HULL_LEVEL = 0.25f;
    private static final float EMERGENCY_REVERT_OVERLOAD_TIME = 5f;

    //The maximum time mult for the system
    private static final float MAX_TIME_MULT = 5f;

    private static final Color FLICKER_COLOR = new Color(129, 110, 99, 101);
    private static final Color AFTERIMAGE_COLOR = new Color(118, 129, 97, 41);

    private IntervalUtil interval = new IntervalUtil(0.1f, 0.1f);
    private boolean hasResetPostProcess = true;

    //Internal variables
    private TimePointData reversalPoint = null;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        //If we are a wreck, we don't run any shipsystem stuff
        if (ship.isHulk() || ship.isPiece()) {
            return;
        }

        //We don't really care about anything as long as the game is paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //If the system is not active, reset variables and jump back in time if we haven't
        if (effectLevel <= 0f) {
            if (reversalPoint != null) {
                jumpBack(ship);
                reversalPoint = null;
            }
        }

        //If the system IS active, we start applying the active effects of the system
        else if (effectLevel > 0f) {
            //If we don't have a reversal point yet (and we aren't overloading!), store where we're going to reverse
            if (reversalPoint == null && !ship.getFluxTracker().isOverloadedOrVenting()) {
                reversalPoint = new TimePointData(ship);
            }

            //Modify the time mult of the ship
            float timeMult = 1f + (effectLevel * (MAX_TIME_MULT - 1f));
            stats.getTimeMult().modifyMult(id, timeMult);

            //Also modify the engine speed if we're the player ship
            float engineTimeMult = 1f + (effectLevel * (MAX_TIME_MULT - 1f) *0.5f);
            if (Global.getCombatEngine().getPlayerShip() == ship) {
                Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / engineTimeMult);
            }

            interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
            if (reversalPoint != null && interval.intervalElapsed()) {

                // Sprite offset fuckery - Don't you love trigonometry?
                SpriteAPI sprite = ship.getSpriteAPI();
                float offsetX = sprite.getWidth()/2 - sprite.getCenterX();
                float offsetY = sprite.getHeight()/2 - sprite.getCenterY();

                float trueOffsetX = (float)FastTrig.cos(Math.toRadians(ship.getFacing()-90f))*offsetX - (float)FastTrig.sin(Math.toRadians(ship.getFacing()-90f))*offsetY;
                float trueOffsetY = (float)FastTrig.sin(Math.toRadians(ship.getFacing()-90f))*offsetX + (float)FastTrig.cos(Math.toRadians(ship.getFacing()-90f))*offsetY;

                MagicRender.battlespace(
                        Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                        new Vector2f(reversalPoint.position.getX()+trueOffsetX+MathUtils.getRandomNumberInRange(-15f,15f),reversalPoint.position.getY()+trueOffsetY+MathUtils.getRandomNumberInRange(-15f,15f)),
                        new Vector2f(0, 0),
                        new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),
                        new Vector2f(0, 0),
                        reversalPoint.angle-90f,
                        0f,
                        AFTERIMAGE_COLOR,
                        true,
                        0.05f,
                        0.1f,
                        0.05f,
                        CombatEngineLayers.BELOW_SHIPS_LAYER);
            }

            ship.setJitter(ship,FLICKER_COLOR,0.5f,5,5f,20f);

            //If we have the system active, but suffer critical damage/an overload, we reverse back in time immediately and overload
            if (reversalPoint != null && ship.getHullLevel() < CRITICAL_HULL_LEVEL || ship.getFluxTracker().isOverloaded()) {
                ship.getSystem().deactivate();
                ship.getFluxTracker().forceOverload(EMERGENCY_REVERT_OVERLOAD_TIME);
                jumpBack(ship);
                reversalPoint = null;
            }
        }

        //Fancy postprocessing effects
        if (tahlan_ModPlugin.isGraphicsLibAvailable()) {
            handlePostprocessing(effectLevel,Global.getCombatEngine().getPlayerShip()==ship);
        }

        //Always unapply the global time-mult if we are not the player ship, or our shipsystem isn't on
        if (ship != Global.getCombatEngine().getPlayerShip() || (!state.equals(State.IN) && !state.equals(State.ACTIVE))) {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }
    }

    private void jumpBack(ShipAPI ship) {
        //If we somehow called this with an empty reverse position, just return
        if (reversalPoint == null) {
            return;
        }

        //Updates the ship to match up to the time point we're jumping to
        ship.setHitpoints(reversalPoint.hitPoints);
        ship.getLocation().x = reversalPoint.position.x;
        ship.getLocation().y = reversalPoint.position.y;
        ship.getVelocity().x = reversalPoint.velocity.x;
        ship.getVelocity().y = reversalPoint.velocity.y;
        ship.setFacing(reversalPoint.angle);
        ship.setAngularVelocity(reversalPoint.angularVelocity);
        ship.getFluxTracker().setHardFlux(reversalPoint.hardFlux);
        ship.getFluxTracker().setCurrFlux(reversalPoint.softFlux + reversalPoint.hardFlux);

        //Armor has to be done iteratively
        for (int ix = 0; ix < (ship.getArmorGrid().getLeftOf() + ship.getArmorGrid().getRightOf()); ix++) {
            for (int iy = 0; iy < (ship.getArmorGrid().getBelow() + ship.getArmorGrid().getAbove()); iy++) {
                ship.getArmorGrid().setArmorValue(ix, iy, reversalPoint.armor[ix][iy]);
            }
        }
    }


    //Unapply never gets called in this script
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            id = id + "_" + ship.getId();
        } else {
            return;
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0 && effectLevel > 0f) {
            return new StatusData("Something's happening, I can feel it...", false);
        }
        return null;
    }


    //Stores the state of a ship at a certain point in time; stores hullpoints, armor, flux, position, angle, velocity and angular velocity
    private static class TimePointData {
        public final float hitPoints;
        public final float[][] armor;
        public final float softFlux;
        public final float hardFlux;
        public final Vector2f position;
        public final float angle;
        public final Vector2f velocity;
        public final float angularVelocity;

        private TimePointData(final ShipAPI ship) {
            hitPoints = ship.getHitpoints();
            armor = deepCopy(ship.getArmorGrid().getGrid());
            softFlux = (ship.getFluxTracker().getCurrFlux() - ship.getFluxTracker().getHardFlux());
            hardFlux = ship.getFluxTracker().getHardFlux();
            position = new Vector2f(ship.getLocation());
            angle = ship.getFacing();
            velocity = new Vector2f(ship.getVelocity());
            angularVelocity = ship.getAngularVelocity();
        }

        //By Rorick from StackOverflow; deep-copies a 2D array
        private static float[][] deepCopy(float[][] original) {
            if (original == null) {
                return null;
            }

            final float[][] result = new float[original.length][];
            for (int i = 0; i < original.length; i++) {
                result[i] = Arrays.copyOf(original[i], original[i].length);
            }
            return result;
        }
    }

    //Handles enabling/disabling postprocess effects, depending on if we're the player and the state of our effectlevel
    private void handlePostprocessing(float effectLevel, boolean player) {
        if (effectLevel > 0f && player) {
            hasResetPostProcess = false;
            PostProcessShader.setContrast(false, 1f+(0.2f*effectLevel));
            PostProcessShader.setSaturation(false, 1f-(0.25f*effectLevel));
            PostProcessShader.setNoise(false, 0.2f * effectLevel);
        } else if (!hasResetPostProcess) {
            PostProcessShader.resetDefaults();
            hasResetPostProcess = true;
        }
    }
}