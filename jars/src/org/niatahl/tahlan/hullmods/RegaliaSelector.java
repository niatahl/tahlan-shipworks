package org.niatahl.tahlan.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;

public class RegaliaSelector extends BaseHullMod {

    @Override
    public int getDisplaySortOrder() {
        return 2000;
    }

    @Override
    public int getDisplayCategoryIndex() {
        return 3;
    }
}
