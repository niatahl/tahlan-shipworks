package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class tahlan_MakeshiftMounts extends BaseHullMod {


	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getWeaponTurnRateBonus().modifyMult(id,0.5f);
		stats.getBallisticRoFMult().modifyMult(id,0.75f);
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "50%";
		if (index == 1) return "75%";
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








