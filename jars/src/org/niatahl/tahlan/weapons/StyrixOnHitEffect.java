package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class StyrixOnHitEffect implements OnHitEffectPlugin {

    //Percentage of the projectile's original damage dealt as bonus damage on hull hit: too high and AI issues start appearing
    //private static final float DAMAGE_MULT = 0.3f;

    //Variables for explosion visuals
    private static final Color EXPLOSION_COLOR = new Color(100f / 255f, 205f / 255f, 255f / 255f, 130f / 255f);
    private static final float EXPLOSION_SIZE = 180f;
    private static final float EXPLOSION_DURATION_MIN = 0.3f;
    private static final float EXPLOSION_DURATION_MAX = 0.7f;

    //Variables for the small particles generated with the explosion
    private static final int PARTICLE_COUNT = 4;
    private static final Color PARTICLE_COLOR = new Color(55f / 255f, 205f / 255f, 255f / 255f, 130f / 255f);
    private static final float PARTICLE_SIZE_MIN = 8f;
    private static final float PARTICLE_SIZE_MAX = 12f;
    private static final float PARTICLE_SPEED_MAX = 3f;
    private static final float PARTICLE_BRIGHTNESS_MIN = 60f;
    private static final float PARTICLE_BRIGHTNESS_MAX = 90f;

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        //MagicLensFlare.createSharpFlare(engine,projectile.getSource(),projectile.getLocation(),10,500,0,new Color(100,215,255),new Color(255,255,255));

        if (shieldHit || target == null) {
            return;
        }

        float bonusDamage = projectile.getDamageAmount()/3.5f;
        Float critLevel = MathUtils.getRandomNumberInRange(0.2f, 1f);
        if (projectile.didDamage()) {
            Global.getCombatEngine().applyDamage(target, point, critLevel * bonusDamage, DamageType.ENERGY, 0, true, false, projectile.getSource(), true);
        }
        Global.getCombatEngine().spawnExplosion(point, new Vector2f(0f, 0f), EXPLOSION_COLOR, critLevel * EXPLOSION_SIZE, EXPLOSION_DURATION_MAX);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Global.getCombatEngine().addHitParticle(point, MathUtils.getRandomPointInCircle(null, PARTICLE_SPEED_MAX), MathUtils.getRandomNumberInRange(PARTICLE_SIZE_MIN, PARTICLE_SIZE_MAX),
                    MathUtils.getRandomNumberInRange(PARTICLE_BRIGHTNESS_MIN, PARTICLE_BRIGHTNESS_MAX), MathUtils.getRandomNumberInRange(EXPLOSION_DURATION_MIN, EXPLOSION_DURATION_MAX), PARTICLE_COLOR);
        }
        MagicLensFlare.createSharpFlare(engine,projectile.getSource(),projectile.getLocation(),5+5*critLevel,300+200*critLevel,0,new Color(100,215,255),new Color(255,255,255));


        //Commented out, but plays a sound when exploding if un-commented
        //Global.getSoundPlayer().playSound("SRD_ArCielExplosion", 1f, 1f, point, new Vector2f(0f, 0f));
    }
}
