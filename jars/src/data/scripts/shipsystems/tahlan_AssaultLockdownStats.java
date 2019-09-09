package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class tahlan_AssaultLockdownStats extends BaseShipSystemScript {

    private static final float HANDLING_MULT = 0.2f;
    private static final float ROF_MULT = 1.25f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        stats.getMaxSpeed().modifyMult(id,HANDLING_MULT);
        stats.getAcceleration().modifyMult(id,HANDLING_MULT);
        stats.getDeceleration().modifyMult(id,HANDLING_MULT);
        stats.getBallisticRoFMult().modifyMult(id,ROF_MULT);

        stats.getEntity().getVelocity().scale(HANDLING_MULT);
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
    }


    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("Lockdown engaged, weapons supercharged", false);
        }
        return null;
    }
}
