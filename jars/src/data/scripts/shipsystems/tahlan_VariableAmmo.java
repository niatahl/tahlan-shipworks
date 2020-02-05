//By Nicke535, used in tandem with a unique hullmod to act as a "Shell Swapper"
package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class tahlan_VariableAmmo extends BaseShipSystemScript {

    private int actualAmmo = 1;
    private boolean runOnce = true;
    private float cooldownSet = 0.25f;

    //We can be locked from activating our system due to an "extra cooldown" from our cannons
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        //Check if we are flagged to not fire
        if (Global.getCombatEngine().getCustomData().get("tahlan_VariableAmmoExtraCooldown" + ship.getId()) instanceof Float) {
            if ((float)Global.getCombatEngine().getCustomData().get("tahlan_VariableAmmoExtraCooldown" + ship.getId()) > 0f) {
                return false;
            }
        }

        //If we didn't find any specific reason to be locked, run default implementation
        return super.isUsable(system, ship);
    }

    //Always display the currently loaded shell type
    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        switch (system.getAmmo()) {
            case 1:
                return "Shock Cannon";
            case 2:
                return "Impact Driver";
            default:
                return "Type-3 Shell";
        }
    }

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (stats.getEntity() != null && stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI)stats.getEntity();

            if (ship.getSystem().getAmmo() != actualAmmo) {
                ship.getSystem().setAmmo(actualAmmo);
            }

            if (runOnce) {
                runOnce = false;
                if (actualAmmo >= 3) {
                    actualAmmo = 1;
                } else {
                    actualAmmo++;
                }

                //Ensures weapons wait to fire until the ammo switching is complete
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (weapon.getCooldownRemaining() < cooldownSet && weapon.getId().contains("tahlan_phira_burst")) {
                        weapon.setRemainingCooldownTo(cooldownSet);
                    }
                }

                //Used to "lock" ammo switching once a shell has been fired: store that we have swapped ammo since last time we fired
                Global.getCombatEngine().getCustomData().put("tahlan_VariableAmmoHasSwapped" + ship.getId(), true);
            }
        }
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        runOnce = true;
        if (stats.getEntity() != null && stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            if (ship.getSystem().getAmmo() != actualAmmo) {
                ship.getSystem().setAmmo(actualAmmo);
            }
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }
}