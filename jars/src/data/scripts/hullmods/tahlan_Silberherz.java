package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class tahlan_Silberherz extends BaseHullMod {

    private static final String SILBER_ID = "Silberherz_ID";
    private static final float DEBUFF_FACTOR = 0.7f;
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
	        if (!(ship.getCaptain().getStats().getLevel() >= 10)) {
	            ship.getMutableStats().getFluxDissipation().modifyMult(SILBER_ID,DEBUFF_FACTOR);
                ship.getMutableStats().getFluxCapacity().modifyMult(SILBER_ID,DEBUFF_FACTOR);
	            ship.getMutableStats().getMaxSpeed().modifyMult(SILBER_ID,DEBUFF_FACTOR);
	            ship.getMutableStats().getAcceleration().modifyMult(SILBER_ID,DEBUFF_FACTOR);
	            ship.getMutableStats().getAutofireAimAccuracy().modifyMult(SILBER_ID,DEBUFF_FACTOR);

                if (player) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(SILBER_ID, "graphics/icons/hullsys/entropy_amplifier.png", "Inexperienced Captain", "Regaliy performance reduced", false);
                }

            } else {
                ship.getMutableStats().getFluxDissipation().unmodify(SILBER_ID);
                ship.getMutableStats().getFluxCapacity().unmodify(SILBER_ID);
                ship.getMutableStats().getMaxSpeed().unmodify(SILBER_ID);
                ship.getMutableStats().getAcceleration().unmodify(SILBER_ID);
                ship.getMutableStats().getAutofireAimAccuracy().unmodify(SILBER_ID);
            }
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
            member.getStats().getSuppliesPerMonth().modifyMult(SILBER_ID,3f);
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
		if (index == 0) return "" + (int)PD_PERCENT + "%";
		if (index == 1) return "" + (int)WEAPON_HP + "%";
		if (index == 2) return "Level 10";
		if (index == 3) return "" + (int)((1f-DEBUFF_FACTOR)*100f) + "%";
		if (index == 4) return "tripled";
		if (index == 5) return "doubled";
		if (index == 6) return "Regalia Gantry";
		if (index == 7) return "Incompatible with Safety Overrides";
		return null;
	}
	

}
