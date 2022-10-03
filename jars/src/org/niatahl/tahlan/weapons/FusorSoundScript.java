package org.niatahl.tahlan.weapons;
 
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponGroupType;
import org.lwjgl.util.vector.Vector2f;
 
/* READ THE FUCKING MANUAL! */
 
 /* This script is meant specifically to create CIWS style weapons in SS with a faithful sound.
    https://youtu.be/KsVUISS8oHs
 
    Have faith in and listen to the comments provided, for it was tested extensively as SS itself did not want this to exist!
    • Example pertinent weapon stats: 0.05 chargedown, 5 burst size, 0.05 burst delay.
    • This must only be used for weapons that fire a 5 shot burst at 1200RPM during the burst!
    • If finite ammo the ammo capacity must be a multiple of the burst size!
    • If also in LINKED mode the ammo capacity must be a multiple of the burst size multiplied by the number of offsets!
    • If also in DUAL mode the ammo capacity must be a multiple of the burst size multiplied by two!
    • If regenerating ammo it must reload in clips as a multiple of the burst size!
    • If also in LINKED mode the clip size must be a multiple of the burst size multiplied by the number of offsets!
    • If also in DUAL mode the clip size must be a multiple of the burst size multiplied by two!
 */
public class FusorSoundScript implements EveryFrameWeaponEffectPlugin
{
    /* Don't change any of these unless you are hearing artifacts! */
    private static final float MIN_LOUDNESS = 0.05f;
    private static final float TIME_TO_FULL_LOUDNESS = 0.025f;
    private static final float TIME_TO_NO_LOUDNESS = 0.025f;
    private static final float ZERO_LOUDNESS = 0.001f; // Yes, seriously.
    private static final Vector2f ZERO = new Vector2f();
 
    /* Skips the start/end sound if in an alternating weapon group and at least this many are firing.
       Default is 0 which ignores this behavior, don't change this unless you are hearing artifacts! */
    private static final int START_END_SOUND_CULLING_NUMBER = 0;
 
    /* Replace these with Sound IDs in "" only for the sounds you wish to play. */
    private static final String START_SOUND = null;
    private static final String LOOP_SOUND = "tahlan_fusor_loop";
    private static final String END_SOUND = null;
 
    private boolean startedThisFiring = false;
    private boolean fadedOutLoop = true;
    private float loudness = MIN_LOUDNESS;
    private int wasThisFiring = 0;
    private boolean wasAnyFiring = false;

    //To cleanly run the muzzle flash script without messing with the sound script
    private FusorMuzzleFlashScript muzzleFlashScript = null;
    //private FlamerScript flamerScript = null;
 
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        //Run our muzzle flash script, and get one if we don't already have one
        if (muzzleFlashScript == null) {
            muzzleFlashScript = new FusorMuzzleFlashScript();
        }
        muzzleFlashScript.advance(amount, engine, weapon);

        /*
        if (flamerScript == null) {
            flamerScript = new FlamerScript();
        }
        flamerScript.advance(amount,engine,weapon);
        */

        boolean isThisWeaponFiring = false;
        if (weapon.getChargeLevel() > 0f)
        {
            isThisWeaponFiring = true;
        }
        if (weapon.isFiring())
        {
            isThisWeaponFiring = true;
        }
        if (weapon.getCooldownRemaining() > 0f && weapon.getAmmo() > 0)
        {
            isThisWeaponFiring = true;
        }
        if (weapon.getShip() == null || weapon.getShip().getFluxTracker().isOverloadedOrVenting())
        {
            isThisWeaponFiring = false;
        }
 
        boolean isAnyWeaponFiring = isThisWeaponFiring;
        ShipAPI ship = weapon.getShip();
        if (ship == null)
        {
            return;
        }
        if (!isAnyWeaponFiring)
        {
            for (WeaponAPI otherWeapon : ship.getAllWeapons())
            {
                if (otherWeapon.getId().contentEquals(weapon.getId()))
                {
                    if (otherWeapon.getChargeLevel() > 0f)
                    {
                        isAnyWeaponFiring = true;
                        break;
                    }
                    if (otherWeapon.isFiring())
                    {
                        isAnyWeaponFiring = true;
                        break;
                    }
                    if (otherWeapon.getCooldownRemaining() > 0f && otherWeapon.getAmmo() > 0)
                    {
                        isAnyWeaponFiring = true;
                        break;
                    }
                }
            }
        }
        if (ship.getFluxTracker().isOverloadedOrVenting())
        {
            isAnyWeaponFiring = false;
        }
 
