package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glPopAttrib;
import static org.lwjgl.opengl.GL11.glPopMatrix;

public class tahlan_Adlerauge extends BaseHullMod {

    private static final String ADLER_ID = "Adlerauge_ID";
    private static final float DAMAGE_MULT = 1.1f;
    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>(1);
    private static final float EFFECT_RANGE = 2000f;
    private static final float AUTOAIM_BONUS = 50f;

    // sprite path - necessary if loaded here and not in settings.json
    public static final String SPRITE_PATH = "graphics/fx/shields256.png";
    public static final Color COLOR = new Color(186, 47, 52, 185);
    public static final float ROTATION_SPEED = 20f;

    private boolean loaded = false;
    private float rotation = 0f;
    private SpriteAPI sprite = null;


    private static final EnumSet<WeaponAPI.WeaponType> WEAPON_TYPES = EnumSet.of(WeaponAPI.WeaponType.MISSILE,WeaponAPI.WeaponType.BALLISTIC,WeaponAPI.WeaponType.ENERGY);

    static {
        BLOCKED_HULLMODS.add("safetyoverrides");
    }

    private List<ShipAPI> targetList = new ArrayList<ShipAPI>();
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

	    stats.getMissileWeaponDamageMult().modifyMult(id,DAMAGE_MULT);
	    stats.getBallisticWeaponDamageMult().modifyMult(id,DAMAGE_MULT);
	    stats.getEnergyWeaponDamageMult().modifyMult(id,DAMAGE_MULT);
        stats.getAutofireAimAccuracy().modifyFlat(id, AUTOAIM_BONUS * 0.01f);
	}

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                ship.getVariant().removeMod(tmp);
            }
        }
    }

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {

        CombatEngineAPI engine = Global.getCombatEngine();

        //Glows off in refit screen
        if (ship.getOriginalOwner() == -1) {
            return;
        }

        if (sprite == null) {
            // Load sprite if it hasn't been loaded yet - not needed if you add it to settings.json
            if (!loaded) {
                try {
                    Global.getSettings().loadTexture(SPRITE_PATH);
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to load sprite '" + SPRITE_PATH + "'!", ex);
                }

                loaded = true;
            }
            sprite = Global.getSettings().getSprite(SPRITE_PATH);
        }

        final Vector2f loc = ship.getLocation();
        final ViewportAPI view = Global.getCombatEngine().getViewport();
        if (view.isNearViewport(loc, EFFECT_RANGE)) {
            glPushAttrib(GL_ENABLE_BIT);
            glMatrixMode(GL_PROJECTION);
            glPushMatrix();
            glViewport(0, 0, Display.getWidth(), Display.getHeight());
            glOrtho(0.0, Display.getWidth(), 0.0, Display.getHeight(), -1.0, 1.0);
            glEnable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            final float radius = (EFFECT_RANGE * 2f) / view.getViewMult();
            sprite.setSize(radius, radius);
            sprite.setColor(COLOR);
            sprite.setAlphaMult(0.1f);
            sprite.renderAtCenter(view.convertWorldXtoScreenX(loc.x), view.convertWorldYtoScreenY(loc.y));
            sprite.setAngle(rotation);
            glPopMatrix();
            glPopAttrib();
        }

        // Spin it
        rotation += ROTATION_SPEED * amount;
        if (rotation > 360f){
            rotation -= 360f;
        }

        //self weapon glow
        //ship.setWeaponGlow(1f,COLOR, WEAPON_TYPES);


        //Lets list our applicable targets first

        for (ShipAPI target : CombatUtils.getShipsWithinRange(ship.getLocation(), EFFECT_RANGE)) {
            if (target.getOwner() == ship.getOwner()) {
                if (target.getOwner() == ship.getOwner() && !targetList.contains(target) && target.getVariant().getHullMods().contains("tahlan_silberherz")) {
                    targetList.add(target);
                }
            }
        }
        List<ShipAPI> purgeList = new ArrayList<ShipAPI>();
        for (ShipAPI target : targetList) {
            if (MathUtils.getDistance(target.getLocation(), ship.getLocation()) <= EFFECT_RANGE) {

                target.getMutableStats().getEnergyWeaponDamageMult().modifyMult(ADLER_ID,DAMAGE_MULT);
                target.getMutableStats().getBallisticWeaponDamageMult().modifyMult(ADLER_ID,DAMAGE_MULT);
                target.getMutableStats().getMissileWeaponDamageMult().modifyMult(ADLER_ID,DAMAGE_MULT);
                target.getMutableStats().getAutofireAimAccuracy().modifyFlat(ADLER_ID, AUTOAIM_BONUS * 0.01f);

                target.setWeaponGlow(1f,COLOR, WEAPON_TYPES);

            } else {

                target.getMutableStats().getEnergyWeaponDamageMult().unmodify(ADLER_ID);
                target.getMutableStats().getBallisticWeaponDamageMult().unmodify(ADLER_ID);
                target.getMutableStats().getMissileWeaponDamageMult().unmodify(ADLER_ID);
                target.getMutableStats().getAutofireAimAccuracy().unmodify(ADLER_ID);

                target.setWeaponGlow(0f,COLOR, WEAPON_TYPES);
                purgeList.add(target);
            }

        }
        for (ShipAPI purge : purgeList) {
            targetList.remove(purge);
        }


	}

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {

    }

    public boolean isApplicableToShip(ShipAPI ship) {
		return false;
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "all weapon damage";
		if (index == 1) return "" + (int)((DAMAGE_MULT-1f)*100f) + "%";
        if (index == 2) return "Silberherz-equipped";
		if (index == 3) return "" + (int)EFFECT_RANGE + "su";

		return null;
	}
	

}
