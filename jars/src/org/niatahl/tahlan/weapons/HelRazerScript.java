package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.magiclib.plugins.MagicTrailPlugin;
import org.magiclib.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.niatahl.tahlan.weapons.deco.LostechRangeEffect;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class HelRazerScript implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private static final Color PARTICLE_COLOR = new Color(255, 52, 52);
    private static final Color GLOW_COLOR = new Color(255, 52, 52, 50);
    private static final Color FLASH_COLOR = new Color(255, 212, 212);
    private static final int NUM_PARTICLES = 30;

    private boolean hasFiredThisCharge = false;
    private final IntervalUtil trailInterval = new IntervalUtil(0.02f, 0.02f);

    private final List<DamagingProjectileAPI> registeredProjectiles = new ArrayList<DamagingProjectileAPI>();

    //A map for known projectiles and their IDs: should be cleared in init
    private final Map<DamagingProjectileAPI, Float> projectileTrailIDs = new WeakHashMap<>();
    private final Map<DamagingProjectileAPI, Float> projectileTrailIDs2 = new WeakHashMap<>();
    private final Map<DamagingProjectileAPI, Float> projectileTrailIDs3 = new WeakHashMap<>();

    private LostechRangeEffect rangeModifier = null;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (engine.isPaused() || weapon == null) {
            return;
        }

        //Projectile trails

        for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(weapon.getLocation(), 100f)) {
            if (weapon == proj.getWeapon() && !registeredProjectiles.contains(proj)) {
                registeredProjectiles.add(proj);
            }
        }

        List<DamagingProjectileAPI> toRemove = new ArrayList<DamagingProjectileAPI>();

        for (DamagingProjectileAPI proj : registeredProjectiles) {
            String specID = proj.getProjectileSpecId();
            Vector2f projVel = new Vector2f(proj.getVelocity());
            SpriteAPI spriteToUse = Global.getSettings().getSprite("fx", "tahlan_trail_foggy");

            //Ignore already-collided projectiles, and projectiles that don't match our IDs
            if (proj.getProjectileSpecId() == null || proj.didDamage() || !engine.isEntityInPlay(proj)) {
                toRemove.add(proj);
                continue;
            }

            trailInterval.advance(amount);
            if (trailInterval.intervalElapsed()) {

                //New IDs for new projectiles
                if (projectileTrailIDs.get(proj) == null) {
                    projectileTrailIDs.put(proj, MagicTrailPlugin.getUniqueID());
                    projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    projectileTrailIDs3.put(proj, MagicTrailPlugin.getUniqueID());

                    //Fix for some first-frame error shenanigans
                    if (projVel.length() < 0.1f && proj.getSource() != null) {
                        projVel = new Vector2f(proj.getSource().getVelocity());
                    }
                }


                //Gets a custom "offset" position, so we can slightly alter the spawn location to account for "natural fade-in", and add that to our spawn position
                Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * 50, (float) Math.sin(Math.toRadians(proj.getFacing())) * 50);
                Vector2f spawnPosition = new Vector2f(offsetPoint.x + proj.getLocation().x, offsetPoint.y + proj.getLocation().y);

                //Sideway offset velocity, for projectiles that use it
                Vector2f projBodyVel = VectorUtils.rotate(projVel, -proj.getFacing());
                Vector2f projLateralBodyVel = new Vector2f(0f, projBodyVel.getY());
                Vector2f sidewayVel = (Vector2f) VectorUtils.rotate(projLateralBodyVel, proj.getFacing());

                //Opacity adjustment for fade-out, if the projectile uses it
                float opacityMult = 1f;
                if (proj.isFading()) {
                    opacityMult = Math.max(0, Math.min(1, proj.getDamageAmount() / proj.getBaseDamageAmount()));
                }

                //Duration adjustment for projectile velocity to normalize trail length
                float durationMult = 1 / weapon.getShip().getMutableStats().getProjectileSpeedMult().getModifiedValue();

//                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, spawnPosition, 50, 0, proj.getFacing() - 180f,
//                        0, 0, 110f, 5f, new Color(255, 52, 52), new Color(255, 52, 52), 0.7f * opacityMult,
//                        0.3f * durationMult, 0.1f * durationMult, 0.1f * durationMult, GL_SRC_ALPHA, GL_ONE, 200, 20,
//                        0f, sidewayVel, null, CombatEngineLayers.CONTRAILS_LAYER, 1f);
//
//                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 50, 0, proj.getFacing() - 180f,
//                        0, 0, 140f, 15f, new Color(255, 59, 59), new Color(255, 59, 59), 0.5f * opacityMult,
//                        0.3f * durationMult, 0.2f * durationMult, 0.2f * durationMult, GL_SRC_ALPHA, GL_ONE, 200, 20,
//                        0f, sidewayVel, null, CombatEngineLayers.CONTRAILS_LAYER, 1f);
//
//                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs3.get(proj), spriteToUse, spawnPosition, 50, 0, proj.getFacing() - 180f,
//                        0, 0, 70f, 3f, Color.white, Color.white, 0.5f * opacityMult,
//                        0.2f * durationMult, 0.1f * durationMult, 0.1f * durationMult, GL_SRC_ALPHA, GL_ONE, 200, 20,
//                        0f, sidewayVel, null, CombatEngineLayers.CONTRAILS_LAYER, 1f);
            }

            SpriteAPI flare1 = Global.getSettings().getSprite("fx", "tahlan_lens_flare2");
