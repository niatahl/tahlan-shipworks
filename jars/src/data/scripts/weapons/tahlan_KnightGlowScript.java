package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class tahlan_KnightGlowScript implements EveryFrameWeaponEffectPlugin {
    private static final float[] COLOR_NORMAL = {255f/255f, 140f/255f, 80f/255f};
    private static final float[] COLOR_OVERDRIVE = {255f/255f, 100f/255f, 40f/255f};
    private static final float[] COLOR_SYSTEM = {60f/255f, 255f/255f, 245f/255f};
    private static final float MAX_JITTER_DISTANCE = 1.2f;
    private static final float MAX_OPACITY = 1f;
    private static final float TRIGGER_PERCENTAGE = 0.3f;
    private boolean overdrive = false;

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
        float currentBrightness = ship.getFluxTracker().getFluxLevel() *  0.8f;

        //If we are in overdrive, we glow even more
        if (ship.getHitpoints() <= ship.getMaxHitpoints()*TRIGGER_PERCENTAGE || ship.getVariant().hasHullMod("tahlan_forcedoverdrive") || ship.getSystem().isActive()) {
            currentBrightness = 1f;
        }

        //A piece should never have glowing lights
        if (ship.isPiece()) {
            currentBrightness = 0f;
            return;
        }

        //Switches to the proper sprite
        if (currentBrightness > 0) {
            weapon.getAnimation().setFrame(1);
        } else {
            weapon.getAnimation().setFrame(0);
        }

        //Now, set the color to the one we want, and include opacity
        Color colorToUse = new Color(COLOR_NORMAL[0], COLOR_NORMAL[1], COLOR_NORMAL[2], currentBrightness*MAX_OPACITY);

        //Change color if in overdrive
        if (currentBrightness > 0.8) {
            colorToUse = new Color(COLOR_OVERDRIVE[0], COLOR_OVERDRIVE[1], COLOR_OVERDRIVE[2], currentBrightness*MAX_OPACITY);
        }

        //Change color again if system is active and set brightness to max
        if (ship.getSystem().isActive()) {
            colorToUse = new Color(COLOR_SYSTEM[0], COLOR_SYSTEM[1], COLOR_SYSTEM[2], currentBrightness*MAX_OPACITY);
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