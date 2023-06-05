package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.magiclib.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class RakatonOnHitEffect implements OnHitEffectPlugin {

    private static final Color COLOR1 = new Color(71, 191, 255);
    private static final Color COLOR2 = new Color(236, 248, 255);

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        if (target == null) return;

        float bonusDamage = projectile.getDamageAmount()*0.6f;

        Global.getCombatEngine().applyDamage(target, point, bonusDamage, DamageType.ENERGY, bonusDamage, false, false, projectile.getSource(), true);
        MagicLensFlare.createSharpFlare(engine, projectile.getSource(), projectile.getLocation(), 10, 600, 0, new Color(186, 240, 255), new Color(255, 255, 255));
        engine.addSmoothParticle(point, ZERO, 600f, 0.5f, 0.1f, COLOR1);
        engine.addHitParticle(point, ZERO, 300f, 0.5f, 0.25f, COLOR2);
    }
}
