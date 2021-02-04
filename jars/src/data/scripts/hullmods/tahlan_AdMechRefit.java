package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static data.scripts.utils.tahlan_txt.txt;
import static java.lang.Math.round;

public class tahlan_AdMechRefit extends BaseHullMod {

	private static final float FLUX_MULT = 1.05f;
	private static final float ARMOR_BONUS = 100f;
	private static final float WEAPON_AMP = 0.2f;

	private final String INNERLARGE = "graphics/tahlan/fx/tahlan_tempshield.png";
	private final String MS_ID = "tahlan_AdMechRefitID";
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

		stats.getArmorBonus().modifyFlat(id,ARMOR_BONUS);
		stats.getFluxDissipation().modifyMult(id, FLUX_MULT);
		stats.getFluxCapacity().modifyMult(id, FLUX_MULT);

	}

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.getShield().setRadius(ship.getShieldRadiusEvenIfNoShield(), INNERLARGE, INNERLARGE);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        ship.getShield().setInnerColor(new Color(112+round(120*ship.getFluxLevel()), 27, 187-round(70*ship.getFluxLevel()), 123));
        ship.getShield().setRingColor(new Color(252, 222, 255,60));

        float power = Math.min((ship.getFluxLevel() / 0.90f), 1f);

        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult(MS_ID,1+WEAPON_AMP*power);
        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult(MS_ID,1+WEAPON_AMP*power);
        ship.getMutableStats().getEnergyRoFMult().modifyMult(MS_ID,1-WEAPON_AMP*power);
        ship.getMutableStats().getBallisticRoFMult().modifyMult(MS_ID,1-WEAPON_AMP*power);
    }

    public boolean isApplicableToShip(ShipAPI ship) {
		return false;
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + round((FLUX_MULT - 1f) * 100f) + txt("%");
		if (index == 1) return "" + (int) (ARMOR_BONUS);
		if (index == 2) return "" + round(WEAPON_AMP*100f) + txt("%");
		return null;
	}
	

}
