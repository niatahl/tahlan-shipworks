package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

import static com.fs.starfarer.api.util.Misc.ZERO;

/**
 * Causes fighters to seemingly "teleport" in and out of fighter bays
 * @author Nicke535
 */
public class tahlan_FilamentPortalBays implements EveryFrameWeaponEffectPlugin {
    //Store which fighters have already spawned their teleport flash, and which ones are about to
    private Set<ShipAPI> fightersAlreadyTeleportedIn = new HashSet<>();
    private Set<ShipAPI> fightersTeleportingOut = new HashSet<>();

    //Sounds for teleporting in/out
    private static final String TELEPORT_IN_SOUND = "system_phase_teleporter";
    private static final String TELEPORT_OUT_SOUND = "system_phase_teleporter";

    //Some basic teleport-flash config. More detailed config can be scripted in the function near the script's bottom
    private static final float BASIC_FLASH_SIZE = 30f;
    private static final Color BASIC_FLASH_COLOR = new Color(84, 255, 218, 200);
    private static final Color BASIC_GLOW_COLOR = new Color(80, 161, 255, 200);
    private static final float BASIC_FLASH_DURATION = 0.2f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null || ship.isHulk()) {
            return;
        }

        //Handles teleporting out any fighter that *was* landing, but just finished
        //Also cleans up data in the process
        List<ShipAPI> toRemove = new ArrayList<>();
        for (ShipAPI fighter : fightersTeleportingOut) {
            if (!engine.isEntityInPlay(fighter) || fighter.isHulk() || !fighter.isLanding() || fighter.isFinishedLanding()) {
                //Apply teleport-out effects
                spawnTeleportFlash(fighter, true);
                toRemove.add(fighter);
            }
        }
        //Cleanup
        for (ShipAPI fighter : toRemove) {
            fightersAlreadyTeleportedIn.remove(fighter);
            fightersTeleportingOut.remove(fighter);
        }

        //Gets all our nearby fighters, and grab each one that is currently landing or taking off (and we haven't grabbed earlier)
        for (ShipAPI fighter : CombatUtils.getShipsWithinRange(ship.getLocation(), ship.getCollisionRadius()*1.5f)) {
            //Check for ignored/removed/landed fighters to avoid infinity loops of re-adding
            if (!engine.isEntityInPlay(fighter) || fighter.isHulk() || fighter.isFinishedLanding()) {
                continue;
            }

            //Landing - track them until they finish landing
            if (fighter.isLanding()) {
                //Don't add already-tracked fighters
                if (fightersTeleportingOut.contains(fighter)) {
                    continue;
                }

                //Only *our* fighters are affected
                if (fighter.getWing() != null && fighter.getWing().getSourceShip() == ship) {
                    fightersTeleportingOut.add(fighter);
                }
            }

            //Takeoff - spawn a flash once per fighter
            else {
                //Don't handle already handled fighters
                if (fightersAlreadyTeleportedIn.contains(fighter)) {
                    continue;
                }

                //Only *our* fighters are affected
                if (fighter.getWing() != null && fighter.getWing().getSourceShip() == ship) {
                    fightersAlreadyTeleportedIn.add(fighter);
                    spawnTeleportFlash(fighter, false);
                }
            }
        }
    }

    //The "teleport flash" of a fighter
    //      Anything slapped in here will be spawned as visuals: go ham
    //      Also handles sounds
    private void spawnTeleportFlash(ShipAPI fighter, boolean isLanding) {
        //Sounds, based on if we're landing or not
        if (isLanding) {
            Global.getSoundPlayer().playSound(TELEPORT_OUT_SOUND, 1.2f, 0.3f, fighter.getLocation(), new Vector2f(0f, 0f));
        } else {
            Global.getSoundPlayer().playSound(TELEPORT_IN_SOUND, 1.2f, 0.3f, fighter.getLocation(), new Vector2f(0f, 0f));
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        //Only spawn visuals when on-screen
        if (Global.getCombatEngine().getViewport().isNearViewport(fighter.getLocation(), 500f)) {
            MagicLensFlare.createSharpFlare(engine,fighter,fighter.getLocation(),5f,100f,0f,BASIC_FLASH_COLOR,Color.white);
            engine.addSmoothParticle(fighter.getLocation(), ZERO, 100f, 0.7f, 0.1f, BASIC_FLASH_COLOR);
            engine.addSmoothParticle(fighter.getLocation(), ZERO, 150f, 0.7f, 1f, BASIC_GLOW_COLOR);
            engine.addHitParticle(fighter.getLocation(), ZERO, 200f, 1f, 0.05f, Color.white);


        }
    }
}