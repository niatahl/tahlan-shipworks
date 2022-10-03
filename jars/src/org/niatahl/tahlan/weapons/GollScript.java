package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class GollScript implements EveryFrameWeaponEffectPlugin {

    private static Color EFFECT_COLOR_FRINGE = new Color(100, 200, 255, 150);
    private static Color EFFECT_COLOR_CORE = new Color(243, 255, 255, 150);

    private boolean hasFired = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon == null) {
            return;
        }

        if (weapon.getChargeLevel() >= 1f && weapon.isFiring() && !hasFired) {

            Vector2f barrelLocation = new Vector2f(0,0);

            if (weapon.getSlot().isTurret()) {
                barrelLocation.x = weapon.getSpec().getTurretFireOffsets().get(0).x;
                barrelLocation.y = weapon.getSpec().getTurretFireOffsets().get(0).y;
            } else if (weapon.getSlot().isHardpoint()) {
                barrelLocation.x = weapon.getSpec().getHardpointFireOffsets().get(0).x;
                barrelLocation.y = weapon.getSpec().getHardpointFireOffsets().get(0).y;
            } else {
                barrelLocation.x = weapon.getSpec().getHiddenFireOffsets().get(0).x;
                barrelLocation.y = weapon.getSpec().getHiddenFireOffsets().get(0).y;
            }

            barrelLocation = VectorUtils.rotate(barrelLocation,weapon.getCurrAngle(),new Vector2f(0,0));
            barrelLocation.x += weapon.getLocation().x;
            barrelLocation.y += weapon.getLocation().y;

            for (int i = 0; i < 3; i++) {
                Vector2f targetpoint = MathUtils.getRandomPointInCone(barrelLocation,50f, weapon.getCurrAngle()-45f, weapon.getCurrAngle()+45f);
                EmpArcEntityAPI arc =  engine.spawnEmpArcPierceShields(weapon.getShip(), barrelLocation, weapon.getShip(),
                        new SimpleEntity(targetpoint),
                        DamageType.FRAGMENTATION,
                        0f,
                        0f,
                        150f,
                        null,
                        3f,
                        EFFECT_COLOR_FRINGE,
                        EFFECT_COLOR_CORE
                );
            }

        }




    }
}
