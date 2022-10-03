package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GleipnirScript implements EveryFrameWeaponEffectPlugin {

    private static Color EFFECT_COLOR_HE = new Color(255, 85, 10);
    private static Color EFFECT_COLOR_KE = new Color(100, 200, 255);

    private boolean hasFiredThisCharge = false;
    private List<DamagingProjectileAPI> registeredProjectiles = new ArrayList<DamagingProjectileAPI>();
    private int shotCounter = 0;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (engine.isPaused() || weapon == null) {
            return;
        }

        if (weapon.getCooldownRemaining() > 0f && weapon.getChargeLevel() < 1f) {
            shotCounter = 0;
        }

        //float chargeLevel = weapon.getChargeLevel();

        //if (chargeLevel >= 1f && !hasFiredThisCharge) {

        for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(weapon.getLocation(), 400f)) {
            if (proj.getWeapon() == weapon && !registeredProjectiles.contains(proj)) {
                shotCounter++;
                registeredProjectiles.add(proj);
                if (shotCounter == 1) {
                    //Visuals for KE shot
                    for (int i = 0; i < 7; i++) {
                        Vector2f point = MathUtils.getRandomPointInCircle(weapon.getLocation(), MathUtils.getRandomNumberInRange(50f, 100f));
                        EmpArcEntityAPI arc =  engine.spawnEmpArcPierceShields(weapon.getShip(), weapon.getLocation(), weapon.getShip(),
                                new SimpleEntity(point),
                                DamageType.FRAGMENTATION,
                                0f,
                                0f,
                                150f,
                                null,
                                5f,
                                EFFECT_COLOR_KE,
                                Color.white
                        );
                    }
                    Global.getCombatEngine().spawnExplosion(weapon.getLocation(), new Vector2f(0f, 0f), EFFECT_COLOR_KE, 150f, 0.5f);
                    MagicLensFlare.createSharpFlare(engine, weapon.getShip(), weapon.getLocation(), 5, 700, 0, EFFECT_COLOR_KE, Color.white);
                } else if (shotCounter == 2) {
                    //Visuals for HE shot
                    for (int i = 0; i < 7; i++) {
                        Vector2f point = MathUtils.getRandomPointInCircle(weapon.getLocation(), MathUtils.getRandomNumberInRange(50f, 100f));
                        EmpArcEntityAPI arc =  engine.spawnEmpArcPierceShields(weapon.getShip(), weapon.getLocation(), weapon.getShip(),
                                new SimpleEntity(point),
                                DamageType.FRAGMENTATION,
                                0f,
                                0f,
                                150f,
                                null,
                                5f,
                                EFFECT_COLOR_HE,
                                Color.white
                        );
                    }
                    Global.getCombatEngine().spawnExplosion(weapon.getLocation(), new Vector2f(0f, 0f), EFFECT_COLOR_HE, 150f, 0.5f);
                    MagicLensFlare.createSharpFlare(engine, weapon.getShip(), weapon.getLocation(), 5, 700, 0, EFFECT_COLOR_HE, Color.white);

                    //Replace projectile with HE version
                    DamagingProjectileAPI newProj = (DamagingProjectileAPI) engine.spawnProjectile(weapon.getShip(),weapon,weapon.getId()+"_dummy", proj.getLocation(),proj.getFacing(),weapon.getShip().getVelocity());
                    Global.getCombatEngine().removeEntity(proj);
                    registeredProjectiles.add(newProj);

                }
            }
        }
    }
}
