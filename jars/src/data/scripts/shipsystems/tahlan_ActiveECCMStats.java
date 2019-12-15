package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class tahlan_ActiveECCMStats extends BaseShipSystemScript {

    private static final float EW_PENALTY_MULT = 0.5f;

    private static final float ECCM_CHANCE = 0.5f;
    private static final float GUIDANCE_IMPROVEMENT = 1f;

    private static final float EFFECT_RANGE = 1000f;
    private static final Color GLOW_COLOR = new Color(50, 255, 50, 200);

    private List<ShipAPI> targetList = new ArrayList<ShipAPI>();
    private static final EnumSet<WeaponAPI.WeaponType> WEAPON_TYPES = EnumSet.of(WeaponAPI.WeaponType.MISSILE);

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }

        stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, 3f);

        //Lets list our applicable targets first

        for (ShipAPI target : CombatUtils.getShipsWithinRange(ship.getLocation(), EFFECT_RANGE)) {
            if (target.getOwner() == ship.getOwner()) {
                if (target.getVariant() == null || target.isPhased() || target.getVariant().getHullMods().contains("eccm")) {
                    continue;
                }
                if (target.getOwner() == ship.getOwner()) {
                    targetList.add(target);
                }
            }
        }
        List<ShipAPI> purgeList = new ArrayList<ShipAPI>();
        for (ShipAPI target : targetList) {
            if (MathUtils.getDistance(target.getLocation(), ship.getLocation()) <= EFFECT_RANGE) {
                //apply ECCM effects to targets in range
                target.getMutableStats().getEccmChance().modifyFlat(id, ECCM_CHANCE);
                target.getMutableStats().getMissileGuidance().modifyFlat(id, GUIDANCE_IMPROVEMENT);

                target.getMutableStats().getMissileMaxSpeedBonus().modifyPercent(id, 10f);
                target.getMutableStats().getMissileAccelerationBonus().modifyPercent(id, 100f);
                target.getMutableStats().getMissileMaxTurnRateBonus().modifyPercent(id, 10f);
                target.getMutableStats().getMissileTurnAccelerationBonus().modifyPercent(id, 50f);

                target.getMutableStats().getDynamic().getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).modifyMult(id, EW_PENALTY_MULT);

                target.setWeaponGlow(1.5f, GLOW_COLOR, WEAPON_TYPES);
            } else {
                //remove any targets that have gone out of range
                target.getMutableStats().getEccmChance().unmodify(id);
                target.getMutableStats().getMissileGuidance().unmodify(id);

                target.getMutableStats().getMissileMaxSpeedBonus().unmodify(id);
                target.getMutableStats().getMissileAccelerationBonus().unmodify(id);
                target.getMutableStats().getMissileMaxTurnRateBonus().unmodify(id);
                target.getMutableStats().getMissileTurnAccelerationBonus().unmodify(id);

                target.getMutableStats().getDynamic().getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).unmodify(id);
                target.setWeaponGlow(0f,GLOW_COLOR, WEAPON_TYPES);

                purgeList.add(target);
            }

        }
        for (ShipAPI purge : purgeList) {
            targetList.remove(purge);
        }

    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        for (ShipAPI target : targetList) {

            //remove any targets that have gone out of range
            target.getMutableStats().getEccmChance().unmodify(id);
            target.getMutableStats().getMissileGuidance().unmodify(id);

            target.getMutableStats().getMissileMaxSpeedBonus().unmodify(id);
            target.getMutableStats().getMissileAccelerationBonus().unmodify(id);
            target.getMutableStats().getMissileMaxTurnRateBonus().unmodify(id);
            target.getMutableStats().getMissileTurnAccelerationBonus().unmodify(id);

            target.getMutableStats().getDynamic().getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).unmodify(id);

            target.setWeaponGlow(0f,GLOW_COLOR, WEAPON_TYPES);
        }

        stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).unmodify(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("ECCM field active", false);
        }
        return null;
    }

}
