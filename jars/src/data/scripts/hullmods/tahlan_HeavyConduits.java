package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class tahlan_HeavyConduits extends BaseHullMod {

	public static final float FLUX_RESISTANCE = 50f;
	public static final float VENT_RATE_BONUS = 50f;
	public static final float SUPPLIES_INCREASE = 100f;

	private static final Set<String> BLOCKED_HULLMODS = new HashSet<>(1);

	static {
		BLOCKED_HULLMODS.add("fluxbreakers");
	}



    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		for (String tmp : BLOCKED_HULLMODS) {
			if (ship.getVariant().getHullMods().contains(tmp)) {
				ship.getVariant().removeMod(tmp);
			}
		}
	}
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getEmpDamageTakenMult().modifyMult(id, 1f - FLUX_RESISTANCE * 0.01f);
		stats.getVentRateMult().modifyPercent(id, VENT_RATE_BONUS);
        stats.getSuppliesPerMonth().modifyPercent(id, SUPPLIES_INCREASE);
        stats.getCRLossPerSecondPercent().modifyPercent(id, SUPPLIES_INCREASE);
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) FLUX_RESISTANCE + "%";
		if (index == 1) return "" + (int) VENT_RATE_BONUS + "%";
		if (index == 2) return "" + (int) SUPPLIES_INCREASE + "%";
		if (index == 3) return "Resistant Flux Conduits";
		return null;
	}


}