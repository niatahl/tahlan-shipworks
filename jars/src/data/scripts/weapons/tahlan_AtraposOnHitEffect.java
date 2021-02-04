package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class tahlan_AtraposOnHitEffect implements OnHitEffectPlugin {

    //Percentage of the projectile's original damage dealt as bonus damage on hull hit: too high and AI issues start appearing
    //private static final float DAMAGE_MULT = 0.3f;

    //Variables for explosion visuals
    private static final Color EXPLOSION_COLOR = new Color(255f / 255f, 205f / 255f, 155f / 255f, 130f / 255f);
    private static final float EXPLOSION_SIZE = 60f;
    private static final float EXPLOSION_DURATION_MIN = 0.2f;
    private static final float EXPLOSION_DURATION_MAX = 0.5f;

    //Variables for the small particles generated with the explosion
    private static final Color PARTICLE_COLOR = new Color(255f / 255f, 205f / 255f, 255f / 255f, 130f / 255f);
    private static final float PARTICLE_SIZE = 60f;
    private static final float PARTICLE_BRIGHTNESS = 70f;

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {

        //MagicLensFlare.createSharpFlare(engine,projectile.getSource(),projectile.getLocation(),10,500,0,new Color(100,215,255),new Color(255,255,255));

        if (shieldHit || target == null || !(target instanceof ShipAPI)) {
            return;
        }

        float bonusDamage = projectile.getDamageAmount()*0.66f;
        if (projectile.didDamage()) {
            Global.getCombatEngine().applyDamage(target, point, bonusDamage, DamageType.FRAGMENTATION, 0, true, false, projectile.getSource(), true);
        }

        Global.getCombatEngine().spawnExplosion(point, new Vector2f(0f, 0f), EXPLOSION_COLOR, EXPLOSION_SIZE, EXPLOSION_DURATION_MAX);
        MagicLensFlare.createSharpFlare(engine, projectile.getSource(), projectile.getLocation(), 10, 600, 0, EXPLOSION_COLOR, new Color(255, 255, 255));

        Global.getCombatEngine().addHitParticle(point, new Vector2f(0f,0f), PARTICLE_SIZE, PARTICLE_BRIGHTNESS, MathUtils.getRandomNumberInRange(EXPLOSION_DURATION_MIN, EXPLOSION_DURATION_MAX), PARTICLE_COLOR);

        //Commented out, but plays a sound when exploding if un-commented
        //Global.getSoundPlayer().playSound("SRD_ArCielExplosion", 1f, 1f, point, new Vector2f(0f, 0f));
    }
}
