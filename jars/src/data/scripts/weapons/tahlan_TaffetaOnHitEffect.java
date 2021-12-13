package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class tahlan_TaffetaOnHitEffect implements OnHitEffectPlugin {
    private static final Color CORE_EXPLOSION_COLOR = new Color(220, 241, 255, 105);
    private static final Color CORE_GLOW_COLOR = new Color(217, 235, 241, 60);
    private static final Color EXPLOSION_COLOR = new Color(232, 176, 255, 10);
    private static final Color FLASH_GLOW_COLOR = new Color(214, 231, 241, 120);
    private static final Color GLOW_COLOR = new Color(193, 221, 255, 50);
    private static final Color ARC_FRINGE_COLOR = new Color(112, 253, 255);
    private static final Color ARC_CORE_COLOR = new Color(213, 255, 221);
    private static final String SOUND_ID = "tahlan_cashmere_impact";
    private static final Vector2f ZERO = new Vector2f();

    private static final int NUM_PARTICLES = 50;
    private static final int NUM_ARCS = 5;
    private static final int NUM_SPLINTERS = 20;
    private static final String SPLINTER_WEAPON_ID = "tahlan_taffeta_dummy";

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (projectile.didDamage() && !(target instanceof MissileAPI)) {

            // Blast visuals
            float FlashGlowRadius = 200f;
            float FlashGlowDuration = 0.02f;

            engine.addHitParticle(point, ZERO, FlashGlowRadius, 1f, FlashGlowDuration, FLASH_GLOW_COLOR);


            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx","tahlan_cashmere_blast"),
                    point,
                    new Vector2f(),
                    new Vector2f(48,48),
                    new Vector2f(280,280),
                    //angle,
                    360*(float)Math.random(),
                    0,
                    new Color(168, 255, 190,205),
                    true,
                    0,
                    0.1f,
                    0.2f
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx","tahlan_cashmere_blast"),
                    point,
                    new Vector2f(),
                    new Vector2f(64,64),
                    new Vector2f(240,240),
                    //angle,
                    360*(float)Math.random(),
                    0,
                    new Color(153, 255, 219,185),
                    true,
                    0.2f,
                    0.0f,
                    0.4f
            );

            Vector2f sPoint = new Vector2f(projectile.getLocation());

            if (!shieldHit) {
                Vector2f pVel = projectile.getVelocity();
                pVel.scale(engine.getElapsedInLastFrame());
                sPoint.set(sPoint.x - pVel.x * 1f, sPoint.y - pVel.y * 1f);
            }

            // This spawns the frag, also distributing them in a nice even 360 degree arc
            Vector2f vel = new Vector2f();
            for (int i = 0; i < NUM_SPLINTERS; i++)
            {
                float angle = projectile.getFacing() + i * 360f / NUM_SPLINTERS + (float) Math.random() * 180f / NUM_SPLINTERS;
                angle %= 360f;
                Vector2f location = MathUtils.getPointOnCircumference(sPoint, 75f, angle);
                ShipAPI source = null;
                //This doesn't work cause the API doesn't do what it says it does
                /*if (!shieldHit && !CollisionUtils.isPointWithinBounds(location,target)){
                    source = (ShipAPI)target;
                }*/
                DamagingProjectileAPI newProj = (DamagingProjectileAPI)Global.getCombatEngine().spawnProjectile(source, projectile.getWeapon(), SPLINTER_WEAPON_ID, location, angle, new Vector2f(0,0));
                Vector2f newVel = new Vector2f(newProj.getVelocity());
                //VectorUtils.rotate(newVel,angle,newVel);
                newVel.scale((float)Math.random());
                newProj.getVelocity().set(newVel.x,newVel.y);
            }

            //Old EMP arc stuff in case I just give up on the shrapnel
            /*
            for (int x = 0; x < NUM_ARCS; x++) {
                //If we have no valid targets, zap a random point near us
                if (validTargets.isEmpty()) {
                    validTargets.add(new SimpleEntity(MathUtils.getRandomPointInCircle(point, 500)));
                }

                //And finally, fire at a random valid target
                CombatEntityAPI arcTarget = validTargets.get(MathUtils.getRandomNumberInRange(0, validTargets.size() - 1));
                Global.getCombatEngine().spawnEmpArc(projectile.getSource(), point, projectile.getSource(), arcTarget,
                        DamageType.ENERGY, //Damage type
                        0, //Damage
                        MathUtils.getRandomNumberInRange(0.8f, 1.2f) * 50, //Emp
                        100000f, //Max range
                        null, //Impact sound
                        5f, // thickness of the lightning bolt
                        ARC_CORE_COLOR, //Central color
                        ARC_FRINGE_COLOR //Fringe Color
                );
            }
            */
        }
    }
}
