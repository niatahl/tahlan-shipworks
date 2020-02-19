package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class tahlan_WeaponStabilizers extends BaseShipSystemScript {

	public static final float DAMAGE_BONUS = 0.25f;
	public static final float RANGE_BOOST = 200f;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		
		float mult = 1f + DAMAGE_BONUS * effectLevel;
		stats.getEnergyWeaponDamageMult().modifyMult(id,mult);
		stats.getBallisticWeaponDamageMult().modifyMult(id,mult);
		stats.getBeamWeaponDamageMult().modifyMult(id,mult);

		stats.getEnergyWeaponRangeBonus().modifyFlat(id, RANGE_BOOST);
		stats.getBallisticWeaponRangeBonus().modifyFlat(id, RANGE_BOOST);
		stats.getProjectileSpeedMult().modifyMult(id, 1.25f);

		stats.getMaxSpeed().modifyMult(id,1f-DAMAGE_BONUS*effectLevel);
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getEnergyWeaponDamageMult().unmodify(id);
		stats.getBeamWeaponDamageMult().unmodify(id);
		stats.getBallisticWeaponDamageMult().unmodify(id);

        stats.getEnergyWeaponRangeBonus().unmodify(id);
        stats.getBallisticWeaponRangeBonus().unmodify(id);
        stats.getProjectileSpeedMult().unmodify(id);

        stats.getMaxSpeed().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		float mult = 1f + DAMAGE_BONUS * effectLevel;
		float bonusPercent = (int) ((mult - 1f) * 100f);
		if (index == 0) {
			return new StatusData("Non-missile weapon damage +" + (int) bonusPercent + "%", false);
		}
		if (index == 1) {
			return new StatusData("Projectile speed +25%", false);
		}
        if (index == 2) {
            return new StatusData("Range +200su", false);
        }
		return null;
	}
}