package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.magiclib.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TaffetaDummyOnHitEffect implements OnHitEffectPlugin {

    private static final Color ARC_FRINGE_COLOR = new Color(185, 52, 155, 250);
    private static final Color ARC_CORE_COLOR = new Color(206, 255, 252, 250);

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (projectile.didDamage() && (target instanceof ShipAPI) && Math.random()<=0.1f) {

            ShipAPI ship = (ShipAPI)target;

            float pierceChance = ((ShipAPI) target).getHardFluxLevel();
            pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

            boolean piercedShield = shieldHit && (float) Math.random() < pierceChance;
            if (!shieldHit || piercedShield) {
                Global.getCombatEngine().spawnEmpArcPierceShields(projectile.getSource(), point, projectile.getSource(), target,
                        DamageType.ENERGY, //Damage type
                        projectile.getDamageAmount(), //Damage
                        projectile.getEmpAmount()*10f, //Emp
                        100000f, //Max range
                        null, //Impact sound
                        10f, // thickness of the lightning bolt
                        ARC_CORE_COLOR, //Central color
                        ARC_FRINGE_COLOR //Fringe Color
                );
            }
        }
    }
}
