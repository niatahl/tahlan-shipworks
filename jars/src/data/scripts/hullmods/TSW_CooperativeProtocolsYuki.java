//By Nicke535, a hullmod which buffs the ship depending on how many of another specific ship is nearby
package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.lazywizard.lazylib.combat.CombatUtils;

public class TSW_CooperativeProtocolsYuki extends BaseHullMod {
    //General hullmod stats
    public static final float AURA_RANGE = 2000f;
    public static final int MAX_NUMBER_OF_KORIS = 2;
    public static final float BONUS_EFFECT_FOR_MAX_KORIS = 0.5f;
    public static final String ID_FOR_BONUS = "TSW_CooperativeProtocolsID";

    //Specific stat bonuses
    public static final float ECM_BONUS = 0.66f;
    public static final float AUTOFIRE_ACCURACY_BONUS = 0.4f;
    public static final float SPEED_BONUS_PERCENTAGE = 10f;

    //Handles all in-combat effects
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Nothing should happen if we are paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //Finds how many Korikazes are in range of our aura (only count up to our max, after that they don't matter)
        int numberOfKoris = 0;
        for (ShipAPI testShip : CombatUtils.getShipsWithinRange(ship.getLocation(), AURA_RANGE)) {
            if (testShip.getVariant().getHullMods().contains("TSW_cooperative_protocols_kori") && !testShip.isHulk() && testShip.getOwner() == ship.getOwner()) {
                numberOfKoris++;
            }

            if (numberOfKoris >= MAX_NUMBER_OF_KORIS) {
                break;
            }
        }

        //Applies our bonuses if we have any Korikaze in range; if we have enough to reach the maximum, improve these bonuses further
        if (numberOfKoris <= 0) {

            ship.getMutableStats().getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).unmodify(ID_FOR_BONUS);
            ship.getMutableStats().getAutofireAimAccuracy().unmodify(ID_FOR_BONUS);
            ship.getMutableStats().getMaxSpeed().unmodify(ID_FOR_BONUS);
            ship.getMutableStats().getAcceleration().unmodify(ID_FOR_BONUS);
            ship.getMutableStats().getDeceleration().unmodify(ID_FOR_BONUS);
            return;
        }
        float effectMult = 1f;
        if (numberOfKoris >= MAX_NUMBER_OF_KORIS) {
            effectMult += BONUS_EFFECT_FOR_MAX_KORIS;
        }

        ship.getMutableStats().getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyMult(ID_FOR_BONUS, 1f + (ECM_BONUS * effectMult));
        ship.getMutableStats().getAutofireAimAccuracy().modifyMult(ID_FOR_BONUS, 1f + (AUTOFIRE_ACCURACY_BONUS * effectMult));
        ship.getMutableStats().getMaxSpeed().modifyPercent(ID_FOR_BONUS, SPEED_BONUS_PERCENTAGE * effectMult);
        ship.getMutableStats().getAcceleration().modifyPercent(ID_FOR_BONUS, SPEED_BONUS_PERCENTAGE * effectMult);
        ship.getMutableStats().getDeceleration().modifyPercent(ID_FOR_BONUS, SPEED_BONUS_PERCENTAGE * effectMult);

        //If we are the player ship, display a tooltip depending on if we have "saturated" our bonus or not
        if (ship == Global.getCombatEngine().getPlayerShip()) {
            if (numberOfKoris < MAX_NUMBER_OF_KORIS) {
                Global.getCombatEngine().maintainStatusForPlayerShip(ID_FOR_BONUS + "_TOOLTIP", "graphics/sylphon/icons/hullsys/cooperative_protocols.png", "Cooperative Protocols", "Stats increased", false);
            } else {
                Global.getCombatEngine().maintainStatusForPlayerShip(ID_FOR_BONUS + "_TOOLTIP", "graphics/sylphon/icons/hullsys/cooperative_protocols_multi.png", "Cooperative Protocols : Saturated", "Stats further increased", false);
            }
        }
    }

    //Prevents the hullmod from being put on ships
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        boolean canBeApplied = false;
        return canBeApplied;
    }

    //Adds the description strings
    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + (int)(AURA_RANGE);
        if (index == 1) return "" + (int)(ECM_BONUS * 100f) + "%";
        if (index == 2) return "" + (int)(AUTOFIRE_ACCURACY_BONUS * 100f) + "%";
        if (index == 3) return "" + (int)(SPEED_BONUS_PERCENTAGE) + "%";
        if (index == 4) return "" + MAX_NUMBER_OF_KORIS;
        if (index == 5) return "" + (int)(BONUS_EFFECT_FOR_MAX_KORIS * 100f) + "%";
        return null;
    }
}