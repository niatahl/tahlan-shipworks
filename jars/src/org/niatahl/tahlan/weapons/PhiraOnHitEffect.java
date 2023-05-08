package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.magiclib.plugins.MagicTrailPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class PhiraOnHitEffect implements OnHitEffectPlugin {

    private static final Color COLOR1 = new Color(255,130,30);
    private static final Color COLOR2 = new Color(255, 246, 234);

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (point == null) {
            return;
        }

        //Benediction-esque on-hit effects, but in a very tight cone, and faster
        SpriteAPI spriteToUse = Global.getSettings().getSprite("fx","tahlan_trail_fuzzy");
        for (int i1 = 0; i1 < 5; i1++) {
            //Color randomization
            float currentMult = MathUtils.getRandomNumberInRange(0f, 1f);
            Color colorToUse = new Color((int)(COLOR1.getRed() * currentMult + COLOR2.getRed() * (1f - currentMult)),
                    (int)(COLOR1.getGreen() * currentMult + COLOR2.getGreen() * (1f - currentMult)),
                    (int)(COLOR1.getBlue() * currentMult + COLOR2.getBlue() * (1f - currentMult)));

            float id = MagicTrailPlugin.getUniqueID();
            float angle = MathUtils.getRandomNumberInRange(projectile.getFacing()-10f, projectile.getFacing()+10f);
            float startSpeed = MathUtils.getRandomNumberInRange(0f, 900f);
            float startAngularVelocity = MathUtils.getRandomNumberInRange(-65f, 65f);
            float startSize = MathUtils.getRandomNumberInRange(17f, 39f);
            float lifetimeMult = MathUtils.getRandomNumberInRange(0.3f, 0.7f);
            for (int i2 = 0; i2 < 70; i2++) {
                //This is for "end fizzle"
                float fizzleConstantSpeed = MathUtils.getRandomNumberInRange(-20f, 20f);
                float fizzleConstantAngle = MathUtils.getRandomNumberInRange(-35f, 35f);
                MagicTrailPlugin.addTrailMemberAdvanced(null, id, spriteToUse, projectile.getLocation(),
                        startSpeed * ((float)i2 / 70f), fizzleConstantSpeed * (1f - (float)i2 / 70f),
                        angle, startAngularVelocity * ((float)i2 / 70f), fizzleConstantAngle * (1f - (float)i2 / 70f), startSize, 0f,
                        colorToUse, colorToUse,0.45f, 0f, 0.5f * ((float)i2 / 70f) * lifetimeMult, 1.1f * ((float)i2 / 70f) * lifetimeMult,
                        GL_SRC_ALPHA, GL_ONE,500f, 600f, 0f, new Vector2f(0f, 0f), null, CombatEngineLayers.CONTRAILS_LAYER, 1f);
            }
        }

        //Also spawns lightning, though only against ships, and only on direct hull hits
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI)target;

            float hitLevel = 0f;
            float emp = projectile.getEmpAmount() * 0.33f;
            float dam = projectile.getDamageAmount() * 0.05f;
            for (int x = 0; x < 4; x++) {
                float pierceChance = 0.25f;
                boolean triggered = (float) Math.random() < pierceChance;
                if (!shieldHit && triggered) {
                    hitLevel += 0.25f;
                    ShipAPI empTarget = ship;
                    EmpArcEntityAPI arc =  engine.spawnEmpArcPierceShields(projectile.getSource(), point, empTarget, empTarget,
                            projectile.getDamageType(), dam, emp, 1000f * hitLevel, null, 20f, COLOR1, COLOR2);
                }
            }

            if (hitLevel > 0f) {
                engine.addSmoothParticle(point, new Vector2f(0f, 0f), 300f * hitLevel, hitLevel, 0.75f, COLOR1);
                Global.getSoundPlayer().playSound("tachyon_lance_emp_impact", 1f - (0.2f * hitLevel), hitLevel, point, new Vector2f(0f, 0f));
            }
        }
    }
}

