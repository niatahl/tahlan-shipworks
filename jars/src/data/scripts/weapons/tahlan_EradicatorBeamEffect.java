package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class tahlan_EradicatorBeamEffect implements BeamEffectPlugin {

	private final IntervalUtil fireInterval = new IntervalUtil(0.2f, 0.3f);
	private final IntervalUtil flashInterval = new IntervalUtil(0.1f,0.1f);
	private boolean wasZero = true;
	
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {

		flashInterval.advance(engine.getElapsedInLastFrame());
		if (flashInterval.intervalElapsed()) {
			float size = beam.getWidth() * MathUtils.getRandomNumberInRange(2f, 2.2f);
			float dur = MathUtils.getRandomNumberInRange(0.2f,0.25f);
			engine.addHitParticle(beam.getFrom(), beam.getSource().getVelocity(), beam.getWidth(), 0.8f, dur, beam.getCoreColor());
			engine.addHitParticle(beam.getFrom(), beam.getSource().getVelocity(), size, 0.8f, dur, beam.getFringeColor().brighter());
			engine.addHitParticle(beam.getTo(), beam.getSource().getVelocity(), size * 3f, 0.8f, dur, beam.getFringeColor());
		}

		CombatEntityAPI target = beam.getDamageTarget();
		if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
			float dur = beam.getDamage().getDpsDuration();
			// needed because when the ship is in fast-time, dpsDuration will not be reset every frame as it should be
			if (!wasZero) dur = 0;
			wasZero = beam.getDamage().getDpsDuration() <= 0;
			fireInterval.advance(dur);
			if (fireInterval.intervalElapsed()) {
				boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
				//piercedShield = true;
				
				if (!hitShield) {

					Vector2f dir = Vector2f.sub(beam.getTo(), beam.getFrom(), new Vector2f());
					if (dir.lengthSquared() > 0) dir.normalise();
					dir.scale(50f);
					Vector2f point = Vector2f.sub(beam.getTo(), dir, new Vector2f());

					float dam = beam.getWeapon().getDamage().getDamage() * 0.1f;
					EmpArcEntityAPI arc =  engine.spawnEmpArc(beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
							DamageType.ENERGY,
							dam,
							0f,
							10000f,
							"tachyon_lance_emp_impact",
							beam.getWidth(),
							beam.getFringeColor(),
							beam.getCoreColor()
					);
				}
			}
		}

	}
}
