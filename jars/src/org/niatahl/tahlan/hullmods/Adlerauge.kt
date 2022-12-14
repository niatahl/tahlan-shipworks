package org.niatahl.tahlan.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import static org.niatahl.tahlan.utils.Utils.txt;

public class Adlerauge extends BaseHullMod {

    private static final float EFFECT_RANGE = 2000f;
    private static final float AUTOAIM_BONUS = 50f;
    private static final float RANGE_BOOST = 100f;
    private static final float SPEED_BOOST = 20f;
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

	    stats.getEnergyWeaponRangeBonus().modifyFlat(id, RANGE_BOOST);
	    stats.getBallisticWeaponRangeBonus().modifyFlat(id, RANGE_BOOST);
	    stats.getProjectileSpeedMult().modifyPercent(id, SPEED_BOOST);
        stats.getAutofireAimAccuracy().modifyFlat(id, AUTOAIM_BONUS * 0.01f);
	}

    public boolean isApplicableToShip(ShipAPI ship) {
		return false;
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int)RANGE_BOOST + txt("su");
		if (index == 1) return "" + (int)SPEED_BOOST + txt("%");
        if (index == 2) return txt("hmd_adler1");
		if (index == 3) return "" + (int)EFFECT_RANGE + txt("su");

		return null;
	}
	

}
