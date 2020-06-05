package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class tahlan_FilamentRingScript implements EveryFrameWeaponEffectPlugin {
    private static final float[] COLOR_NORMAL = {78f / 255f, 238f / 255f, 255f / 255f};

    private static final float TIMER_MULT = 0.5f;
    private static final float RECHARGE_TIME = 0.5f;
    private static final float OVERLOAD_FADE_TIME = 0.25f;
    private static final float HULK_FADE_TIME = 40f;

    private float timer = 0f;
    private float currentBrightness = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        if (ship == null) {
            return;
        }

        if (engine == null || !engine.isEntityInPlay(ship) || ship.isPiece()) {       //Refit screen! Just use frame 1 instead of our "proper" frame
            weapon.getAnimation().setFrame(0);
            return;
        } else if (ship.isPiece()) {                                //First: are we a piece? If so, instantly lose all opacity
            currentBrightness = 0f;
        } else if (ship.isHulk()) {                                 //Second: are we a hulk? In that case, slowly fade out our color
            currentBrightness -= amount * (1f / HULK_FADE_TIME);
        } else if (ship.getFluxTracker().isOverloadedOrVenting()) { //Third: are we overloading or venting? Then fade out, but pretty fast
            currentBrightness -= amount * (1f / OVERLOAD_FADE_TIME);
        } else {                                                    //If none of the above are correct, we are recharging our lights. Increase the color
            currentBrightness += amount * (1f / RECHARGE_TIME);
        }

        ShipSystemAPI system = ship.getSystem();

        //Keeps track of our timer for blinking
        timer += amount * TIMER_MULT;

        //Sets our current maximum brightness
        float currentMaxBrightness = 0.2f;
        if (system.isActive() || ship.getFluxTracker().isEngineBoostActive() || ship.getTravelDrive().isActive()) {
            currentMaxBrightness += 0.1f;
        }
        if (ship.getEngineController().isFlamedOut()) {
            currentMaxBrightness -= 0.1f;
        }
        //We glow when the system or overdrive is active
        if (ship.getSystem().isActive()) {
            currentMaxBrightness += 0.2f;
        }




        //Adds a clock-like effect to our maximum brightness
        currentMaxBrightness += 0.3f * (float)Math.pow(Math.sin(timer), 2f);

        MathUtils.clamp(currentMaxBrightness,0f,1f);

        //If our color is above the maximum, set it to the maximum. If it's less than 0, set it to 0
        currentBrightness = MathUtils.clamp(currentBrightness, 0f, currentMaxBrightness);

        //Then, actually set the proper opacity that we determined earlier
        weapon.getSprite().setAlphaMult(currentBrightness);



        //Now, set the color to the one we want, and include opacity
        Color colorToUse = new Color(COLOR_NORMAL[0], COLOR_NORMAL[1], COLOR_NORMAL[2], currentBrightness);


        //Switches to the proper sprite
        if (currentBrightness > 0) {
            weapon.getAnimation().setFrame(1);
        } else {
            weapon.getAnimation().setFrame(0);
        }

        //And finally actually apply the color
        weapon.getSprite().setColor(colorToUse);

    }
}