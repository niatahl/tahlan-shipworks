package org.niatahl.tahlan.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.magiclib.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FluxBlastStats extends BaseShipSystemScript {

    private static final Color EXPLOSION_COLOR = new Color(232, 176, 255, 10);
    private static final String SOUND_ID = "tahlan_cashmere_impact";
    private static final Vector2f ZERO = new Vector2f();

    boolean runOnce = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {


        ShipAPI ship = null;
        boolean player = false;
        CombatEngineAPI engine = Global.getCombatEngine();

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        if (state == State.OUT) {
            runOnce = false;
            return;
        }

        if (state == State.ACTIVE && !runOnce) {
            runOnce = true;

            Vector2f point = ship.getLocation();
            float fluxLevel = ship.getFluxLevel();

            ship.getFluxTracker().decreaseFlux(ship.getFluxTracker().getCurrFlux() * 0.5f);

            DamagingExplosionSpec blast = new DamagingExplosionSpec(0.1f,
                    500f,
                    250f,
                    1000f * fluxLevel,
                    500f * fluxLevel,
                    CollisionClass.PROJECTILE_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    10f,
                    10f,
                    0f,
                    0,
                    EXPLOSION_COLOR,
                    null);
            blast.setDamageType(DamageType.FRAGMENTATION);
            blast.setShowGraphic(false);
            engine.spawnDamagingExplosion(blast, ship, point, false);

            // Blast visuals
            engine.addHitParticle(
                    point,
                    new Vector2f(),
                    350,
                    0.1f,
                    1f,
                    Color.blue);
            engine.addSmoothParticle(
                    point,
                    new Vector2f(),
                    600,
                    2f,
                    0.25f,
                    Color.white);
            engine.addSmoothParticle(
                    point,
                    new Vector2f(),
                    450,
                    2f,
                    0.1f,
                    Color.white);

            Global.getSoundPlayer().playSound(SOUND_ID, 1f, 1f, point, ZERO);

            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "tahlan_cashmere_blast"),
                    point,
                    new Vector2f(),
                    new Vector2f(256, 256),
                    new Vector2f(480, 480),
                    //angle,
                    360 * (float) Math.random(),
                    0,
                    new Color(255, 95, 126, 50+(int)(205*fluxLevel)),
                    true,
                    0,
                    0.1f,
                    0.2f
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "tahlan_cashmere_blast"),
                    point,
                    new Vector2f(),
                    new Vector2f(384, 384),
                    new Vector2f(240, 240),
                    //angle,
                    360 * (float) Math.random(),
                    0,
                    new Color(255, 225, 225, 50+(int)(205*fluxLevel)),
                    true,
                    0.2f,
                    0.0f,
                    0.4f
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "tahlan_cashmere_blast"),
                    point,
                    new Vector2f(),
                    new Vector2f(512, 512),
                    new Vector2f(120, 120),
                    //angle,
                    360 * (float) Math.random(),
                    0,
                    new Color(116, 148, 255, 50+(int)(205*fluxLevel)),
                    true,
                    0.3f,
                    0.0f,
                    0.4f
            );
        }
    }
}
