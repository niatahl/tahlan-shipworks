package org.niatahl.tahlan.utils;

import com.fs.starfarer.api.Global;

public class Utils {
    private static final String tahlan = "tahlan";

    // For translation friendliness
    public static String txt(String id) {
        return Global.getSettings().getString(tahlan, id);
    }

    // Interpolation
    public static float lerp(float x, float y, float alpha) {
        return (1f - alpha) * x + alpha * y;
    }
}