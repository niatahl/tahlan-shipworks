package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.ArrayList;
import java.util.List;

public class tahlan_StahlhagelAmmoScript implements EveryFrameWeaponEffectPlugin {

    private List<DamagingProjectileAPI> registeredProjectiles = new ArrayList<DamagingProjectileAPI>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        if (ship.getSystem() == null) {
            return;
        }

        if (ship.getSystem().getId().equals("tahlan_explosiverounds") && ship.getSystem().isActive()) {

            for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(weapon.getLocation(), 100f)) {
                if (weapon == proj.getWeapon() && !registeredProjectiles.contains(proj)) {
                    registeredProjectiles.add(proj);
                    //Replace projectile with explosive version
                    DamagingProjectileAPI newProj = (DamagingProjectileAPI) engine.spawnProjectile(ship, weapon, "tahlan_stahlhagel_dummy", proj.getLocation(), proj.getFacing(), ship.getVelocity());
                    newProj.setDamageAmount(proj.getDamageAmount());
                    Global.getCombatEngine().removeEntity(proj);
                    registeredProjectiles.add(newProj);
                }
            }


        }
    }
}
