package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class tahlan_UnstableTPCScript implements EveryFrameWeaponEffectPlugin {

    private List<DamagingProjectileAPI> registeredProjectiles = new ArrayList<DamagingProjectileAPI>();

    private tahlan_UnstableTPCMuzzleFlashScript muzzleFlashScript = null;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        //Run our muzzle flash script, and get one if we don't already have one
        if (muzzleFlashScript == null) {
            muzzleFlashScript = new tahlan_UnstableTPCMuzzleFlashScript();
        }
        muzzleFlashScript.advance(amount, engine, weapon);

        //Spawns the additional projectiles
        for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(weapon.getLocation(), 300f)) {
            if (proj.getWeapon() == weapon && !registeredProjectiles.contains(proj)) {
                registeredProjectiles.add(proj);

                Vector2f loc = proj.getLocation();
                Vector2f vel = weapon.getShip().getVelocity();
                int splinterCount = MathUtils.getRandomNumberInRange(0,5);
                for (int j = 0; j < splinterCount; j++) {
                    //Gets a random "offset velocity" for our projectile, so they can spread out slightly more tha with just an angle adjustment
                    Vector2f randomVel = MathUtils.getRandomPointOnCircumference(null, MathUtils.getRandomNumberInRange(15f, 40f));
                    randomVel.x += vel.x;
                    randomVel.y += vel.y;

                    //Gets a random angle to offset the projectile by
                    float randomAngle = MathUtils.getRandomNumberInRange(-10f, 10f);

                    DamagingProjectileAPI newProj = (DamagingProjectileAPI) engine.spawnProjectile(proj.getSource(), proj.getWeapon(), weapon.getId() + "_splinter", loc, proj.getFacing()+randomAngle, randomVel);
                    registeredProjectiles.add(newProj);
                }
            }
        }

        //Cleans our memory of all unloaded projectiles to avoid memory leaks
        List<DamagingProjectileAPI> cleanList = new ArrayList<>();
        for (DamagingProjectileAPI proj : registeredProjectiles) {
            if (!engine.isEntityInPlay(proj)) {
                cleanList.add(proj);
            }
        }
        for (DamagingProjectileAPI proj : cleanList) {
            registeredProjectiles.remove(proj);
        }
    }
}
