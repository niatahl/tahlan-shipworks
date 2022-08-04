package data.scripts.hullmods;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.combat.DefenseUtils;

import static data.scripts.utils.tahlan_Utils.txt;

public class tahlan_DaemonArmor extends BaseHullMod {

    private static final float ARMOR_CAP = 2000f;
    private static final float REGEN_PER_SEC_PERCENT = 10f;
    private static final float CALC_PERCENT = 50f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getEffectiveArmorBonus().modifyPercent(id,CALC_PERCENT);
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

        float baseCell = armorGrid.getMaxArmorInCell() * Math.min(ship.getHullSpec().getArmorRating(),ARMOR_CAP) / armorGrid.getArmorRating();
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

    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + Math.round(REGEN_PER_SEC_PERCENT) + txt("%");
        if (index == 1) return "" + Math.round(ARMOR_CAP);
        if (index == 2) return "" + Math.round(CALC_PERCENT) + txt("%");
        if (index == 3) return txt("halved");
        if (index == 4) return txt("disabled");
        return null;
    }
}
