package org.niatahl.tahlan.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TenPercentCritChance extends BaseHullMod {

    private static final float CRIT_CHANCE = 0.1f;
    private static final float CRIT_MULT = 3f;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        String id = ship.getId();
        Data data;
        Map<String, Object> customCombatData = Global.getCombatEngine().getCustomData();
        if (customCombatData.get("tahlan_crit" + id) instanceof TenPercentCritChance.Data)
            data = (TenPercentCritChance.Data) customCombatData.get("tahlan_crit" + id);
        else {
            data = new TenPercentCritChance.Data();
            customCombatData.put("tahlan_crit" + id, data);
        }

        if (!ship.isHulk() && !ship.isPiece()) {

            for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(ship.getLocation(), 500f)) {
                if (ship == proj.getSource() && !data.toCrit.contains(proj) && !data.hasHit.contains(proj)) {
                    if (Math.random() < CRIT_CHANCE) {
                        data.toCrit.add(proj);
                        proj.setDamageAmount(proj.getDamageAmount()*CRIT_MULT);
                    } else {
                        data.hasHit.add(proj);
                    }
                }
            }

            for (DamagingProjectileAPI proj : data.toCrit) {
                if (proj.didDamage() && !data.hasHit.contains(proj)) {
                    data.hasHit.add(proj);
                    if (proj.getDamageTarget() instanceof ShipAPI) {
                        Global.getCombatEngine().addFloatingText(proj.getLocation(), "CRIT!", 30f, Color.red, proj, 5f, 1f);
                        Global.getCombatEngine().addHitParticle(proj.getLocation(), proj.getVelocity(), 100f, 1f, 0.05f, Color.white);
                    }
                }
            }

            final List<DamagingProjectileAPI> toRemove = new ArrayList<>();
            for (DamagingProjectileAPI proj : data.hasHit) {
                if (!Global.getCombatEngine().isEntityInPlay(proj)) {
                    data.toCrit.remove(proj);
                    toRemove.add(proj);
                }
            }

            for (DamagingProjectileAPI proj : toRemove) {
                data.hasHit.remove(proj);
            }
            toRemove.clear();
        }
    }

    private static class Data {
        final List<DamagingProjectileAPI> toCrit = new ArrayList<>();
        final List<DamagingProjectileAPI> hasHit = new ArrayList<>();
    }
}
