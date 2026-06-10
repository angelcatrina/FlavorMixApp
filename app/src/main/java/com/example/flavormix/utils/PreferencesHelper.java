package com.example.flavormix.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PreferencesHelper {

    private static final String PREFS_NAME      = "yumrecipe_prefs";
    private static final String KEY_DARK_MODE   = "dark_mode";
    private static final String KEY_HISTORY     = "search_history";
    private static final int    MAX_HISTORY     = 10;

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isDarkMode(Context context) {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkMode(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public static List<String> getSearchHistory(Context context) {
        String raw = getPrefs(context).getString(KEY_HISTORY, "");
        if (raw == null || raw.isEmpty()) return new ArrayList<>();
        List<String> list = new ArrayList<>(Arrays.asList(raw.split("\\|")));
        list.removeIf(String::isEmpty);
        return list;
    }

    public static void addSearchHistory(Context context, String query) {
        List<String> history = getSearchHistory(context);
        history.remove(query);
        history.add(0, query);
        if (history.size() > MAX_HISTORY) {
            history = history.subList(0, MAX_HISTORY);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(history.get(i));
        }
        getPrefs(context).edit().putString(KEY_HISTORY, sb.toString()).apply();
    }

    public static void clearSearchHistory(Context context) {
        getPrefs(context).edit().remove(KEY_HISTORY).apply();
    }
}
