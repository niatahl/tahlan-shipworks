package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class VesuvioOnHitEffect implements OnHitEffectPlugin {

    private static final Color COLOR1 = new Color(52, 231, 255);
    private static final Color COLOR2 = new Color(237, 255, 246);

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (point == null) {
            return;
        }

        //Also spawns lightning, though only against ships, and only on direct hull hits
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI)target;
            float emp = projectile.getEmpAmount();
            float dam = projectile.getDamageAmount();
            boolean triggered = Math.random() < 0.2f;
            if (!shieldHit && triggered) {
                engine.spawnEmpArcPierceShields(projectile.getSource(), point, ship, ship,
                        DamageType.ENERGY, dam, emp, 1000f, "tachyon_lance_emp_impact", 15f, COLOR1, COLOR2);
            }
        }
    }
}
