package org.niatahl.tahlan.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import static org.niatahl.tahlan.utils.Utils.txt;

public class BroadsideMounts extends BaseHullMod {

	public static final float REPAIR_BONUS = 25f;
	public static final float WEAPON_HEALTH_BONUS = 50f;
	public static final float LARGE_OP_REDUCTION = 10f;
	//public static final float BALLISTIC_FLUX_MULT = 0.9f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getCombatEngineRepairTimeMult().modifyMult(id, 1f - REPAIR_BONUS*0.01f);
		stats.getCombatWeaponRepairTimeMult().modifyMult(id, 1f - REPAIR_BONUS*0.01f);
		stats.getWeaponHealthBonus().modifyPercent(id, WEAPON_HEALTH_BONUS);
		stats.getDynamic().getMod(Stats.LARGE_BALLISTIC_MOD).modifyFlat(id,-LARGE_OP_REDUCTION);
		stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD).modifyFlat(id,-LARGE_OP_REDUCTION);
	}

//  Just in case Nick's retardation ever reaches critical levels
//	@Override
//	public void advanceInCombat(ShipAPI ship, float amount) {
//		for (WeaponAPI weapon: ship.getAllWeapons()) {
//			if (weapon.getSpec().getWeaponId().equals("devastator") && weapon.getSlot().getId().equals("WS0003")) {
//				throw new RuntimeException("Stop being trash lmao");
//			}
//		}
//	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) WEAPON_HEALTH_BONUS + txt("%");
		if (index == 1) return "" + (int) REPAIR_BONUS + txt("%");
		if (index == 2) return "" + (int) LARGE_OP_REDUCTION;
		//if (index == 1) return "" + Math.round((1f - BALLISTIC_FLUX_MULT)*100f) + "%";
		return null;
	}

    //Built-in only
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

	@Override
	public boolean affectsOPCosts() {
		return true;
	}

}








