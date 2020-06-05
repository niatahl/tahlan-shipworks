package data.scripts.utils;

import com.fs.starfarer.api.Global;

public class tahlan_txt {
    private static final String tahlan="tahlan";
    public static String txt(String id){
        return Global.getSettings().getString(tahlan, id);
    }    
}