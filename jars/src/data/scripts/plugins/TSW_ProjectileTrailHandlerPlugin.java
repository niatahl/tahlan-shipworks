//By Nicke535, handles the trails for various projectiles in the mod
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class TSW_ProjectileTrailHandlerPlugin extends BaseEveryFrameCombatPlugin {

    //A map of all the trail sprites used (note that all the sprites must be under TSW_fx): ensure this one has the same keys as the other maps
    private static final Map<String, String> TRAIL_SPRITES = new HashMap<String, String>();

    static {
        TRAIL_SPRITES.put("TSW_adloquium_shot", "projectile_trail_standard");
    }

    //A map for known projectiles and their IDs: should be cleared in init
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs = new HashMap<DamagingProjectileAPI, Float>();

    //Used when doing dual-core sprites
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs2 = new HashMap<DamagingProjectileAPI, Float>();

    //Used for the Equity
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs3 = new HashMap<DamagingProjectileAPI, Float>();

    //--------------------------------------THESE ARE ALL MAPS FOR DIFFERENT VISUAL STATS FOR THE TRAILS: THEIR NAMES ARE FAIRLY SELF_EXPLANATORY---------------------------------------------------
    private static final Map<String, Float> TRAIL_DURATIONS_IN = new HashMap<String, Float>();

    static {
        TRAIL_DURATIONS_IN.put("TSW_adloquium_shot", 0f);
    }

    private static final Map<String, Float> TRAIL_DURATIONS_MAIN = new HashMap<String, Float>();

    static {
        TRAIL_DURATIONS_MAIN.put("TSW_adloquium_shot", 0f);
    }

    private static final Map<String, Float> TRAIL_DURATIONS_OUT = new HashMap<String, Float>();

    static {
        TRAIL_DURATIONS_OUT.put("TSW_adloquium_shot", 0.6f);
    }

    private static final Map<String, Float> START_SIZES = new HashMap<String, Float>();

    static {
        START_SIZES.put("TSW_adloquium_shot", 13f);
    }

    private static final Map<String, Float> END_SIZES = new HashMap<String, Float>();

    static {
        END_SIZES.put("TSW_adloquium_shot", 7f);
    }

    private static final Map<String, Color> TRAIL_START_COLORS = new HashMap<String, Color>();

    static {
        TRAIL_START_COLORS.put("TSW_adloquium_shot", new Color(255, 55, 185));
    }

    private static final Map<String, Color> TRAIL_END_COLORS = new HashMap<String, Color>();

    static {
        TRAIL_END_COLORS.put("TSW_adloquium_shot", new Color(255, 55, 185));
    }

    private static final Map<String, Float> TRAIL_OPACITIES = new HashMap<String, Float>();

    static {
        TRAIL_OPACITIES.put("TSW_adloquium_shot", 0.8f);
    }

    private static final Map<String, Integer> TRAIL_BLEND_SRC = new HashMap<String, Integer>();

    static {
        TRAIL_BLEND_SRC.put("TSW_adloquium_shot", GL_SRC_ALPHA);
    }

    private static final Map<String, Integer> TRAIL_BLEND_DEST = new HashMap<String, Integer>();

    static {
        TRAIL_BLEND_DEST.put("TSW_adloquium_shot", GL_ONE);
    }

    private static final Map<String, Float> TRAIL_LOOP_LENGTHS = new HashMap<String, Float>();

    static {
        TRAIL_LOOP_LENGTHS.put("TSW_adloquium_shot", -1f);
    }

    private static final Map<String, Float> TRAIL_SCROLL_SPEEDS = new HashMap<String, Float>();

    static {
        TRAIL_SCROLL_SPEEDS.put("TSW_adloquium_shot", 0f);
    }

    @Override
    public void init(CombatEngineAPI engine) {
        //Reinitialize the lists
        projectileTrailIDs.clear();
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

            //-------------------------------------------For visual effects---------------------------------------------
            if (!TRAIL_SPRITES.keySet().contains(proj.getProjectileSpecId())) {
                continue;
            }
            String specID = proj.getProjectileSpecId();
            SpriteAPI spriteToUse = Global.getSettings().getSprite("TSW_fx", TRAIL_SPRITES.get(specID));

            //If we haven't already started a trail for this projectile, get an ID for it
            if (projectileTrailIDs.get(proj) == null) {
                projectileTrailIDs.put(proj, NicToyCustomTrailPlugin.getUniqueID());
            }

            //CUSTOM: The Benediciton's missiles use a different trail, with randomness to the end
            if (specID.contains("TSW_benediction_msl")) {
                NicToyCustomTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, proj.getLocation(), 0f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 180f,
                        0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                        TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID));
            } else {
                //Then, actually spawn a trail
                NicToyCustomTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, proj.getLocation(), 0f, 0f, proj.getFacing() - 180f,
                        0f, 0f, START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                        TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID));


                //The Phira is an evil Purgatory that I just hacked together from the purgatory trail code  -Nia
                if (specID.contains("TSW_phira_shock_shot")) {
                    //If we haven't already started a second trail for this projectile, get an ID for it
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, NicToyCustomTrailPlugin.getUniqueID());
                    }
                    NicToyCustomTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, proj.getLocation(), 0f, 0f, proj.getFacing() - 180f,
                            0f, 0f, START_SIZES.get(specID) / 2f, END_SIZES.get(specID) / 2f, Color.white, Color.white,
                            TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID));
                }

                //The Equity has a whopping 3 trails, with custom behaviour to boot!
                if (specID.contains("TSW_equity_shot")) {
                    //If we haven't already started a second trail for this projectile, get an ID for it
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, NicToyCustomTrailPlugin.getUniqueID());
                    }
                    NicToyCustomTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, proj.getLocation(), MathUtils.getRandomNumberInRange(0f, 200f), MathUtils.getRandomNumberInRange(0f, 500f), proj.getFacing() - 180f,
                            MathUtils.getRandomNumberInRange(-200f, 200f), MathUtils.getRandomNumberInRange(-500f, 500f), START_SIZES.get(specID) * 2f, END_SIZES.get(specID) * 2f, TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID) * 0.4f, TRAIL_DURATIONS_MAIN.get(specID) * 0.5f, TRAIL_DURATIONS_OUT.get(specID) * 0.5f, TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID));

                    //If we haven't already started a third trail for this projectile, get an ID for it
                    if (projectileTrailIDs3.get(proj) == null) {
                        projectileTrailIDs3.put(proj, NicToyCustomTrailPlugin.getUniqueID());
                    }
                    NicToyCustomTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs3.get(proj), spriteToUse, proj.getLocation(), MathUtils.getRandomNumberInRange(0f, 220f), MathUtils.getRandomNumberInRange(0f, 600f), proj.getFacing() - 180f,
                            MathUtils.getRandomNumberInRange(-220f, 220f), MathUtils.getRandomNumberInRange(-500f, 500f), START_SIZES.get(specID) * 2f, END_SIZES.get(specID) * 2f, TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID) * 0.25f, TRAIL_DURATIONS_MAIN.get(specID) * 0.25f, TRAIL_DURATIONS_OUT.get(specID) * 0.25f, TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID));
                }

                //The Ar Ciel is one angry gun it so needs some angry trails
                if (specID.contains("TSW_arciel_shot")) {
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, NicToyCustomTrailPlugin.getUniqueID());
                    }
                    NicToyCustomTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, proj.getLocation(), 20f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 160f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            0.4f, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID));

                    if (projectileTrailIDs3.get(proj) == null) {
                        projectileTrailIDs3.put(proj, NicToyCustomTrailPlugin.getUniqueID());
                    }
                    NicToyCustomTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs3.get(proj), spriteToUse, proj.getLocation(), 20f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 200f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            0.4f, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID));

                }

                //The Qoga is a smaller Ar Ciel so we do the same trail diffusion here cause it looks sexy af for these kinda weapons
                if (specID.contains("TSW_qoga_shot")) {
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, NicToyCustomTrailPlugin.getUniqueID());
                    }
                    NicToyCustomTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, proj.getLocation(), 0f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 180f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID));

                }

                //Harmonius is a biiiig gun so it needs some fancy shit
                if (specID.contains("TSW_harmonius_shot")) {
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, NicToyCustomTrailPlugin.getUniqueID());
                    }
                    NicToyCustomTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, proj.getLocation(), 0f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 180f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), 10f, 10f, new Color(205, 100, 255), new Color(205, 100, 255),
                            TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID), 0.5f, TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID));
                }
            }
        }
    }
}