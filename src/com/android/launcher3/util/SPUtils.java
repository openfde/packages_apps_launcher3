package com.android.launcher3.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.List;
import java.util.stream.Collectors;


public class SPUtils {
    private static final String USER_INFO = "user_info";
    private static final String TAG = "SPUtils";


    public static String getUserInfo(Context context, String key) {
        SharedPreferences shared_user_info = context.getSharedPreferences(USER_INFO, context.MODE_PRIVATE);
        return shared_user_info.getString(key, "");
    }

    public static void putUserInfo(Context context, String key, String values) {
        SharedPreferences shared_user_info = context.getSharedPreferences(USER_INFO, context.MODE_PRIVATE);
        shared_user_info.edit().putString(key, values).commit();
    }

    public static int getIntUserInfo(Context context, String key) {
        SharedPreferences shared_user_info = context.getSharedPreferences(USER_INFO, context.MODE_PRIVATE);
        return shared_user_info.getInt(key, 0);
    }

    public static void putIntUserInfo(Context context, String key, int values) {
        SharedPreferences shared_user_info = context.getSharedPreferences(USER_INFO, context.MODE_PRIVATE);
        shared_user_info.edit().putInt(key, values).commit();
    }

    public static void cleanUserInfo(Context context) {
        SharedPreferences shared_user_info = context.getSharedPreferences(USER_INFO, context.MODE_PRIVATE);
        shared_user_info.edit().clear().commit();
    }
}
