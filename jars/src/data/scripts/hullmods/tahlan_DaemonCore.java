package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

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

    private static final Color JITTER_COLOR = new Color(255, 0, 0, 20);
    private static final Color JITTER_UNDER_COLOR = new Color(255, 0, 0, 80);

    private static final IntervalUtil kaboom = new IntervalUtil(1f, 10f);
    private static final String dc_id = "tahlan_daemoncore";

    private static final IntervalUtil yoinkTimer = new IntervalUtil(5f, 10f);

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        stats.getProjectileSpeedMult().modifyMult(id, 1.25f);
        stats.getMaxRecoilMult().modifyMult(id, 0.75f);
        stats.getRecoilDecayMult().modifyMult(id, 1.25f);
        stats.getRecoilPerShotMult().modifyMult(id, 0.75f);
        stats.getDamageToMissiles().modifyMult(id, 1.5f);

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

        // Hackery to make the ships uniquely more poteent while in Legio fleets in attempt to keep them more balanced in player hands
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
            ship.getMutableStats().getTimeMult().modifyMult(id,0.9f);
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

        if (engine.getFleetManager(ship.getOwner()) == engine.getFleetManager(FleetSide.PLAYER)) {
            //Only run this in campaign context
            if (!engine.isInCampaign()) {
                return;
            }
            yoinkTimer.advance(amount);
            if (yoinkTimer.intervalElapsed()) {
                for (ShipAPI bote : CombatUtils.getShipsWithinRange(ship.getLocation(), 2000f)) {
                    if (bote.getHullSpec().getHullId().contains("tahlan_DunScaith_dmn")
                            && (Math.random() > 0.75f)
                            && bote.getFleetMember().getFleetCommander().getFaction().getId().contains("legioinfernalis")) {
                        engine.addFloatingText(ship.getLocation(), "ASSUMING DIRECT CONTROL", 40f, Color.RED, ship, 0.5f, 3f);
                        ship.setOwner(bote.getOwner());
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
            float enrage = 1f + (ship.getHullLevel() * 0.25f);
            ship.getMutableStats().getTimeMult().modifyMult(dc_id, enrage);
            ship.setJitter(dc_id, JITTER_COLOR, 1f - ship.getHullLevel(), 3, 5f);
            ship.setJitterUnder(dc_id, JITTER_UNDER_COLOR, 1f - ship.getHullLevel(), 20, 10f);


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
        if (index == 0) return "" + 25 + txt("%");
        if (index == 1) return "" + 25 + txt("%");
        if (index == 2) return "" + 50 + txt("%");
        if (index == 3) return "" + 90 + txt("%");
        return null;
    }

}
