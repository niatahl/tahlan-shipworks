package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import static data.scripts.utils.tahlan_Utils.txt;

public class tahlan_AftermarketPhasecoils extends BaseHullMod {

    private static final float RANGE_THRESHOLD = 800f;
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
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().getHullMods().contains("safetyoverrides")) {
            ship.getMutableStats().getWeaponRangeThreshold().unmodify(id);
            ship.getMutableStats().getWeaponRangeMultPastThreshold().unmodify(id);
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + (int)RANGE_THRESHOLD + txt("su");
        if (index == 1) return txt("hmd_AftermPhCoils1");
        return null;
    }
}
