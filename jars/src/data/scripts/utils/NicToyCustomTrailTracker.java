//By Nicke535
//
//DO NOT RENAME OR MODIFY THIS FILE IN ANY WAY: IT WILL BREAK COMPATIBILITY WITH OTHER MODS USING THIS SCRIPT AND MAY CAUSE SIGNIFICANT PERFORMANCE LOSS FOR THE USER
package data.scripts.utils;

import com.fs.starfarer.api.Global;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

//This class handles each "segment" of a trail: each CustomTrailObject within the CustomTrailTracker is considered to be linked to the other objects.
//To make a new "segment" of trail, unrelated to the others, you have to create a new Tracker. The trail is invisible until at least two objects are in it
public class NicToyCustomTrailTracker {
    //For scrolling textures - NOTE: we always use the most recent scroll speed for the trail, if it for some reason changes mid-trail
    private float scrollingTextureOffset = 0f;
    public float scrollSpeed = 0f;

    //For animated textures: the trail counts as animated only if isAnimated = true
    public boolean isAnimated = false;
    public int currentAnimRenderTexture = 0;

    private List<NicToyCustomTrailObject> allTrailParts = new ArrayList<NicToyCustomTrailObject>();

    //Adds a new object to the trail, at the end (start visually) of our existing ones
    public void addNewTrailObject (NicToyCustomTrailObject objectToAdd) {
        allTrailParts.add(objectToAdd);
    }

