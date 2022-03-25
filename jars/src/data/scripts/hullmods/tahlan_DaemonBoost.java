package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.HashMap;
import java.util.Map;

public class tahlan_DaemonBoost extends BaseHullMod {

    public static final Map<ShipAPI.HullSize, Float> SUPPLYMALUS = new HashMap<>();
    static {
        SUPPLYMALUS.put(ShipAPI.HullSize.DEFAULT, 0f);
        SUPPLYMALUS.put(ShipAPI.HullSize.FRIGATE, -2f);
        SUPPLYMALUS.put(ShipAPI.HullSize.DESTROYER, -4f);
        SUPPLYMALUS.put(ShipAPI.HullSize.CRUISER, -6f);
        SUPPLYMALUS.put(ShipAPI.HullSize.CAPITAL_SHIP, -10f);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSuppliesPerMonth().modifyFlat(id,SUPPLYMALUS.get(hullSize));
        stats.getSuppliesToRecover().modifyFlat(id,SUPPLYMALUS.get(hullSize));
        stats.getPeakCRDuration().modifyFlat(id,-60);
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(id,SUPPLYMALUS.get(hullSize));
        stats.getDamageToCruisers().modifyMult(id,1.1f);
        stats.getDamageToCapital().modifyMult(id,1.2f);
    }
}
