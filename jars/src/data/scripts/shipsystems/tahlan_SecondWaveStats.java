package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;

import java.util.ArrayList;
import java.util.List;

public class tahlan_SecondWaveStats extends BaseShipSystemScript {

    public static float EXTRA_FIGHTER_DURATION = 10;
    public static float SPEED_BOOST = 100;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        if (effectLevel == 1) {
            for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                if (bay.getWing() == null) continue;

                bay.makeCurrentIntervalFast();
                FighterWingSpecAPI spec = bay.getWing().getSpec();

                int addForWing = spec.getNumFighters();
                int maxTotal = spec.getNumFighters() + addForWing;
                int actualAdd = maxTotal - bay.getWing().getWingMembers().size();
                actualAdd = Math.min(spec.getNumFighters(), actualAdd);
                if (actualAdd > 0) {
                    bay.setFastReplacements(bay.getFastReplacements() + addForWing);
                    bay.setExtraDeployments(actualAdd);
                    bay.setExtraDeploymentLimit(maxTotal);
                    bay.setExtraDuration(EXTRA_FIGHTER_DURATION);
                }
            }
        }

        if (state == State.OUT) {
            for (ShipAPI fighter : getFighters(ship)) {
                fighter.getMutableStats().getMaxSpeed().modifyFlat(id,SPEED_BOOST+effectLevel);
                fighter.getMutableStats().getAcceleration().modifyMult(id, 1f+effectLevel);
                fighter.getEngineController().extendFlame(this,1f+effectLevel,1f+effectLevel*0.5f,1f+effectLevel*0.5f);
            }
        }
    }

    public static List<ShipAPI> getFighters(ShipAPI carrier) {
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
            fighter.getMutableStats().getMaxSpeed().unmodify(id);
            fighter.getMutableStats().getAcceleration().unmodify(id);
        }
    }



    public StatusData getStatusData(int index, State state, float effectLevel) {
//		if (index == 0) {
//			return new StatusData("deploying additional fighters", false);
//		}
        return null;
    }


    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return true;
    }



}








