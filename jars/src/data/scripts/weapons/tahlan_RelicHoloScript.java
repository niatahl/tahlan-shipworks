package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class tahlan_RelicHoloScript implements EveryFrameWeaponEffectPlugin {
    private static Color COLOR = new Color(255, 137, 39, 173);
    private static final float MAX_JITTER_DISTANCE = 0.2f;
    private static final float MAX_OPACITY = 1f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        if (ship == null) {
            return;
        }

        float currentBrightness = 0f;

        //We glow when the system or overdrive is active
        if (ship.getSystem().isActive()) {
            currentBrightness = ship.getSystem().getEffectLevel();
        }

        //No glows on wrecks
        if (ship.isPiece() || !ship.isAlive()) {
            return;
        }

        //And finally actually apply the color
        weapon.getSprite().setColor(COLOR);

        //Jitter! Jitter based on our maximum jitter distance and our flux level
        if (currentBrightness > 0) {
            Vector2f randomOffset = MathUtils.getRandomPointInCircle(new Vector2f(weapon.getSprite().getWidth() / 2f, weapon.getSprite().getHeight() / 2f), MAX_JITTER_DISTANCE);
            weapon.getSprite().setCenter(randomOffset.x, randomOffset.y);
        }
    }
}