package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;

public class tahlan_SteamlightStats extends BaseShipSystemScript {

    private static final Color JITTER_COLOR = new Color(100, 200, 255, 30);
    private static final Color JITTER_UNDER_COLOR = new Color(100, 200, 255, 80);
    private static final Color ENGINE_COLOR = new Color(100, 200, 255, 220);
    private static final float TIME_MULT = 1.5f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = null;
        boolean player = false;

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
        } else {
            return;
        }

        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, (1f / (1f+(TIME_MULT-1f)*effectLevel)));
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

        stats.getMaxSpeed().modifyFlat(id, 100f * effectLevel);
        stats.getAcceleration().modifyPercent(id, 400f * effectLevel);
        stats.getDeceleration().modifyPercent(id, 400f * effectLevel);
        stats.getBallisticRoFMult().modifyMult(id, 1f + (1f * effectLevel));
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - (0.5f * effectLevel));

        ship.setJitter(id, JITTER_COLOR, 0.5f, 3, 5f);
        ship.setJitterUnder(id, JITTER_UNDER_COLOR, 0.7f, 20, 10f);

        ship.getEngineController().fadeToOtherColor(this,ENGINE_COLOR,null,effectLevel,1f);
        ship.getEngineController().extendFlame(this,1f*effectLevel,0.5f*effectLevel,0.5f*effectLevel);

    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getBallisticWeaponFluxCostMod().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
        Global.getCombatEngine().getTimeMult().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) return new StatusData("Steamlight boost active:", false);
        if (index == 1) return new StatusData("Ballistic Rate of Fire boosted", false);
        if (index == 2) return new StatusData("Mobility boosted", false);
        if (index == 3) return new StatusData("Perception of time slowed", false);
        return null;
    }

}