    //The heavy, main function: render the entire trail
    public void renderTrail (int textureID) {
        //First, clear all dead objects, as they can be a pain to calculate around
        clearAllDeadObjects();

        //Then, if we have too few segments to render properly, cancel the function
        if (allTrailParts.size() <= 1) {
            return;
        }

        //If we are animated, we use our "currentAnimRenderTexture" rather than the textureID we just got supplied
        int trueTextureID = textureID;
        if (isAnimated) {
            trueTextureID = currentAnimRenderTexture;
        }

        //Otherwise, we actually render the thing
        //This part instantiates OpenGL
        glEnable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glBlendFunc(allTrailParts.get(allTrailParts.size()-1).blendModeSRC, allTrailParts.get(allTrailParts.size()-1).blendModeDEST); //NOTE: uses the most recent blend mode added to the trail
        glBindTexture(GL_TEXTURE_2D, trueTextureID);
        glBegin(GL_QUADS);

        //Iterate through all trail parts except the most recent one: the idea is that each part renders in relation to the *next* part
        float texDistTracker = 0f;
        for (int i = 0; i < allTrailParts.size()-1; i++) {
            //First, get a handle for our parts so we can make the code shorter
            NicToyCustomTrailObject part1 = allTrailParts.get(i);   //Current part
            NicToyCustomTrailObject part2 = allTrailParts.get(i+1); //Next part

            //Then, determine the corner points of both this and the next trail part
            Vector2f point1Left = new Vector2f(part1.currentLocation.x + ((part1.currentSize / 2) * (float)FastTrig.cos(Math.toRadians(part1.angle + 90))), part1.currentLocation.y + ((part1.currentSize / 2) * (float)FastTrig.sin(Math.toRadians(part1.angle + 90))));
            Vector2f point1Right = new Vector2f(part1.currentLocation.x + ((part1.currentSize / 2) * (float)FastTrig.cos(Math.toRadians(part1.angle - 90))), part1.currentLocation.y + ((part1.currentSize / 2) * (float)FastTrig.sin(Math.toRadians(part1.angle - 90))));
            Vector2f point2Left = new Vector2f(part2.currentLocation.x + ((part2.currentSize / 2) * (float)FastTrig.cos(Math.toRadians(part2.angle + 90))), part2.currentLocation.y + ((part2.currentSize / 2) * (float)FastTrig.sin(Math.toRadians(part2.angle + 90))));
            Vector2f point2Right = new Vector2f(part2.currentLocation.x + ((part2.currentSize / 2) * (float)FastTrig.cos(Math.toRadians(part2.angle - 90))), part2.currentLocation.y + ((part2.currentSize / 2) * (float)FastTrig.sin(Math.toRadians(part2.angle - 90))));

            //Saves an easy value for the distance between the current two parts
            float partDistance = MathUtils.getDistance(part1.currentLocation, part2.currentLocation);

            //-------------------------------------------------------------------Actual rendering shenanigans------------------------------------------------------------------------------------------
            //If we are outside the viewport, don't render at all! Just tick along our texture tracker, and do nothing else
            if (!Global.getCombatEngine().getViewport().isNearViewport(part1.currentLocation, part1.currentSize + 1f)) {
                //Change our texture distance tracker depending on looping mode
                //  -If we have -1 as loop length, we ensure that the entire texture is used over the entire trail
                //  -Otherwise, we adjust the texture distance upward to account for how much distance there is between our two points
                if (part1.textureLoopLength <= 0f) {
                    texDistTracker = (float)(i + 1) / (float)allTrailParts.size();
                } else {
                    texDistTracker += partDistance / part1.textureLoopLength;
                }

                continue;
            }

            //Changes opacity slightly at beginning and end: the last and first 3 segments have lower opacity
            float opacityMult = 1f;
            if (i < 3) {
                opacityMult *= ((float)i/3f);
            } else if (i > allTrailParts.size()-4) {
                opacityMult *= ((float)allTrailParts.size()-1f-(float)i)/3f;
            }

            //Sets the current render color
            glColor4ub((byte)part1.currentColor.getRed(),(byte)part1.currentColor.getGreen(),(byte)part1.currentColor.getBlue(),(byte)(part1.currentOpacity * opacityMult * 255));

            //Sets corner 1, or the first left corner
            glTexCoord2f(0, texDistTracker + scrollingTextureOffset);
            glVertex2f(point1Left.getX(),point1Left.getY());

            //Sets corner 2, or the first right corner
            glTexCoord2f(1, texDistTracker + scrollingTextureOffset);
            glVertex2f(point1Right.getX(),point1Right.getY());

            //Change our texture distance tracker depending on looping mode
            //  -If we have -1 as loop length, we ensure that the entire texture is used over the entire trail
            //  -Otherwise, we adjust the texture distance upward to account for how much distance there is between our two points
            if (part1.textureLoopLength <= 0f) {
                texDistTracker = (float)(i + 1) / (float)allTrailParts.size();
            } else {
                texDistTracker += partDistance / part1.textureLoopLength;
            }

            //Changes opacity slightly at beginning and end: the last 5 and first 5 segments have lower opacity
            opacityMult = 1f;
            if ((i + 1) < 3) {
                opacityMult *= ((float)(i+1)/3f);
            } else if ((i + 1) > allTrailParts.size()-4) {
                opacityMult *= ((float)allTrailParts.size()-2f-(float)i)/3f;
            }

            //Changes render color to our next segment's opacity
            glColor4ub((byte)part2.currentColor.getRed(),(byte)part2.currentColor.getGreen(),(byte)part2.currentColor.getBlue(),(byte)(part2.currentOpacity * opacityMult * 255));

            //Sets corner 3, or the second right corner
            glTexCoord2f(1, texDistTracker + scrollingTextureOffset);
            glVertex2f(point2Right.getX(),point2Right.getY());

            //Sets corner 4, or the second left corner
            glTexCoord2f(0, texDistTracker + scrollingTextureOffset);
            glVertex2f(point2Left.getX(),point2Left.getY());
        }

        //And finally stops OpenGL
        glEnd();
    }

    //Quickhand function to tick down all trail objects at once, by an equal amount of time. Also ticks texture scrolling, if we have it
    public void tickTimersInTrail (float amount) {
        for (NicToyCustomTrailObject part : allTrailParts) {
            part.tick(amount);
        }

        //Defines the scroll speed in 1/1000th of a full texture per second
        scrollingTextureOffset += (amount * scrollSpeed) / 1000f;
    }

    //Quickhand function to remove all trail objects which has timed out... this can start looking *really* wierd if you try making a trail with varying fade time
    public void clearAllDeadObjects (){
        List<NicToyCustomTrailObject> toRemove = new ArrayList<NicToyCustomTrailObject>();
        for (NicToyCustomTrailObject part : allTrailParts) {
            if (part.getSpentLifetime() >= part.getTotalLifetime()) {
                toRemove.add(part);
            }
        }

        for (NicToyCustomTrailObject part : toRemove) {
            allTrailParts.remove(part);
        }
    }
}