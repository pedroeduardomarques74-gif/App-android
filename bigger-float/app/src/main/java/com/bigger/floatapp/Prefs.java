package com.bigger.floatapp;
import android.content.Context;
import android.content.SharedPreferences;
public final class Prefs {
    private static final String NAME = "bigger_float_prefs";
    public static SharedPreferences get(Context c){ return c.getSharedPreferences(NAME, Context.MODE_PRIVATE); }
}
