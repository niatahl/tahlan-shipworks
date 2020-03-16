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

		stats.getMaxRecoilMult().modifyMult(id, 0.5f);
		stats.getRecoilDecayMult().modifyMult(id, 1.5f);
		stats.getRecoilPerShotMult().modifyMult(id, 0.5f);

		stats.getProjectileSpeedMult().modifyMult(id, 1.5f);

		//stats.getMaxSpeed().modifyMult(id,1f-DAMAGE_BONUS*effectLevel);
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getEnergyWeaponDamageMult().unmodify(id);
		stats.getBeamWeaponDamageMult().unmodify(id);
		stats.getBallisticWeaponDamageMult().unmodify(id);

        stats.getEnergyWeaponRangeBonus().unmodify(id);
        stats.getBallisticWeaponRangeBonus().unmodify(id);

        stats.getMaxRecoilMult().unmodify(id);
        stats.getRecoilDecayMult().unmodify(id);
        stats.getRecoilPerShotMult().unmodify(id);

        stats.getProjectileSpeedMult().unmodify(id);

        //stats.getMaxSpeed().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		float mult = 1f + DAMAGE_BONUS * effectLevel;
		float bonusPercent = (int) ((mult - 1f) * 100f);
		if (index == 0) {
			return new StatusData("Non-missile weapon damage +" + (int) bonusPercent + "%", false);
		}
		if (index == 1) {
			return new StatusData("Projectile speed and recoil improved", false);
		}
        if (index == 2) {
            return new StatusData("Range +200su", false);
        }
		return null;
	}
}