package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
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

        for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(weapon.getLocation(), 200f)) {
            if (proj.getWeapon() == weapon && !registeredProjectiles.contains(proj)) {
                registeredProjectiles.add(proj);

                Vector2f loc = proj.getLocation();
                Vector2f vel = proj.getVelocity();
                int splinterCount = MathUtils.getRandomNumberInRange(0,5);
                for (int j = 0; j < splinterCount; j++) {
                    Vector2f randomVel = MathUtils.getRandomPointOnCircumference(null, MathUtils.getRandomNumberInRange(20f, 60f));
                    randomVel.x += vel.x;
                    randomVel.y += vel.y;

                    DamagingProjectileAPI newProj = (DamagingProjectileAPI) engine.spawnProjectile(proj.getSource(), proj.getWeapon(), weapon.getId() + "_splinter", loc, proj.getFacing(),randomVel);
                    registeredProjectiles.add(newProj);
                }
            }
        }
    }
}
