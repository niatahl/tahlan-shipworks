package data.scripts.hullmods;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.combat.DefenseUtils;

import static data.scripts.utils.tahlan_Utils.txt;

public class tahlan_DaemonArmor extends BaseHullMod {

    private static final float REGEN_PER_SEC_PERCENT = 10f;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!DefenseUtils.hasArmorDamage(ship)) return;
        if (ship.isHulk()) return;

        ArmorGridAPI armorGrid = ship.getArmorGrid();
        final float[][] grid = armorGrid.getGrid();
        final float max = armorGrid.getMaxArmorInCell();

        float repairAmount = max * (REGEN_PER_SEC_PERCENT / 100) * amount;

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
        return null;
    }
}
