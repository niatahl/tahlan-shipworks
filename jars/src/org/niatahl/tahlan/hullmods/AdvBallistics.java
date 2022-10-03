package org.niatahl.tahlan.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class AdvBallistics extends BaseHullMod {

	public static final float COST_REDUCTION_SMALL  = 1;
	public static final float COST_REDUCTION_MEDIUM  = 3;
	public static final float COST_REDUCTION_LARGE = 6;
	//public static final float BALLISTIC_FLUX_MULT = 0.9f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getMod(Stats.SMALL_BALLISTIC_MOD).modifyFlat(id, -COST_REDUCTION_SMALL);
		stats.getDynamic().getMod(Stats.MEDIUM_BALLISTIC_MOD).modifyFlat(id, -COST_REDUCTION_MEDIUM);
		stats.getDynamic().getMod(Stats.LARGE_BALLISTIC_MOD).modifyFlat(id, -COST_REDUCTION_LARGE);
		//stats.getBallisticWeaponFluxCostMod().modifyMult(id, BALLISTIC_FLUX_MULT);
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) COST_REDUCTION_SMALL + "/" + (int) COST_REDUCTION_MEDIUM + "/" + (int) COST_REDUCTION_LARGE + "";
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








