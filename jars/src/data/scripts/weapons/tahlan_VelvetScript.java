package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class tahlan_VelvetScript implements EveryFrameWeaponEffectPlugin {

    private float flux = 0;
    private IntervalUtil time = new IntervalUtil(0.18f, 0.22f);

    private static final Color ARC_FRINGE_COLOR = new Color(185, 52, 255);
    private static final Color ARC_CORE_COLOR = new Color(255, 212, 215);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //weapon is actually firing
        if (weapon.getChargeLevel() == 1) {
            //EXTRA FLUX
            time.advance(amount);
            if (time.intervalElapsed()) {
                flux = Math.min(flux + time.getElapsed() / 2, 2f);
                float fluxToVent = time.getElapsed() * weapon.getDerivedStats().getFluxPerSecond() / flux;
                weapon.getShip().getFluxTracker().setCurrFlux(weapon.getShip().getFluxTracker().getCurrFlux() - fluxToVent);

                if (flux > 1) {

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

                    engine.spawnEmpArcPierceShields(weapon.getShip(), point, weapon.getShip(), weapon.getShip(),
                            DamageType.ENERGY,
                            0,
                            fluxToVent / 2,
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
