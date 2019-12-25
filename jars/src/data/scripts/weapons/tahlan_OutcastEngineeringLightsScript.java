package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class tahlan_OutcastEngineeringLightsScript implements EveryFrameWeaponEffectPlugin {
    private static final float[] COLORS = {255f/255f, 255f/255f, 255f/255f};
    private static final float MAX_JITTER_DISTANCE = 1.4f;
    private static final float MAX_OPACITY = 0.9f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null) {
            return;
        }

        //Brightness is based on current flux, starting at 30% and maxing at 90%. It scales quicker the lower it is
        float currentBrightness = (float)Math.sqrt((Math.max(0.3f, Math.min(0.9f, ship.getFluxTracker().getFluxLevel())) - 0.3f) / 0.6f);

        //A piece should never have glowing lights
        if (ship.isPiece()) {
            currentBrightness = 0f;
        }

        //Switches to the proper sprite
        if (currentBrightness > 0) {
            weapon.getAnimation().setFrame(1);
        } else {
            weapon.getAnimation().setFrame(0);
        }

        //Now, set the color to the one we want, and include opacity
        Color colorToUse = new Color(COLORS[0], COLORS[1], COLORS[2], currentBrightness*MAX_OPACITY);

        //And finally actually apply the color
        weapon.getSprite().setColor(colorToUse);

        //Jitter! Jitter based on our maximum jitter distance and our flux level
        Vector2f randomOffset = MathUtils.getRandomPointInCircle(new Vector2f(weapon.getSprite().getWidth()/2f, weapon.getSprite().getHeight()/2f), MAX_JITTER_DISTANCE*currentBrightness);
        weapon.getSprite().setCenter(randomOffset.x, randomOffset.y);
    }
}