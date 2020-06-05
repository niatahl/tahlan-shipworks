package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static data.scripts.utils.tahlan_txt.txt;

public class tahlan_FilamentSpecs extends BaseHullMod {

	private static final float FIGHTER_RATE = -25f;
	private static final float ZERO_FLUX_BOOST = 20f;

    private final String INNERLARGE = "graphics/tahlan/fx/tahlan_tempshield.png";
    private final String OUTERLARGE = "graphics/tahlan/fx/tahlan_tempshield_ring.png";

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		
		stats.getFighterRefitTimeMult().modifyPercent(id, FIGHTER_RATE);
        stats.getDynamic().getStat(Stats.REPLACEMENT_RATE_DECREASE_MULT).modifyPercent(id, FIGHTER_RATE);
        stats.getZeroFluxSpeedBoost().modifyFlat(id,ZERO_FLUX_BOOST);

	}

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.getShield().setRadius(ship.getShieldRadiusEvenIfNoShield(), INNERLARGE, OUTERLARGE);
    }

	public boolean isApplicableToShip(ShipAPI ship) {
		return false;
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int)-FIGHTER_RATE + "%";
		if (index == 1) return "" + (int)ZERO_FLUX_BOOST;
		return null;
	}
	

}
