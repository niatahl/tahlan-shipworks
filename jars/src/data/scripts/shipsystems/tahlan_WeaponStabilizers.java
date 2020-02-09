package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class tahlan_WeaponStabilizers extends BaseShipSystemScript {

	public static final float DAMAGE_BONUS = 0.5f;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		
		float mult = 1f + DAMAGE_BONUS * effectLevel;
		stats.getEnergyWeaponDamageMult().modifyMult(id,mult);
		stats.getBallisticWeaponDamageMult().modifyMult(id,mult);
		stats.getBeamWeaponDamageMult().modifyMult(id,mult);

		stats.getEnergyWeaponRangeBonus().modifyMult(id,1.25f);
		stats.getBallisticWeaponRangeBonus().modifyMult(id, 1.25f);
		stats.getProjectileSpeedMult().modifyMult(id, 1.25f);

		stats.getMaxSpeed().modifyMult(id,DAMAGE_BONUS);
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
			return new StatusData("Weapon range and projectile speed +25%", false);
		}
		return null;
	}
}