package org.niatahl.tahlan.hullmods;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.DefenseUtils;

import static org.niatahl.tahlan.utils.Utils.txt;

public class DaemonPlating extends BaseHullMod {

    private static final float ARMOR_MULT = 0.3014275134f;
    private static final float ARMOR_MULT_SMOD = 0.49815742465f;
    private static final float CALC_FLAT = 200f;
    private static final float ARMOR_CAP = 2000f;
    private static final float REGEN_PER_SEC_PERCENT = 5f;

    private static final float REGEN_PER_SEC_PERCENT_SMOD = 3f;
    private static final float DISUPTION_TIME = 2f;

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return (!ship.getVariant().hasHullMod("tahlan_daemonarmor") && !ship.getVariant().hasHullMod("tahlan_heavyconduits"));
    }

    @Override
    public String getCanNotBeInstalledNowReason(ShipAPI ship, MarketAPI marketOrNull, CampaignUIAPI.CoreUITradeMode mode) {
        if (ship.getVariant().hasHullMod("tahlan_daemonarmor")) return "Already equipped with Hel Carapace";
        if (ship.getVariant().hasHullMod("tahlan_heavyconduits")) return "Incompatible with LosTech";
        return null;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.hasListenerOfClass(DaemonArmor.DaemonArmorListener.class)) {
            ship.addListener(new DaemonArmor.DaemonArmorListener());
        }


        if (!DefenseUtils.hasArmorDamage(ship)) {
//            ship.clearDamageDecals();
            return;
        }

        if (ship.isHulk()) return;
        if (ship.getFluxTracker().isVenting()) return;

        ship.getMutableStats().getDynamic().getStat("tahlan_daemonarmor").modifyFlat("nuller", -1);

        float timer = ship.getMutableStats().getDynamic().getStat("tahlan_daemonarmor").getModifiedValue() + amount;
        ship.getMutableStats().getDynamic().getStat("tahlan_daemonarmor").modifyFlat("tracker", timer);

        if (timer < DISUPTION_TIME) return;

        ArmorGridAPI armorGrid = ship.getArmorGrid();
        final float[][] grid = armorGrid.getGrid();
        final float max = armorGrid.getMaxArmorInCell();

        float statusMult = ship.getFluxTracker().isOverloaded() ? 0.5f : 1f;

        float regenPercent = REGEN_PER_SEC_PERCENT;
        if (ship.getVariant().getSMods().contains("tahlan_daemonplating") || ship.getVariant().getHullSpec().isBuiltInMod("tahlan_daemonplating")) {
            regenPercent = REGEN_PER_SEC_PERCENT_SMOD;
        }
        float baseCell = armorGrid.getMaxArmorInCell() * Math.min(ship.getHullSpec().getArmorRating(), ARMOR_CAP) / armorGrid.getArmorRating();
        float repairAmount = baseCell * (regenPercent / 100f) * statusMult * amount;

        // Iterate through all armor cells and find any that aren't at max
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[0].length; y++) {
                if (grid[x][y] < max) {
                    float regen = grid[x][y] + repairAmount;
                    armorGrid.setArmorValue(x, y, regen);
                }
            }
        }
        ship.syncWithArmorGridState();


    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (isForModSpec) {
            tooltip.addPara(txt("daemonPlatingSmod"), 10f, Misc.getGrayColor(), Misc.getHighlightColor(), "" + Math.round((1f-ARMOR_MULT_SMOD)*100f) + txt("%"), "" + Math.round(REGEN_PER_SEC_PERCENT_SMOD) + txt("%"));
            return;
        } else if (ship.getVariant().getSMods().contains("tahlan_daemonplating") || ship.getHullSpec().isBuiltInMod("tahlan_daemonplating")) {
            tooltip.addPara(txt("daemonPlatingSmod"), 10f, Misc.getPositiveHighlightColor(), Misc.getHighlightColor(), "" + Math.round((1f-ARMOR_MULT_SMOD)*100f) + txt("%"), "" + Math.round(REGEN_PER_SEC_PERCENT_SMOD) + txt("%"));
        } else {
            tooltip.addPara(txt("daemonPlatingSmod"), 10f, Misc.getGrayColor(), Misc.getHighlightColor(), "" + Math.round((1f-ARMOR_MULT_SMOD)*100f) + txt("%"), "" + Math.round(REGEN_PER_SEC_PERCENT_SMOD) + txt("%"));
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + Math.round(REGEN_PER_SEC_PERCENT) + txt("%");
        if (index == 1) return "" + Math.round(ARMOR_CAP / 100 * REGEN_PER_SEC_PERCENT) + "/s";
        if (index == 2) return "" + Math.round(CALC_FLAT);
        if (index == 3) return txt("halved");
        if (index == 4) return txt("disabled");
        if (index == 5) return "" + Math.round(DISUPTION_TIME) + "s";
        if (index == 6) return "" + Math.round((1f-ARMOR_MULT)*100f) + txt("%");
        if (index == 7) return txt("heavyarmor");
        return null;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getVariant().getSMods().contains("tahlan_daemonplating") || stats.getVariant().getHullSpec().isBuiltInMod("tahlan_daemonplating")) {
            stats.getArmorBonus().modifyMult(id, ARMOR_MULT_SMOD);
        } else {
            stats.getArmorBonus().modifyMult(id, ARMOR_MULT);
        }
        stats.getEffectiveArmorBonus().modifyFlat(id, CALC_FLAT);
    }
}
