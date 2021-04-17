package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

public class tahlan_RaidestJetsStats extends BaseShipSystemScript {

    private static final float MAX_SPEED_BONUS = 300f;
    private static final float MAX_SPEED_PENALTY = 200f;

    private float penalty;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = null;
        CombatEngineAPI engine = Global.getCombatEngine();

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        //Some code magic from DR
        float forwardDir = ship.getFacing();
        float currDir = VectorUtils.getFacing(ship.getVelocity());
        float reverseScale = Math.abs(MathUtils.getShortestRotation(currDir, forwardDir) / 90f);
        if (ship.getVelocity().length() < 1f) {
            reverseScale = 0f;
        }
        if (reverseScale > 1f) {
            /* Going backwards */
            penalty = ((reverseScale - 1f) * MAX_SPEED_PENALTY)*effectLevel;
        } else {
            /* Going forwards */
            penalty = reverseScale * MAX_SPEED_BONUS * effectLevel;
        }

        stats.getMaxSpeed().modifyFlat(id, (MAX_SPEED_BONUS-penalty) * effectLevel);
        stats.getAcceleration().modifyPercent(id, 400f + 400f * effectLevel);
        stats.getDeceleration().modifyPercent(id, 400f + 400f* effectLevel);
        stats.getTurnAcceleration().modifyMult(id, 1f + 2f * effectLevel);
        stats.getMaxTurnRate().modifyMult(id, 1f + effectLevel);



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
            return new StatusData("Speed increased by " + Math.round((MAX_SPEED_BONUS-penalty)*effectLevel) + "su", false);
        }
        return null;
    }

}
