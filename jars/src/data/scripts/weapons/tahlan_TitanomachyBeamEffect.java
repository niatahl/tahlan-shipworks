package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;

public class tahlan_TitanomachyBeamEffect implements BeamEffectPlugin {

	private IntervalUtil fireInterval = new IntervalUtil(0.4f, 0.6f);
	private IntervalUtil effectInterval = new IntervalUtil(0.2f, 0.3f);
	private boolean wasZero = true;
	
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();
		if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
			float dur = beam.getDamage().getDpsDuration();
			// needed because when the ship is in fast-time, dpsDuration will not be reset every frame as it should be
			if (!wasZero) dur = 0;
			wasZero = beam.getDamage().getDpsDuration() <= 0;
			fireInterval.advance(dur);
			if (fireInterval.intervalElapsed()) {

				if (beam.getWeapon().getShip().getFluxTracker().getFluxLevel() > 0.75f) {



					engine.spawnEmpArc(beam.getSource(), beam.getFrom(), beam.getSource(), beam.getDamageTarget(),
							DamageType.ENERGY,
							50f,
							250f,
							3000f,
							"tachyon_lance_emp_impact",
							beam.getWidth(),
							beam.getFringeColor(),
							beam.getCoreColor()
					);
				}
			}
		}
		if (beam.getBrightness() >= 1) {
		    effectInterval.advance(engine.getElapsedInLastFrame());
		    if (effectInterval.intervalElapsed()){
		        engine.spawnEmpArcPierceShields(beam.getSource(), beam.getFrom(), beam.getSource(), beam.getSource(), DamageType.FRAGMENTATION,
                        0f,
                        0f,
                        75f,
                        null,
                        beam.getWidth()*0.5f,
                        beam.getFringeColor(),
                        beam.getCoreColor()
                );
            }
        }
	}
}
