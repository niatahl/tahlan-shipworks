package data.scripts.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.vector.Vector2f;

import static data.scripts.TahlanUtils.Utils.txt;

public class tahlan_DaemonArmor extends BaseHullMod {

    private static final float ARMOR_CAP = 2000f;
    private static final float REGEN_PER_SEC_PERCENT = 10f;
    private static final float CALC_PERCENT = 50f;
    private static final float CALC_FLAT = 200f;

    private static final float DISUPTION_TIME = 2f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
//        stats.getEffectiveArmorBonus().modifyPercent(id,CALC_PERCENT);
        stats.getEffectiveArmorBonus().modifyFlat(id,CALC_FLAT);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.hasListenerOfClass(tahlan_DaemonArmorListener.class)) {
            ship.addListener(new tahlan_DaemonArmorListener());

        }

        if (!DefenseUtils.hasArmorDamage(ship)) {
//            ship.clearDamageDecals();
            return;
        }
        if (ship.isHulk()) return;
        if (ship.getFluxTracker().isVenting()) return;

        ship.getMutableStats().getDynamic().getStat("tahlan_daemonarmor").modifyFlat("nuller",-1);

        float timer = ship.getMutableStats().getDynamic().getStat("tahlan_daemonarmor").getModifiedValue() + amount;
        ship.getMutableStats().getDynamic().getStat("tahlan_daemonarmor").modifyFlat("tracker", timer);

        if (timer < DISUPTION_TIME) return;

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
        ship.syncWithArmorGridState();
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + Math.round(REGEN_PER_SEC_PERCENT) + txt("%");
        if (index == 1) return "" + Math.round(ARMOR_CAP/100*REGEN_PER_SEC_PERCENT) + "/s";
//        if (index == 2) return "" + Math.round(CALC_PERCENT) + txt("%");
        if (index == 2) return "" + Math.round(CALC_FLAT);
        if (index == 3) return txt("halved");
        if (index == 4) return txt("disabled");
        if (index == 5) return "" + Math.round(DISUPTION_TIME) + "s";
        return null;
    }

    static class tahlan_DaemonArmorListener implements DamageTakenModifier {
        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (shieldHit) return null;
            if (!(target instanceof ShipAPI)) return null;

            if (((ShipAPI) target).getVariant().hasHullMod("tahlan_daemonarmor") || ((ShipAPI) target).getVariant().hasHullMod("tahlan_daemonplating")) {
                if (damage.getDamage() > 0 ) {
                    ((ShipAPI) target).getMutableStats().getDynamic().getStat("tahlan_daemonarmor").unmodify("tracker");
                }
            }
            return null;
        }
    }

}
