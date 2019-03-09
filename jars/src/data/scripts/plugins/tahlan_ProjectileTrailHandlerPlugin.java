//By Nicke535, handles the trails for various projectiles in the mod
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class tahlan_ProjectileTrailHandlerPlugin extends BaseEveryFrameCombatPlugin {

    //Sort of a performance-sanity option; projectile trails only spawn when this much *actual* engine time has passed (at least).
    //  Still only spawns trails at most once per frame, but helps when high time-mult is in play
    private static final float MINIMUM_ENGINE_TIME_WAIT = 1f/180f;

    //A map of all the trail sprites used (note that all the sprites must be under TSW_fx): ensure this one has the same keys as the other maps
    private static final Map<String, String> TRAIL_SPRITES = new HashMap<String, String>();

    static {
        TRAIL_SPRITES.put("tahlan_utpc_shot", "tahlan_trail_zappy");
        TRAIL_SPRITES.put("tahlan_utpc_shot_splinter", "tahlan_trail_zappy");
        TRAIL_SPRITES.put("tahlan_porph_shot", "tahlan_trail_zappy");
        TRAIL_SPRITES.put("tahlan_armiger_shot", "tahlan_trail_fuzzy");
        TRAIL_SPRITES.put("tahlan_styrix_shot", "tahlan_trail_smooth");
        TRAIL_SPRITES.put("tahlan_magaera_shot", "tahlan_trail_smooth");
        TRAIL_SPRITES.put("tahlan_gallant_shot", "tahlan_trail_smooth");
        TRAIL_SPRITES.put("tahlan_hekaton_torp", "tahlan_trail_fuzzy");
        TRAIL_SPRITES.put("tahlan_astrix_shot", "tahlan_trail_zappy");
        TRAIL_SPRITES.put("tahlan_disparax_shot", "tahlan_trail_zappy");
    }

    //A map for known projectiles and their IDs: should be cleared in init
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs = new WeakHashMap<>();

    //Used when doing dual-core sprites
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs2 = new WeakHashMap<>();

    //Need more trails
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs3 = new WeakHashMap<>();

    //MORE TRAILS
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs4 = new WeakHashMap<>();

    //--------------------------------------THESE ARE ALL MAPS FOR DIFFERENT VISUAL STATS FOR THE TRAILS: THEIR NAMES ARE FAIRLY SELF_EXPLANATORY---------------------------------------------------
    private static final Map<String, Float> TRAIL_DURATIONS_IN = new HashMap<String, Float>();

    static {
        TRAIL_DURATIONS_IN.put("tahlan_utpc_shot", 0.05f);
        TRAIL_DURATIONS_IN.put("tahlan_utpc_shot_splinter", 0.05f);
        TRAIL_DURATIONS_IN.put("tahlan_porph_shot", 0.05f);
        TRAIL_DURATIONS_IN.put("tahlan_armiger_shot", 0.05f);
        TRAIL_DURATIONS_IN.put("tahlan_styrix_shot", 0.05f);
        TRAIL_DURATIONS_IN.put("tahlan_magaera_shot", 0.05f);
        TRAIL_DURATIONS_IN.put("tahlan_gallant_shot", 0.05f);
        TRAIL_DURATIONS_IN.put("tahlan_hekaton_torp", 0f);
        TRAIL_DURATIONS_IN.put("tahlan_astrix_shot", 0.03f);
        TRAIL_DURATIONS_IN.put("tahlan_disparax_shot", 0.03f);
    }

    private static final Map<String, Float> TRAIL_DURATIONS_MAIN = new HashMap<String, Float>();

    static {
        TRAIL_DURATIONS_MAIN.put("tahlan_utpc_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("tahlan_utpc_shot_splinter", 0f);
        TRAIL_DURATIONS_MAIN.put("tahlan_porph_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("tahlan_armiger_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("tahlan_styrix_shot", 0.5f);
        TRAIL_DURATIONS_MAIN.put("tahlan_magaera_shot", 0.2f);
        TRAIL_DURATIONS_MAIN.put("tahlan_gallant_shot", 0.2f);
        TRAIL_DURATIONS_MAIN.put("tahlan_hekaton_torp", 0.2f);
        TRAIL_DURATIONS_MAIN.put("tahlan_astrix_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("tahlan_disparax_shot", 0f);
    }

    private static final Map<String, Float> TRAIL_DURATIONS_OUT = new HashMap<String, Float>();

    static {
        TRAIL_DURATIONS_OUT.put("tahlan_utpc_shot", 0.4f);
        TRAIL_DURATIONS_OUT.put("tahlan_utpc_shot_splinter", 0.2f);
        TRAIL_DURATIONS_OUT.put("tahlan_porph_shot", 0.6f);
        TRAIL_DURATIONS_OUT.put("tahlan_armiger_shot", 0.6f);
        TRAIL_DURATIONS_OUT.put("tahlan_styrix_shot", 1.5f);
        TRAIL_DURATIONS_OUT.put("tahlan_magaera_shot", 1f);
        TRAIL_DURATIONS_OUT.put("tahlan_gallant_shot", 1f);
        TRAIL_DURATIONS_OUT.put("tahlan_hekaton_torp", 1f);
        TRAIL_DURATIONS_OUT.put("tahlan_astrix_shot", 0.2f);
        TRAIL_DURATIONS_OUT.put("tahlan_disparax_shot", 0.5f);
    }

    private static final Map<String, Float> START_SIZES = new HashMap<String, Float>();

    static {
        START_SIZES.put("tahlan_utpc_shot", 10f);
        START_SIZES.put("tahlan_utpc_shot_splinter", 5f);
        START_SIZES.put("tahlan_porph_shot", 16f);
        START_SIZES.put("tahlan_armiger_shot", 8f);
        START_SIZES.put("tahlan_styrix_shot", 4f);
        START_SIZES.put("tahlan_magaera_shot", 4f);
        START_SIZES.put("tahlan_gallant_shot", 4f);
        START_SIZES.put("tahlan_hekaton_torp", 10f);
        START_SIZES.put("tahlan_astrix_shot", 15f);
        START_SIZES.put("tahlan_disparax_shot", 30f);
    }

    private static final Map<String, Float> END_SIZES = new HashMap<String, Float>();

    static {
        END_SIZES.put("tahlan_utpc_shot", 5f);
        END_SIZES.put("tahlan_utpc_shot_splinter", 0f);
        END_SIZES.put("tahlan_porph_shot", 4f);
        END_SIZES.put("tahlan_armiger_shot", 15f);
        END_SIZES.put("tahlan_styrix_shot", 2f);
        END_SIZES.put("tahlan_magaera_shot", 2f);
        END_SIZES.put("tahlan_gallant_shot", 2f);
        END_SIZES.put("tahlan_hekaton_torp", 20f);
        END_SIZES.put("tahlan_astrix_shot", 6f);
        END_SIZES.put("tahlan_disparax_shot", 10f);
    }

    private static final Map<String, Color> TRAIL_START_COLORS = new HashMap<String, Color>();

    static {
        TRAIL_START_COLORS.put("tahlan_utpc_shot", new Color(255, 150, 150));
        TRAIL_START_COLORS.put("tahlan_utpc_shot_splinter", new Color(255, 150, 150));
        TRAIL_START_COLORS.put("tahlan_porph_shot", new Color(255, 180, 100));
        TRAIL_START_COLORS.put("tahlan_armiger_shot", new Color(160, 140, 100));
        TRAIL_START_COLORS.put("tahlan_styrix_shot", new Color(255, 255, 255));
        TRAIL_START_COLORS.put("tahlan_magaera_shot", new Color(255, 255, 255));
        TRAIL_START_COLORS.put("tahlan_gallant_shot", new Color(255, 255, 255));
        TRAIL_START_COLORS.put("tahlan_hekaton_torp", new Color(255, 125, 65));
        TRAIL_START_COLORS.put("tahlan_astrix_shot", new Color(70,170,255));
        TRAIL_START_COLORS.put("tahlan_disparax_shot", new Color(255,175,50));
    }

    private static final Map<String, Color> TRAIL_END_COLORS = new HashMap<String, Color>();

    static {
        TRAIL_END_COLORS.put("tahlan_utpc_shot", new Color(255, 150, 150));
        TRAIL_END_COLORS.put("tahlan_utpc_shot_splinter", new Color(255, 150, 150));
        TRAIL_END_COLORS.put("tahlan_porph_shot", new Color(255, 150, 150));
        TRAIL_END_COLORS.put("tahlan_armiger_shot", new Color(160, 140, 100));
        TRAIL_END_COLORS.put("tahlan_styrix_shot", new Color(0, 0, 255));
        TRAIL_END_COLORS.put("tahlan_magaera_shot", new Color(0, 0, 255));
        TRAIL_END_COLORS.put("tahlan_gallant_shot", new Color(0, 0, 255));
        TRAIL_END_COLORS.put("tahlan_hekaton_torp", new Color(150, 150, 150));
        TRAIL_END_COLORS.put("tahlan_astrix_shot", new Color(90,180,255));
        TRAIL_END_COLORS.put("tahlan_disparax_shot", new Color(255, 120, 30));
    }

    private static final Map<String, Float> TRAIL_OPACITIES = new HashMap<String, Float>();

    static {
        TRAIL_OPACITIES.put("tahlan_utpc_shot", 0.4f);
        TRAIL_OPACITIES.put("tahlan_utpc_shot_splinter", 0.4f);
        TRAIL_OPACITIES.put("tahlan_porph_shot", 0.4f);
        TRAIL_OPACITIES.put("tahlan_armiger_shot", 0.3f);
        TRAIL_OPACITIES.put("tahlan_styrix_shot", 0.5f);
        TRAIL_OPACITIES.put("tahlan_magaera_shot", 0.3f);
        TRAIL_OPACITIES.put("tahlan_gallant_shot", 0.3f);
        TRAIL_OPACITIES.put("tahlan_hekaton_torp", 0.2f);
        TRAIL_OPACITIES.put("tahlan_astrix_shot", 0.6f);
        TRAIL_OPACITIES.put("tahlan_disparax_shot", 0.5f);
    }

    private static final Map<String, Integer> TRAIL_BLEND_SRC = new HashMap<String, Integer>();

    static {
        TRAIL_BLEND_SRC.put("tahlan_utpc_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("tahlan_utpc_shot_splinter", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("tahlan_porph_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("tahlan_armiger_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("tahlan_styrix_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("tahlan_magaera_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("tahlan_gallant_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("tahlan_hekaton_torp", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("tahlan_astrix_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("tahlan_disparax_shot", GL_SRC_ALPHA);
    }

    private static final Map<String, Integer> TRAIL_BLEND_DEST = new HashMap<String, Integer>();

    static {
        TRAIL_BLEND_DEST.put("tahlan_utpc_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("tahlan_utpc_shot_splinter", GL_ONE);
        TRAIL_BLEND_DEST.put("tahlan_porph_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("tahlan_armiger_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("tahlan_styrix_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("tahlan_magaera_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("tahlan_gallant_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("tahlan_hekaton_torp", GL_ONE);
        TRAIL_BLEND_DEST.put("tahlan_astrix_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("tahlan_disparax_shot", GL_ONE);
    }

    private static final Map<String, Float> TRAIL_LOOP_LENGTHS = new HashMap<String, Float>();

    static {
        TRAIL_LOOP_LENGTHS.put("tahlan_utpc_shot", 300f);
        TRAIL_LOOP_LENGTHS.put("tahlan_utpc_shot_splinter", 300f);
        TRAIL_LOOP_LENGTHS.put("tahlan_porph_shot", 300f);
        TRAIL_LOOP_LENGTHS.put("tahlan_armiger_shot", 300f);
        TRAIL_LOOP_LENGTHS.put("tahlan_styrix_shot", 300f);
        TRAIL_LOOP_LENGTHS.put("tahlan_magaera_shot", 300f);
        TRAIL_LOOP_LENGTHS.put("tahlan_gallant_shot", 300f);
        TRAIL_LOOP_LENGTHS.put("tahlan_hekaton_torp", 300f);
        TRAIL_LOOP_LENGTHS.put("tahlan_astrix_shot", 200f);
        TRAIL_LOOP_LENGTHS.put("tahlan_disparax_shot", 500f);
    }

    private static final Map<String, Float> TRAIL_SCROLL_SPEEDS = new HashMap<String, Float>();

    static {
        TRAIL_SCROLL_SPEEDS.put("tahlan_utpc_shot", 500f);
        TRAIL_SCROLL_SPEEDS.put("tahlan_utpc_shot_splinter", 500f);
        TRAIL_SCROLL_SPEEDS.put("tahlan_porph_shot", 500f);
        TRAIL_SCROLL_SPEEDS.put("tahlan_armiger_shot", 500f);
        TRAIL_SCROLL_SPEEDS.put("tahlan_styrix_shot", 500f);
        TRAIL_SCROLL_SPEEDS.put("tahlan_magaera_shot", 500f);
        TRAIL_SCROLL_SPEEDS.put("tahlan_gallant_shot", 500f);
        TRAIL_SCROLL_SPEEDS.put("tahlan_hekaton_torp", 500f);
        TRAIL_SCROLL_SPEEDS.put("tahlan_astrix_shot", 1000f);
        TRAIL_SCROLL_SPEEDS.put("tahlan_disparax_shot", 1000f);
    }

    private static final Map<String, Float> TRAIL_SPAWN_OFFSETS = new HashMap<>();

    static {
        TRAIL_SPAWN_OFFSETS.put("tahlan_utpc_shot", 30f);
        TRAIL_SPAWN_OFFSETS.put("tahlan_utpc_shot_splinter", 30f);
        TRAIL_SPAWN_OFFSETS.put("tahlan_porph_shot", 30f);
        TRAIL_SPAWN_OFFSETS.put("tahlan_armiger_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("tahlan_styrix_shot", 20f);
        TRAIL_SPAWN_OFFSETS.put("tahlan_magaera_shot", 15f);
        TRAIL_SPAWN_OFFSETS.put("tahlan_gallant_shot", 15f);
        TRAIL_SPAWN_OFFSETS.put("tahlan_hekaton_torp", 10f);
        TRAIL_SPAWN_OFFSETS.put("tahlan_astrix_shot", 20f);
        TRAIL_SPAWN_OFFSETS.put("tahlan_disparax_shot", 20f);
    }

    //NEW: compensates for lateral movement of a projectile. Should generally be 0f in most cases, due to some oddities
    //in behaviour with direction-changing scripts, but can be helpful for aligning certain projectiles
    private static final Map<String, Float> LATERAL_COMPENSATION_MULT = new HashMap<>();

    static {
        LATERAL_COMPENSATION_MULT.put("tahlan_utpc_shot", 1f);
        LATERAL_COMPENSATION_MULT.put("tahlan_utpc_shot_splinter", 1f);
        LATERAL_COMPENSATION_MULT.put("tahlan_porph_shot", 1f);
        LATERAL_COMPENSATION_MULT.put("tahlan_armiger_shot", 1f);
        LATERAL_COMPENSATION_MULT.put("tahlan_styrix_shot", 1f);
        LATERAL_COMPENSATION_MULT.put("tahlan_magaera_shot", 1f);
        LATERAL_COMPENSATION_MULT.put("tahlan_gallant_shot", 1f);
        LATERAL_COMPENSATION_MULT.put("tahlan_hekaton_torp", 0f);
        LATERAL_COMPENSATION_MULT.put("tahlan_astrix_shot", 1f);
        LATERAL_COMPENSATION_MULT.put("tahlan_disparax_shot", 1f);
    }

    //NEW: whether a shot's trail loses opacity as the projectile fades out. Should generally be true, but may need to
    //be set to false on some scripted weapons. Has no real effect on flak rounds or missiles, and should thus be set
    //false for those
    private static final Map<String, Boolean> FADE_OUT_FADES_TRAIL = new HashMap<>();

    static {
        FADE_OUT_FADES_TRAIL.put("tahlan_utpc_shot", true);
        FADE_OUT_FADES_TRAIL.put("tahlan_utpc_shot_splinter", true);
        FADE_OUT_FADES_TRAIL.put("tahlan_porph_shot", true);
        FADE_OUT_FADES_TRAIL.put("tahlan_armiger_shot", true);
        FADE_OUT_FADES_TRAIL.put("tahlan_styrix_shot", true);
        FADE_OUT_FADES_TRAIL.put("tahlan_magaera_shot", true);
        FADE_OUT_FADES_TRAIL.put("tahlan_gallant_shot", true);
        FADE_OUT_FADES_TRAIL.put("tahlan_hekaton_torp", true);
        FADE_OUT_FADES_TRAIL.put("tahlan_astrix_shot", true);
        FADE_OUT_FADES_TRAIL.put("tahlan_disparax_shot", true);
    }

    //NEW: whether a shot should have its direction adjusted to face the same way as its velocity vector, thus
    //helping with trail alignment for projectiles without using lateral compensation. DOES NOT WORK FOR
    //PROJECTILES SPAWNED WITH BALLISTIC_AS_BEAM AS SPAWNTYPE, and should not be used on missiles
    private static final Map<String, Boolean> PROJECTILE_ANGLE_ADJUSTMENT = new HashMap<>();

    static {
        PROJECTILE_ANGLE_ADJUSTMENT.put("tahlan_utpc_shot", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("tahlan_utpc_shot_splinter", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("tahlan_porph_shot", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("tahlan_armiger_shot", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("tahlan_styrix_shot", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("tahlan_magaera_shot", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("tahlan_gallant_shot", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("tahlan_hekaton_torp", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("tahlan_astrix_shot", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("tahlan_disparax_shot", false);
    }

    //Don't touch: this is for tracking our spawn delay at high time mults
    float timer = 0f;

    @Override
    public void init(CombatEngineAPI engine) {
        //Reinitialize the lists
        projectileTrailIDs.clear();
        projectileTrailIDs2.clear();
        projectileTrailIDs3.clear();
        projectileTrailIDs4.clear();
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();

        //Only run once our timer is finished
        timer += amount;
        if (timer < MINIMUM_ENGINE_TIME_WAIT) {
            return;
        } else {
            timer = 0f;
        }

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
            SpriteAPI spriteToUse = Global.getSettings().getSprite("fx", TRAIL_SPRITES.get(specID));
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
            Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * TRAIL_SPAWN_OFFSETS.get(specID), (float) Math.sin(Math.toRadians(proj.getFacing())) * TRAIL_SPAWN_OFFSETS.get(specID));
            Vector2f spawnPosition = new Vector2f(offsetPoint.x + proj.getLocation().x, offsetPoint.y + proj.getLocation().y);

            //Sideway offset velocity, for projectiles that use it
            Vector2f projBodyVel = VectorUtils.rotate(projVel, -proj.getFacing());
            Vector2f projLateralBodyVel = new Vector2f(0f, projBodyVel.getY());
            Vector2f sidewayVel = (Vector2f) VectorUtils.rotate(projLateralBodyVel, proj.getFacing()).scale(LATERAL_COMPENSATION_MULT.get(specID));

            //Opacity adjustment for fade-out, if the projectile uses it
            float opacityMult = 1f;
            if (FADE_OUT_FADES_TRAIL.get(specID) && proj.isFading()) {
                opacityMult = Math.max(0f, Math.min(1f, proj.getDamageAmount() / proj.getBaseDamageAmount()));
            }

            //So we have one less trail for the Magaera
            if (specID.contains("tahlan_magaera_shot")) {
                if (projectileTrailIDs.get(proj) == null) {
                    projectileTrailIDs.put(proj, MagicTrailPlugin.getUniqueID());
                }
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, spawnPosition, 20f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 180f,
                        0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                        TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                if (projectileTrailIDs2.get(proj) == null) {
                    projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                }
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 20f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 180f,
                        0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                        TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                Vector2f glowOffsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * 20f, (float) Math.sin(Math.toRadians(proj.getFacing())) * 20f);
                Vector2f glowSpawnPosition = new Vector2f(glowOffsetPoint.x + proj.getLocation().x, glowOffsetPoint.y + proj.getLocation().y);

                if (projectileTrailIDs3.get(proj) == null) {
                    projectileTrailIDs3.put(proj, MagicTrailPlugin.getUniqueID());
                }
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs3.get(proj), Global.getSettings().getSprite("fx", "tahlan_trail_smooth"), glowSpawnPosition, 0f, 0f, proj.getFacing() - 180f,
                        0f, 0f, 40f, 10f, new Color(140, 215, 255), new Color(140, 215, 255),
                        0.4f * opacityMult, 0f, 0.05f, 0.15f, TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), 0f, sidewayVel, null);


            } else if (specID.contains("tahlan_gallant_shot")) {
                if (projectileTrailIDs.get(proj) == null) {
                    projectileTrailIDs.put(proj, MagicTrailPlugin.getUniqueID());
                }
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, spawnPosition, 20f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 180f,
                        0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                        TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                Vector2f glowOffsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * 20f, (float) Math.sin(Math.toRadians(proj.getFacing())) * 20f);
                Vector2f glowSpawnPosition = new Vector2f(glowOffsetPoint.x + proj.getLocation().x, glowOffsetPoint.y + proj.getLocation().y);

                if (projectileTrailIDs2.get(proj) == null) {
                    projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                }
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), Global.getSettings().getSprite("fx", "tahlan_trail_smooth"), glowSpawnPosition, 0f, 0f, proj.getFacing() - 180f,
                        0f, 0f, 40f, 10f, new Color(140, 215, 255), new Color(140, 215, 255),
                        0.4f * opacityMult, 0f, 0.05f, 0.15f, TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), 0f, sidewayVel, null);


            } else if (specID.contains("tahlan_hekaton_torp")||specID.contains("tahlan_disparax_shot")) {
                if (projectileTrailIDs.get(proj) == null) {
                    projectileTrailIDs.put(proj, MagicTrailPlugin.getUniqueID());
                }
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, spawnPosition, 20f, MathUtils.getRandomNumberInRange(20f, 125f), proj.getFacing() - 175f,
                        0f, MathUtils.getRandomNumberInRange(-200f, 200f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                        TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                if (projectileTrailIDs2.get(proj) == null) {
                    projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                }
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 20f, MathUtils.getRandomNumberInRange(20f, 125f), proj.getFacing() - 185f,
                        0f, MathUtils.getRandomNumberInRange(-200f, 200f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                        TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);


            } else {

                //Then, actually spawn a trail
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, spawnPosition, 0f, 0f, proj.getFacing() - 180f,
                        0f, 0f, START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                        TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                if (specID.contains("tahlan_utpc_shot")) {
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), Global.getSettings().getSprite("fx", "tahlan_trail_smooth"), spawnPosition, 0f, 0f, proj.getFacing() - 180f,
                            0f, 0f, 50f, 20f, new Color(255, 54, 0), new Color(255, 54, 0),
                            0.3f * opacityMult, 0f, 0.05f, 0.15f, TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), 0f, sidewayVel, null);
                }

                //The Ar Ciel is one angry gun it so needs some angry trails
                if (specID.contains("tahlan_styrix_shot")) {
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 20f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 170f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            0.3f * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                    if (projectileTrailIDs3.get(proj) == null) {
                        projectileTrailIDs3.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs3.get(proj), spriteToUse, spawnPosition, 20f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 190f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            0.3f * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                    Vector2f glowOffsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * 40f, (float) Math.sin(Math.toRadians(proj.getFacing())) * 40f);
                    Vector2f glowSpawnPosition = new Vector2f(glowOffsetPoint.x + proj.getLocation().x, glowOffsetPoint.y + proj.getLocation().y);

                    if (projectileTrailIDs4.get(proj) == null) {
                        projectileTrailIDs4.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs4.get(proj), Global.getSettings().getSprite("fx", "tahlan_trail_smooth"), glowSpawnPosition, 0f, 0f, proj.getFacing() - 180f,
                            0f, 0f, 60f, 30f, new Color(140, 215, 255), new Color(140, 215, 255),
                            0.5f * opacityMult, 0f, 0.05f, 0.15f, TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), 0f, sidewayVel, null);
                }

                if (specID.contains("tahlan_porph_shot")) {
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 20f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 180f,
                            MathUtils.getRandomNumberInRange(-100f, 100f), MathUtils.getRandomNumberInRange(-380f, 380f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            0.3f * opacityMult, TRAIL_DURATIONS_IN.get(specID), 0f, 0.5f * TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                    if (projectileTrailIDs3.get(proj) == null) {
                        projectileTrailIDs3.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs3.get(proj), Global.getSettings().getSprite("fx", "tahlan_trail_smooth"), spawnPosition, 0f, 0f, proj.getFacing() - 180f,
                            0f, 0f, 40f, 10f, new Color(255, 48, 0), new Color(223, 43, 22),
                            0.4f * opacityMult, 0f, 0.1f, 0.2f, TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), 0f, sidewayVel, null);

                }

            }
        }
    }
}