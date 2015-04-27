package com.openmindnetworks.golgichat.utils;

import android.util.Log;

/**
 * Created by derekdoherty on 13/01/2015.
 */
public class DBG {
    public static void write(String where, String str){
        Log.i(where, str);
    }

    public static void write(String str){
        write("GolgiChat", str);
    }

}
