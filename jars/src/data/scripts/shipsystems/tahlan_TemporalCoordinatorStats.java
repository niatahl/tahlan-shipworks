package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;

import static com.fs.starfarer.api.impl.combat.RecallDeviceStats.getFighters;

public class tahlan_TemporalCoordinatorStats extends BaseShipSystemScript {

    private static final Color JITTER_COLOR = new Color(141, 255, 28, 55);
    private static final Color JITTER_UNDER_COLOR = new Color(141, 255, 28, 105);
    private static final Color ENGINE_COLOR = new Color(141, 255, 28, 155);
    private static final Object KEY_JITTER = new Object();
    private static final float TIME_MULT = 1.5f;
    private static final float DAMAGE_MULT = 1.5f;


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        boolean player = false;

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
        } else {
            return;
        }

        stats.getTimeMult().modifyMult(id, TIME_MULT);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, (1f / TIME_MULT));
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

        //stats.getEnergyWeaponDamageMult().modifyMult(id,DAMAGE_MULT);
        //stats.getBallisticWeaponDamageMult().modifyMult(id,DAMAGE_MULT);

        if (effectLevel > 0) {
            float maxRangeBonus = 5f;
            float jitterRangeBonus = effectLevel * maxRangeBonus;
            for (ShipAPI fighter : getFighters(ship)) {
                if (fighter.isHulk()) continue;
                MutableShipStatsAPI fStats = fighter.getMutableStats();

                fStats.getTimeMult().modifyMult(id,TIME_MULT);

                fighter.getEngineController().fadeToOtherColor(this, ENGINE_COLOR, null, effectLevel, 0.5f);

                fighter.setJitterUnder(KEY_JITTER, JITTER_COLOR, effectLevel, 5, 0f, jitterRangeBonus);
                fighter.setJitter(KEY_JITTER, JITTER_UNDER_COLOR, effectLevel, 2, 0f, 0 + jitterRangeBonus * 1f);
                Global.getSoundPlayer().playLoop("system_targeting_feed_loop", ship, 1f, 1f, fighter.getLocation(), fighter.getVelocity());

            }
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);
        //stats.getBallisticWeaponDamageMult().unmodify(id);
        //stats.getEnergyWeaponDamageMult().unmodify(id);

        for (ShipAPI fighter : getFighters(ship)) {
            if (fighter.isHulk()) continue;
            MutableShipStatsAPI fStats = fighter.getMutableStats();
            fStats.getTimeMult().unmodify(id);
        }

    }
}
