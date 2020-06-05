package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.util.MagicIncompatibleHullmods;

import java.util.HashSet;
import java.util.Set;

import static data.scripts.utils.tahlan_txt.txt;

public class tahlan_ParallelTargetingUnit extends BaseHullMod {

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>(2);

    static final float RANGE_BOOST = 120f;
    static final float PD_MINUS = 60f;
    static final float RANGE_CAP = 1400f;
    static final float RANGE_MULT = 0.25f;

    static {
        BLOCKED_HULLMODS.add("dedicated_targeting_core");
        BLOCKED_HULLMODS.add("targetingunit");
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return true;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getWeaponRangeThreshold().modifyFlat(id,RANGE_CAP);
        stats.getWeaponRangeMultPastThreshold().modifyMult(id,RANGE_MULT);

        stats.getBallisticWeaponRangeBonus().modifyPercent(id,RANGE_BOOST);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id,RANGE_BOOST);

        stats.getNonBeamPDWeaponRangeBonus().modifyPercent(id, -PD_MINUS);
        stats.getBeamPDWeaponRangeBonus().modifyPercent(id, -PD_MINUS);

    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                //ship.getVariant().removeMod(tmp);
                MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(),tmp,"tahlan_paralleltargeting");
            }
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + (int) RANGE_BOOST + txt("%");
        if (index == 1) return "" + (int) (RANGE_BOOST-PD_MINUS) + txt("%");
        if (index == 2) return "" + (int) RANGE_CAP + txt("su");
        if (index == 3) return "" + (int) (RANGE_MULT*100f) + txt("%");
        return null;
    }
}
