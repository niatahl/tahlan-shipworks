package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class tahlan_NavigationFeedStats extends BaseShipSystemScript {

    private static final float MOBILITY_BOOST = 1.5f;
    private static final Object KEY_JITTER = new Object();
    private static final Color ENGINE_COLOR = new Color(255, 0, 0, 150);
    private static final Color JITTER_UNDER_COLOR = new Color(255, 50, 0, 125);
    private static final Color JITTER_COLOR = new Color(255, 50, 0, 75);

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }


        if (effectLevel > 0) {
            float maxRangeBonus = 5f;
            float jitterRangeBonus = effectLevel * maxRangeBonus;
            for (ShipAPI fighter : getFighters(ship)) {
                if (fighter.isHulk()) continue;
                MutableShipStatsAPI fStats = fighter.getMutableStats();

                fStats.getAcceleration().modifyMult(id, MOBILITY_BOOST);
                fStats.getTurnAcceleration().modifyMult(id, MOBILITY_BOOST);
                fStats.getDeceleration().modifyMult(id, MOBILITY_BOOST);
                fStats.getMaxTurnRate().modifyMult(id, MOBILITY_BOOST);
                fStats.getMaxSpeed().modifyMult(id, MOBILITY_BOOST);

                fighter.getEngineController().fadeToOtherColor(this, ENGINE_COLOR, null, effectLevel, 0.5f);

                fighter.setJitterUnder(KEY_JITTER, JITTER_COLOR, effectLevel, 5, 0f, jitterRangeBonus);
                fighter.setJitter(KEY_JITTER, JITTER_UNDER_COLOR, effectLevel, 2, 0f, 0 + jitterRangeBonus * 1f);
                Global.getSoundPlayer().playLoop("system_targeting_feed_loop", ship, 1f, 1f, fighter.getLocation(), fighter.getVelocity());

            }
        }
    }

    private List<ShipAPI> getFighters(ShipAPI carrier) {
        List<ShipAPI> result = new ArrayList<ShipAPI>();

        for (ShipAPI ship : Global.getCombatEngine().getShips()) {
            if (!ship.isFighter()) continue;
            if (ship.getWing() == null) continue;
            if (ship.getWing().getSourceShip() == carrier) {
                result.add(ship);
            }
        }

        return result;
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }
        for (ShipAPI fighter : getFighters(ship)) {
            if (fighter.isHulk()) continue;
            MutableShipStatsAPI fStats = fighter.getMutableStats();
            fStats.getAcceleration().unmodify(id);
            fStats.getTurnAcceleration().unmodify(id);
            fStats.getDeceleration().unmodify(id);
            fStats.getMaxTurnRate().unmodify(id);
            fStats.getMaxSpeed().unmodify(id);
        }

    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("Fighter mobility boosted by 50%", false);
        }
        return null;
    }
}
