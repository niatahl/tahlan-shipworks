package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.magiclib.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class GleipnirKinOnHitEffect implements OnHitEffectPlugin {

    //Percentage of the projectile's original damage dealt as bonus damage on hull hit: too high and AI issues start appearing
    //private static final float DAMAGE_MULT = 0.3f;

    //Variables for explosion visuals
    private static final Color EXPLOSION_COLOR = new Color(100, 200, 255);
    private static final float EXPLOSION_SIZE = 150f;
    private static final float EXPLOSION_DURATION_MIN = 0.3f;
    private static final float EXPLOSION_DURATION_MAX = 0.7f;

    //Variables for the small particles generated with the explosion
    private static final Color PARTICLE_COLOR = new Color(100, 200, 255);
    private static final float PARTICLE_SIZE = 120f;
    private static final float PARTICLE_BRIGHTNESS = 90f;

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        MagicLensFlare.createSharpFlare(engine, projectile.getSource(), projectile.getLocation(), 12, 750, 0, EXPLOSION_COLOR, new Color(255, 255, 255));

        Global.getCombatEngine().addHitParticle(point, new Vector2f(0f,0f), PARTICLE_SIZE, PARTICLE_BRIGHTNESS, MathUtils.getRandomNumberInRange(EXPLOSION_DURATION_MIN, EXPLOSION_DURATION_MAX), PARTICLE_COLOR);
    }
}
