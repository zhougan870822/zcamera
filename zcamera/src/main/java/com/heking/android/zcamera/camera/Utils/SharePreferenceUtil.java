package com.heking.android.zcamera.camera.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 描述：
 * zhougan
 * 2019/3/24
 **/
public class SharePreferenceUtil {

    public static SharedPreferences getSharedPreferences(Context context,String name){
        if(TextUtils.isEmpty(name)){
            return PreferenceManager.getDefaultSharedPreferences(context);
        }else{
            return context.getSharedPreferences(name,Context.MODE_PRIVATE);
        }
    }

    public static <T> void put(SharedPreferences sp,String key,T value){
        SharedPreferences.Editor edit = sp.edit();
        if(value instanceof Integer){
            edit.putInt(key, (Integer) value);
        }else if(value instanceof String){
            edit.putString(key, (String) value);
        }else if(value instanceof Boolean){
            edit.putBoolean(key, (Boolean) value);
        }else if(value instanceof Float){
            edit.putFloat(key, (Float) value);
        }else if(value instanceof Long){
            edit.putLong(key, (Long) value);
        }else if(value instanceof Set){
            edit.putStringSet(key, (Set<String>) value);
        }
        edit.apply();
    }

    public static <T> void put(SharedPreferences sp, Map<String,T> map){
        SharedPreferences.Editor edit = sp.edit();

        Set<Map.Entry<String, T>> entries = map.entrySet();
        Iterator<Map.Entry<String, T>> iterator = entries.iterator();
        while (iterator.hasNext()){
            Map.Entry<String, T> next = iterator.next();
            T value = next.getValue();
            if(value instanceof Integer){
                edit.putInt(next.getKey(), (Integer) value);
            }else if(value instanceof String){
                edit.putString(next.getKey(), (String) value);
            }else if(value instanceof Boolean){
                edit.putBoolean(next.getKey(), (Boolean) value);
            }else if(value instanceof Float){
                edit.putFloat(next.getKey(), (Float) value);
            }else if(value instanceof Long){
                edit.putLong(next.getKey(), (Long) value);
            }else if(value instanceof Set){
                edit.putStringSet(next.getKey(), (Set<String>) value);
            }
        }
        edit.apply();
    }

}
