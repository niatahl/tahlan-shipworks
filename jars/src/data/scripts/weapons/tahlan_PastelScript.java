package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicInterference;
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

public class tahlan_PastelScript implements EveryFrameWeaponEffectPlugin {

    private static final Color PARTICLE_COLOR = new Color(41, 255, 150);
    private static final Color GLOW_COLOR = new Color(50, 238, 255, 50);
    private static final Color FLASH_COLOR = new Color(223, 255, 248);
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

        if(weapon.getShip().getOriginalOwner()<0 && !weapon.getSlot().isBuiltIn()){

            MagicInterference.ApplyInterference(weapon.getShip().getVariant());

        }

        float chargelevel = weapon.getChargeLevel();

        if (hasFiredThisCharge && (chargelevel <= 0f || !weapon.isFiring())) {
            hasFiredThisCharge = false;
        }

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

        //Firing visuals
        if (chargelevel >= 1f && !hasFiredThisCharge) {
            hasFiredThisCharge = true;

            Global.getCombatEngine().spawnExplosion(point, new Vector2f(0f, 0f), PARTICLE_COLOR, 80f, 0.1f);
            Global.getCombatEngine().spawnExplosion(point, new Vector2f(0f, 0f), FLASH_COLOR, 40f, 0.1f);
            engine.addSmoothParticle(point, ZERO, 100f, 0.5f, 0.08f, PARTICLE_COLOR);
            engine.addSmoothParticle(point, ZERO, 150f, 0.5f, 0.05f, GLOW_COLOR);
            engine.addHitParticle(point, ZERO, 200f, 0.8f, 0.03f, FLASH_COLOR);
        }


    }
}


