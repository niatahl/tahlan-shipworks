package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import static data.scripts.utils.tahlan_txt.txt;

public class tahlan_HeavyEnergy extends BaseHullMod {

	public static final float COST_REDUCTION = 10;
	public static final float ROF_PERCENT = -25f;
	public static final float DAMAGE_PERCENT = 25f;
	//public static final float BALLISTIC_FLUX_MULT = 0.9f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

		stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION);

		stats.getEnergyRoFMult().modifyPercent(id,ROF_PERCENT);
		stats.getEnergyWeaponDamageMult().modifyPercent(id,DAMAGE_PERCENT);
		stats.getBeamWeaponDamageMult().modifyPercent(id, -DAMAGE_PERCENT);

	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) COST_REDUCTION + "";
		if (index == 1) return "" + (int) DAMAGE_PERCENT + txt("%");
        if (index == 2) return "" + (int) ROF_PERCENT*-1 + txt("%");
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








