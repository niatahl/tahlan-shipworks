package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.plugins.MagicFakeBeamPlugin;
import data.scripts.plugins.MagicTrailPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static com.fs.starfarer.api.util.Misc.ZERO;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class RyzaScript implements EveryFrameWeaponEffectPlugin {

    private static final Color PARTICLE_COLOR = new Color(19, 206, 255);
    private static final Color GLOW_COLOR = new Color(75, 159, 255, 50);
    private static final Color FLASH_COLOR = new Color(227, 255, 253);
    private static final int NUM_PARTICLES = 30;


    private final String CHARGE_SOUND_ID = "tahlan_ryza_charge";

    private boolean hasFiredThisCharge = false;

    private boolean runOnce = true;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (engine.isPaused() || weapon == null) {
            return;
        }

        float chargelevel = weapon.getChargeLevel();

        if (hasFiredThisCharge && (chargelevel <= 0f || !weapon.isFiring())) {
            hasFiredThisCharge = false;
            runOnce = true;
        }

        if (chargelevel <= 0f) {
            runOnce = true;
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


        //Chargeup visuals
        if (chargelevel > 0f && !hasFiredThisCharge) {
            //Global.getSoundPlayer().playLoop(CHARGE_SOUND_ID, weapon, (0.85f + weapon.getChargeLevel()*2f), (0.6f + (weapon.getChargeLevel() * 0.4f)), weapon.getLocation(), new Vector2f(0f, 0f));
            if (runOnce) {
                Global.getSoundPlayer().playSound(CHARGE_SOUND_ID, 1f, 1f, weapon.getLocation(), weapon.getShip().getVelocity());
                runOnce = false;
            }
        }

        //Firing visuals
        if (chargelevel >= 1f && !hasFiredThisCharge) {
            hasFiredThisCharge = true;

            engine.spawnExplosion(point, new Vector2f(0f, 0f), PARTICLE_COLOR, 90f, 0.2f);
            engine.spawnExplosion(point, new Vector2f(0f, 0f), FLASH_COLOR, 60f, 0.2f);
            engine.addSmoothParticle(point, ZERO, 100f, 0.7f, 0.1f, PARTICLE_COLOR);
            engine.addSmoothParticle(point, ZERO, 200f, 0.7f, 1f, GLOW_COLOR);
            engine.addHitParticle(point, ZERO, 320f, 1f, 0.05f, FLASH_COLOR);
            for (int x = 0; x < NUM_PARTICLES; x++) {
                engine.addHitParticle(point,
                        MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 150f), (float) Math.random() * 360f),
                        5f, 1f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), PARTICLE_COLOR);
            }
        }

    }
}


