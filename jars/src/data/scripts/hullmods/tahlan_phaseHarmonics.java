package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.util.C;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static data.scripts.utils.tahlan_Utils.txt;

public class tahlan_phaseHarmonics extends BaseHullMod {

    private static final String ID = "tahlan_phaseHarmonics";
    private static final float VENT_MOD = 1.2f;
    private static final float DAMAGE_MOD = 0.9f;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        ShipSystemAPI system = ship.getSystem();
        MutableShipStatsAPI stats = ship.getMutableStats();
        if (system == null) return;

        if (system.getAmmo() == 0) {
            stats.getVentRateMult().modifyMult(ID, VENT_MOD);
            stats.getShieldDamageTakenMult().modifyMult(ID, DAMAGE_MOD);
            stats.getArmorDamageTakenMult().modifyMult(ID, DAMAGE_MOD);
            stats.getHullDamageTakenMult().modifyMult(ID, DAMAGE_MOD);
            stats.getEmpDamageTakenMult().modifyMult(ID, DAMAGE_MOD);
        } else {
            stats.getVentRateMult().unmodify(ID);
            stats.getShieldDamageTakenMult().unmodify(ID);
            stats.getArmorDamageTakenMult().unmodify(ID);
            stats.getHullDamageTakenMult().unmodify(ID);
            stats.getEmpDamageTakenMult().unmodify(ID);
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return txt("phaseBreaker");
        if (index == 1) return "" + Math.round((1f - DAMAGE_MOD) * 100f) + txt("%");
        if (index == 2) return "" + Math.round((VENT_MOD - 1f) * 100f) + txt("%");
        return null;
    }
}
