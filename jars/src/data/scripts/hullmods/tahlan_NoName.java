package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.awt.*;
import java.util.EnumSet;

public class tahlan_NoName extends BaseHullMod {


    public static final float SUPPLIES_MULT = 1.5f;

    public static final float TIME_MULT = 1.2f;
    private static final Color AFTERIMAGE_COLOR = new Color(133, 126, 116, 102);
    private static final float AFTERIMAGE_THRESHOLD = 0.1f;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        stats.getSuppliesPerMonth().modifyMult(id, SUPPLIES_MULT);
        stats.getDynamic().getStat(Stats.REPLACEMENT_RATE_DECREASE_MULT).modifyMult(id, 0f);

    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        //don't run while paused because duh
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //The Great Houses are actually timelords
        boolean player = ship == Global.getCombatEngine().getPlayerShip();
        String id = "tahlan_NoNameID";
        if (player) {
            ship.getMutableStats().getTimeMult().modifyMult(id, TIME_MULT);
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / TIME_MULT);
        } else {
            ship.getMutableStats().getTimeMult().modifyMult(id, TIME_MULT);
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

        ship.getMutableStats().getDynamic().getStat("tahlan_NNAfterimageTracker").modifyFlat("tahlan_NNAfterimageTrackerNullerID", -1);
        ship.getMutableStats().getDynamic().getStat("tahlan_NNAfterimageTracker").modifyFlat("tahlan_NNAfterimageTrackerID",
                ship.getMutableStats().getDynamic().getStat("tahlan_NNAfterimageTracker").getModifiedValue() + amount);
        if (ship.getMutableStats().getDynamic().getStat("tahlan_NNAfterimageTracker").getModifiedValue() > AFTERIMAGE_THRESHOLD) {
            ship.addAfterimage(
                    AFTERIMAGE_COLOR,
                    0, //X-location
                    0, //Y-location
                    ship.getVelocity().getX() * (-1f), //X-velocity
                    ship.getVelocity().getY() * (-1f), //Y-velocity
                    3f, //Maximum jitter
                    0.1f, //In duration
                    0f, //Mid duration
                    0.3f, //Out duration
                    true, //Additive blend?
                    false, //Combine with sprite color?
                    false //Above ship?
            );
            ship.getMutableStats().getDynamic().getStat("tahlan_NNAfterimageTracker").modifyFlat("tahlan_NNAfterimageTrackerID",
                    ship.getMutableStats().getDynamic().getStat("tahlan_NNAfterimageTracker").getModifiedValue() - AFTERIMAGE_THRESHOLD);
        }

    }


    //Built-in only
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "Temporal Circuit Grid";
        if (index == 1) return "" + Math.round((TIME_MULT - 1f) * 100f) + "%";
        if (index == 2) return "without replacement rate decreasing";
        if (index == 3) return "" + Math.round((SUPPLIES_MULT - 1f) * 100f) + "%";
        return null;
    }
}
