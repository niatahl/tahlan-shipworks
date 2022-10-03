package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ProximityExplosionEffect;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;

public class FlakOnExplosionEffect implements ProximityExplosionEffect {
    @Override
    public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
        CombatEngineAPI engine = Global.getCombatEngine();

        Color effectCol = new Color(
                originalProjectile.getProjectileSpec().getFringeColor().getRed(),
                originalProjectile.getProjectileSpec().getFringeColor().getGreen(),
                originalProjectile.getProjectileSpec().getFringeColor().getBlue(),
                70
        );

        engine.addHitParticle(
                explosion.getLocation(),
                Misc.ZERO,
                explosion.getCollisionRadius()*2f,
                0.8f,
                0.1f,
                effectCol
        );

        engine.addSmoothParticle(
                explosion.getLocation(),
                Misc.ZERO,
                explosion.getCollisionRadius()*3f,
                0.8f,
                0.1f,
                effectCol
        );

        for (int i = 0; i < 10; i++) {
            engine.addNebulaParticle(
                    explosion.getLocation(),
                    MathUtils.getRandomPointInCircle(Misc.ZERO,15f),
                    MathUtils.getRandomNumberInRange(explosion.getCollisionRadius()*0.5f, explosion.getCollisionRadius()*2f),
                    2f,
                    0f,
                    0.3f,
                    MathUtils.getRandomNumberInRange(1.5f, 3f),
                    new Color(22, 21, 20, 140),
                    true
            );
        }


    }
}
