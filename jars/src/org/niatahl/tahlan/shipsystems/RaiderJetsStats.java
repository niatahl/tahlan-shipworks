package org.niatahl.tahlan.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class RaiderJetsStats extends BaseShipSystemScript {

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        stats.getMaxSpeed().modifyFlat(id, 100f * effectLevel);
        stats.getAcceleration().modifyPercent(id, 400f * effectLevel);
        stats.getDeceleration().modifyPercent(id, 400f * effectLevel);
        stats.getTurnAcceleration().modifyMult(id, 1f + 2f * effectLevel);
        stats.getMaxTurnRate().modifyMult(id, 1f + 1f * effectLevel);
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("improved maneuverability", false);
        }
        return null;
    }

}
