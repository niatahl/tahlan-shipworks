package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.ArrayList;
import java.util.List;

public class tahlan_LichtAmmoScript implements EveryFrameWeaponEffectPlugin {

    private List<DamagingProjectileAPI> registeredProjectiles = new ArrayList<DamagingProjectileAPI>();

    private tahlan_GlanzMuzzleFlashScript muzzleFlashScript = null;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (engine.isPaused()) {
            return;
        }

        //Run our muzzle flash script, and get one if we don't already have one
        if (muzzleFlashScript == null) {
            muzzleFlashScript = new tahlan_GlanzMuzzleFlashScript();
        }
        muzzleFlashScript.advance(amount, engine, weapon);

        ShipAPI ship = weapon.getShip();

        if (ship.getSystem().getId().equals("tahlan_plasmarounds") && ship.getSystem().isActive()) {

            for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(weapon.getLocation(), 100f)) {
                if (weapon == proj.getWeapon() && !registeredProjectiles.contains(proj)) {
                    registeredProjectiles.add(proj);
                    //Replace projectile with explosive version
                    DamagingProjectileAPI newProj = (DamagingProjectileAPI) engine.spawnProjectile(ship, weapon, "tahlan_sturmkanone_m2", proj.getLocation(), proj.getFacing(), ship.getVelocity());
                    Global.getSoundPlayer().playSound("tahlan_sturm_fire", 1.1f, 0.9f, newProj.getLocation(), newProj.getVelocity());
                    Global.getCombatEngine().removeEntity(proj);
                    registeredProjectiles.add(newProj);
                }
            }


        }
    }
}
