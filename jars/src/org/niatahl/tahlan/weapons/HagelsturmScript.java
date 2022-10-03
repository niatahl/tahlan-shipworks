package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.plugins.MagicFakeBeamPlugin;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HagelsturmScript implements EveryFrameWeaponEffectPlugin {

    private List<DamagingProjectileAPI> registeredProjectiles = new ArrayList<DamagingProjectileAPI>();

    public static final float BEAM_WIDTH = 3f;
    public static final Color BEAM_CORE = Color.red;
    public static final Color BEAM_FRINGE = Color.red.darker();

    private final IntervalUtil render = new IntervalUtil(0.01f, 0.01f);

    private HagelsturmMuzzleFlashScript muzzleFlashScript = null;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        //Run our muzzle flash script, and get one if we don't already have one
        if (muzzleFlashScript == null) {
            muzzleFlashScript = new HagelsturmMuzzleFlashScript();
        }
        muzzleFlashScript.advance(amount, engine, weapon);

        float fadeDuration = 0f;
        float angle = weapon.getCurrAngle();
        float muzzle = weapon.getSpec().getTurretFireOffsets().get(0).getX();
        Vector2f weaponLoc = weapon.getLocation();
        float x = weaponLoc.x + 5f;
        float y = weaponLoc.y - 1f;
        Vector2f offset = new Vector2f(x, y);
        Vector2f from = VectorUtils.rotateAroundPivot(offset, weapon.getLocation(), angle);
        float length = 60f;

        MagicFakeBeamPlugin.addBeam(0f, fadeDuration, BEAM_WIDTH, from, angle, length, BEAM_CORE, BEAM_FRINGE);

    }
}
