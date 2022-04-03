package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

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

    private static final  String dc_id = "tahlan_daemoncore";

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

        if (ship.getVariant().hasHullMod("es_shiplevelHM")) {
            ship.getMutableStats().getEngineMalfunctionChance().modifyFlat(id,0.1f);
            ship.getMutableStats().getWeaponMalfunctionChance().modifyFlat(id,0.1f);
            ship.getMutableStats().getCriticalMalfunctionChance().modifyFlat(id,0.01f);
        }

        if (!ship.hasListenerOfClass(daemonListener.class)) {
            ship.addListener(new daemonListener(ship));
        }

        if (Global.getSector().getPlayerFleet() == null) {
            return;
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

        if (ship.getFleetMember().getFleetData() != null) {
            // no hidden stuff for player
            if (ship.getFleetMember().getFleetData().getFleet().isPlayerFleet()) {
                return;
            }
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
        if (index == 0) return "" + 25 + txt("%");
        if (index == 1) return "" + 25 + txt("%");
        if (index == 2) return "" + 50 + txt("%");
        return null;
    }

    private static class daemonListener implements DamageTakenModifier, DamageDealtModifier {
        protected ShipAPI ship;

        public daemonListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (target != ship) {
                return null;
            }
            WeaponAPI weapon;
            if (param instanceof DamagingProjectileAPI) {
                weapon = ((DamagingProjectileAPI) param).getWeapon();
            } else if ( param instanceof BeamAPI ){
                weapon = ((BeamAPI) param).getWeapon();
            } else if ( param instanceof WeaponAPI ) {
                weapon = (WeaponAPI) param;
            } else {
                return null;
            }
            if (weapon == null) {
                return null;
            }
            if (weapon.getId() == null) {
                return null;
            }
            // If you're not balancing your mods, I'll just counterbalans
            if (weapon.getId().contains("sw_") || weapon.getId().contains("HIVER_")) {
                damage.setDamage(damage.getDamage() * 0.25f);
            }
            return null;
        }

        // Scrub police v2
        // If someone finds this, I might remove it. Kinda just want to see if the pl*yers manage to figure it out.
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!(target instanceof ShipAPI)) {
                return null;
            }
            ShipAPI enemy = (ShipAPI) target;
            if (enemy.getVariant().hasHullMod("es_shiplevelHM")) {
                // Clown mod gets clown damage
                damage.setDamage(damage.getDamage()*MathUtils.getRandomNumberInRange(1f,5f));
                return null;
            }
            if (enemy.getHullSpec().getHullId().contains("MSS_") || enemy.getVariant().hasHullMod("CHM_mayasura") || enemy.getHullSpec().getHullId().contains("missp_")) {
                damage.setDamage(damage.getDamage()*1.5f);
                return null;
            }
            if (enemy.getHullSpec().getHullId().contains("tesseract") || enemy.getHullSpec().getHullId().contains("facet") || enemy.getHullSpec().getHullId().contains("shard_")) {
                damage.setDamage(damage.getDamage()*2f);
                return null;
            }
            return null;
        }
    }

}
