package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual.NEParams;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.impl.combat.dweller.DarkenedGazeSystemScript;
import com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AbyssalGlareEffect implements BeamEffectPlugin {

	public static float RIFT_DAMAGE = 200f; // happens approximately every 1.25 seconds; ~160 dps per beam
	public static float DAMAGE_MULT_NORMAL_WEAPON = 0.5f;
	
	protected IntervalUtil fireInterval = new IntervalUtil(0.2f, 0.3f);
	protected boolean hadDamageTargetPrev = false;
	protected boolean lengthChangedPrev = false;
	protected CombatEntityAPI lastCombatEntity = null;
	protected float sinceRiftSpawn = 0f;
	protected Vector2f prevTo = null;
	protected Vector2f prevFrom = null;
	
	public AbyssalGlareEffect() {
		fireInterval.randomize();
	}
	
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		//if (true) return;

		if (beam.getSource() == null || beam.getWeapon() == null) return;
		
		boolean normalWeaponMode = !beam.getSource().hasTag(DarkenedGazeSystemScript.DARKENED_GAZE_SYSTEM_TAG);
		//normalWeaponMode = true;
		boolean primary = beam.getWeapon().getCustom() == DarkenedGazeSystemScript.DARKENED_GAZE_PRIMARY_WEAPON_TAG;
		if (normalWeaponMode) primary = true;
		
		sinceRiftSpawn += amount;
		
		float maxRange = beam.getWeapon().getRange();
		Vector2f from = beam.getFrom();
		Vector2f to = beam.getRayEndPrevFrame();
		Vector2f to2 = beam.getTo();
		float dist = Misc.getDistance(from, to);
		float dist2 = Misc.getDistance(from, to2);
		if (dist2 < dist) {
			to = to2;
			dist = dist2;
		}
		
//		if (beam.getDamageTarget() instanceof ShipAPI) {
//			((ShipAPI)beam.getDamageTarget()).setSkipNextDamagedExplosion(true);
//		}
		
		boolean hasDamageTarget = beam.getDamageTarget() != null;
		boolean lengthChanged = prevTo == null || 
						Math.abs(Misc.getDistance(prevFrom, prevTo) - Misc.getDistance(from, to)) > 2f;
		
		boolean forceRiftSpawn = (hasDamageTarget && !hadDamageTargetPrev) ||
						(!lengthChanged && lengthChangedPrev && lastCombatEntity != beam.getDamageTarget());
		if (!primary) forceRiftSpawn = false;
		
		lengthChangedPrev = lengthChanged;
		hadDamageTargetPrev = hasDamageTarget;
		lastCombatEntity = beam.getDamageTarget();
		prevFrom = new Vector2f(from);
		prevTo = new Vector2f(to);
		
//		forceRiftSpawn = false;
//		if (forceRiftSpawn) {
//			System.out.println("efwfwefwe");
//		}
		fireInterval.advance(amount);
		if (fireInterval.intervalElapsed() || forceRiftSpawn) {
			
			if (beam.getDamageTarget() == null && dist < maxRange * 0.9f) {
				return;
			}
			if (beam.getBrightness() < 1) {
				return;
			}
			
			
			Color color = RiftLightningEffect.RIFT_LIGHTNING_COLOR;
			
			boolean spawnedExplosion = false;
			float maxTimeWithoutExplosion = 1f;
			if (normalWeaponMode) {
				maxTimeWithoutExplosion = 0.5f;
			}
			if ((float) Math.random() > 0.8f || forceRiftSpawn || (primary && sinceRiftSpawn > maxTimeWithoutExplosion)) {
				DamagingProjectileAPI explosion = engine.spawnDamagingExplosion(
								createExplosionSpec(normalWeaponMode ? DAMAGE_MULT_NORMAL_WEAPON : 1f),
								beam.getSource(), to);
				//explosion.addDamagedAlready(target);
				//color = new Color(255,75,75,255);
				
				float distFactor = 0f;
				if (dist > 500f) {
					distFactor = (dist - 500f) / 1500f;
					if (distFactor < 0f) distFactor = 0f;
					if (distFactor > 1f) distFactor = 1f;
				}
				
				float sizeAdd = 5f * distFactor;
				float baseSize = 15f;
				if (normalWeaponMode) {
					baseSize *= 0.5f;
					sizeAdd = 0f; // beam is not getting wider at end, so don't increase explosion size
				}
				
				NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(
											color, baseSize + sizeAdd);
				//p.hitGlowSizeMult = 0.5f;
				p.noiseMult = 6f;
				p.thickness = 25f;
				p.fadeOut = 0.5f;
				p.spawnHitGlowAt = 1f;
				p.additiveBlend = true;
				p.blackColor = Color.white;
				p.underglow = null;
				p.withNegativeParticles = false;
				p.withHitGlow = false;
				p.fadeIn = 0f;
				//p.numRiftsToSpawn = 1;
				
				RiftCascadeMineExplosion.spawnStandardRift(explosion, p);
				
				spawnedExplosion = true;
				sinceRiftSpawn = 0f;
			}
			
			if (dist > 100f && ((float) Math.random() > 0.5f || (normalWeaponMode && spawnedExplosion))) {
			//if (dist > 100f && spawnedExplosion) {
				EmpArcParams params = new EmpArcParams();
				params.segmentLengthMult = 8f;
				params.zigZagReductionFactor = 0.15f;
				params.fadeOutDist = 50f;
				params.minFadeOutMult = 10f;
	//			params.flickerRateMult = 0.7f;
				params.flickerRateMult = 0.3f;
	//			params.flickerRateMult = 0.05f;
	//			params.glowSizeMult = 3f;
				
				float fraction = Math.min(0.33f, 300f / dist);
				params.brightSpotFullFraction = fraction;
				params.brightSpotFadeFraction = fraction;
				
				float arcSpeed = RiftLightningEffect.RIFT_LIGHTNING_SPEED;
				params.movementDurOverride = Math.max(0.05f, dist / arcSpeed);
				
				ShipAPI ship = beam.getSource();
				//Color color = weapon.getSpec().getGlowColor();
				EmpArcEntityAPI arc = (EmpArcEntityAPI)engine.spawnEmpArcVisual(from, ship, to, ship,
						80f, // thickness
						color,
						new Color(255,255,255,255),
						params
						);
				arc.setCoreWidthOverride(40f);
				
				arc.setRenderGlowAtStart(false);
				arc.setFadedOutAtStart(true);
				arc.setSingleFlickerMode(true);
				
				Vector2f pt = Vector2f.add(from, to, new Vector2f());
				pt.scale(0.5f);
				
				Global.getSoundPlayer().playSound("abyssal_glare_lightning", 1f, 1f, pt, new Vector2f());
			}
			
		}
		
		if (normalWeaponMode) {
			Vector2f pt = Vector2f.add(from, to, new Vector2f());
			pt.scale(0.5f);
			Global.getSoundPlayer().playLoop("abyssal_glare_loop", 
											 beam.getSource(), 1f, beam.getBrightness(),
											 pt, beam.getSource().getVelocity());
		} else if (primary) {
			Vector2f pt = Vector2f.add(from, to, new Vector2f());
			pt.scale(0.5f);
			Global.getSoundPlayer().playLoop("darkened_gaze_loop", 
					 beam.getSource(), 1f, beam.getBrightness(),
					 pt, beam.getSource().getVelocity());
		}
	}
	


	public DamagingExplosionSpec createExplosionSpec(float damageMult) {
		float damage = RIFT_DAMAGE * damageMult;
		DamagingExplosionSpec spec = new DamagingExplosionSpec(
				0.1f, // duration
				75f, // radius
				50f, // coreRadius
				damage, // maxDamage
				damage / 2f, // minDamage
				CollisionClass.PROJECTILE_FF, // collisionClass
				CollisionClass.PROJECTILE_FIGHTER, // collisionClassByFighter
				3f, // particleSizeMin
				3f, // particleSizeRange
				0.5f, // particleDuration
				0, // particleCount
				new Color(255,255,255,0), // particleColor
				new Color(255,100,100,0)  // explosionColor
				);

		spec.setDamageType(DamageType.ENERGY);
		spec.setUseDetailedExplosion(false);
		spec.setSoundSetId("abyssal_glare_explosion");
		spec.setSoundVolume(damageMult);
		return spec;		
	}
	
}


