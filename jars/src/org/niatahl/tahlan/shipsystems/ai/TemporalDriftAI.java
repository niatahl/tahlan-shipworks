package org.niatahl.tahlan.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class TemporalDriftAI implements ShipSystemAIScript {
    private ShipAPI ship;
    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private ShipSystemAPI system;
    private final IntervalUtil tracker = new IntervalUtil(0.1F, 0.2F);
    private final float targetVentTime = 3.5f; // Ease down is 4s, so ~3.5s leaves 0.5s for shield raising after vent
    private final float fluxMargin = 0.05f; // Flux left as margin of error for overloads, 5% seems fine

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
        this.system = system;
    }

    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (!engine.isPaused() && ship.getAIFlags() != null){
            ShipwideAIFlags shipAIFlags = ship.getAIFlags();
            FluxTrackerAPI shipFlux = ship.getFluxTracker();
            float fluxPerUse = system.getFluxPerUse()/shipFlux.getMaxFlux();

            // Force shields on every frame. Izanami can dump flux using system, but AI doesn't know that and will try and armor tank unnecessarily
            if (shipAIFlags.hasFlag(AIFlags.HAS_INCOMING_DAMAGE) && shipFlux.getFluxLevel()+fluxPerUse+fluxMargin < 1f) {
                if (ship.getShield().isOn())
                    ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                else
                    ship.getShield().toggleOn();
            }

            tracker.advance(amount);
            if (tracker.intervalElapsed()) {
                if (ship.getShipAI() == null) {
                    ShipAIConfig config = new ShipAIConfig();
                    Global.getSettings().createDefaultShipAI(ship, config);
                }
                // Force Izanami to not back off even at high flux if shipsystem is available
                if(!system.isCoolingDown() || shipFlux.getFluxLevel()+fluxPerUse+fluxMargin+(system.getCooldownRemaining()/system.getCooldown()) < 1f){
                    shipAIFlags.setFlag(AIFlags.DO_NOT_BACK_OFF, 0.2f);
                    shipAIFlags.setFlag(AIFlags.DO_NOT_BACK_OFF_EVEN_WHILE_VENTING,0.2f);
                    ship.getShipAI().getConfig().backingOffWhileNotVentingAllowed = false;
                    ship.getShipAI().getConfig().alwaysStrafeOffensively = true;
                }
                else {
                    ship.getShipAI().getConfig().backingOffWhileNotVentingAllowed = true;
                    ship.getShipAI().getConfig().alwaysStrafeOffensively = false;
                }
                ship.getShipAI().getConfig().turnToFaceWithUndamagedArmor = false;

                if (!system.isActive()) {

                    // use system to vent, but only sometimes
                    if ((shipFlux.getFluxLevel() + fluxPerUse) > Math.min(targetVentTime / shipFlux.getTimeToVent(), 1f - fluxMargin) && Math.random() > 0.9f) {
                        ship.useSystem();
                    }

                    // use system to attack
                    if (shipFlux.getFluxLevel() < 0.1f &&
                            (shipAIFlags.hasFlag(AIFlags.PURSUING) || (ship.areAnyEnemiesInRange() && !shipAIFlags.hasFlag(AIFlags.BACKING_OFF)))) {
                        ship.useSystem();
                    }

                    // use system to punish
                    if (target != null && target.getFluxTracker().isVenting()) {
                        ship.useSystem();
                    }
                }
                else if(shipFlux.getFluxLevel() > 0.25f) {
                    ship.giveCommand(ShipCommand.VENT_FLUX, (Object)null, 0);
                }
            }
        }
    }
}
