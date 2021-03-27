package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

import java.util.HashSet;
import java.util.Set;

import static data.scripts.utils.tahlan_txt.txt;

public class tahlan_Silberherz extends BaseHullMod {

    private static final String SILBER_ID = "Silberherz_ID";
    private static final float BUFF_FACTOR = 1.2f;
    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>(1);

    static {
        BLOCKED_HULLMODS.add("safetyoverrides");
    }
	
	private static final float PD_PERCENT = 100f;
	private static final float WEAPON_HP = 100f;
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

	    stats.getDamageToFighters().modifyPercent(id,PD_PERCENT);
	    stats.getDamageToMissiles().modifyPercent(id,PD_PERCENT);
	    stats.getWeaponHealthBonus().modifyPercent(id,WEAPON_HP);

	}

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                ship.getVariant().removeMod(tmp);
            }
        }
    }

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {

        boolean player = ship == Global.getCombatEngine().getPlayerShip();

	    if (ship.getCaptain() != null) {
            PersonAPI captain = ship.getCaptain();


	        if ((captain.getStats().getLevel() > 4)) {

	            ship.getMutableStats().getFluxDissipation().modifyMult(SILBER_ID, BUFF_FACTOR);
                ship.getMutableStats().getFluxCapacity().modifyMult(SILBER_ID, BUFF_FACTOR);
	            ship.getMutableStats().getMaxSpeed().modifyMult(SILBER_ID, BUFF_FACTOR);
	            ship.getMutableStats().getAcceleration().modifyMult(SILBER_ID, BUFF_FACTOR);
	            ship.getMutableStats().getAutofireAimAccuracy().modifyMult(SILBER_ID, BUFF_FACTOR);
	            ship.getMutableStats().getShieldDamageTakenMult().modifyMult(SILBER_ID,1/ BUFF_FACTOR);

                if (player) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(SILBER_ID, "graphics/icons/hullsys/entropy_amplifier.png", txt("hmd_silberherz8"), txt("hmd_silberherz9"), false);
                }

            } else {
                ship.getMutableStats().getFluxDissipation().unmodify(SILBER_ID);
                ship.getMutableStats().getFluxCapacity().unmodify(SILBER_ID);
                ship.getMutableStats().getMaxSpeed().unmodify(SILBER_ID);
                ship.getMutableStats().getAcceleration().unmodify(SILBER_ID);
                ship.getMutableStats().getAutofireAimAccuracy().unmodify(SILBER_ID);
                ship.getMutableStats().getShieldDamageTakenMult().unmodify(SILBER_ID);
            }

//            if (captain.getStats().getSkillLevel(Skills.GUNNERY_IMPLANTS) == 3) {
//	            ship.getMutableStats().getBallisticWeaponRangeBonus().modifyMult(SILBER_ID,1.1f);
//	            ship.getMutableStats().getEnergyWeaponRangeBonus().modifyMult(SILBER_ID, 1.1f);
//	            ship.getMutableStats().getVentRateMult().modifyMult(SILBER_ID, 1.1f);
//            } else {
//                ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify(SILBER_ID);
//                ship.getMutableStats().getEnergyWeaponRangeBonus().unmodify(SILBER_ID);
//                ship.getMutableStats().getVentRateMult().unmodify(SILBER_ID);
//            }

        }

	}

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {

	    boolean hasGantry = false;

        for (FleetMemberAPI ship: member.getFleetData().getMembersListCopy()) {
            if (ship.getVariant().hasHullMod("tahlan_regaliagantry")) {
                hasGantry = true;
            }
        }

        if (!hasGantry) {
            member.getStats().getSuppliesPerMonth().modifyMult(SILBER_ID,2f);
            member.getStats().getBaseCRRecoveryRatePercentPerDay().modifyMult(SILBER_ID,0.5f);
        } else {
            member.getStats().getSuppliesPerMonth().unmodify(SILBER_ID);
            member.getStats().getBaseCRRecoveryRatePercentPerDay().unmodify(SILBER_ID);
        }
    }

    public boolean isApplicableToShip(ShipAPI ship) {
		return false;
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int)PD_PERCENT + txt("%");
		if (index == 1) return "" + (int)WEAPON_HP + txt("%");
		if (index == 2) return txt("hmd_silberherz1");
		if (index == 3) return "" + (int)((BUFF_FACTOR-1f)*100f) + txt("%");
		//if (index == 4) return txt("hmd_silberherz2");
		if (index == 4) return txt("hmd_silberherz3");
		if (index == 5) return txt("hmd_silberherz4");
		if (index == 6) return txt("hmd_silberherz5");
		if (index == 7) return txt("hmd_silberherz6");
		if (index == 8) return txt("hmd_silberherz7");
		return null;
	}
	

}
