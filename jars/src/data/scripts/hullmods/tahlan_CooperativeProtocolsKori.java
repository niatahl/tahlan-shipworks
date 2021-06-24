//By Nicke535, a hullmod which buffs the ship if another specific ship is nearby
package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.lazywizard.lazylib.combat.CombatUtils;

import static data.scripts.utils.tahlan_Utils.txt;

public class tahlan_CooperativeProtocolsKori extends BaseHullMod {
    //General hullmod stats
    public static final float AURA_RANGE = 2000f;
    public static final String ID_FOR_BONUS = "tahlan_CooperativeProtocolsID";

    //Specific stat bonuses
    public static final float ECM_BONUS = 0.5f;
    public static final float SELF_ECM_REDUCTION_MULT = 0.5f;
    public static final float RANGE_BONUS_PERCENTAGE = 20f;

    //Handles all in-combat effects
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Nothing should happen if we are paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //Finds how many Korikazes are in range of our aura (only count up to our max, after that they don't matter)
        boolean hasYuki = false;
        for (ShipAPI testShip : CombatUtils.getShipsWithinRange(ship.getLocation(), AURA_RANGE)) {
            if (testShip.getVariant().getHullMods().contains("tahlan_cooperative_protocols_yuki") && !testShip.isHulk() && testShip.getOwner() == ship.getOwner()) {
                hasYuki = true;
                break;
            }
        }

        //Applies our bonuses if we have a Yukikaze in range
        if (!hasYuki) {
            ship.getMutableStats().getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).unmodify(ID_FOR_BONUS);
            ship.getMutableStats().getDynamic().getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).unmodify(ID_FOR_BONUS);
            ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify(ID_FOR_BONUS);
            ship.getMutableStats().getEnergyWeaponRangeBonus().unmodify(ID_FOR_BONUS);
            return;
        }

        ship.getMutableStats().getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyMult(ID_FOR_BONUS, 1f + ECM_BONUS);
        ship.getMutableStats().getDynamic().getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).modifyMult(ID_FOR_BONUS, SELF_ECM_REDUCTION_MULT);
        ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent(ID_FOR_BONUS, RANGE_BONUS_PERCENTAGE );
        ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent(ID_FOR_BONUS, RANGE_BONUS_PERCENTAGE);

        //If we are the player ship, display a tooltip
        if (ship == Global.getCombatEngine().getPlayerShip()) {
            Global.getCombatEngine().maintainStatusForPlayerShip(ID_FOR_BONUS + "_TOOLTIP", "graphics/sylphon/icons/hullsys/cooperative_protocols.png", "Cooperative Protocols", "Stats increased", false);
        }
    }

    //Prevents the hullmod from being put on ships
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    //Adds the description strings
    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + (int)(AURA_RANGE);
        if (index == 1) return "" + (int)(SELF_ECM_REDUCTION_MULT * 100f) + txt("%");
        if (index == 2) return "" + (int)(ECM_BONUS * 100f) + txt("%");
        if (index == 3) return "" + (int)(RANGE_BONUS_PERCENTAGE) + txt("%");
        return null;
    }
}