package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class tahlan_SturmOnHitEffect implements OnHitEffectPlugin {

    private static final Color PARTICLE_COLOR = new Color(132, 236, 255, 174);
    private static final Color BLAST_COLOR = new Color(255, 16, 16, 0);
    private static final Color CORE_COLOR = new Color(119, 255, 194);
    private static final Color FLASH_COLOR = new Color(212, 248, 255);
    private static final int NUM_PARTICLES = 30;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        //MagicLensFlare.createSharpFlare(engine, projectile.getSource(), projectile.getLocation(), 8, 400, 0, new Color(186, 240, 255), new Color(255, 255, 255));

        engine.spawnExplosion(point, ZERO, PARTICLE_COLOR, 150f, 1f);
        engine.spawnExplosion(point, ZERO, CORE_COLOR, 75f, 1f);

        float bonusDamage = projectile.getDamageAmount()/3f;
        DamagingExplosionSpec blast = new DamagingExplosionSpec(0.1f,
                100f,
                50f,
                bonusDamage,
                bonusDamage/2f,
                CollisionClass.PROJECTILE_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                10f,
                10f,
                0f,
                0,
                BLAST_COLOR,
                null);
        blast.setDamageType(DamageType.FRAGMENTATION);
        blast.setShowGraphic(false);
        engine.spawnDamagingExplosion(blast,projectile.getSource(),point,false);

        engine.addSmoothParticle(point, ZERO, 400f, 0.5f, 0.1f, PARTICLE_COLOR);
        engine.addHitParticle(point, ZERO, 200f, 0.5f, 0.25f, FLASH_COLOR);
        for (int x = 0; x < NUM_PARTICLES; x++) {
            engine.addHitParticle(point,
                    MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 300f), (float) Math.random() * 360f),
                    10f, 1f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), PARTICLE_COLOR);
        }
        Global.getSoundPlayer().playSound("tahlan_porph_impact",1f,1f,point,ZERO);
    }
}
