package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;

public class BeamFlashEffect implements BeamEffectPlugin {

    private final IntervalUtil flashInterval = new IntervalUtil(0.1f,0.2f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {

        flashInterval.advance(engine.getElapsedInLastFrame());
        if (flashInterval.intervalElapsed()) {
            float size = beam.getWidth() * MathUtils.getRandomNumberInRange(2f, 2.2f);
            float dur = MathUtils.getRandomNumberInRange(0.2f,0.25f);
            engine.addHitParticle(beam.getFrom(), beam.getSource().getVelocity(), beam.getWidth(), 0.8f, dur, beam.getCoreColor());
            engine.addHitParticle(beam.getFrom(), beam.getSource().getVelocity(), size, 0.8f, dur, beam.getFringeColor().brighter());
            engine.addHitParticle(beam.getTo(), beam.getSource().getVelocity(), size * 3f, 0.8f, dur, beam.getFringeColor());
        }

    }
}
