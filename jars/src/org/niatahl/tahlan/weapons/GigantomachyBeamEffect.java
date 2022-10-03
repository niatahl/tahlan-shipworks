package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class GigantomachyBeamEffect implements BeamEffectPlugin {

    private IntervalUtil fireInterval = new IntervalUtil(0.2f, 0.3f);
    private IntervalUtil effectInterval = new IntervalUtil(0.2f, 0.3f);
    private boolean wasZero = true;

    private static final Color FLASH_COLOR = new Color(237, 255, 237);
    private static final int NUM_PARTICLES = 40;

    private boolean detonated = false;

    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {

        if (engine.isPaused()) {
            return;
        }

        CombatEntityAPI target = beam.getDamageTarget();
        if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
            float dur = beam.getDamage().getDpsDuration();
            // needed because when the ship is in fast-time, dpsDuration will not be reset every frame as it should be
            if (!wasZero) dur = 0;
            wasZero = beam.getDamage().getDpsDuration() <= 0;
            fireInterval.advance(dur);
            if (fireInterval.intervalElapsed()) {
                EmpArcEntityAPI arc = engine.spawnEmpArc(beam.getSource(), beam.getFrom(), beam.getSource(), beam.getDamageTarget(),
                        DamageType.ENERGY,
                        500f,
                        500f,
                        3000f,
                        "tachyon_lance_emp_impact",
                        beam.getWidth(),
                        beam.getFringeColor(),
                        beam.getCoreColor()
                );
            }
        }

        if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
            boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());

            if (!hitShield && !detonated) {

                Vector2f point = beam.getTo();

                engine.spawnExplosion(point, ZERO, beam.getCoreColor(), 200f, 1f);
                engine.spawnExplosion(point, ZERO, FLASH_COLOR, 100f, 1f);

                DamagingExplosionSpec blast = new DamagingExplosionSpec(0.1f,
                        200f,
                        100f,
                        500f,
                        500f,
                        CollisionClass.PROJECTILE_FF,
                        CollisionClass.PROJECTILE_FIGHTER,
                        10f,
                        10f,
                        0f,
                        0,
                        beam.getFringeColor(),
                        null);
                blast.setDamageType(DamageType.ENERGY);
                blast.setShowGraphic(false);
                engine.spawnDamagingExplosion(blast, beam.getSource(), point, false);

                engine.addSmoothParticle(point, ZERO, 400f, 0.5f, 0.1f, beam.getFringeColor());
                engine.addHitParticle(point, ZERO, 200f, 0.5f, 0.25f, FLASH_COLOR);
                for (int x = 0; x < NUM_PARTICLES; x++) {
                    engine.addHitParticle(point,
                            MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 300f), (float) Math.random() * 360f),
                            6f, 1f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), beam.getFringeColor());
                }
                MagicLensFlare.createSharpFlare(engine, beam.getSource(), point, 8, 400, 0, new Color(100, 215, 255), new Color(255, 255, 255));
                Global.getSoundPlayer().playSound("tahlan_ethniu_blast", 1f, 1f, point, ZERO);

                detonated = true;
            }
        }

        if (beam.getBrightness() >= 1) {
            effectInterval.advance(engine.getElapsedInLastFrame());
            if (effectInterval.intervalElapsed()) {
                Vector2f point = MathUtils.getRandomPointInCircle(beam.getFrom(), 50f);
                EmpArcEntityAPI arc = engine.spawnEmpArcPierceShields(beam.getSource(), beam.getFrom(), beam.getSource(),
                        new SimpleEntity(point),
                        DamageType.FRAGMENTATION,
                        0f,
                        0f,
                        75f,
                        null,
                        beam.getWidth() * 0.5f,
                        beam.getFringeColor(),
                        beam.getCoreColor()
                );
            }
        }
    }
}
