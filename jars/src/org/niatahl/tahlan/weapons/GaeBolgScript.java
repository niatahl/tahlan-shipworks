package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

public class GaeBolgScript implements BeamEffectPlugin {

    private IntervalUtil effectInterval = new IntervalUtil(0.2f, 0.3f);
    private IntervalUtil fireInterval = new IntervalUtil(0.2f, 0.3f);
    private boolean wasZero = true;

    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {

        if (engine.isPaused()) {
            return;
        }

        //EMP Arcs
        CombatEntityAPI target = beam.getDamageTarget();
        if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
            float dur = beam.getDamage().getDpsDuration();
            // needed because when the ship is in fast-time, dpsDuration will not be reset every frame as it should be
            if (!wasZero) dur = 0;
            wasZero = beam.getDamage().getDpsDuration() <= 0;
            fireInterval.advance(dur);
            if (fireInterval.intervalElapsed()) {
                ShipAPI ship = (ShipAPI) target;
                boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
                float pierceChance = ((ShipAPI)target).getHardFluxLevel() - 0.1f;
                pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

                boolean piercedShield = hitShield && (float) Math.random() < pierceChance;
                //piercedShield = true;

                if (!hitShield || piercedShield) {
                    Vector2f dir = Vector2f.sub(beam.getTo(), beam.getFrom(), new Vector2f());
                    if (dir.lengthSquared() > 0) dir.normalise();
                    dir.scale(50f);
                    Vector2f point = Vector2f.sub(beam.getTo(), dir, new Vector2f());
                    float emp = beam.getWeapon().getDamage().getFluxComponent() * 0.5f;
                    float dam = beam.getWeapon().getDamage().getDamage() * 0.25f;
                    EmpArcEntityAPI arc =  engine.spawnEmpArcPierceShields(
                            beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                            DamageType.ENERGY,
                            dam, // damage
                            emp, // emp
                            100000f, // max range
                            "tachyon_lance_emp_impact",
                            beam.getWidth() + 10f,
                            beam.getFringeColor(),
                            beam.getCoreColor()
                    );
                }
            }
        }

        //Deco Arcs
        if (beam.getBrightness() >= 0.1) {
            effectInterval.advance(engine.getElapsedInLastFrame());

            engine.addSmoothParticle(beam.getFrom(),new Vector2f(0f,0f),80f,beam.getBrightness(),0.1f,beam.getFringeColor());
            engine.addSmoothParticle(beam.getFrom(),new Vector2f(0f,0f),40f,beam.getBrightness(),0.1f,beam.getCoreColor());

            Vector2f point = MathUtils.getRandomPointInCircle(beam.getFrom(),50f);
            if (effectInterval.intervalElapsed()){
                EmpArcEntityAPI arc =  engine.spawnEmpArcPierceShields(beam.getSource(), beam.getFrom(), beam.getSource(),
                        new SimpleEntity(point),
                        DamageType.FRAGMENTATION,
                        0f,
                        0f,
                        75f,
                        null,
                        beam.getWidth(),
                        beam.getFringeColor(),
                        beam.getCoreColor()
                );
            }
        }


    }
}

