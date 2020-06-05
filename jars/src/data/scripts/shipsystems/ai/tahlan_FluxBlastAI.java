package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class tahlan_FluxBlastAI implements ShipSystemAIScript {

    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private ShipwideAIFlags flags;

    private final IntervalUtil timer = new IntervalUtil(0.35f, 0.6f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.system = system;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {

        if (!AIUtils.canUseSystemThisFrame(ship)) {
            return;
        }

        float desire = 0.25f;

        timer.advance(amount);

        if ( timer.intervalElapsed() ) {

            if (flags.hasFlag(ShipwideAIFlags.AIFlags.NEEDS_HELP)) {
                desire += 0.25f;
            }
            if (flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_VENT)) {
                desire += 0.25f;
            }
            if (flags.hasFlag(ShipwideAIFlags.AIFlags.IN_ATTACK_RUN)) {
                desire += 0.25f;
            }
            if (flags.hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE)) {
                desire += 0.25f;
            }
            if (flags.hasFlag(ShipwideAIFlags.AIFlags.CARRIER_FIGHTER_TARGET)) {
                desire += 0.25f;
            }

            desire += ship.getFluxLevel();

            if ( !system.isActive() && (desire >= 1f || ship.getFluxLevel() > 0.9f) ) {
                ship.useSystem();
            }


        }
    }
}
