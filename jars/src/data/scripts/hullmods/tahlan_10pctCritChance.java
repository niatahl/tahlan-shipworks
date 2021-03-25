package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class tahlan_10pctCritChance extends BaseHullMod {

    private static final float CRIT_CHANCE = 0.1f;
    private static final float CRIT_MULT = 3f;

    private List<DamagingProjectileAPI> toCrit = new ArrayList<DamagingProjectileAPI>();
    private List<DamagingProjectileAPI> hasHit = new ArrayList<DamagingProjectileAPI>();
    private List<DamagingProjectileAPI> toRemove = new ArrayList<DamagingProjectileAPI>();

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        if (!ship.isHulk() && !ship.isPiece()) {

            for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(ship.getLocation(), 500f)) {
                if (ship == proj.getSource() && !toCrit.contains(proj) && !hasHit.contains(proj)) {
                    toCrit.add(proj);
                }
            }

            for (DamagingProjectileAPI proj : toCrit) {
                if (proj.didDamage() && !hasHit.contains(proj)) {
                    hasHit.add(proj);
                    if ((Math.random() < CRIT_CHANCE) && (proj.getDamageTarget() instanceof ShipAPI)) {
                        Global.getCombatEngine().addFloatingText(proj.getLocation(), "CRIT!", 30f, Color.red, proj, 5f, 1f);
                        Global.getCombatEngine().addHitParticle(proj.getLocation(), proj.getVelocity(), 100f, 1f, 0.05f, Color.white);
                        Global.getCombatEngine().applyDamage(proj.getDamageTarget(),
                                proj.getLocation(),
                                proj.getDamageAmount() * (CRIT_MULT - 1f),
                                proj.getDamageType(),
                                proj.getEmpAmount() * (CRIT_MULT - 1f),
                                false,
                                false,
                                proj.getSource());
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
