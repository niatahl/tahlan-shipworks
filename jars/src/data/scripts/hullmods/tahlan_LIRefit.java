package data.scripts.hullmods;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class tahlan_LIRefit extends BaseHullMod {

	private static Map mag = new HashMap();
	static {
		mag.put(HullSize.FRIGATE, 25f);
		mag.put(HullSize.DESTROYER, 20f);
		mag.put(HullSize.CRUISER, 15f);
		mag.put(HullSize.CAPITAL_SHIP, 10f);
	}
	
	private static final float RANGE_MULT = 0.9f;
	private static final float FIGHTER_RATE = 10f;
	private static final float BURN_LEVEL_BONUS = 1f;
	private static final float PROFILE_MULT = 0.75f;
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().modifyFlat(id, (Float) mag.get(hullSize));
		stats.getMaxBurnLevel().modifyFlat(id, BURN_LEVEL_BONUS);
		stats.getSensorProfile().modifyMult(id, PROFILE_MULT);

		stats.getBallisticWeaponRangeBonus().modifyMult(id, RANGE_MULT);
		stats.getEnergyWeaponRangeBonus().modifyMult(id, RANGE_MULT);
		
		stats.getFighterRefitTimeMult().modifyPercent(id, FIGHTER_RATE);

	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + ((Float) mag.get(HullSize.FRIGATE)).intValue();
		if (index == 1) return "" + ((Float) mag.get(HullSize.DESTROYER)).intValue();
		if (index == 2) return "" + ((Float) mag.get(HullSize.CRUISER)).intValue();
		if (index == 3) return "" + ((Float) mag.get(HullSize.CAPITAL_SHIP)).intValue();

		if (index == 4) return "" + (int) BURN_LEVEL_BONUS;
		if (index == 5) return "" + (int) Math.round((1f - PROFILE_MULT) * 100f) + "%";

		if (index == 6) return "" + (int) Math.round((1f - RANGE_MULT) * 100f) + "%";
		if (index == 7) return "" + (int) Math.round(FIGHTER_RATE) + "%";

		return null;
	}
	

}
