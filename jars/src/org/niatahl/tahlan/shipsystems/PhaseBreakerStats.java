package org.niatahl.tahlan.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;

public class PhaseBreakerStats extends BaseShipSystemScript {


    public static final float SHIP_ALPHA_MULT = 0.25f;
    public static final float VULNERABLE_FRACTION = 0f;

    public static final float MAX_TIME_MULT = 3f;

    private float activeTime = 0f;
    private boolean runOnce = false;

    private static final float PHASE_RATIO = 0.33f;


    protected Object STATUSKEY2 = new Object();


    public static float getMaxTimeMult(MutableShipStatsAPI stats) {
        return 1f + (MAX_TIME_MULT - 1f) * stats.getDynamic().getValue(Stats.PHASE_TIME_BONUS_MULT);
    }


    protected float getDisruptionLevel(ShipAPI ship) {
        return 0f;
    }

    protected void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {
        float f = VULNERABLE_FRACTION;

        ShipSystemAPI cloak = playerShip.getPhaseCloak();
        if (cloak == null) cloak = playerShip.getSystem();
        if (cloak == null) return;

        if (effectLevel > f) {
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
                    cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "time flow altered", false);
        }
    }

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship;
        boolean player;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        if (player) {
            maintainStatus(ship, state, effectLevel);
        }

        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        ShipSystemAPI cloak = ship.getPhaseCloak();
        if (cloak == null) cloak = ship.getSystem();
        if (cloak == null) return;

        if (state == State.COOLDOWN || state == State.IDLE) {
            unapply(stats, id);
            return;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        activeTime += engine.getElapsedInLastFrame();

        float activeFraction = activeTime/cloak.getChargeActiveDur();
        float levelForAlpha = effectLevel;




        if (state == State.IN) {
            ship.setPhased(true);
            levelForAlpha = effectLevel;
        } else if (state == State.ACTIVE || state == State.OUT) {
            if (activeFraction <= PHASE_RATIO) {
                ship.setPhased(true);
                levelForAlpha = effectLevel;
            } else {
                if (!runOnce) {
                    Global.getSoundPlayer().playSound("system_phase_cloak_deactivate",1f,1f,ship.getLocation(),ship.getVelocity());
                    runOnce = true;
                }
                Global.getSoundPlayer().playLoop("system_temporalshell_loop",ship,1f,1f,ship.getLocation(),ship.getVelocity());
                ship.setPhased(false);
                levelForAlpha = Math.max(1f-(activeFraction-PHASE_RATIO)*10f, 0f);
                ship.setJitterUnder(id,new Color(239, 40, 110,80),1f-levelForAlpha,10,8f);
            }
        }

//        Global.getCombatEngine().maintainStatusForPlayerShip("tahlan_debug",cloak.getSpecAPI().getIconSpriteName(),"cloak","active: "+levelForAlpha,false);

        ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha);
        ship.setApplyExtraAlphaToEngines(true);

        float shipTimeMult = 1f + (getMaxTimeMult(stats) - 1f) * effectLevel;
        stats.getTimeMult().modifyMult(id, shipTimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);

        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

        float speedPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(0f);
        stats.getMaxSpeed().modifyPercent(id+"_skillmod", speedPercentMod * effectLevel);

    }


    public void unapply(MutableShipStatsAPI stats, String id) {

        ShipAPI ship;

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        ShipSystemAPI cloak = ship.getPhaseCloak();
        if (cloak == null) cloak = ship.getSystem();
        if (cloak == null) return;

        runOnce = false;

        ship.setJitterUnder(id,cloak.getSpecAPI().getEffectColor2(),0f,10,2f);

        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);

        ship.setPhased(false);
        ship.setExtraAlphaMult(1f);
        activeTime = 0f;
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }
}
