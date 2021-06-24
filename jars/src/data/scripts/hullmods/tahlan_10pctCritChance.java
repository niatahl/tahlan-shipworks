package data.scripts.hullmods;

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

public class tahlan_10pctCritChance extends BaseHullMod {

    private static final float CRIT_CHANCE = 0.1f;
    private static final float CRIT_MULT = 3f;

    private final List<DamagingProjectileAPI> toCrit = new ArrayList<DamagingProjectileAPI>();
    private final List<DamagingProjectileAPI> hasHit = new ArrayList<DamagingProjectileAPI>();
    private final List<DamagingProjectileAPI> toRemove = new ArrayList<DamagingProjectileAPI>();

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        if (!ship.isHulk() && !ship.isPiece()) {

            for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(ship.getLocation(), 500f)) {
                if (ship == proj.getSource() && !toCrit.contains(proj) && !hasHit.contains(proj)) {
                    if (Math.random() < CRIT_CHANCE) {
                        toCrit.add(proj);
                        proj.setDamageAmount(proj.getDamageAmount()*CRIT_MULT);
                    } else {
                        hasHit.add(proj);
                    }
                }
            }

            for (DamagingProjectileAPI proj : toCrit) {
                if (proj.didDamage() && !hasHit.contains(proj)) {
                    hasHit.add(proj);
                    if (proj.getDamageTarget() instanceof ShipAPI) {
                        Global.getCombatEngine().addFloatingText(proj.getLocation(), "CRIT!", 30f, Color.red, proj, 5f, 1f);
                        Global.getCombatEngine().addHitParticle(proj.getLocation(), proj.getVelocity(), 100f, 1f, 0.05f, Color.white);
                    }
                }
            }


            for (DamagingProjectileAPI proj : hasHit) {
                if (!Global.getCombatEngine().isEntityInPlay(proj)) {
                    toCrit.remove(proj);
                    toRemove.add(proj);
                }
            }

            for (DamagingProjectileAPI proj : toRemove) {
                hasHit.remove(proj);
            }
            toRemove.clear();
        }
    }
}