//            SpriteAPI flare2 = Global.getSettings().getSprite("fx", "tahlan_lens_flare2");
            MagicRender.singleframe(
                    flare1,
                    MathUtils.getRandomPointInCircle(proj.getLocation(), MathUtils.getRandomNumberInRange(0f, 2f)),
                    new Vector2f(MathUtils.getRandomNumberInRange(650f, 700f), MathUtils.getRandomNumberInRange(30f, 35f)),
                    0f,
                    new Color(255,50,50,50),
                    true
            );
//            MagicRender.singleframe(
//                    flare2,
//                    MathUtils.getRandomPointInCircle(proj.getLocation(), MathUtils.getRandomNumberInRange(0f, 2f)),
//                    new Vector2f(MathUtils.getRandomNumberInRange(350f, 400f), MathUtils.getRandomNumberInRange(8f, 12f)),
//                    0f,
//                    Color.white,
//                    true
//            );


        }

        for (DamagingProjectileAPI proj : toRemove) {
            registeredProjectiles.remove(proj);
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        Vector2f point = projectile.getLocation();
        Global.getCombatEngine().spawnExplosion(point, new Vector2f(0f, 0f), PARTICLE_COLOR, 160f, 0.2f);
        Global.getCombatEngine().spawnExplosion(point, new Vector2f(0f, 0f), FLASH_COLOR, 80f, 0.2f);
        engine.addSmoothParticle(point, ZERO, 200f, 0.7f, 0.1f, PARTICLE_COLOR);
        engine.addSmoothParticle(point, ZERO, 300f, 0.7f, 1f, GLOW_COLOR);
        engine.addHitParticle(point, ZERO, 400f, 1f, 0.05f, FLASH_COLOR);
        for (int x = 0; x < NUM_PARTICLES; x++) {
            engine.addHitParticle(point,
                    MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 150f), (float) Math.random() * 360f),
                    5f, 1f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), PARTICLE_COLOR);
            if (Math.random() > 0.5f) {
                Vector2f vel = new Vector2f();
                Vector2f.add(weapon.getShip().getVelocity(), MathUtils.getRandomPointInCircle(ZERO, 40f), vel);
                engine.addNebulaParticle(
                        point,
                        vel,
                        MathUtils.getRandomNumberInRange(40f, 100f),
                        1.2f,
                        0.1f,
                        0.3f,
                        MathUtils.getRandomNumberInRange(2f, 5f),
                        new Color(60, 60, 60, 140),
                        true
                );
            }
        }
    }
}


