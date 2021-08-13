package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class tahlan_BoostThrusterStats extends BaseShipSystemScript {

    private static final float MAX_SPEED_BONUS = 80f;
    private static final float MAX_SPEED_PENALTY = 60f;

    private float penalty;

    private IntervalUtil interval = new IntervalUtil(0.01f,0.01f);

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship;
        CombatEngineAPI engine = Global.getCombatEngine();

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        float actualMaxPenalty = MAX_SPEED_PENALTY;
        if (ship.getCaptain() != null) {
            if (ship.getCaptain().getStats().getSkillLevel(Skills.SYSTEMS_EXPERTISE) > 0) {
                actualMaxPenalty = MAX_SPEED_PENALTY - 10f;
            }
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
            penalty = ((reverseScale - 1f) * actualMaxPenalty)*effectLevel;
        } else {
            /* Going forwards */
            penalty = reverseScale * MAX_SPEED_BONUS * effectLevel;
        }

        stats.getMaxSpeed().modifyFlat(id, (MAX_SPEED_BONUS-penalty) * effectLevel);
        stats.getAcceleration().modifyMult(id, 1f + effectLevel);
        stats.getDeceleration().modifyMult(id, 1f + effectLevel);
        stats.getTurnAcceleration().modifyFlat(id, 15f * effectLevel);
        stats.getTurnAcceleration().modifyPercent(id, 100f * effectLevel);
        stats.getMaxTurnRate().modifyFlat(id, 10f);
        stats.getMaxTurnRate().modifyPercent(id, 50f);

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
