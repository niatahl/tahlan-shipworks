package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glEnable;

//Original by MesoTroniK and Machine
public class tahlan_ProjectileFX extends BaseEveryFrameCombatPlugin
{
    private static final Set<String> ProjFX_IDS = new HashSet<>();
    static {
        //add Projectile IDs here.
        ProjFX_IDS.add("tahlan_utpc_shot");
        ProjFX_IDS.add("tahlan_utpc_shot_splinter");
    }

    private static final float UTPC_Y_OFFSET = 0f;
    private static final float UTPC_SPLINTER_Y_OFFSET = 0f;

    private static final float TIME_BETWEEN_CHECKS = 0.33f;
    private final Map<DamagingProjectileAPI, Jitter> projectiles = new HashMap<>(30);
    private float nextCheck = TIME_BETWEEN_CHECKS;

    private SpriteAPI utpc_sprite_1;
    private SpriteAPI utpc_sprite_2;
    private SpriteAPI utpc_splinter_sprite_1;
    private SpriteAPI utpc_splinter_sprite_2;


    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused())
        {
            return;
        }

        // Scan all projectiles for new projectiles periodically
        nextCheck -= amount;
        if (nextCheck <= 0f)
        {
            nextCheck = TIME_BETWEEN_CHECKS;

            // Scan for projectiles
            for (DamagingProjectileAPI missile : engine.getProjectiles()) {

                String spec = missile.getProjectileSpecId();

                if (ProjFX_IDS.contains(spec)) {

                    switch (spec) {

                        case "tahlan_utpc_shot": {
                            if (!projectiles.containsKey(missile)){
                                projectiles.put(missile, new Jitter(missile));
                            }
                            break;
                        }
                        case "tahlan_utpc_shot_splinter": {
                            if (!projectiles.containsKey(missile)){
                                projectiles.put(missile, new Jitter(missile));
                            }
                            break;
                        }

                    }
                }
            }
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {

        //UTPC
        utpc_sprite_1 = Global.getSettings().getSprite("fx_projectiles", "tahlan_utpc_shot");
        utpc_sprite_1.setAdditiveBlend();
        utpc_sprite_2 = Global.getSettings().getSprite("fx_projectiles", "tahlan_utpc_shot");
        utpc_sprite_2.setAdditiveBlend();

        //UTPC Splinter
        utpc_splinter_sprite_1 = Global.getSettings().getSprite("fx_projectiles", "tahlan_utpc_shot_splinter");
        utpc_splinter_sprite_1.setAdditiveBlend();
        utpc_splinter_sprite_2 = Global.getSettings().getSprite("fx_projectiles", "tahlan_utpc_shot_splinter");
        utpc_splinter_sprite_2.setAdditiveBlend();

        projectiles.clear();
    }

    @Override
    public void renderInWorldCoords(ViewportAPI view)
    {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null)
        {
            return;
        }

        if (!projectiles.isEmpty())
        {
            float amount = (engine.isPaused() ? 0f : engine.getElapsedInLastFrame());
            glEnable(GL_TEXTURE_2D);
            for (Iterator<Map.Entry<DamagingProjectileAPI, Jitter>> iter = projectiles.entrySet().iterator(); iter.hasNext();)
            {
                Map.Entry<DamagingProjectileAPI, Jitter> entry = iter.next();
                DamagingProjectileAPI missile = entry.getKey();

                // Remove dead missiles
                if (missile.didDamage() || !engine.isEntityInPlay(missile))
                {
                    iter.remove();
                    continue;
                }

                // Advance effect
                entry.getValue().advanceAndRender(amount);
            }
        }
    }

    private class Jitter
    {
        private float ProjectileFading;
        private final DamagingProjectileAPI shot;
        private float timer;

        private Jitter(DamagingProjectileAPI shot)
        {
            this.shot = shot;
            ProjectileFading = 1f;
            timer = 0f;
        }

        private void advanceAndRender(float amount) {


            if (!shot.isFading()){
                ProjectileFading = 1f;
            }

            if (shot.isFading()){
                ProjectileFading = (1f - 1f*Math.min(1f,timer));
                timer += 85*amount;
            }

            String spec = shot.getProjectileSpecId();

            if (shot.getProjectileSpecId().equals(spec)){

                switch (spec) {

                    case "tahlan_utpc_shot": {
                        utpc_sprite_1.setAlphaMult((float) (ProjectileFading * (Math.min(0.5f, (Math.random()/2 + 0.1f)))));
                        utpc_sprite_2.setAlphaMult((float) (ProjectileFading * (Math.min(0.5f, (Math.random()/2 + 0.1f)))));
                        utpc_sprite_1.setAngle(shot.getFacing() - 90f);
                        utpc_sprite_2.setAngle(shot.getFacing() - 90f);
                        Vector2f loc = MathUtils.getPointOnCircumference(
                                shot.getLocation(), UTPC_Y_OFFSET, shot.getFacing());
                        utpc_sprite_1.renderAtCenter((float) (loc.x + 2*Math.random()), (float) (loc.y + 2*Math.random()));
                        utpc_sprite_2.renderAtCenter((float) (loc.x - 2*Math.random()), (float) (loc.y - 2*Math.random()));
                        break;
                    }

                    case "tahlan_utpc_shot_splinter": {
                        utpc_splinter_sprite_1.setAlphaMult((float) (ProjectileFading * (Math.min(0.5f, (Math.random()/2 + 0.1f)))));
                        utpc_splinter_sprite_2.setAlphaMult((float) (ProjectileFading * (Math.min(0.5f, (Math.random()/2 + 0.1f)))));
                        utpc_splinter_sprite_1.setAngle(shot.getFacing() - 90f);
                        utpc_splinter_sprite_2.setAngle(shot.getFacing() - 90f);
                        Vector2f loc = MathUtils.getPointOnCircumference(
                                shot.getLocation(), UTPC_SPLINTER_Y_OFFSET, shot.getFacing());
                        utpc_splinter_sprite_1.renderAtCenter((float) (loc.x + 2*Math.random()), (float) (loc.y + 2*Math.random()));
                        utpc_splinter_sprite_2.renderAtCenter((float) (loc.x - 2*Math.random()), (float) (loc.y - 2*Math.random()));
                        break;
                    }
                }
            }
        }
    }
}
