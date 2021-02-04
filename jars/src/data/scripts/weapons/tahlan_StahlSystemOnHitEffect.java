package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class tahlan_StahlSystemOnHitEffect implements OnHitEffectPlugin {

    private static final Color PARTICLE_COLOR = new Color(76, 255, 155, 200);
    private static final Color BLAST_COLOR = new Color(255, 16, 16, 0);
    private static final Color CORE_COLOR = new Color(210, 245, 255);
    private static final Color FLASH_COLOR = new Color(238, 255, 234);
    private static final int NUM_PARTICLES = 30;
    private static final float EMP_DAMAGE = 100;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {

        //MagicLensFlare.createSharpFlare(engine, projectile.getSource(), projectile.getLocation(), 8, 400, 0, new Color(186, 240, 255), new Color(255, 255, 255));

        //engine.spawnExplosion(point, ZERO, PARTICLE_COLOR, 60f, 0.1f);

        float bonusDamage = projectile.getDamageAmount()*1.2f;
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
        blast.setDamageType(DamageType.ENERGY);
        blast.setShowGraphic(false);
        engine.spawnDamagingExplosion(blast,projectile.getSource(),point,false);

        engine.addSmoothParticle(point, ZERO, 150f, 0.5f, 0.1f, PARTICLE_COLOR);
        engine.addHitParticle(point, ZERO, 150f, 0.3f, 0.05f, FLASH_COLOR);
        for (int x = 0; x < NUM_PARTICLES; x++) {
            engine.addHitParticle(point,
                    MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(150f, 200f), (float) Math.random() * 360f),
                    3f, 1f, MathUtils.getRandomNumberInRange(0.2f, 0.3f), PARTICLE_COLOR);
        }

        if (point != null) {

            if ((target instanceof ShipAPI) && projectile.didDamage()) {
                ShipAPI ship = (ShipAPI) target;

                float proc = MathUtils.getRandomNumberInRange(0,3);
                if (proc > 2) {

                    float pierceChance = ((ShipAPI) target).getHardFluxLevel() - 0.5f;
                    pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

                    bonusDamage = projectile.getEmpAmount();
                    boolean piercedShield = shieldHit && (float) Math.random() < pierceChance;
                    if (!shieldHit || piercedShield) {
                        engine.spawnEmpArcPierceShields(projectile.getSource(), point, ship, ship,
                                DamageType.ENERGY, bonusDamage*0.1f, bonusDamage, 100000f, null, 20f, PARTICLE_COLOR, CORE_COLOR);
                    }

                    Global.getSoundPlayer().playSound("tachyon_lance_emp_impact", 1f, 1f, point, new Vector2f(0f, 0f));
                }
            }
        }

        bonusDamage = projectile.getDamageAmount()*1.25f;
        if ( Math.random() > 0.9 ) {
            engine.applyDamage(target, point, bonusDamage, DamageType.FRAGMENTATION, 0f, false, false, projectile.getSource(),true);

            engine.addSmoothParticle(point, ZERO, 25f, 1f, 0.1f, PARTICLE_COLOR);
            engine.addHitParticle(point, ZERO, 50f, 0.5f, 0.05f, FLASH_COLOR);
        }

    }
}
