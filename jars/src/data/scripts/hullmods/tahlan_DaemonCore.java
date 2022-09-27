package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static data.scripts.utils.tahlan_Utils.txt;

// There was some fun here. It was silly indeed.

public class tahlan_DaemonCore extends BaseHullMod {

    private static final Map<HullSize, Integer> MAG = new HashMap<>();

    static {
        MAG.put(HullSize.FRIGATE, 2);
        MAG.put(HullSize.DESTROYER, 1);
        MAG.put(HullSize.CRUISER, 0);
        MAG.put(HullSize.CAPITAL_SHIP, 0);
    }

    private static final float SUPPLIES_PERCENT = 100f;
    private static final float ACC_BUFF = 0.25f;
    private static final float MSSL_DAMAGE = 0.5f;
    private static final float SPEED_BUFF = 0.2f;
    private static final float SPEED_CAP = 0.6f;
    private static final float PLAYER_NERF = 0.9f;
    private static final Color JITTER_COLOR = new Color(255, 0, 0, 30);
    private static final Color JITTER_UNDER_COLOR = new Color(255, 0, 0, 80);

    private static final IntervalUtil kaboom = new IntervalUtil(1f, 10f);
    private static final String dc_id = "tahlan_daemoncore";

    private static final IntervalUtil yoinkTimer = new IntervalUtil(10f, 30f);

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        stats.getProjectileSpeedMult().modifyMult(id, 1f + ACC_BUFF);
        stats.getMaxRecoilMult().modifyMult(id, 1f - ACC_BUFF);
        stats.getRecoilDecayMult().modifyMult(id, 1f + ACC_BUFF);
        stats.getRecoilPerShotMult().modifyMult(id, 1f - ACC_BUFF);
        stats.getDamageToMissiles().modifyMult(id, 1f + MSSL_DAMAGE);

        stats.getSuppliesPerMonth().modifyPercent(id, SUPPLIES_PERCENT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getShield() != null) {
            String inner = "graphics/tahlan/fx/tahlan_shellshield.png";
            String outer = "graphics/tahlan/fx/tahlan_tempshield_ring.png";
            ship.getShield().setRadius(ship.getShieldRadiusEvenIfNoShield(), inner, outer);
        }


        if (Global.getSector().getPlayerFleet() == null) {
            return;
        }