        /* Checks if enough weapons are firing in our alternating weapon group for start/end sounds to be culled. */
        boolean shouldBeCulled = false;
        if (ship.getWeaponGroupFor(weapon) == null)
        {
            return;
        }
        if (START_END_SOUND_CULLING_NUMBER != 1 && ship.getWeaponGroupFor(weapon).getType().equals(WeaponGroupType.ALTERNATING))
        {
            int firingWeapons = 0;
            for (WeaponAPI wpn : ship.getWeaponGroupFor(weapon).getWeaponsCopy())
            {
                if (wpn.getChargeLevel() > 0f || weapon.isFiring())
                {
                    firingWeapons++;
                    if (firingWeapons >= START_END_SOUND_CULLING_NUMBER)
                    {
                        shouldBeCulled = true;
                        break;
                    }
                }
            }
        }
 
        /* If the weapon is firing, indicates that it's in some kind of firing cycle, was firing on the previous frame,
           or is within our grace period from the last time it reported firing, continue on with the "Firing" part of
           the algorithm.
         */
        if ((wasThisFiring > 0) || isThisWeaponFiring)
        {
            /* "Firing"
               If this is the start of the firing cycle, play a startup sound once only. */
            if (startedThisFiring == false)
            {
                if (!shouldBeCulled)
                {
                    if (START_SOUND != null)
                    {
                        Global.getSoundPlayer().playSound(START_SOUND, 1f, 1f, weapon.getLocation(), ZERO);
                    }
 
                    startedThisFiring = true;
                }
            }
        }
        else
        {
            /* Play the end sound only once. */
            if (startedThisFiring)
            {
                if (END_SOUND != null)
                {
                    if (!shouldBeCulled)
                    {
                        Global.getSoundPlayer().playSound(END_SOUND, 1f, 1f, weapon.getLocation(), ZERO);
                    }
                }
 
                startedThisFiring = false;
            }
        }
 
        if (isThisWeaponFiring)
        {
            wasThisFiring = Math.min(2, wasThisFiring + 1);
        }
        else
        {
            wasThisFiring = Math.max(0, wasThisFiring - 1);
        }
 
        /* Same logic, but for all of the weapons together. The loop will work the same for each weapon
           simultaneously, only fading out or cutting off if *all* of the weapons stop.
         */
        if (wasAnyFiring || isAnyWeaponFiring)
        {
            /* "Firing" */
            if (fadedOutLoop == true)
            {
                /* Set loudness to minimum to start with.  This is always the volume on frame 1, making it more
                   consistent overall. */
                loudness = MIN_LOUDNESS;
 
                /* Indicate loop has begun. */
                fadedOutLoop = false;
            }
 
            /* Play the loop, which is what we came here to do in the first place. */
            if (LOOP_SOUND != null)
            {
                Global.getSoundPlayer().playLoop(LOOP_SOUND, weapon.getShip(), 1f, loudness, weapon.getLocation(), ZERO);
            }
 
            /* Update loudness and cap to the maximum. */
            loudness = Math.min(loudness + ((amount / TIME_TO_FULL_LOUDNESS) * (1f - MIN_LOUDNESS)), 1f);
        }
        else
        {
            /* "Not Firing"
               Update the loop loudness for the fade-out.  We do this to begin with so that the lower volume is applied
               right away.
             */
            loudness = Math.max(loudness - (amount / TIME_TO_NO_LOUDNESS), ZERO_LOUDNESS);
 
            /* Keep playing the loop.  If we play with 0 volume, we should stop trying to play the loop anymore, since
               it will overlap with other weapons.
             */
            if (!fadedOutLoop)
            {
                if (LOOP_SOUND != null)
                {
                    Global.getSoundPlayer().playLoop(LOOP_SOUND, weapon.getShip(), 1f, loudness, weapon.getLocation(), ZERO);
                }
 
                if (loudness <= (ZERO_LOUDNESS + 0.0001f))
                {
                    /* Indicate loop has ended. */
                    fadedOutLoop = true;
                }
            }
        }
 
        wasAnyFiring = isAnyWeaponFiring;
    }
}