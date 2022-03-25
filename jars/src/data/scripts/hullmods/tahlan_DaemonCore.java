package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static data.scripts.utils.tahlan_Utils.txt;

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

    private final String INNERLARGE = "graphics/tahlan/fx/tahlan_shellshield.png";
    private final String OUTERLARGE = "graphics/tahlan/fx/tahlan_tempshield_ring.png";
    private static final  String dc_id = "tahlan_daemoncore";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        stats.getProjectileSpeedMult().modifyMult(id, 1.5f);
        stats.getMaxRecoilMult().modifyMult(id, 0.75f);
        stats.getRecoilDecayMult().modifyMult(id, 1.25f);
        stats.getRecoilPerShotMult().modifyMult(id, 0.75f);

    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getShield() != null) {
            ship.getShield().setRadius(ship.getShieldRadiusEvenIfNoShield(), INNERLARGE, OUTERLARGE);
        }

        boolean isPlayerFleet = false;
        for (FleetMemberAPI member: Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (member.getVariant().getHullVariantId().equals(ship.getVariant().getHullVariantId())) {
                isPlayerFleet = true;
            }
        }

        if (ship.getVariant().hasHullMod("tahlan_daemonboost")) {
            ship.getVariant().removeMod("tahlan_daemonboost");
        }

        if (!isPlayerFleet) {
            ship.getVariant().addMod("tahlan_daemonboost");
        }

    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        // no hidden stuff for player
        if (ship.getFleetMember().getFleetData().getFleet().isPlayerFleet()) {
            return;
        }

        if (!ship.isAlive() || ship.isHulk() || ship.isPiece()) {
            ship.setJitter(dc_id,JITTER_COLOR,0f,0,0f);
            ship.setJitterUnder(dc_id,JITTER_UNDER_COLOR,0f,0,0f);
            return;
        }

        // Enrage function
        float enrage = 1f + (ship.getHullLevel() * 0.25f);
        ship.getMutableStats().getTimeMult().modifyMult(dc_id,enrage);
        ship.setJitter(dc_id, JITTER_COLOR, 1f-ship.getHullLevel(), 3, 5f);
        ship.setJitterUnder(dc_id, JITTER_UNDER_COLOR, 1f-ship.getHullLevel(), 20, 10f);

        // Scrub Police starts here
        boolean scrub = false;
        boolean turboscrub = false;
        for (ShipAPI enemy: Global.getCombatEngine().getShips()) {
            if (enemy.getOwner() != ship.getOwner()) {
                continue;
            }
            // find scrub botes
            if (enemy.getVariant().hasHullMod("MSS_Prime") || enemy.getVariant().hasHullMod("CHM_mayasura") || enemy.getHullSpec().getHullId().contains("missp_")) {
                scrub = true;
                break;
            }
            if (enemy.getHullSpec().getHullId().contains("tesseract") || enemy.getHullSpec().getHullId().contains("facet") || enemy.getHullSpec().getHullId().contains("shard_")) {
                turboscrub = true;
            }
            if (!scrub) { // only check weapons if we haven't found scrub botes
                for (WeaponAPI weapon : enemy.getAllWeapons()) {
                    if (weapon.getSpec().getWeaponId().contains("sw_")) {
                        scrub = true;
                        break;
                    }
                }
            }
        }

        if (turboscrub) {
            ship.getMutableStats().getDamageToCapital().modifyMult("scrub_police",3f);
            ship.getMutableStats().getDamageToCruisers().modifyMult("scrub_police",2f);
        } else if (scrub) { // then get fucked lmao
            ship.getMutableStats().getDamageToCapital().modifyMult("scrub_police",2f);
            ship.getMutableStats().getDamageToCruisers().modifyMult("scrub_police",1.5f);
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
            member.getStats().getDynamic().getMod("individual_ship_recovery_mod").modifyFlat("tahlan_daemoncore",-100f);
        } else {
            person = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE).createPerson(Commodities.ALPHA_CORE, "tahlan_legioinfernalis", Misc.random);
            member.getStats().getDynamic().getMod("individual_ship_recovery_mod").modifyFlat("tahlan_daemoncore",-1000f);
        }
        member.setCaptain(person);
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + 50 + txt("%");
        if (index == 1) return "" + 25 + txt("%");
        return null;
    }


}