        // Hackery to make the ships uniquely more potent while in Legio fleets in attempt to keep them more balanced in player hands
        boolean isPlayerFleet = false;
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (member.getVariant().getHullVariantId().equals(ship.getVariant().getHullVariantId())) {
                isPlayerFleet = true;
            }
        }

        if (ship.getVariant().hasHullMod("tahlan_daemonboost")) {
            ship.getVariant().removeMod("tahlan_daemonboost");
        }

        if (isPlayerFleet) {
            ship.getMutableStats().getTimeMult().modifyMult(id, PLAYER_NERF);
        } else {
            ship.getVariant().addMod("tahlan_daemonboost");
        }

    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }

        float speedBoost = 1f - Math.min(1f, (ship.getFluxLevel() / SPEED_CAP));
        ship.getMutableStats().getMaxSpeed().modifyMult(dc_id, 1f + (speedBoost * SPEED_BUFF));

        if (engine.getFleetManager(ship.getOwner()) == engine.getFleetManager(FleetSide.PLAYER)) {
            //Only run this in campaign context, not missions
            if (!engine.isInCampaign()) {
                return;
            }
            yoinkTimer.advance(amount);
            if (yoinkTimer.intervalElapsed()) {
                // Legio-owned Hel Scaiths can hijack enemy Daemons
                for (ShipAPI bote : CombatUtils.getShipsWithinRange(ship.getLocation(), 2000f)) {
                    if (bote.getHullSpec().getHullId().contains("tahlan_DunScaith_dmn")
                            && (Math.random() > 0.75f)
                            && bote.getFleetMember().getFleetCommander().getFaction().getId().contains("legioinfernalis")) {
                        engine.addFloatingText(ship.getLocation(), "ASSUMING DIRECT CONTROL", 40f, Color.RED, ship, 0.5f, 3f);
                        ship.setOwner(bote.getOwner());

                        // yoinked from Xhan
                        if (ship.getShipAI() != null) {

                            //cancel orders so the AI doesn't get confused
                            DeployedFleetMemberAPI member_a = Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getDeployedFleetMember(ship);
                            if (member_a != null)
                                Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getTaskManager(false).orderSearchAndDestroy(member_a, false);

                            DeployedFleetMemberAPI member_aa = Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getDeployedFleetMember(ship);
                            if (member_aa != null)
                                Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getTaskManager(true).orderSearchAndDestroy(member_aa, false);

                            DeployedFleetMemberAPI member_b = Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).getDeployedFleetMember(ship);
                            if (member_b != null)
                                Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).getTaskManager(false).orderSearchAndDestroy(member_b, false);

                            ship.getShipAI().forceCircumstanceEvaluation();
                        }
                    }
                }
            }
        } else {

            if (!ship.isAlive() || ship.isHulk() || ship.isPiece()) {
                ship.setJitter(dc_id, JITTER_COLOR, 0f, 0, 0f);
                ship.setJitterUnder(dc_id, JITTER_UNDER_COLOR, 0f, 0, 0f);
                return;
            }

            // Enrage function
            float enrage = 1f - ship.getHullLevel();
            ship.getMutableStats().getTimeMult().modifyMult(dc_id, 1f + (enrage * 0.25f));
            ship.setJitter(dc_id, JITTER_COLOR, enrage, 3, 5f);
            ship.setJitterUnder(dc_id, JITTER_UNDER_COLOR, enrage, 20, 15f);


        }

    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {

        // Don't do this if we're in player fleet
        if (member.getFleetCommander().isPlayer() || member.getFleetCommander().isDefault()) {
            return;
        }

        // Another check, I guess
        if (Global.getSector() != null && Global.getSector().getPlayerFleet() != null) {
            for (FleetMemberAPI mem : Global.getSector().getPlayerFleet().getMembersWithFightersCopy()) {
                if (mem.getId().equals(member.getId())) {
                    return;
                }
            }
        }

        // and another
        if (!member.getFleetCommander().getFaction().getId().contains("legioinfernalis")) {
            return;
        }

        // Now we make a new captain if we don't have an AI captain already
        if (member.getCaptain() != null) {
            if (member.getCaptain().isAICore()) {
                return;
            }
        }

        // Apparently this can be the case
        if (Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE) == null) {
            return;
        }

        int die = MathUtils.getRandomNumberInRange(1, 5) - MAG.get(member.getHullSpec().getHullSize());
        if (member.getHullSpec().getHullId().contains("tahlan_DunScaith_dmn")) {
            die = 3;    // Hel Scaith always gets an alpha
        }
        PersonAPI person; // yes, a "person"
        if (die <= 1) {
            person = Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE).createPerson(Commodities.GAMMA_CORE, "tahlan_legioinfernalis", Misc.random);
        } else if (die == 2) {
            person = Misc.getAICoreOfficerPlugin(Commodities.BETA_CORE).createPerson(Commodities.BETA_CORE, "tahlan_legioinfernalis", Misc.random);
            member.getStats().getDynamic().getMod("individual_ship_recovery_mod").modifyFlat("tahlan_daemoncore", -100f);
        } else {
            person = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE).createPerson(Commodities.ALPHA_CORE, "tahlan_legioinfernalis", Misc.random);
            member.getStats().getDynamic().getMod("individual_ship_recovery_mod").modifyFlat("tahlan_daemoncore", -1000f);
        }
        member.setCaptain(person);
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + Math.round(ACC_BUFF * 100f) + txt("%");
        if (index == 1) return "" + Math.round(MSSL_DAMAGE * 100f) + txt("%");
        if (index == 2) return "" + Math.round(SPEED_CAP * 100f) + txt("%");
        if (index == 3) return "" + Math.round(SPEED_BUFF * 100f) + txt("%");
        if (index == 4) return "" + Math.round(PLAYER_NERF * 100f) + txt("%");
        if (index == 5) return "" + (int) SUPPLIES_PERCENT + txt("%");
        return null;
    }

}
