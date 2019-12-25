package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class tahlan_StrongholdShieldStats extends BaseShipSystemScript {

    private static final float SHIELD_MULT = 0.1f;
    private static final float DISSIPATON_MULT = 1.2f;
    private static final float UNFOLD_MULT = 10f;
    private static final float ARC_BONUS = 360f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        /*
        ShipAPI ship = null;
        CombatEngineAPI engine = Global.getCombatEngine();

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }


        if (ship.getShield() != null) {
            if (ship.getShield().isOff()) {
                ship.getShield().toggleOn();
            }
        }*/

        stats.getShieldDamageTakenMult().modifyMult(id,SHIELD_MULT);
        stats.getFluxDissipation().modifyMult(id,DISSIPATON_MULT);
        stats.getShieldUnfoldRateMult().modifyMult(id,UNFOLD_MULT);
        stats.getShieldArcBonus().modifyFlat(id,ARC_BONUS);
        stats.getShieldUpkeepMult().modifyMult(id, 0f);

    }

    public void unapply(MutableShipStatsAPI stats, String id) {

        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getFluxDissipation().unmodify(id);
        stats.getShieldArcBonus().unmodify(id);
        stats.getShieldUnfoldRateMult().unmodify(id);
        stats.getShieldUpkeepMult().unmodify(id);

    }
}
