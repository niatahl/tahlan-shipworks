package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class tahlan_AftermarketPhasecoils extends BaseHullMod {

    private static final float RANGE_THRESHOLD = 700f;
    private static final float RANGE_MULT = 0.25f;
    private static final float MAINTENANCE_MULT = 2f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getWeaponRangeThreshold().modifyFlat(id,RANGE_THRESHOLD);
        stats.getWeaponRangeMultPastThreshold().modifyMult(id,RANGE_MULT);
        stats.getSuppliesPerMonth().modifyMult(id,MAINTENANCE_MULT);
        stats.getSensorProfile().modifyMult(id, 0f);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + (int)RANGE_THRESHOLD + "su";
        if (index == 1) return "doubled";
        return null;
    }
}
