package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.tahlan_ModPlugin;
import data.scripts.util.MagicIncompatibleHullmods;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

import static data.scripts.utils.tahlan_txt.txt;

public class tahlan_HeavyConduits extends BaseHullMod {

	public static final float FLUX_RESISTANCE = 50f;
	public static final float VENT_RATE_BONUS = 50f;
	public static final float SUPPLIES_INCREASE = 100f;
    public static final float REPAIR_BONUS = 50f;

	private static final Set<String> BLOCKED_HULLMODS = new HashSet<>(1);

	private final String INNERLARGE = "graphics/tahlan/fx/tahlan_pinshield.png";
	private final String OUTERLARGE = "graphics/tahlan/fx/tahlan_tempshield_ring.png";

	static {
		BLOCKED_HULLMODS.add("fluxbreakers");
	}



    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		for (String tmp : BLOCKED_HULLMODS) {
			if (ship.getVariant().getHullMods().contains(tmp)) {
                MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(),tmp,"tahlan_heavyconduits");
			}
		}
        for (String tmp : tahlan_ModPlugin.SHIELD_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(),tmp,"tahlan_heavyconduits");
            }
        }
        if (ship.getShield() != null) {
			ship.getShield().setRadius(ship.getShieldRadiusEvenIfNoShield(), INNERLARGE, OUTERLARGE);
		}
	}
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getEmpDamageTakenMult().modifyMult(id, 1f - FLUX_RESISTANCE * 0.01f);
		stats.getVentRateMult().modifyPercent(id, VENT_RATE_BONUS);
        stats.getSuppliesPerMonth().modifyPercent(id, SUPPLIES_INCREASE);
        stats.getCRLossPerSecondPercent().modifyPercent(id, SUPPLIES_INCREASE);

        stats.getCombatEngineRepairTimeMult().modifyMult(id, 1f - REPAIR_BONUS*0.01f);
        stats.getCombatWeaponRepairTimeMult().modifyMult(id, 1f - REPAIR_BONUS*0.01f);
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) FLUX_RESISTANCE + txt("%");
		if (index == 1) return "" + (int) VENT_RATE_BONUS + txt("%");
        if (index == 2) return "" + (int) REPAIR_BONUS + txt("%");
		if (index == 3) return "" + (int) SUPPLIES_INCREASE + txt("%");
		if (index == 4) return txt("hmd_HeavyCond1");
		if (index == 5) return txt("hmd_HeavyCond2");
		return null;
	}


}