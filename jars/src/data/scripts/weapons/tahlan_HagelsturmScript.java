package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.plugins.MagicFakeBeamPlugin;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class tahlan_HagelsturmScript implements EveryFrameWeaponEffectPlugin {

    private List<DamagingProjectileAPI> registeredProjectiles = new ArrayList<DamagingProjectileAPI>();

    public static final float BEAM_WIDTH = 3f;
    public static final Color BEAM_CORE = Color.red;
    public static final Color BEAM_FRINGE = Color.red.darker();

    private final IntervalUtil render = new IntervalUtil(0.05f, 0.05f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        render.advance(amount);

        if (render.intervalElapsed()) {
            float duration = render.getIntervalDuration();
            float fadeDuration = 0f;
            float angle = weapon.getCurrAngle();
            float muzzle = weapon.getSpec().getTurretFireOffsets().get(0).getX();
            Vector2f weaponLoc = weapon.getLocation();
            //float x = weaponLoc.x + muzzle - 30f;
            float x = weaponLoc.x + 5f;
            float y = weaponLoc.y - 1f;
            Vector2f offset = new Vector2f(x, y);
            Vector2f from = VectorUtils.rotateAroundPivot(offset, weapon.getLocation(), angle);
            float length = 40f;

            MagicFakeBeamPlugin.addBeam(duration, fadeDuration, BEAM_WIDTH, from, angle, length, BEAM_CORE, BEAM_FRINGE);
        }

        ShipAPI ship = weapon.getShip();

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
