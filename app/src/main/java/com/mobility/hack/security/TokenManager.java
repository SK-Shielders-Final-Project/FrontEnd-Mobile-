package com.mobility.hack.security;

import android.content.SharedPreferences;

public class TokenManager {

    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_AUTO_LOGIN = "auto_login";


    private final SharedPreferences prefs;

    public TokenManager(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void saveAuthToken(String token) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_JWT_TOKEN, token);
        editor.apply();
    }

    public void saveRefreshToken(String token) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_REFRESH_TOKEN, token);
        editor.apply();
    }

    public void saveUserId(long userId) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_USER_ID, userId);
        editor.apply();
    }

    public void saveAutoLogin(boolean enabled) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_AUTO_LOGIN, enabled);
        editor.apply();
    }

    public String fetchAuthToken() {
        return prefs.getString(KEY_JWT_TOKEN, null);
    }

    public String fetchRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public long fetchUserId() {
        return prefs.getLong(KEY_USER_ID, 0L);
    }

    public boolean isAutoLoginEnabled() {
        return prefs.getBoolean(KEY_AUTO_LOGIN, false);
    }

    public void clearData() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}
