/*
By Tartiflette
 */
package data.scripts.utils;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import java.awt.Color;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lwjgl.util.vector.Vector2f;

public class tahlan_graphicLibEffects {
    
    public static void customLight(Vector2f loc, CombatEntityAPI anchor, float size, float intensity, Color color, float fadeIn, float last, float fadeOut){
        StandardLight light = new StandardLight();
        light.setLocation(loc);
        if(anchor!=null){
            light.attachTo(anchor);
        }
        light.setSize(size);
        light.setIntensity(intensity);
        light.setColor(color);
        light.setLifetime(last);
        light.fadeIn(fadeIn);
        light.fadeOut(fadeOut);
        LightShader.addLight(light);
    }
    
    public static void CustomBubbleDistortion (Vector2f loc, Vector2f vel, float size, float intensity, boolean flip, float angle, float arc, float edgeSmooth, float fadeIn, float last, float fadeOut, float growthTime, float shrinkTime){
                
        WaveDistortion wave = new WaveDistortion(loc, vel);

        wave.setIntensity(intensity);
        wave.setSize(size);
        wave.setArc(angle-arc/2,angle+arc/2);
        if(edgeSmooth!=0){
            wave.setArcAttenuationWidth(edgeSmooth);            
        }
        wave.flip(flip);
        if(fadeIn!=0){
            wave.fadeInIntensity(fadeIn);
        }
        wave.setLifetime(last);
        if(fadeOut!=0){
            wave.setAutoFadeIntensityTime(fadeOut);
//            wave.fadeOutIntensity(fadeOut);
        } else {
            wave.setAutoFadeIntensityTime(99);
        }
        if(growthTime!=0){
            wave.fadeInSize(growthTime);
        }
        if(shrinkTime!=0){
            wave.setAutoFadeSizeTime(shrinkTime);
//            wave.fadeOutSize(shrinkTime);
        } else {
            wave.setAutoFadeSizeTime(99);
        }
        DistortionShader.addDistortion(wave);
    }

    public static void CustomRippleDistortion (Vector2f loc, Vector2f vel, float size, float intensity, boolean flip, float angle, float arc, float edgeSmooth, float fadeIn, float last, float fadeOut, float growthTime, float shrinkTime){
                
        RippleDistortion ripple = new RippleDistortion(loc, vel);

        ripple.setIntensity(intensity);
        ripple.setSize(size);
        ripple.setArc(angle-arc/2,angle+arc/2);
        if(edgeSmooth!=0){
            ripple.setArcAttenuationWidth(edgeSmooth);            
        }
        ripple.flip(flip);
        if(fadeIn!=0){
            ripple.fadeInIntensity(fadeIn);
        }
        ripple.setLifetime(last);
        if(fadeOut!=0){
            ripple.setAutoFadeIntensityTime(fadeOut);
        }
        if(growthTime!=0){
            ripple.fadeInSize(growthTime);
        }
        if(shrinkTime!=0){
            ripple.setAutoFadeSizeTime(shrinkTime);
        } 
        ripple.setFrameRate(60);
        DistortionShader.addDistortion(ripple);
        
    }

        public static void CustomStaticRippleDistortion (Vector2f loc, Vector2f vel, float size, float intensity, boolean flip, float angle, float arc, float edgeSmooth, float fadeIn, float last, float fadeOut, float growthTime, float shrinkTime, boolean wide){
                
        RippleDistortion ripple = new RippleDistortion(loc, vel);

        ripple.setFrameRate(0);
        if(wide){
            ripple.setCurrentFrame(10);
        } else {
            ripple.setCurrentFrame(40);
        }
        
        ripple.setIntensity(intensity);
        ripple.setSize(size);
        ripple.setArc(angle-arc/2,angle+arc/2);
        if(edgeSmooth!=0){
            ripple.setArcAttenuationWidth(edgeSmooth);            
        }
        ripple.flip(flip);
        if(fadeIn!=0){
            ripple.fadeInIntensity(fadeIn);
        }
        ripple.setLifetime(last);
        if(fadeOut!=0){
            ripple.setAutoFadeIntensityTime(fadeOut);
        }
        if(growthTime!=0){
            ripple.fadeInSize(growthTime);
        }
        if(shrinkTime!=0){
            ripple.setAutoFadeSizeTime(shrinkTime);
        } 
        DistortionShader.addDistortion(ripple);
        
    }
}