package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class KriegsmesserOnHitEffect implements OnHitEffectPlugin {

    private static final float BONUS_DAMAGE = 400f;
    private static final Color COLOR1 = new Color(255, 68, 112);
    private static final Color COLOR2 = new Color(255, 233, 240);
    private static final int NUM_ECHOES = 5;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        float bonusDamage = projectile.getDamageAmount()*0.1f;
        DamagingExplosionSpec blast = new DamagingExplosionSpec(0.1f,
                140f,
                70f,
                bonusDamage,
                bonusDamage/2f,
                CollisionClass.PROJECTILE_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                10f,
                10f,
                0f,
                0,
                COLOR1,
                null);
        blast.setDamageType(DamageType.ENERGY);
        blast.setShowGraphic(false);

        for (int echo = 0; echo < NUM_ECHOES; echo++) {
            Vector2f center = MathUtils.getRandomPointInCircle(point,120f);

            engine.spawnDamagingExplosion(blast,projectile.getSource(),center,false);

            engine.addSmoothParticle(center, ZERO, 200f, 0.5f, 0.1f, COLOR1);
            engine.addHitParticle(center, ZERO, 200f, 0.3f, 0.1f, COLOR2);

            MagicLensFlare.createSharpFlare(engine, projectile.getSource(), center, 8, 400, 0,COLOR1,COLOR2);

        }

        Global.getSoundPlayer().playSound("tahlan_kriegsmesser_onhit",1f,1f,point,ZERO);

        MagicLensFlare.createSharpFlare(engine, projectile.getSource(), point, 12, 800, 0,COLOR1,COLOR2);



    }
}
