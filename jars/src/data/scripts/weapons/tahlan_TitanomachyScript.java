package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicLensFlare;

import java.awt.*;

public class tahlan_TitanomachyScript implements EveryFrameWeaponEffectPlugin {

    private IntervalUtil interval = new IntervalUtil(0.2f, 0.3f);

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (weapon.isFiring()){
            interval.advance(engine.getElapsedInLastFrame());
            if (interval.intervalElapsed())
                MagicLensFlare.createSharpFlare(engine,weapon.getShip(),weapon.getLocation(),2f,10f,0f,weapon.getSpec().getGlowColor(),new Color(255,255,255));
        }

    }
}
