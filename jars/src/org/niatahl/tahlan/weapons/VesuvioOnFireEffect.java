package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class VesuvioOnFireEffect implements OnFireEffectPlugin {

    static final int NUM_PARTICLES = 20;

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        engine.spawnExplosion(projectile.getLocation(), weapon.getShip().getVelocity(), new Color(120, 200,255,120), 10f, 0.2f);
        for (int i = 0; i < NUM_PARTICLES; i++) {
            float arcPoint = MathUtils.getRandomNumberInRange(projectile.getFacing()-5f, projectile.getFacing()+2f);
            Vector2f velocity = MathUtils.getPointOnCircumference(weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(10f, 50f), arcPoint);
            Vector2f spawnLocation = MathUtils.getPointOnCircumference(projectile.getLocation(), MathUtils.getRandomNumberInRange(0f, 20f), arcPoint);
            engine.addHitParticle(spawnLocation, velocity, MathUtils.getRandomNumberInRange(2f,10f), 10f, MathUtils.getRandomNumberInRange(0.5f,1f),new Color(120, 200,255,120));
        }
    }
}
