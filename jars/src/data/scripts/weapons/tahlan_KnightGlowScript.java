package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicRender;
import data.scripts.utils.tahlan_Utils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class tahlan_KnightGlowScript implements EveryFrameWeaponEffectPlugin {
    private static final float[] COLOR_NORMAL = {255f/255f, 140f/255f, 80f/255f};
    private static final float[] COLOR_OVERDRIVE = {255f/255f, 100f/255f, 40f/255f};
    private static final float[] COLOR_SYSTEM = {60f/255f, 255f/255f, 245f/255f};
    private static final float MAX_JITTER_DISTANCE = 0.8f;
    private static final float MAX_OPACITY = 1f;
    private static final float TRIGGER_PERCENTAGE = 0.3f;
    private static final float FADE_RATE = 2f;

    private float overdriveLevel = 0f;
    private float prevBrightness = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        if (ship == null) {
            return;
        }

        //Brightness based on flux under normal conditions
        float targetBrightness = ship.getFluxTracker().getFluxLevel() *  0.8f;

        //If we are in overdrive, we glow even more
        if (ship.getVariant().hasHullMod("tahlan_knightrefit") && (ship.getHitpoints() <= ship.getMaxHitpoints()*TRIGGER_PERCENTAGE || ship.getVariant().hasHullMod("tahlan_forcedoverdrive"))) {
            targetBrightness = 1f;
        } else if (ship.getSystem().isActive()){
            targetBrightness = Math.max(targetBrightness, ship.getSystem().getEffectLevel());
        }

        //Fading the brightness levels
        float currentBrightness;
        if (targetBrightness > prevBrightness) {
            currentBrightness = Math.min(prevBrightness + FADE_RATE*amount,targetBrightness);
        } else {
            currentBrightness = Math.max(prevBrightness - FADE_RATE*amount,targetBrightness);
        }
        prevBrightness = currentBrightness;

        //No glows on wrecks or in refit
        if ( ship.isPiece() || !ship.isAlive() || ship.getOriginalOwner() == -1) {
            currentBrightness = 0f;
        }

        //Switches to the proper sprite
        if (currentBrightness > 0) {
            weapon.getAnimation().setFrame(1);
        } else {
            weapon.getAnimation().setFrame(0);
        }

        //Brightness clamp, cause there's some weird cases with flux level > 1f, I guess
        currentBrightness = Math.max(0f,Math.min(currentBrightness,1f));

        //Now, set the color to the one we want, and include opacity
        Color colorToUse = new Color(COLOR_NORMAL[0], COLOR_NORMAL[1], COLOR_NORMAL[2], currentBrightness*MAX_OPACITY);

        //Change color if in overdrive
        if (targetBrightness > 0.8) {
            overdriveLevel = Math.min(1f,overdriveLevel+FADE_RATE*amount);
        } else {
            overdriveLevel = Math.max(0f,overdriveLevel-FADE_RATE*amount);
        }

        colorToUse = new Color(
                tahlan_Utils.lerp(COLOR_NORMAL[0],COLOR_OVERDRIVE[0],overdriveLevel),
                tahlan_Utils.lerp(COLOR_NORMAL[1],COLOR_OVERDRIVE[1],overdriveLevel),
                tahlan_Utils.lerp(COLOR_NORMAL[2],COLOR_OVERDRIVE[2],overdriveLevel),
                currentBrightness*MAX_OPACITY);


        float systemLevel = ship.getSystem().getEffectLevel();
        //Change color again if system is active and set brightness to max
        if (ship.getSystem().isActive()) {
            colorToUse = new Color(
                    tahlan_Utils.lerp(COLOR_NORMAL[0],COLOR_SYSTEM[0],systemLevel),
                    tahlan_Utils.lerp(COLOR_NORMAL[1],COLOR_SYSTEM[1],systemLevel),
                    tahlan_Utils.lerp(COLOR_NORMAL[2],COLOR_SYSTEM[2],systemLevel),
                    currentBrightness*MAX_OPACITY);
        }

        //And finally actually apply the color
        weapon.getSprite().setColor(colorToUse);

        //Jitter! Jitter based on our maximum jitter distance and our flux level
        if (currentBrightness > 0.8) {
            Vector2f randomOffset = MathUtils.getRandomPointInCircle(new Vector2f(weapon.getSprite().getWidth() / 2f, weapon.getSprite().getHeight() / 2f), MAX_JITTER_DISTANCE);
            weapon.getSprite().setCenter(randomOffset.x, randomOffset.y);
        }
    }
}