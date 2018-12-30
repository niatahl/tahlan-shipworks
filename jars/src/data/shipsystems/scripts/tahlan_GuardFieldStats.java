package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class tahlan_GuardFieldStats extends BaseShipSystemScript {

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        stats.getShieldDamageTakenMult().modifyMult(id, 1f - 1f * effectLevel);

        stats.getShieldUpkeepMult().modifyMult(id, 0f);

        stats.getShieldUnfoldRateMult().modifyMult(id,10f);

        stats.getMaxSpeed().modifyFlat(id, 50f);
        stats.getAcceleration().modifyFlat(id, 200f);

        //System.out.println("level: " + effectLevel);
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getShieldAbsorptionMult().unmodify(id);
        stats.getShieldArcBonus().unmodify(id);
        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getShieldTurnRateMult().unmodify(id);
        stats.getShieldUnfoldRateMult().unmodify(id);
        stats.getShieldUpkeepMult().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("shield impenetrable, engines boosted", false);
        }
//		else if (index == 1) {
//			return new StatusData("shield upkeep reduced to 0", false);
//		} else if (index == 2) {
//			return new StatusData("shield upkeep reduced to 0", false);
//		}
        return null;
    }
}
