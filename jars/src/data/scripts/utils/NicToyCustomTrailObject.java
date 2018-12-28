//By Nicke535, with help from Originem
//
//DO NOT RENAME OR MODIFY THIS FILE IN ANY WAY: IT WILL BREAK COMPATIBILITY WITH OTHER MODS USING THIS SCRIPT AND MAY CAUSE SIGNIFICANT PERFORMANCE LOSS FOR THE USER
package data.scripts.utils;

import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;
import java.awt.*;

public class NicToyCustomTrailObject {
    //Private, non-varying values
    private float inDuration = 0f;
    private float mainDuration = 0f;
    private float outDuration = 0f;
    private float startSize = 0f;
    private float endSize = 0f;
    private float startAngleVelocity = 0f;
    private float endAngleVelocity = 0f;
    private float mainOpacity = 0f;
    private float startSpeed = 0f;
    private float endSpeed = 0f;
    private Color startColor = new Color(255, 255, 255);
    private Color endColor = new Color(255, 255, 255);

    //Public, non-varying values
    public int blendModeSRC = 0;
    public int blendModeDEST = 0;
    public float textureLoopLength = 0;

    //Public, varying values
    public Color currentColor = new Color(255, 255, 255);
    public float currentSize = 0f;
    public float spentLifetime = 0f;
    public float currentAngularVelocity = 0f;
    public float angle = 0f;
    public float currentSpeed = 0f;
    public Vector2f currentLocation = new Vector2f(0f, 0f);
    public float currentOpacity = 0f;

    //Main instantiator: generates a full CustomTrailObject with all necessary values
    public NicToyCustomTrailObject (float inDuration, float mainDuration, float outDuration, float startSize, float endSize, float startAngleVelocity,
                                    float endAngleVelocity, float mainOpacity, int blendModeSRC, int blendModeDEST, float startSpeed, float endSpeed,
                                    Color startColor, Color endColor, float angle, Vector2f spawnLocation, float textureLoopLength) {
        this.inDuration = inDuration;
        this.mainDuration = mainDuration;
        this.outDuration = outDuration;
        this.startSize = startSize;
        this.endSize = endSize;
        this.startAngleVelocity = startAngleVelocity;
        this.endAngleVelocity = endAngleVelocity;
        this.mainOpacity = mainOpacity;
        this.blendModeSRC = blendModeSRC;
        this.blendModeDEST = blendModeDEST;
        this.startSpeed = startSpeed;
        this.endSpeed = endSpeed;
        this.startColor = startColor;
        this.endColor = endColor;
        this.angle = angle;
        this.currentLocation.x = spawnLocation.x;
        this.currentLocation.y = spawnLocation.y;
        this.textureLoopLength = textureLoopLength;

        this.currentColor = startColor;
        this.currentSize = startSize;
        this.spentLifetime = 0f;
        this.currentAngularVelocity = startAngleVelocity;
        this.currentSpeed = startSpeed;
        if (inDuration > 0) {
            this.currentOpacity = 0f;
        } else {
            this.currentOpacity = mainOpacity;
        }
    }

    //Modifies lifetime, position and all other things time-related
    public void tick (float amount) {
        //Increases lifetime
        spentLifetime += amount;

        //If our spent lifetime is higher than our total lifetime, set it to our total lifetime
        if (spentLifetime > getTotalLifetime()) {
            spentLifetime = getTotalLifetime();
        }

        //Slides all values along depending on lifetime
        currentSize = startSize * (1 - (spentLifetime / getTotalLifetime())) + endSize * (spentLifetime / getTotalLifetime());
        currentSpeed = startSpeed * (1 - (spentLifetime / getTotalLifetime())) + endSpeed * (spentLifetime / getTotalLifetime());
        currentAngularVelocity = startAngleVelocity * (1 - (spentLifetime / getTotalLifetime())) + endAngleVelocity * (spentLifetime / getTotalLifetime());
        int red = ((int)(startColor.getRed() * (1 - (spentLifetime / getTotalLifetime())) + endColor.getRed() * (spentLifetime / getTotalLifetime())));
        int green = ((int)(startColor.getGreen() * (1 - (spentLifetime / getTotalLifetime())) + endColor.getGreen() * (spentLifetime / getTotalLifetime())));
        int blue = ((int)(startColor.getBlue() * (1 - (spentLifetime / getTotalLifetime())) + endColor.getBlue() * (spentLifetime / getTotalLifetime())));
        currentColor = new Color(red, green, blue);

        //Adjusts opacity: slightly differently handled than the otherwise pure linear value sliding
        currentOpacity = mainOpacity;
        if (spentLifetime < inDuration) {
            currentOpacity = mainOpacity * spentLifetime / inDuration;
        } else if (spentLifetime > (inDuration + mainDuration)) {
            currentOpacity = mainOpacity * (1f - ((spentLifetime - (inDuration + mainDuration))/outDuration));
        }

        //Calculates new position and angle from respective velocities
        angle += currentAngularVelocity * amount;
        currentLocation.x += FastTrig.cos(Math.toRadians(angle)) * currentSpeed * amount;
        currentLocation.y += FastTrig.sin(Math.toRadians(angle)) * currentSpeed * amount;
    }

    public float getSpentLifetime () {
        return spentLifetime;
    }
    public float getTotalLifetime () {
        return inDuration + mainDuration + outDuration;
    }
}
