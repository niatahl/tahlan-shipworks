package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class tahlan_RelparaxOnHitEffect implements OnHitEffectPlugin {

    private static final Color COLOR1 = new Color(121,255,228);
    private static final Color COLOR2 = new Color(225,255,225);

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {

        if (point != null) {

            if ((target instanceof ShipAPI) && projectile.didDamage()) {
                ShipAPI ship = (ShipAPI) target;

                float bonusDamage = projectile.getEmpAmount();
                float hitLevel = 0f;
                int numHits = MathUtils.getRandomNumberInRange(0,2);
                for (int x = 0; x < numHits; x++) {
                    float pierceChance = ((ShipAPI) target).getHardFluxLevel() - 0.5f;
                    pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

                    boolean piercedShield = shieldHit && (float) Math.random() < pierceChance;
                    if (!shieldHit || piercedShield) {
                        hitLevel += 0.25f;
                        engine.spawnEmpArcPierceShields(projectile.getSource(), point, ship, ship,
                                DamageType.ENERGY, 0, bonusDamage, 100000f, null, 5f, COLOR1, COLOR2);
                    }
                }

                if (hitLevel > 0f) {
                    Global.getSoundPlayer().playSound("tachyon_lance_emp_impact", 1f, 1f * hitLevel, point, new Vector2f(0f, 0f));
                }
            }
        }
    }
}
