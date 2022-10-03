package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicLensFlare;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class CrusherDriveBeamEffect implements BeamEffectPlugin {

    private static final Color PARTICLE_COLOR = new Color(255, 76, 19, 150);
    private static final Color CORE_COLOR = new Color(255, 114, 31);
    private static final Color FLASH_COLOR = new Color(255, 249, 226);
    private static final int NUM_PARTICLES = 40;

    private final IntervalUtil arc = new IntervalUtil(0.25f, 0.25f);
    private int arcs = 1;

    private boolean detonated = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {

        WeaponAPI weapon = beam.getWeapon();
        //Don't run if we are paused, or our weapon is null
        if (engine.isPaused() || weapon == null) {
            return;
        }

        CombatEntityAPI target = beam.getDamageTarget();
        if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
            boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());

            if (!detonated) {

                float damageAmount = beam.getSource().getMass();

                Vector2f point = beam.getTo();

                engine.spawnExplosion(point, ZERO, CORE_COLOR, 200f, 1f);
                engine.spawnExplosion(point, ZERO, FLASH_COLOR, 100f, 1f);

                DamagingExplosionSpec blast = new DamagingExplosionSpec(0.1f,
                        100f,
                        50f,
                        damageAmount,
                        damageAmount,
                        CollisionClass.PROJECTILE_FF,
                        CollisionClass.PROJECTILE_FIGHTER,
                        10f,
                        10f,
                        0f,
                        0,
                        PARTICLE_COLOR,
                        null);
                blast.setDamageType(DamageType.OTHER);
                blast.setShowGraphic(false);
                engine.spawnDamagingExplosion(blast,beam.getSource(),point,false);

                engine.addSmoothParticle(point, ZERO, 400f, 0.5f, 0.1f, PARTICLE_COLOR);
                engine.addHitParticle(point, ZERO, 200f, 0.5f, 0.25f, FLASH_COLOR);
                for (int x = 0; x < NUM_PARTICLES; x++) {
                    engine.addHitParticle(point,
                            MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 300f), (float) Math.random() * 360f),
                            6f, 1f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), PARTICLE_COLOR);
                }

                Global.getSoundPlayer().playSound("hit_heavy",2f,1f,point,ZERO);

                detonated = true;
            }
        }

    }

}