package com.example.slotsmart;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "slotsmart_prefs";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_SERVER_URL = "server_url";
    public static final String DEFAULT_URL = "http://10.0.2.2/Slotsmart-1/";
    public static final String ADMIN_EMAIL = "admin@slotsmart.com";
    public static final String ADMIN_PASSWORD = "admin123";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setLoggedIn(boolean value) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public void setServerUrl(String url) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply();
    }

    public String getServerUrl() {
        String url = prefs.getString(KEY_SERVER_URL, DEFAULT_URL);
        if (!url.endsWith("/")) url += "/";
        return url;
    }

    public void logout(Context context) {
        prefs.edit().clear().apply();
        android.content.Intent intent = new android.content.Intent(context, LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
