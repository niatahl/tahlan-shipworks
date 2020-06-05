package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.*;

/**
 * Tries to align projectile streams with each other to give a "flamer-esque" effect.
 *      Only supports a single-barrel implementation for now, but it's decently simple to convert it to a multi-barrel
 *      solution if necessary or desired.
 * @author Nicke535
 */
public class tahlan_FlamerScript implements EveryFrameWeaponEffectPlugin {
    //List of all registered projectiles, saved in individual "streams"
    private HashMap<Integer, List<DamagingProjectileAPI>> registeredProjectiles = new HashMap<>();

    //Current "stream ID". Each time a stream is broken for too long a new ID is generated
    private int currentActiveStream = 0;

    //How long was it since our last projectile was found?
    private float timeSinceLastNewProj = 0f;

    //What's the maximum time you can cease firing and still count the projectiles as one "stream"?
    private static final float STREAM_MAX_TIME_GAP = 0.15f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Check if we need to instantiate a new stream or not
        timeSinceLastNewProj += amount;
        if (timeSinceLastNewProj > STREAM_MAX_TIME_GAP) {
            timeSinceLastNewProj = 0f;
            currentActiveStream++;
        }

        //Ensure our current stream is instantiated
        if (registeredProjectiles.get(currentActiveStream) == null) {
            registeredProjectiles.put(currentActiveStream, new LinkedList<DamagingProjectileAPI>());
        }

        //Find all our projectiles that haven't been registered yet and register them
        for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(weapon.getLocation(), 150f)) {
            if (proj.getWeapon() != weapon || !engine.isEntityInPlay(proj)) {
                continue;
            }

            boolean alreadyRegistered = false;
            for (int stream : registeredProjectiles.keySet()) {
                if (registeredProjectiles.get(stream).contains(proj)) {
                    alreadyRegistered = true;
                    break;
                }
            }
            if (!alreadyRegistered) {
                registeredProjectiles.get(currentActiveStream).add(proj);
                timeSinceLastNewProj = 0f;
            }
        }

        //Go through each stream and manually adjust the projectile facings
        //Also clean up any unused and/or outdated projectiles or lists
        Set<Integer> streamsToRemove = new HashSet<>();
        for (int stream : registeredProjectiles.keySet()) {
            List<DamagingProjectileAPI> streamProjs = registeredProjectiles.get(stream);
            List<DamagingProjectileAPI> toRemove = new LinkedList<>();

            //(note : the first and last projectile aren't rotated)
            for (int i = 1; i < streamProjs.size()-1; i++) {
                DamagingProjectileAPI proj = streamProjs.get(i);
                if (engine.isEntityInPlay(proj)) {
                    Vector2f oldVelocity = new Vector2f(proj.getVelocity());
                    Vector2f previousShotPos = streamProjs.get(i-1).getLocation();
                    Vector2f nextShotPos = streamProjs.get(i+1).getLocation();

                    float desiredAngle = VectorUtils.getAngle(nextShotPos, previousShotPos);
                    proj.setFacing(desiredAngle);
                    proj.getVelocity().x = oldVelocity.x;
                    proj.getVelocity().y = oldVelocity.y;
                } else {
                    toRemove.add(proj);
                }
            }

            //Cleanup!
            for (DamagingProjectileAPI proj : toRemove) {
                streamProjs.remove(proj);
            }
            if (streamProjs.isEmpty() && stream != currentActiveStream) {
                streamsToRemove.add(stream);
            }
        }
        for (int streamToRemove : streamsToRemove) {
            registeredProjectiles.remove(streamToRemove);
        }
    }
}