package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class tahlan_FusorOnHitEffect implements OnHitEffectPlugin {

    //Percentage of the projectile's original damage dealt as bonus damage on hull hit: too high and AI issues start appearing
    //private static final float DAMAGE_MULT = 0.3f;

    //Variables for explosion visuals
    private static final Color EXPLOSION_COLOR = new Color(255f / 255f, 100f / 255f, 50f / 255f, 100f / 255f);
    private static final float EXPLOSION_SIZE = 110f;
    private static final float EXPLOSION_DURATION_MAX = 0.2f;


    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {

        if (shieldHit || target == null) {
            return;
        }

        float critLevel = MathUtils.getRandomNumberInRange(0.2f, 1f);
        float bonusDamage = projectile.getDamageAmount()/10f;
        if (projectile.didDamage()) {
            Global.getCombatEngine().applyDamage(target, point, critLevel * bonusDamage, DamageType.HIGH_EXPLOSIVE, 0, true, false, projectile.getSource(), true);
        }
        Global.getCombatEngine().spawnExplosion(point, new Vector2f(0f, 0f), EXPLOSION_COLOR, critLevel * EXPLOSION_SIZE, EXPLOSION_DURATION_MAX);
    }
}
