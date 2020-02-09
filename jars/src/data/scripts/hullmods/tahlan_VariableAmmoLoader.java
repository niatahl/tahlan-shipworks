//By Nicke535, enables a ship to swap between different shell types on-the-fly
package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class tahlan_VariableAmmoLoader extends BaseHullMod {
    //Cooldown each time we fire after switching shell type: only applies once per switching
    public final float COOLDOWN_ON_LOCK = 3f;
    public final float COOLDOWN_ON_FIRE = 0.25f;

    public final String T1_WEAPON_NAME = "tahlan_phira_shock";
    public final float T1_DAMAGE_BONUS = 0.2f;
    public final Color T1_MUZZLE_COLOR = new Color(255, 180, 113);

    public final String T2_WEAPON_NAME = "tahlan_phira_impact";
    public final float T2_DAMAGE_BONUS = -0.15f;
    public final float T2_INACCURACY = 0.5f;
    public final float T2_SPEED_VARIATION = 0.02f;
    public final Color T2_MUZZLE_COLOR = new Color(255, 180, 113);

    public final String T3_PRIMARY_WEAPON_NAME = "tahlan_phira_burst";
    public final String T3_SPLIT_WEAPON_NAME = "tahlan_phira_burst_split";
    public final float T3_PRIMARY_INACCURACY = 4f;
    public final float T3_SPLIT_INACCURACY = 10f;
    public final float T3_PRIMARY_SPEED_VARIATION = 0.03f;
    public final float T3_SPLIT_SPEED_VARIATION = 0.1f;
    public final int T3_SPLIT_COUNT = 10;
    public final Color T3_MUZZLE_COLOR = new Color(255, 180, 113);
    public final Color T3_SPLIT_COLOR = new Color(232, 221, 204);

    //Hacky, but it works: register which projectiles don't need swapping, since they were fired when we were in "burst" mode (type-3 shells)
    private List<DamagingProjectileAPI> registeredProjectiles = new ArrayList<DamagingProjectileAPI>();

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //Nothing, really
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        //Don't run if we are paused
        if (engine.isPaused() || ship == null || !engine.isEntityInPlay(ship)) {
            return;
        }

        //Displays the correct tooltip for the player at all times
        if (ship == Global.getCombatEngine().getPlayerShip()) {
            String textToPrint = "Type-3 Shells Ready";
            if (ship.getSystem().getAmmo() == 1) {
                textToPrint = "Shock Cannons Ready";
            } else if (ship.getSystem().getAmmo() == 2) {
                textToPrint = "Impact Driver Ready";
            }
            Global.getCombatEngine().maintainStatusForPlayerShip("tahlan_VariableAmmoTooltip", "graphics/tahlan/icons/hullsys/tahlan_shell_swapping.png", "Variable Ammo", textToPrint, false);

            //If we are "locked" to an ammo type, display that as well
            if (engine.getCustomData().get("tahlan_VariableAmmoExtraCooldown" + ship.getId()) instanceof Float) {
                //Check if we are on cooldown; if we aren't, don't display anything
                float cooldownRemaining = (float)engine.getCustomData().get("tahlan_VariableAmmoExtraCooldown" + ship.getId());
                if (cooldownRemaining > 0f) {
                    //If we are on cooldown, display that as a tooltip...
                    int cooldownWholes = (int)(Math.floor(cooldownRemaining));
                    int cooldownParts = (int)(Math.floor(cooldownRemaining*10f)) - (int)(Math.floor(cooldownRemaining)*10);
                    Global.getCombatEngine().maintainStatusForPlayerShip("tahlan_VariableAmmoLockTooltip", "graphics/tahlan/icons/hullsys/tahlan_shell_swapping.png", "Variable Ammo", "Locked for "
                            + cooldownWholes + "." + cooldownParts + " more seconds", true);

                    //...and actually tick down our cooldown, too
                    cooldownRemaining -= amount;
                    engine.getCustomData().put("tahlan_VariableAmmoExtraCooldown" + ship.getId(), cooldownRemaining);
                }
            }
        }

        //List for cleaning up dead projectiles from memory
        List<DamagingProjectileAPI> cleanList = new ArrayList<>();

        //Splits shots that should be splitting
        for (DamagingProjectileAPI proj : registeredProjectiles) {
            //Only split burst shots
            if (proj.getProjectileSpecId().contains("tahlan_phira_burst_shot")) {
                //Calculates split range : hard-coded to match up with range properly
                WeaponAPI weapon = proj.getWeapon();
                Vector2f loc = proj.getLocation();
                float projAngle = proj.getFacing();
                float projDamage = proj.getDamageAmount();
                float splitDuration = (weapon.getRange() / weapon.getProjectileSpeed()) * 0.5f;

                //Split once our duration has passed; spawn a bunch of shots
                if (proj.getElapsed() > splitDuration) {
                    //Hide the explosion with some muzzle flash
                    spawnDecoParticles(loc, projAngle, Misc.ZERO, engine, T3_SPLIT_COLOR, 1.5f, 1f);
                    engine.addSmoothParticle(loc, ZERO, 200f, 0.5f, 0.1f, T3_MUZZLE_COLOR);
                    engine.addHitParticle(loc, ZERO, 100f, 0.5f, 0.25f, T3_SPLIT_COLOR);

                    //Actually spawn shots
                    for (int i = 0; i < T3_SPLIT_COUNT; i++) {
                        //Spawns the shot, with some inaccuracy
                        float angleOffset = MathUtils.getRandomNumberInRange(-T3_SPLIT_INACCURACY / 2, T3_SPLIT_INACCURACY / 2) + MathUtils.getRandomNumberInRange(-T3_SPLIT_INACCURACY / 2, T3_SPLIT_INACCURACY / 2);
                        DamagingProjectileAPI newProj = (DamagingProjectileAPI) engine.spawnProjectile(ship, weapon, T3_SPLIT_WEAPON_NAME, loc, projAngle + angleOffset, new Vector2f(0,0));
                        //Varies the speed slightly, for a more artillery-esque look
                        float rand = MathUtils.getRandomNumberInRange(1 - T3_SPLIT_SPEED_VARIATION, 1 + T3_SPLIT_SPEED_VARIATION);
                        newProj.getVelocity().x *= rand;
                        newProj.getVelocity().y *= rand;
                        //Splits up the damage
                        newProj.setDamageAmount(projDamage / (float) T3_SPLIT_COUNT);
                        ProximityFuseAIAPI AI = (ProximityFuseAIAPI)(newProj.getAI());
                        AI.updateDamage();
                        //Removes the original projectile
                        engine.removeEntity(proj);
                    }
                    cleanList.add(proj);
                    continue;
                }
            }

            //If this projectile is not loaded in memory, cleaning time!
            if (!engine.isEntityInPlay(proj)) {
                cleanList.add(proj);
            }
        }

        //Runs the cleaning
        for (DamagingProjectileAPI proj : cleanList) {
            registeredProjectiles.remove(proj);
        }

        //Finds all projectiles within a a short range from our ship
        for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(ship.getLocation(), ship.getCollisionRadius()+200f)) {
            //Saves some memory, and makes the rest of the code slightly more compact, while also ignoring anything not from our ship
            if (proj.getProjectileSpecId() == null || proj.getSource() != ship || registeredProjectiles.contains(proj) || !engine.isEntityInPlay(proj)) {
                continue;
            }

            if (proj.getProjectileSpecId().contains("tahlan_phira_burst_shot")) {
                //We found a applicable shell, meaning we have definitely fired: store that in CustomData so our system knows we are on cooldown (cooldown is longer the first time we switch shell type)
                if (engine.getCustomData().get("tahlan_VariableAmmoHasSwapped" + ship.getId()) instanceof Boolean) {
                    if ((boolean)engine.getCustomData().get("tahlan_VariableAmmoHasSwapped" + ship.getId())) {
                        engine.getCustomData().put("tahlan_VariableAmmoExtraCooldown" + ship.getId(), COOLDOWN_ON_LOCK);
                        engine.getCustomData().put("tahlan_VariableAmmoHasSwapped" + ship.getId(), false);
                    } else {
                        //This is to ensure we actually increase the cooldown instead of decrease it
                        if (Global.getCombatEngine().getCustomData().get("tahlan_VariableAmmoExtraCooldown" + ship.getId()) instanceof Float) {
                            if ((float)Global.getCombatEngine().getCustomData().get("tahlan_VariableAmmoExtraCooldown" + ship.getId()) < COOLDOWN_ON_FIRE) {
                                engine.getCustomData().put("tahlan_VariableAmmoExtraCooldown" + ship.getId(), COOLDOWN_ON_FIRE);
                            }
                        }
                    }
                } else {
                    //This is to ensure we actually increase the cooldown instead of decrease it
                    if (Global.getCombatEngine().getCustomData().get("tahlan_VariableAmmoExtraCooldown" + ship.getId()) instanceof Float) {
                        if ((float)Global.getCombatEngine().getCustomData().get("tahlan_VariableAmmoExtraCooldown" + ship.getId()) < COOLDOWN_ON_FIRE) {
                            engine.getCustomData().put("tahlan_VariableAmmoExtraCooldown" + ship.getId(), COOLDOWN_ON_FIRE);
                        }
                    } else {
                        engine.getCustomData().put("tahlan_VariableAmmoExtraCooldown" + ship.getId(), COOLDOWN_ON_FIRE);
                    }
                }

                //Stores the data all "shrapnel" needs anyway
                WeaponAPI weapon = proj.getWeapon();
                Vector2f loc = proj.getLocation();
                float projAngle = proj.getFacing();
                float projDamage = proj.getDamageAmount();

                //Beam shells: no spread
                if (ship.getSystem().getAmmo() == 1) {
                    //Muzzle flash!
                    //spawnDecoParticles(loc, projAngle, ship.getVelocity(), engine, T1_MUZZLE_COLOR, 1f, 0.7f);

                    //Spawns the shot
                    DamagingProjectileAPI newProj = (DamagingProjectileAPI)engine.spawnProjectile(ship, weapon, T1_WEAPON_NAME, loc, projAngle, ship.getVelocity());
                    //Corrects the damage
                    newProj.setDamageAmount(projDamage * (1 + T1_DAMAGE_BONUS));
                    //Removes the original projectile
                    engine.removeEntity(proj);
                    registeredProjectiles.add(newProj);
                    continue;
                }

                //Kinetic slugs: some spread
                if (ship.getSystem().getAmmo() == 2) {
                    //Muzzle flash!
                    //spawnDecoParticles(loc, projAngle, ship.getVelocity(), engine, T2_MUZZLE_COLOR, 1.5f, 0.9f);

                    //Spawns the shot, with some inaccuracy
                    float angleOffset = MathUtils.getRandomNumberInRange(-T2_INACCURACY / 2, T2_INACCURACY / 2) + MathUtils.getRandomNumberInRange(-T2_INACCURACY / 2, T2_INACCURACY / 2);
                    DamagingProjectileAPI newProj = (DamagingProjectileAPI)engine.spawnProjectile(ship, weapon, T2_WEAPON_NAME, loc, projAngle + angleOffset, ship.getVelocity());
                    //Corrects the damage
                    newProj.setDamageAmount(projDamage * (1 + T2_DAMAGE_BONUS));
                    //Varies the speed very slightly, for a more artillery-esque look
                    float rand = MathUtils.getRandomNumberInRange(1-T2_SPEED_VARIATION, 1+T2_SPEED_VARIATION);
                    newProj.getVelocity().x *= rand;
                    newProj.getVelocity().y *= rand;
                    //Removes the original projectile
                    engine.removeEntity(proj);
                    registeredProjectiles.add(newProj);
                    continue;
                }

                //If we have type-3 ammo loaded, spawn shots from our *own* weapon and prepare them for turning into submunitions, but do not adjust damage
                if (ship.getSystem().getAmmo() == 3) {
                    //Muzzle flash!
                    //spawnDecoParticles(loc, projAngle, ship.getVelocity(), engine, T3_MUZZLE_COLOR, 2.5f, 1f);

                    //Spawns the shot, with some inaccuracy
                    float angleOffset = MathUtils.getRandomNumberInRange(-T3_PRIMARY_INACCURACY / 2, T3_PRIMARY_INACCURACY / 2) + MathUtils.getRandomNumberInRange(-T3_PRIMARY_INACCURACY / 2, T3_PRIMARY_INACCURACY / 2);
                    DamagingProjectileAPI newProj = (DamagingProjectileAPI)engine.spawnProjectile(ship, weapon, T3_PRIMARY_WEAPON_NAME, loc, projAngle + angleOffset, ship.getVelocity());
                    //Varies the speed slightly, for a more artillery-esque look
                    float rand = MathUtils.getRandomNumberInRange(1-T3_PRIMARY_SPEED_VARIATION, 1+T3_PRIMARY_SPEED_VARIATION);
                    newProj.getVelocity().x *= rand;
                    newProj.getVelocity().y *= rand;
                    //Removes the original projectile
                    engine.removeEntity(proj);
                    registeredProjectiles.add(newProj);
                }
            }
        }
    }

    //Prevents the hullmod from being put on ships
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    //A whole bunch of descriptions, most unused for now
    public String getDescriptionParam(int index, HullSize hullSize) {
        return null;
    }

    //For the cool extra description section
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 10f;
        tooltip.addSectionHeading("Details", Alignment.MID, pad);

        //Shock cannons
        TooltipMakerAPI text = tooltip.beginImageWithText("graphics/tahlan/hullmods/ammo_shockcannon_icon.png", 36);
        text.addPara("Shock Cannons", 0, Color.ORANGE, "Shock Cannons");
        text.addPara("Energy damage, deals bonus EMP damage against hull", 0, Color.CYAN, "Energy");
        text.addPara(Math.round((T1_DAMAGE_BONUS*100f)) + " percent additional damage.", 0, Color.YELLOW, "" + Math.round((T1_DAMAGE_BONUS*100f)));
        tooltip.addImageWithText(pad);

        //Impact Drivers
        text = tooltip.beginImageWithText("graphics/tahlan/hullmods/ammo_impactdriver_icon.png", 36);
        text.addPara("Impact Driver", 0, Color.ORANGE, "Impact Driver");
        text.addPara("Kinetic Damage.", 0, Color.YELLOW, "Kinetic");
        text.addPara(Math.round((T2_DAMAGE_BONUS*-100f)) + " percent reduced damage.", 0, Color.YELLOW, "" + Math.round((T2_DAMAGE_BONUS*-100f)));
        tooltip.addImageWithText(pad);

        //Type-3 Shells
        text = tooltip.beginImageWithText("graphics/tahlan/hullmods/ammo_type3_icon.png", 36);
        text.addPara("Type-3 Shells", 0, Color.ORANGE, "Type-3 Shells");
        text.addPara("High-Explosive Damage." + "\n"
                + "Splits into a cluster of AoE shells.", 0, Color.RED, "High-Explosive");
        tooltip.addImageWithText(pad);
    }

    //For spawning muzzle flash
    private void spawnDecoParticles(Vector2f point, float angle, Vector2f offsetVelocity, CombatEngineAPI engine, Color color, float widthMod, float sizeMod) {

        //Moves the offset backwards slightly
        float offsetDistance = 20f;
        point = MathUtils.getPointOnCircumference(point, offsetDistance, angle+180f);

        //Spawns particles in a cone
        Vector2f offsetPoint = MathUtils.getPointOnCircumference(point, 60f * sizeMod, angle);
        for (int i = 0; i < 5; i++) {
            Vector2f spawnPointStart = MathUtils.getRandomPointOnLine(point, offsetPoint);
            Vector2f spawnPoint = MathUtils.getRandomPointInCircle(spawnPointStart, 8f);

            engine.addSmokeParticle(spawnPoint, offsetVelocity, MathUtils.getRandomNumberInRange(10f, 20f) * sizeMod, MathUtils.getRandomNumberInRange(0.5f,0.8f), MathUtils.getRandomNumberInRange(0.55f, 0.7f), color);
        }
        offsetPoint = MathUtils.getPointOnCircumference(point, 60f * sizeMod, angle+(2f*widthMod));
        for (int i = 0; i < 3; i++) {
            Vector2f spawnPointStart = MathUtils.getRandomPointOnLine(point, offsetPoint);
            Vector2f spawnPoint = MathUtils.getRandomPointInCircle(spawnPointStart, 8f);

            engine.addSmokeParticle(spawnPoint, offsetVelocity, MathUtils.getRandomNumberInRange(10f, 20f) * sizeMod, MathUtils.getRandomNumberInRange(0.5f,0.8f), MathUtils.getRandomNumberInRange(0.55f, 0.7f), color);
        }
        offsetPoint = MathUtils.getPointOnCircumference(point, 60f * sizeMod, angle-(2f*widthMod));
        for (int i = 0; i < 3; i++) {
            Vector2f spawnPointStart = MathUtils.getRandomPointOnLine(point, offsetPoint);
            Vector2f spawnPoint = MathUtils.getRandomPointInCircle(spawnPointStart, 8f);

            engine.addSmokeParticle(spawnPoint, offsetVelocity, MathUtils.getRandomNumberInRange(10f, 20f) * sizeMod, MathUtils.getRandomNumberInRange(0.5f,0.8f), MathUtils.getRandomNumberInRange(0.55f, 0.7f), color);
        }
    }
}
