package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class tahlan_TaffetaScript implements EveryFrameWeaponEffectPlugin {

    private static final Color PARTICLE_COLOR = new Color(132, 215, 145);
    private static final Color GLOW_COLOR = new Color(161, 243, 255, 50);
    private static final Color FLASH_COLOR = new Color(216, 255, 253, 100);
    private static final int NUM_PARTICLES = 30;

    private final String CHARGE_SOUND_ID = "tahlan_virtue_loop";

    private boolean hasFiredThisCharge = false;
    private IntervalUtil effectInterval = new IntervalUtil(0.05f, 0.1f);

    private List<DamagingProjectileAPI> registeredProjectiles = new ArrayList<DamagingProjectileAPI>();

    //A map for known projectiles and their IDs: should be cleared in init
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs = new WeakHashMap<>();

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (engine.isPaused() || weapon == null) {
            return;
        }

        float chargelevel = weapon.getChargeLevel();

        if (hasFiredThisCharge && (chargelevel <= 0f || !weapon.isFiring())) {
            hasFiredThisCharge = false;
        }

        //Muzzle location calculation
        Vector2f point = new Vector2f();
        Vector2f point2 = new Vector2f();

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

        if (weapon.getSlot().isHardpoint()) {
            point2.x = weapon.getSpec().getHardpointFireOffsets().get(1).x;
            point2.y = weapon.getSpec().getHardpointFireOffsets().get(1).y;
        } else if (weapon.getSlot().isTurret()) {
            point2.x = weapon.getSpec().getTurretFireOffsets().get(1).x;
            point2.y = weapon.getSpec().getTurretFireOffsets().get(1).y;
        } else {
            point2.x = weapon.getSpec().getHiddenFireOffsets().get(1).x;
            point2.y = weapon.getSpec().getHiddenFireOffsets().get(1).y;
        }

        point = VectorUtils.rotate(point, weapon.getCurrAngle(), new Vector2f(0f, 0f));
        point.x += weapon.getLocation().x;
        point.y += weapon.getLocation().y;

        point2 = VectorUtils.rotate(point2, weapon.getCurrAngle(), new Vector2f(0f, 0f));
        point2.x += weapon.getLocation().x;
        point2.y += weapon.getLocation().y;

        //Chargeup visuals
        if (chargelevel > 0f && !hasFiredThisCharge) {
            Global.getSoundPlayer().playLoop(CHARGE_SOUND_ID, weapon, (1.45f + weapon.getChargeLevel()*2f), (0.2f + (weapon.getChargeLevel() * 0.3f)), weapon.getLocation(), new Vector2f(0f, 0f));
        }

        //Firing visuals
        if (chargelevel >= 1f && !hasFiredThisCharge) {
            hasFiredThisCharge = true;

            Global.getCombatEngine().spawnExplosion(point, new Vector2f(0f, 0f), PARTICLE_COLOR, 60f, 0.2f);
            Global.getCombatEngine().spawnExplosion(point, new Vector2f(0f, 0f), FLASH_COLOR, 30f, 0.2f);
            engine.addSmoothParticle(point, ZERO, 100f, 0.7f, 0.1f, PARTICLE_COLOR);
            engine.addSmoothParticle(point, ZERO, 150f, 0.7f, 1f, GLOW_COLOR);
            engine.addHitParticle(point, ZERO, 200f, 1f, 0.05f, FLASH_COLOR);

            Global.getCombatEngine().spawnExplosion(point2, new Vector2f(0f, 0f), PARTICLE_COLOR, 60f, 0.2f);
            Global.getCombatEngine().spawnExplosion(point2, new Vector2f(0f, 0f), FLASH_COLOR, 30f, 0.2f);
            engine.addSmoothParticle(point2, ZERO, 100f, 0.7f, 0.1f, PARTICLE_COLOR);
            engine.addSmoothParticle(point2, ZERO, 150f, 0.7f, 1f, GLOW_COLOR);
            engine.addHitParticle(point2, ZERO, 200f, 1f, 0.05f, FLASH_COLOR);
        }


    }
}


