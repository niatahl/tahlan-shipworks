//By Nicke535, handles the trails for various projectiles in the mod
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class tahlan_ProjectileTrailHandlerPlugin extends BaseEveryFrameCombatPlugin {

    //A map of all the trail sprites used (note that all the sprites must be under TSW_fx): ensure this one has the same keys as the other maps
    private static final Map<String, String> TRAIL_SPRITES = new HashMap<String, String>();

    static {
        TRAIL_SPRITES.put("tahlan_utpc_shot", "trail_zappy");
        TRAIL_SPRITES.put("tahlan_utpc_shot_splinter", "trail_zappy");
    }

    //A map for known projectiles and their IDs: should be cleared in init
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs = new HashMap<DamagingProjectileAPI, Float>();

    //Used when doing dual-core sprites
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs2 = new HashMap<DamagingProjectileAPI, Float>();

    //--------------------------------------THESE ARE ALL MAPS FOR DIFFERENT VISUAL STATS FOR THE TRAILS: THEIR NAMES ARE FAIRLY SELF_EXPLANATORY---------------------------------------------------
    private static final Map<String, Float> TRAIL_DURATIONS_IN = new HashMap<String, Float>();

    static {
        TRAIL_DURATIONS_IN.put("tahlan_utpc_shot", 0.05f);
        TRAIL_DURATIONS_IN.put("tahlan_utpc_shot_splinter", 0.05f);
    }

    private static final Map<String, Float> TRAIL_DURATIONS_MAIN = new HashMap<String, Float>();

    static {
        TRAIL_DURATIONS_MAIN.put("tahlan_utpc_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("tahlan_utpc_shot_splinter", 0f);
    }

    private static final Map<String, Float> TRAIL_DURATIONS_OUT = new HashMap<String, Float>();

    static {
        TRAIL_DURATIONS_OUT.put("tahlan_utpc_shot", 0.4f);
        TRAIL_DURATIONS_OUT.put("tahlan_utpc_shot_splinter", 0.2f);
    }

    private static final Map<String, Float> START_SIZES = new HashMap<String, Float>();

    static {
        START_SIZES.put("tahlan_utpc_shot", 10f);
        START_SIZES.put("tahlan_utpc_shot_splinter", 5f);
    }

    private static final Map<String, Float> END_SIZES = new HashMap<String, Float>();

    static {
        END_SIZES.put("tahlan_utpc_shot", 5f);
        END_SIZES.put("tahlan_utpc_shot_splinter", 0f);
    }

    private static final Map<String, Color> TRAIL_START_COLORS = new HashMap<String, Color>();

    static {
        TRAIL_START_COLORS.put("tahlan_utpc_shot", new Color(255, 150, 150));
        TRAIL_START_COLORS.put("tahlan_utpc_shot_splinter", new Color(255, 150, 150));
    }

    private static final Map<String, Color> TRAIL_END_COLORS = new HashMap<String, Color>();

    static {
        TRAIL_END_COLORS.put("tahlan_utpc_shot", new Color(255, 150, 150));
        TRAIL_END_COLORS.put("tahlan_utpc_shot_splinter", new Color(255, 150, 150));
    }

    private static final Map<String, Float> TRAIL_OPACITIES = new HashMap<String, Float>();

    static {
        TRAIL_OPACITIES.put("tahlan_utpc_shot", 0.4f);
        TRAIL_OPACITIES.put("tahlan_utpc_shot_splinter", 0.4f);
    }

    private static final Map<String, Integer> TRAIL_BLEND_SRC = new HashMap<String, Integer>();

    static {
        TRAIL_BLEND_SRC.put("tahlan_utpc_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("tahlan_utpc_shot_splinter", GL_SRC_ALPHA);
    }

    private static final Map<String, Integer> TRAIL_BLEND_DEST = new HashMap<String, Integer>();

    static {
        TRAIL_BLEND_DEST.put("tahlan_utpc_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("tahlan_utpc_shot_splinter", GL_ONE);
    }

    private static final Map<String, Float> TRAIL_LOOP_LENGTHS = new HashMap<String, Float>();

    static {
        TRAIL_LOOP_LENGTHS.put("tahlan_utpc_shot", 300f);
        TRAIL_LOOP_LENGTHS.put("tahlan_utpc_shot_splinter", 300f);
    }

    private static final Map<String, Float> TRAIL_SCROLL_SPEEDS = new HashMap<String, Float>();

    static {
        TRAIL_SCROLL_SPEEDS.put("tahlan_utpc_shot", 500f);
        TRAIL_SCROLL_SPEEDS.put("tahlan_utpc_shot_splinter", 500f);
    }

    private static final Map<String, Float> TRAIL_SPAWN_OFFSETS = new HashMap<>();
    static {
        TRAIL_SPAWN_OFFSETS.put("tahlan_utpc_shot", 30f);
        TRAIL_SPAWN_OFFSETS.put("tahlan_utpc_shot_splinter", 30f);
    }
    //NEW: compensates for lateral movement of a projectile. Should generally be 0f in most cases, due to some oddities
    //in behaviour with direction-changing scripts, but can be helpful for aligning certain projectiles
    private static final Map<String, Float> LATERAL_COMPENSATION_MULT = new HashMap<>();
    static {
        LATERAL_COMPENSATION_MULT.put("tahlan_utpc_shot", 1f);
        LATERAL_COMPENSATION_MULT.put("tahlan_utpc_shot_splinter", 1f);
    }
    //NEW: whether a shot's trail loses opacity as the projectile fades out. Should generally be true, but may need to
    //be set to false on some scripted weapons. Has no real effect on flak rounds or missiles, and should thus be set
    //false for those
    private static final Map<String, Boolean> FADE_OUT_FADES_TRAIL = new HashMap<>();
    static {
        FADE_OUT_FADES_TRAIL.put("tahlan_utpc_shot", true);
        FADE_OUT_FADES_TRAIL.put("tahlan_utpc_shot_splinter", true);
    }
    //NEW: whether a shot should have its direction adjusted to face the same way as its velocity vector, thus
    //helping with trail alignment for projectiles without using lateral compensation. DOES NOT WORK FOR
    //PROJECTILES SPAWNED WITH BALLISTIC_AS_BEAM AS SPAWNTYPE, and should not be used on missiles
    private static final Map<String, Boolean> PROJECTILE_ANGLE_ADJUSTMENT = new HashMap<>();
    static {
        PROJECTILE_ANGLE_ADJUSTMENT.put("tahlan_utpc_shot", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("tahlan_utpc_shot_splinter", false);
    }

    @Override
    public void init(CombatEngineAPI engine) {
        //Reinitialize the lists
        projectileTrailIDs.clear();
        projectileTrailIDs2.clear();
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();

        //Runs once on each projectile that matches one of the IDs specified in our maps
        for (DamagingProjectileAPI proj : engine.getProjectiles()) {
            //Ignore already-collided projectiles, and projectiles that don't match our IDs
            if (proj.getProjectileSpecId() == null || proj.didDamage()) {
                continue;
            }

            if (!TRAIL_SPRITES.keySet().contains(proj.getProjectileSpecId())) {
                continue;
            }

            //-------------------------------------------For visual effects---------------------------------------------
            String specID = proj.getProjectileSpecId();
            SpriteAPI spriteToUse = Global.getSettings().getSprite("tahlan_fx", TRAIL_SPRITES.get(specID));
            Vector2f projVel = new Vector2f(proj.getVelocity());

            //If we use angle adjustment, do that here
            if (PROJECTILE_ANGLE_ADJUSTMENT.get(specID) && projVel.length() > 0.1f && !proj.getSpawnType().equals(ProjectileSpawnType.BALLISTIC_AS_BEAM)) {
                proj.setFacing(VectorUtils.getAngle(new Vector2f(0f, 0f), projVel));
            }

            //If we haven't already started a trail for this projectile, get an ID for it
            if (projectileTrailIDs.get(proj) == null) {
                projectileTrailIDs.put(proj, MagicTrailPlugin.getUniqueID());

                //Fix for some first-frame error shenanigans
                if (projVel.length() < 0.1f && proj.getSource() != null) {
                    projVel = new Vector2f(proj.getSource().getVelocity());
                }
            }

            //Gets a custom "offset" position, so we can slightly alter the spawn location to account for "natural fade-in", and add that to our spawn position
            Vector2f offsetPoint = new Vector2f((float)Math.cos(Math.toRadians(proj.getFacing())) * TRAIL_SPAWN_OFFSETS.get(specID), (float)Math.sin(Math.toRadians(proj.getFacing())) * TRAIL_SPAWN_OFFSETS.get(specID));
            Vector2f spawnPosition = new Vector2f(offsetPoint.x + proj.getLocation().x, offsetPoint.y + proj.getLocation().y);

            //Sideway offset velocity, for projectiles that use it
            Vector2f projBodyVel = VectorUtils.rotate(projVel, -proj.getFacing());
            Vector2f projLateralBodyVel = new Vector2f(0f, projBodyVel.getY());
            Vector2f sidewayVel = (Vector2f)VectorUtils.rotate(projLateralBodyVel, proj.getFacing()).scale(LATERAL_COMPENSATION_MULT.get(specID));

            //Opacity adjustment for fade-out, if the projectile uses it
            float opacityMult = 1f;
            if (FADE_OUT_FADES_TRAIL.get(specID) && proj.isFading()) {
                opacityMult = Math.max(0f,Math.min(1f,proj.getDamageAmount() / proj.getBaseDamageAmount()));
            }



                //Then, actually spawn a trail
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, spawnPosition, 0f, 0f, proj.getFacing() - 180f,
                        0f, 0f, START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                        TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                if (specID.contains("tahlan_utpc_shot")) {
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), Global.getSettings().getSprite("tahlan_fx", "trail_smooth"), spawnPosition, 0f, 0f, proj.getFacing() - 180f,
                            0f, 0f, 50f, 20f, new Color(255,0,0), new Color(255,0,0),
                            0.3f * opacityMult, 0f, 0.05f, 0.15f, TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), 0f, sidewayVel, null);
                }


        }
    }
}