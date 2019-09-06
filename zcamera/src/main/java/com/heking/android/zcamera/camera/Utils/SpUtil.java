package com.heking.android.zcamera.camera.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;

public class SpUtil {
    private static final String DEF="def_sp";

    public static void keepFlashMode(Context context, String flashMode){
        SharedPreferences sp = context.getSharedPreferences(DEF, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putString("flashMode", flashMode);
        edit.apply();
    }

    public static String getFlashMode(Context context ){
        SharedPreferences sp = context.getSharedPreferences(DEF, Context.MODE_PRIVATE);
        return sp.getString("flashMode", null);
    }

    public static void keepCameraFacing(Context context, int cameraFacing){
        SharedPreferences sp = context.getSharedPreferences(DEF, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putInt("cameraFacing", cameraFacing);
        edit.apply();
    }

    public static int getCameraFacing(Context context ){
        SharedPreferences sp = context.getSharedPreferences(DEF, Context.MODE_PRIVATE);
        return sp.getInt("cameraFacing",  Camera.CameraInfo.CAMERA_FACING_BACK);
    }


}
