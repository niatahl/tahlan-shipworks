package org.niatahl.tahlan.shipsystems;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class PeriodicBreakerStats extends BaseShipSystemScript {
    public static final float TIME_MULT_PLAYER = 100.0f;
    public static final float TIME_MULT_AI = 100.0f;

    public static final float BEAM_DAMAGE_PENALTY = 1.0f;
    public static final float BEAM_FLUX_PENALTY = 1.0f;

    public static final Color JITTER_COLOR = new Color(255, 106, 32,55);
    public static final Color JITTER_UNDER_COLOR = new Color(255, 54, 0,155);

    public static final float ELECTRIC_SIZE = 80.0f;
    public static final float ELECTRIC_SIZE_SCHIAVONA = 300.0f;

    public boolean HAS_FIRED_LIGHTNING = false;


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        //Jitter-based code
        float jitterLevel = effectLevel;
        float jitterRangeBonus = 0;
        float maxRangeBonus = 10f;
        if (state == State.IN) {
            jitterLevel = effectLevel / (1f / ship.getSystem().getChargeUpDur());
            if (jitterLevel > 1) {
                jitterLevel = 1f;
            }
            jitterRangeBonus = jitterLevel * maxRangeBonus;
        } else if (state == State.ACTIVE) {
            jitterLevel = 1f;
            jitterRangeBonus = maxRangeBonus;
        } else if (state == State.OUT) {
            jitterRangeBonus = jitterLevel * maxRangeBonus;
        }
        jitterLevel = (float) Math.sqrt(jitterLevel);
        effectLevel *= effectLevel;
        effectLevel *= effectLevel;

        ship.setJitter(this, JITTER_COLOR, jitterLevel, 3, 0, 0 + jitterRangeBonus);
        ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 25, 0f, 7f + jitterRangeBonus);

        //Adjusts time mult
        float shipTimeMult = 1f;
        if (player) {
            shipTimeMult = 1f + (TIME_MULT_PLAYER - 1f) * effectLevel;
            stats.getTimeMult().modifyMult(id, shipTimeMult);
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
        } else {
            shipTimeMult = 1f + (TIME_MULT_AI - 1f) * effectLevel;
            stats.getTimeMult().modifyMult(id, shipTimeMult);
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

        //Changes engine color
        ship.getEngineController().fadeToOtherColor(this, JITTER_COLOR, new Color(0,0,0,0), 1.0f, 1.0f);
        ship.getEngineController().extendFlame(this, -0.25f, -0.25f, -0.25f);

        //Makes beams do 0 damage
        stats.getBeamWeaponFluxCostMult().modifyMult(id, 1 - BEAM_FLUX_PENALTY * effectLevel);
        stats.getBeamWeaponDamageMult().modifyMult(id, 1 - BEAM_DAMAGE_PENALTY * effectLevel);

        //Fires lightning at full charge, once
        float actualElectricSize = ELECTRIC_SIZE;
        if (ship.getHullSpec().getHullId().contains("tahlan_Izanami")) {
            actualElectricSize = ELECTRIC_SIZE_SCHIAVONA;
        }

        if (effectLevel >= 0.8f) {
            if (!HAS_FIRED_LIGHTNING) {
                HAS_FIRED_LIGHTNING = true;
                /*Lightning based code...*/
                float tempCounter = 0;
                while (tempCounter <= (6.0f / ELECTRIC_SIZE) * actualElectricSize) {
                    Global.getCombatEngine().spawnEmpArc(ship,new Vector2f(ship.getLocation().x + MathUtils.getRandomNumberInRange(-actualElectricSize, actualElectricSize), ship.getLocation().y + MathUtils.getRandomNumberInRange(-actualElectricSize, actualElectricSize)), null, ship,
                            DamageType.ENERGY, //Damage type
                            0f, //Damage
                            0f, //Emp
                            100000f, //Max range
                            "tachyon_lance_emp_impact",
                            (10f / ELECTRIC_SIZE) * actualElectricSize, // thickness
                            JITTER_COLOR, //Central color
                            JITTER_UNDER_COLOR //Fringe Color
                    );
                    tempCounter++;
                }
                //visual effect
                Global.getCombatEngine().spawnExplosion(
                        //where
                        ship.getLocation(),
                        //speed
                        (Vector2f) new Vector2f(0,0),
                        //color
                        JITTER_COLOR,
                        //size
                        (MathUtils.getRandomNumberInRange(75f,100f) / ELECTRIC_SIZE) * actualElectricSize,
                        //duration
                        1.0f
                );
            }
        } else {
            HAS_FIRED_LIGHTNING = false;
        }

    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        stats.getFluxDissipation().unmodify(id);
        stats.getFluxCapacity().unmodify(id);
        if (ship.getFluxTracker().getFluxLevel() >= 1f) {
            ship.getFluxTracker().forceOverload(ship.getFluxTracker().getFluxLevel() * 3);
            ship.getFluxTracker().setCurrFlux(stats.getFluxCapacity().getModifiedValue());
        }
        stats.getMaxSpeed().unmodify(id);

        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);

        stats.getBeamWeaponFluxCostMult().unmodify(id);
        stats.getBeamWeaponDamageMult().unmodify(id);

        HAS_FIRED_LIGHTNING = false;
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0 && state == State.IN) {
            return new StatusData("rupturing time-space...", false);
        } else if (index == 0 && state == State.ACTIVE) {
            return new StatusData("time is at a standstill", false);
        } else if (index == 0 && state == State.OUT) {
            return new StatusData("readjusting protocols...", false);
        }
//		if (index == 1) {
//			return new StatusData("beam weapons are useless now", false);
//		}
//		if (index == 2) {
//			return new StatusData("increased acceleration", false);
//		}
        return null;
    }
}