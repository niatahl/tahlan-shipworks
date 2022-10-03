package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BalorWeaponScript implements EveryFrameWeaponEffectPlugin {

    private final List<DamagingProjectileAPI> alreadyRegisteredProjectiles = new ArrayList<DamagingProjectileAPI>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        ShipAPI source = weapon.getShip();
        ShipAPI target = null;

        if (source.getWeaponGroupFor(weapon) != null) {
            //WEAPON IN AUTOFIRE
            if (source.getWeaponGroupFor(weapon).isAutofiring()  //weapon group is autofiring
                    && source.getSelectedGroupAPI() != source.getWeaponGroupFor(weapon)) { //weapon group is not the selected group
                target = source.getWeaponGroupFor(weapon).getAutofirePlugin(weapon).getTargetShip();
            } else {
                target = source.getShipTarget();
            }
        }

        for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(weapon.getLocation(), 200f)) {
            if (proj.getWeapon() == weapon && !alreadyRegisteredProjectiles.contains(proj) && engine.isEntityInPlay(proj) && !proj.didDamage()) {
                engine.addPlugin(new BalorProjectileScript(proj, target));
                alreadyRegisteredProjectiles.add(proj);

                Vector2f projVel;
                Vector2f smokeVel;

                for (int i = 0; i < 10; i++) {
                    projVel = new Vector2f();
                    smokeVel = new Vector2f();
                    proj.getVelocity().normalise(projVel);
                    projVel.scale(MathUtils.getRandomNumberInRange(0f,30f));
                    Vector2f.add(weapon.getShip().getVelocity(),projVel,smokeVel);

                    engine.addNebulaParticle(
                            proj.getLocation(),
                            smokeVel,
                            MathUtils.getRandomNumberInRange(20f, 30f),
                            1.5f,
                            0.1f,
                            0.3f,
                            MathUtils.getRandomNumberInRange(2f, 2.5f),
                            new Color(50, 48, 45, 120),
                            true
                    );
                }
            }
        }

        //And clean up our registered projectile list
        List<DamagingProjectileAPI> cloneList = new ArrayList<>(alreadyRegisteredProjectiles);
        for (DamagingProjectileAPI proj : cloneList) {
            if (!engine.isEntityInPlay(proj) || proj.didDamage()) {
                alreadyRegisteredProjectiles.remove(proj);
            }
        }

    }
}
