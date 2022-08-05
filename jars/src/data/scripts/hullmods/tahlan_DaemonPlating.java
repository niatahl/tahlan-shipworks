package data.scripts.hullmods;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.combat.DefenseUtils;

import static data.scripts.utils.tahlan_Utils.txt;

public class tahlan_DaemonPlating extends BaseHullMod {

    private static final float ARMOR_MULT = 0.33f;
    private static final float CALC_PERCENT = 50f;
    private static final float ARMOR_CAP = 2000f;
    private static final float REGEN_PER_SEC_PERCENT = 4f;

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
        if (!DefenseUtils.hasArmorDamage(ship)) return;
        if (ship.isHulk()) return;
        if (ship.getFluxTracker().isVenting()) return;

        ArmorGridAPI armorGrid = ship.getArmorGrid();
        final float[][] grid = armorGrid.getGrid();
        final float max = armorGrid.getMaxArmorInCell();

        float statusMult = ship.getFluxTracker().isOverloaded() ? 0.5f : 1f;

        float baseCell = armorGrid.getMaxArmorInCell() * Math.min(ship.getHullSpec().getArmorRating(), ARMOR_CAP) / armorGrid.getArmorRating();
        float repairAmount = baseCell * (REGEN_PER_SEC_PERCENT / 100f) * statusMult * amount;

        // Iterate through all armor cells and find any that aren't at max
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[0].length; y++) {
                if (grid[x][y] < max) {
                    float regen = grid[x][y] + repairAmount;
                    armorGrid.setArmorValue(x, y, regen);
                }
            }
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + Math.round(REGEN_PER_SEC_PERCENT) + txt("%");
        if (index == 1) return "" + Math.round(ARMOR_CAP/100*REGEN_PER_SEC_PERCENT) + "/s";
        if (index == 2) return "" + Math.round(CALC_PERCENT) + txt("%");
        if (index == 3) return txt("halved");
        if (index == 4) return txt("disabled");
        if (index == 5) return "66"+txt("%");
        return null;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getArmorBonus().modifyMult(id, ARMOR_MULT);
        stats.getEffectiveArmorBonus().modifyPercent(id, CALC_PERCENT);
    }
}
