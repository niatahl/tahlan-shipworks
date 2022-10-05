package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.niatahl.tahlan.weapons.deco.LostechRangeEffect;

import java.awt.*;

public class VelvetScript implements EveryFrameWeaponEffectPlugin {

    private float flux = 0;
    private final IntervalUtil time = new IntervalUtil(0.18f, 0.22f);

    private static final Color ARC_FRINGE_COLOR = new Color(185, 52, 255);
    private static final Color ARC_CORE_COLOR = new Color(255, 212, 215);

    private LostechRangeEffect rangeModifier = null;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (engine.isPaused()) {
            return;
        }

        if (rangeModifier == null) {
            rangeModifier = new LostechRangeEffect();
        }
        rangeModifier.advance(amount, engine, weapon);

        //weapon is actually firing
        if (weapon.getChargeLevel() == 1) {
            //EXTRA FLUX
            time.advance(amount);
            if (time.intervalElapsed()) {
                flux = Math.min(flux + time.getElapsed() * 0.1f, 0.5f);
                float fluxToVent = time.getElapsed() * weapon.getDerivedStats().getFluxPerSecond() * flux;
                weapon.getShip().getFluxTracker().setCurrFlux(weapon.getShip().getFluxTracker().getCurrFlux() - fluxToVent);

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

                if (flux > 0.25f) {

                    EmpArcEntityAPI arc =  engine.spawnEmpArcPierceShields(weapon.getShip(), point, weapon.getShip(), weapon.getShip(),
                            DamageType.FRAGMENTATION,
                            0f,
                            fluxToVent,
                            200f,
                            null,
                            5f + 5f * flux,
                            ARC_FRINGE_COLOR,
                            ARC_CORE_COLOR
                    );
                }
            }
        } else {
            flux = 0;
        }
    }
}
