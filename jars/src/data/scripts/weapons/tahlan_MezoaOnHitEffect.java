package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class tahlan_MezoaOnHitEffect implements OnHitEffectPlugin {

    private static final Color PARTICLE_COLOR = new Color(220, 34, 255, 150);
    private static final Color BLAST_COLOR = new Color(24, 131, 255, 171);
    private static final Color CORE_COLOR = new Color(30, 218, 255, 150);
    private static final Color FLASH_COLOR = new Color(224, 255, 248);
    private static final int NUM_PARTICLES = 50;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        // Blast visuals
        float CoreExplosionRadius = 50f;
        float CoreExplosionDuration = 1f;
        float ExplosionRadius = 180f;
        float ExplosionDuration = 1f;
        float CoreGlowRadius = 200f;
        float CoreGlowDuration = 1f;
        float GlowRadius = 250f;
        float GlowDuration = 1f;
        float FlashGlowRadius = 300f;
        float FlashGlowDuration = 0.05f;

        engine.spawnExplosion(point, ZERO, CORE_COLOR, CoreExplosionRadius, CoreExplosionDuration);
        engine.spawnExplosion(point, ZERO, BLAST_COLOR, ExplosionRadius, ExplosionDuration);
        engine.addHitParticle(point, ZERO, CoreGlowRadius, 1f, CoreGlowDuration, CORE_COLOR);
        engine.addSmoothParticle(point, ZERO, GlowRadius, 1f, GlowDuration, PARTICLE_COLOR);
        engine.addHitParticle(point, ZERO, FlashGlowRadius, 1f, FlashGlowDuration, FLASH_COLOR);

        for (int x = 0; x < NUM_PARTICLES; x++) {
            engine.addHitParticle(point,
                    MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 400f), (float) Math.random() * 360f),
                    5f, 1f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), PARTICLE_COLOR);
        }

        float damage = projectile.getDamageAmount() * 0.1f;

        DamagingExplosionSpec blast = new DamagingExplosionSpec(0.1f,
                140f,
                70f,
                damage,
                damage/2,
                CollisionClass.PROJECTILE_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                10f,
                10f,
                0f,
                0,
                BLAST_COLOR,
                null);
        blast.setDamageType(DamageType.ENERGY);
        blast.setShowGraphic(false);
        engine.spawnDamagingExplosion(blast,projectile.getSource(),point,false);

        Global.getSoundPlayer().playSound("tahlan_ryza_impact",1f,1f,point,ZERO);
    }
}
