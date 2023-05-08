//By Nicke535, a script that spawns clones of a ship which follows its traveled path after teleportation
package org.niatahl.tahlan.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.magiclib.plugins.MagicTrailPlugin;
import org.magiclib.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SlipDrive extends BaseShipSystemScript {

    public static Color AFTERIMAGE_COLOR = new Color(0.25f, 0.05f, 0.40f, 0.3f);
    public static float SHADOW_DELAY = 0.05f;
    public static float SHADOW_ANGLE_DIFFERENCE = 8f;
    public static float SHADOW_DISTANCE_DIFFERENCE = 45f;
    public static float SHADOW_FLICKER_DIFFERENCE = 8f;
    public static int SHADOW_FLICKER_CLONES = 4;

    private Vector2f shadowPos = new Vector2f(0f, 0f);
    private Vector2f startPos = new Vector2f(0f, 0f);

    private float shadowDelayCounter = 0f;
    private boolean runOnce = true;
    private float globalCounter = 0f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        //Don't run when paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //Ensures we have a ship
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        //Makes sure to store our position before the jump
        if (effectLevel > 0.5f && runOnce) {
            shadowPos.x = ship.getLocation().getX();
            shadowPos.y = ship.getLocation().getY();
            startPos.x = ship.getLocation().getX();
            startPos.y = ship.getLocation().getY();
            runOnce = false;
        }

        if (state == State.IN) {
            stats.getAcceleration().modifyFlat(id,500f);
            stats.getDeceleration().modifyFlat(id,500f);
            return;
        }

        //If we are in the main usage loop, start ticking our global counter
        if (effectLevel >= 1f) {
            globalCounter += Global.getCombatEngine().getElapsedInLastFrame();
        }

        //We use this instead of using effectLevel, since we need to run the script in the Active period, not the Charge-Down period
        float shadowProgress = globalCounter / ship.getSystem().getChargeActiveDur();

        //Once we are past the charge-up state, we start moving the shadow towards our ship, while keeping the actual ship invisible and phased
        //(also cuts our trails, since we are technically entering another dimension)
        MagicTrailPlugin.cutTrailsOnEntity(ship);
        ship.setPhased(true);
        if (player) {
            //We can make the ship slightly visible to the player, but that kinda breaks the immersion a bit
            //ship.setExtraAlphaMult(0.1f);
            ship.setExtraAlphaMult(0f);
            ship.setApplyExtraAlphaToEngines(true);
        } else {
            ship.setExtraAlphaMult(0f);
            ship.setApplyExtraAlphaToEngines(true);
        }

        //Moves the shadow to its appropriate location
        Vector2f tempMovementVector = Vector2f.sub(ship.getLocation(), startPos, new Vector2f(0f, 0f));
        tempMovementVector.x = tempMovementVector.x * shadowProgress + MathUtils.getRandomNumberInRange(-SHADOW_DISTANCE_DIFFERENCE, SHADOW_DISTANCE_DIFFERENCE);
        tempMovementVector.y = tempMovementVector.y * shadowProgress + MathUtils.getRandomNumberInRange(-SHADOW_DISTANCE_DIFFERENCE, SHADOW_DISTANCE_DIFFERENCE);
        shadowPos = Vector2f.add(startPos, tempMovementVector, new Vector2f(0f, 0f));

        //If enough time has passed, render a new shadow
        shadowDelayCounter += Global.getCombatEngine().getElapsedInLastFrame();
        if (shadowDelayCounter > SHADOW_DELAY) {
            float angleDifference = MathUtils.getRandomNumberInRange(-SHADOW_ANGLE_DIFFERENCE, SHADOW_ANGLE_DIFFERENCE) - 90f;

            for (int i = 0; i < SHADOW_FLICKER_CLONES; i++) {
                Vector2f modifiedShadowPos = new Vector2f(MathUtils.getRandomNumberInRange(-SHADOW_FLICKER_DIFFERENCE, SHADOW_FLICKER_DIFFERENCE), MathUtils.getRandomNumberInRange(-SHADOW_FLICKER_DIFFERENCE, SHADOW_FLICKER_DIFFERENCE));
                modifiedShadowPos.x += shadowPos.x;
                modifiedShadowPos.y += shadowPos.y;
                MagicRender.battlespace(Global.getSettings().getSprite("fx", "" + ship.getHullSpec().getBaseHullId() + "_phantom"), modifiedShadowPos, new Vector2f(0f, 0f),
                        new Vector2f(168f, 229f),
                        new Vector2f(0f, 0f), ship.getFacing() + angleDifference,
                        0f, AFTERIMAGE_COLOR, true, 0.1f, 0f, 0.3f);
            }

            shadowDelayCounter -= SHADOW_DELAY;
        }

        //Cuts our custom trails
        MagicTrailPlugin.cutTrailsOnEntity(ship);

        //Always render smoke at the shadow's position
        for (int i = 0; i < (500 * Global.getCombatEngine().getElapsedInLastFrame()); i++) {
            Global.getCombatEngine().addSmokeParticle(MathUtils.getRandomPointInCircle(shadowPos, SHADOW_DISTANCE_DIFFERENCE),MathUtils.getRandomPointInCircle(null, 10f),
                    MathUtils.getRandomNumberInRange(70f, 90f), 0.7f, MathUtils.getRandomNumberInRange(0.38f, 0.67f),new Color(0f, 0f, 0f));
        }
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        stats.getDeceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);

        if (ship.getSystem().getEffectLevel() <= 0f) {
            startPos = new Vector2f(0f, 0f);
            shadowPos = new Vector2f(0f, 0f);
            shadowDelayCounter = 0f;
            globalCounter = 0f;
            ship.setPhased(false);
            ship.setExtraAlphaMult(1f);
            runOnce = true;

            //Render a puff of smoke to hide that we just appear out of nowhere
            for (int i = 0; i < 100; i++) {
                Global.getCombatEngine().addSmokeParticle(
                        MathUtils.getRandomPointInCircle(ship.getLocation(), SHADOW_DISTANCE_DIFFERENCE * 2.8f), MathUtils.getRandomPointInCircle(null, 10f),
                        MathUtils.getRandomNumberInRange(50f, 90f), 0.7f, MathUtils.getRandomNumberInRange(0.1f, 0.78f), new Color(0f, 0f, 0f)
                );
            }
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("#%s$diving=%", false);
        }
        return null;
    }
}