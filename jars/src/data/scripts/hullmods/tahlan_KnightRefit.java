package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;

import java.awt.*;
import java.util.EnumSet;

public class tahlan_KnightRefit extends BaseHullMod {


    public static final float ARMOR_MALUS_FRIGATE = 50f;
    public static final float ARMOR_MALUS_DESTROYER = 100f;
    public static final float ARMOR_MALUS_CRUISER = 200f;
    public static final float ARMOR_MALUS_CAPITAL = 300f;

    public static final float SUPPLIES_MULT = 1.5f;

    public static final float OVERDRIVE_TRIGGER_PERCENTAGE = 0.3f;
    public static final float OVERDRIVE_TIME_MULT = 1.3f;

    public static final float TIME_MULT = 1.1f;
    private static final Color AFTERIMAGE_COLOR = new Color(133, 126, 116, 102);
    private static final float AFTERIMAGE_THRESHOLD = 0.1f;

    private static final Color OVERDRIVE_ENGINE_COLOR = new Color(255, 44, 0);
    private static final Color OVERDRIVE_GLOW_COLOR = new Color(255, 120, 16);
    private static final Color OVERDRIVE_JITTER_COLOR = new Color(255, 63, 0, 50);
    private static final Color OVERDRIVE_JITTER_UNDER_COLOR = new Color(255, 63, 0, 100);

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        //Better flux stats
        //stats.getFluxCapacity().modifyMult(id,FLUX_MULT);
        //stats.getFluxDissipation().modifyMult(id,FLUX_MULT);

        switch (hullSize) {
            case FRIGATE:
                stats.getArmorBonus().modifyFlat(id, -ARMOR_MALUS_FRIGATE);
                break;
            case DESTROYER:
                stats.getArmorBonus().modifyFlat(id, -ARMOR_MALUS_DESTROYER);
                break;
            case CRUISER:
                stats.getArmorBonus().modifyFlat(id, -ARMOR_MALUS_CRUISER);
                break;
            case CAPITAL_SHIP:
                stats.getArmorBonus().modifyFlat(id, -ARMOR_MALUS_CAPITAL);
        }

        //stats.getMaxSpeed().modifyMult(id,HANDLING_MULT);
        //stats.getAcceleration().modifyMult(id,HANDLING_MULT);
        //stats.getDeceleration().modifyMult(id,HANDLING_MULT);
        //stats.getTurnAcceleration().modifyMult(id,HANDLING_MULT);

        stats.getSuppliesPerMonth().modifyMult(id, SUPPLIES_MULT);

    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        //don't run while paused because duh
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //The Great Houses are actually timelords
        boolean player = ship == Global.getCombatEngine().getPlayerShip();
        String id = "tahlan_KnightRefitID";
        if (ship.getHitpoints() <= ship.getMaxHitpoints() * OVERDRIVE_TRIGGER_PERCENTAGE || ship.getVariant().getHullMods().contains("tahlan_forcedoverdrive")) {

            if (player) {
                ship.getMutableStats().getTimeMult().modifyMult(id, OVERDRIVE_TIME_MULT);
                Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / OVERDRIVE_TIME_MULT);
            } else {
                ship.getMutableStats().getTimeMult().modifyMult(id, OVERDRIVE_TIME_MULT);
                Global.getCombatEngine().getTimeMult().unmodify(id);
            }

            EnumSet<WeaponType> WEAPON_TYPES = EnumSet.of(WeaponType.BALLISTIC, WeaponType.COMPOSITE, WeaponType.MISSILE);
            ship.setWeaponGlow(0.4f, OVERDRIVE_GLOW_COLOR, WEAPON_TYPES);

            ship.getEngineController().fadeToOtherColor(this, OVERDRIVE_ENGINE_COLOR, null, 1f, 0.7f);
            ship.setJitter(id, OVERDRIVE_JITTER_COLOR, 0.5f, 3, 5f);
            ship.setJitterUnder(id, OVERDRIVE_JITTER_UNDER_COLOR, 0.5f, 20, 10f);

        } else {

            if (player) {
                ship.getMutableStats().getTimeMult().modifyMult(id, TIME_MULT);
                Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / TIME_MULT);
            } else {
                ship.getMutableStats().getTimeMult().modifyMult(id, TIME_MULT);
                Global.getCombatEngine().getTimeMult().unmodify(id);
            }

        }

        ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").modifyFlat("tahlan_KRAfterimageTrackerNullerID", -1);
        ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").modifyFlat("tahlan_KRAfterimageTrackerID",
                ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").getModifiedValue() + amount);
        if (ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").getModifiedValue() > AFTERIMAGE_THRESHOLD) {
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
            ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").modifyFlat("tahlan_KRAfterimageTrackerID",
                    ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").getModifiedValue() - AFTERIMAGE_THRESHOLD);
        }

    }


    //Built-in only
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "Temporal Circuit Grid";
        if (index == 1) return "" + Math.round((TIME_MULT - 1f) * 100f) +"%";
        if (index == 2) return "" + (int) ARMOR_MALUS_FRIGATE + "/" + (int) ARMOR_MALUS_DESTROYER + "/" + (int) ARMOR_MALUS_CRUISER + "/" + (int) ARMOR_MALUS_CAPITAL;
        if (index == 3) return "" + Math.round((SUPPLIES_MULT - 1f) * 100f) + "%";
        if (index == 4) return "" + Math.round(OVERDRIVE_TRIGGER_PERCENTAGE * 100f) + "%";
        if (index == 5) return "" + Math.round((OVERDRIVE_TIME_MULT - 1f) * 100f) + "%";
        return null;
    }
}
