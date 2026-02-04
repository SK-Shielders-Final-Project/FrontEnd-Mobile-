package com.mobility.hack.security;

import android.content.SharedPreferences;

public class TokenManager {

    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_AUTO_LOGIN = "auto_login";
    private static final String KEY_WEAK_KEY = "weak_key"; // 암호화 키 저장을 위한 키


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

    // 암호화 키 저장
    public void saveWeakKey(String key) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_WEAK_KEY, key);
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

    // 암호화 키 불러오기
    public String getWeakKey() {
        return prefs.getString(KEY_WEAK_KEY, null);
    }

    public void clearData() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}
