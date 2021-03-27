package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.plugins.MagicTrailPlugin;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static org.lwjgl.opengl.GL11.*;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class tahlan_CashmereScript implements EveryFrameWeaponEffectPlugin {

    private static final Color PARTICLE_COLOR = new Color(185, 52, 255);
    private static final Color GLOW_COLOR = new Color(185, 52, 255, 50);
    private static final Color FLASH_COLOR = new Color(255, 212, 215);
    private static final int NUM_PARTICLES = 30;

    private final String CHARGE_SOUND_ID = "tahlan_virtue_loop";

    private boolean hasFiredThisCharge = false;
    private IntervalUtil effectInterval = new IntervalUtil(0.05f, 0.1f);
    private IntervalUtil trailInterval = new IntervalUtil(0.02f, 0.02f);

    private List<DamagingProjectileAPI> registeredProjectiles = new ArrayList<DamagingProjectileAPI>();

    //A map for known projectiles and their IDs: should be cleared in init
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs = new WeakHashMap<>();
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs2 = new WeakHashMap<>();
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs3 = new WeakHashMap<>();

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (engine.isPaused() || weapon == null) {
            return;
        }

        float chargelevel = weapon.getChargeLevel();

        if (hasFiredThisCharge && (chargelevel <= 0f || !weapon.isFiring())) {
            hasFiredThisCharge = false;
        }

        //Muzzle location calculation
        Vector2f point = new Vector2f();

        if (weapon.getSlot().isHardpoint()) {
            point.x = weapon.getSpec().getHardpointFireOffsets().get(0).x;
            point.y = weapon.getSpec().getHardpointFireOffsets().get(0).y;
        } else if (weapon.getSlot().isTurret()) {
            point.x = weapon.getSpec().getTurretFireOffsets().get(0).x;
            point.y = weapon.getSpec().getTurretFireOffsets().get(0).y;
        } else {
            point.x = weapon.getSpec().getHiddenFireOffsets().get(0).x;
            point.y = weapon.getSpec().getHiddenFireOffsets().get(0).y;
        }

        point = VectorUtils.rotate(point, weapon.getCurrAngle(), new Vector2f(0f, 0f));
        point.x += weapon.getLocation().x;
        point.y += weapon.getLocation().y;


        //Chargeup visuals
        if (chargelevel > 0f && !hasFiredThisCharge) {
            Global.getSoundPlayer().playLoop(CHARGE_SOUND_ID, weapon, (0.85f + weapon.getChargeLevel()*2f), (0.6f + (weapon.getChargeLevel() * 0.4f)), weapon.getLocation(), new Vector2f(0f, 0f));

            effectInterval.advance(engine.getElapsedInLastFrame());
            if (effectInterval.intervalElapsed()){
                Vector2f arcPoint = MathUtils.getRandomPointInCircle(point,75f*chargelevel);
                EmpArcEntityAPI arc =  engine.spawnEmpArcPierceShields(weapon.getShip(), point, weapon.getShip(),
                        new SimpleEntity(arcPoint),
                        DamageType.FRAGMENTATION,
                        0f,
                        0f,
                        75f,
                        null,
                        5f+10f*chargelevel,
                        PARTICLE_COLOR,
                        FLASH_COLOR
                );
            }
        }

        //Firing visuals
        if (chargelevel >= 1f && !hasFiredThisCharge) {
            hasFiredThisCharge = true;

            Global.getCombatEngine().spawnExplosion(point, new Vector2f(0f, 0f), PARTICLE_COLOR, 160f, 0.2f);
            Global.getCombatEngine().spawnExplosion(point, new Vector2f(0f, 0f), FLASH_COLOR, 80f, 0.2f);
            engine.addSmoothParticle(point, ZERO, 200f, 0.7f, 0.1f, PARTICLE_COLOR);
            engine.addSmoothParticle(point, ZERO, 300f, 0.7f, 1f, GLOW_COLOR);
            engine.addHitParticle(point, ZERO, 400f, 1f, 0.05f, FLASH_COLOR);
            for (int x = 0; x < NUM_PARTICLES; x++) {
                engine.addHitParticle(point,
                        MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 150f), (float) Math.random() * 360f),
                        5f, 1f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), PARTICLE_COLOR);
            }
        }

        trailInterval.advance(amount);
        if (trailInterval.intervalElapsed()) {
            //Projectile trails

            for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(weapon.getLocation(), 100f)) {
                if (weapon == proj.getWeapon() && !registeredProjectiles.contains(proj)) {
                    registeredProjectiles.add(proj);
                }
            }

            for (DamagingProjectileAPI proj : registeredProjectiles) {
                String specID = proj.getProjectileSpecId();
                Vector2f projVel = new Vector2f(proj.getVelocity());
                SpriteAPI spriteToUse = Global.getSettings().getSprite("fx", "tahlan_trail_foggy");

                //Ignore already-collided projectiles, and projectiles that don't match our IDs
                if (proj.getProjectileSpecId() == null || proj.didDamage()) {
                    continue;
                }

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

                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, spawnPosition, 0, 0, proj.getFacing() - 180f,
                        0, 0, 90f, 5f, new Color(185, 52, 255), new Color(165, 52, 255), 0.7f * opacityMult,
                        0.2f * durationMult, 0.05f * durationMult, 0.05f * durationMult, GL_SRC_ALPHA, GL_ONE, 200, 1600,
                        sidewayVel, null, CombatEngineLayers.CONTRAILS_LAYER, 1f);


                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 0, 0, proj.getFacing() - 180f,
                        0, 0, 120f, 15f, new Color(150, 59, 255), new Color(150, 59, 255), 0.5f * opacityMult,
                        0.2f * durationMult, 0.07f * durationMult, 0.1f * durationMult, GL_SRC_ALPHA, GL_ONE, 200, 1400,
                        sidewayVel, null, CombatEngineLayers.CONTRAILS_LAYER, 1f);

                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs3.get(proj), spriteToUse, spawnPosition, 0, 0, proj.getFacing() - 180f,
                        0, 0, 60f, 3f, Color.white, Color.white, 0.5f * opacityMult,
                        0.1f * durationMult, 0.05f * durationMult, 0.05f * durationMult, GL_SRC_ALPHA, GL_ONE, 200, 1400,
                        sidewayVel, null, CombatEngineLayers.CONTRAILS_LAYER, 1f);
            }

        }
    }
}


