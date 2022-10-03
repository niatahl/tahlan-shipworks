package org.niatahl.tahlan.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;

public class InertialessDriveStats extends BaseShipSystemScript {

    private static final Color SHIMMER_COLOR = new Color(119, 226, 176, 117);

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
        } else {

            stats.getMaxSpeed().modifyFlat(id, 100f * effectLevel);
            stats.getAcceleration().modifyFlat(id, 1000f * effectLevel);
            stats.getDeceleration().modifyFlat(id, 1000f * effectLevel);
            stats.getTurnAcceleration().modifyFlat(id, 300f * effectLevel);
            stats.getMaxTurnRate().modifyMult(id, 1f + 2f * effectLevel);
        }

        ship.setJitterUnder(ship, SHIMMER_COLOR, 1f, 6, 2f, 6f);



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
