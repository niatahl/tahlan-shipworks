package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.tahlan_ModPlugin;

import static data.scripts.hullmods.tahlan_KnightRefit.*;


public class tahlan_ForcedOverdrive extends BaseHullMod {

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {

        switch (hullSize) {
            case FRIGATE:
                stats.getArmorBonus().modifyFlat(id, ARMOR_MALUS_FRIGATE);
                break;
            case DESTROYER:
                stats.getArmorBonus().modifyFlat(id, ARMOR_MALUS_DESTROYER);
                break;
            case CRUISER:
                stats.getArmorBonus().modifyFlat(id, ARMOR_MALUS_CRUISER);
                break;
            case CAPITAL_SHIP:
                stats.getArmorBonus().modifyFlat(id, ARMOR_MALUS_CAPITAL);
        }

        stats.getShieldDamageTakenMult().modifyMult(id, 0f);
        stats.getShieldUpkeepMult().modifyMult(id, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.setShield(ShieldType.NONE,0,0,0);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        boolean canBeApplied = ship.getVariant().hasHullMod("tahlan_knightrefit");

        if ( ship.getHullSpec().getDefenseType() == ShieldType.PHASE || ship.getHullSpec().getDefenseType() == ShieldType.NONE ) {
            canBeApplied = false;
        }

        for (String s : ship.getVariant().getHullMods()) {
            if (tahlan_ModPlugin.SHIELD_HULLMODS.contains(s) && !s.equals("tahlan_forcedoverdrive")) {
                canBeApplied = false;
                break;
            }
        }
        return canBeApplied;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (!ship.getVariant().hasHullMod("tahlan_knightrefit")) {
            return "Only applicable to Kassadari refits";
        }

        if ( ship.getHullSpec().getDefenseType() == ShieldType.PHASE || ship.getHullSpec().getDefenseType() == ShieldType.NONE ) {
            return "Ship has no shield";
        }

        for (String s : ship.getVariant().getHullMods()) {
                if (tahlan_ModPlugin.SHIELD_HULLMODS.contains(s) && !s.equals("tahlan_forcedoverdrive")) {
                    return "Incompatible with shield-related hullmods";
                }
        }
        return null;
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "permanent Overdrive activation";
        if (index == 1) return "negate the Kassadari Engineering armor reduction";
        return null;
    }
}
