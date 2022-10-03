package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class StahlOnHitEffect implements OnHitEffectPlugin {

    private static final Color PARTICLE_COLOR = new Color(67, 255, 221, 200);
    private static final Color BLAST_COLOR = new Color(255, 16, 16, 0);
    private static final Color CORE_COLOR = new Color(210, 245, 255);
    private static final Color FLASH_COLOR = new Color(234, 255, 253);
    private static final int NUM_PARTICLES = 20;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        if (!(target instanceof ShipAPI) || shieldHit){
            return;
        }

        if ( Math.random() > 0.1 ) {
            return;
        }

        float bonusDamage = projectile.getDamageAmount()*2.5f;
        engine.applyDamage(target,point,bonusDamage,DamageType.FRAGMENTATION,0f,false,false,projectile.getSource(),true);

        engine.addSmoothParticle(point, ZERO, 25f, 1f, 0.1f, PARTICLE_COLOR);
        engine.addHitParticle(point, ZERO, 50f, 0.5f, 0.05f, FLASH_COLOR);
    }
}
