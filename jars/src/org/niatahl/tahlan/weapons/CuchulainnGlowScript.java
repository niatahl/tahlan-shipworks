package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class CuchulainnGlowScript implements EveryFrameWeaponEffectPlugin {
    private static final float[] COLOR_NORMAL = {165f/255f, 100f/255f, 255f/255f};
    private static final float MAX_JITTER_DISTANCE = 0.8f;
    private static final float MAX_OPACITY = 1f;

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

        //No glows on wrecks
        if ( ship.isPiece() || !ship.isAlive() ) {
            return;
        }

        WeaponAPI gaebolg = null;
        for ( WeaponAPI shipweapon : ship.getAllWeapons() ) {
            if (shipweapon.getId().startsWith("tahlan_gaebolg"))
                gaebolg = shipweapon;
        }

        //Brightness based on flux under normal conditions
        if ( gaebolg != null ) {
            currentBrightness = gaebolg.getChargeLevel();
        }

        //Glows off in refit screen
        if (ship.getOriginalOwner() == -1) {
            return;
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


        //And finally actually apply the color
        weapon.getSprite().setColor(colorToUse);

        //Jitter! Jitter based on our maximum jitter distance and our flux level
        if (currentBrightness > 0.8) {
            Vector2f randomOffset = MathUtils.getRandomPointInCircle(new Vector2f(weapon.getSprite().getWidth() / 2f, weapon.getSprite().getHeight() / 2f), MAX_JITTER_DISTANCE);
            weapon.getSprite().setCenter(randomOffset.x, randomOffset.y);
        }
    }
}