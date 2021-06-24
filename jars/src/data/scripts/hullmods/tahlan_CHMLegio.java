package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.HashMap;
import java.util.Map;

public class tahlan_CHMLegio extends BaseHullMod {

    private static final Map<ShipAPI.HullSize, Float> mag = new HashMap<ShipAPI.HullSize, Float>();
    static {
        mag.put(ShipAPI.HullSize.FRIGATE, 20f);
        mag.put(ShipAPI.HullSize.DESTROYER, 15f);
        mag.put(ShipAPI.HullSize.CRUISER, 10f);
        mag.put(ShipAPI.HullSize.CAPITAL_SHIP, 5f);
    }
    private static final float ZERO_FLUX_BOOST = 10f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().modifyFlat(id, mag.get(hullSize));
        stats.getZeroFluxSpeedBoost().modifyFlat(id,ZERO_FLUX_BOOST);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().hasHullMod("CHM_commission")) {
            ship.getVariant().removeMod("CHM_commission");
        }
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (mag.get(ShipAPI.HullSize.FRIGATE)).intValue();
        if (index == 1) return "" + (mag.get(ShipAPI.HullSize.DESTROYER)).intValue();
        if (index == 2) return "" + (mag.get(ShipAPI.HullSize.CRUISER)).intValue();
        if (index == 3) return "" + (mag.get(ShipAPI.HullSize.CAPITAL_SHIP)).intValue();
        if (index == 4) return "" + (int)ZERO_FLUX_BOOST;
        return null;
    }
}
