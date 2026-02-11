// TokenManager.java
package com.mobility.hack.security;

import android.content.SharedPreferences;
import java.util.UUID;

public class TokenManager {
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_AUTO_LOGIN = "auto_login";
    private static final String KEY_WEAK_KEY = "weak_key";

    // ===== 추가 =====
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_INTEGRITY_TOKEN = "integrity_token";

    private final SharedPreferences prefs;

    public TokenManager(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    // 기존 메서드들...
    public void saveAuthToken(String token) {
        prefs.edit().putString(KEY_JWT_TOKEN, token).apply();
    }

    public void saveRefreshToken(String token) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply();
    }

    public void saveUserId(long userId) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply();
    }

    public void saveAutoLogin(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_LOGIN, enabled).apply();
    }

    public void saveWeakKey(String key) {
        prefs.edit().putString(KEY_WEAK_KEY, key).apply();
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

    public String getWeakKey() {
        return prefs.getString(KEY_WEAK_KEY, null);
    }

    public void clearData() {
        prefs.edit().clear().apply();
    }

    // ===== 추가 메서드 =====

    /**
     * Device ID 가져오기 (없으면 생성)
     */
    public String getOrCreateDeviceId() {
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
        }
        return deviceId;
    }

    /**
     * Integrity Token 저장
     */
    public void saveIntegrityToken(String token) {
        prefs.edit().putString(KEY_INTEGRITY_TOKEN, token).apply();
    }

    /**
     * Integrity Token 가져오기
     */
    public String getIntegrityToken() {
        return prefs.getString(KEY_INTEGRITY_TOKEN, null);
    }

    /**
     * Integrity Token 삭제 (1회용)
     */
    public void clearIntegrityToken() {
        prefs.edit().remove(KEY_INTEGRITY_TOKEN).apply();
    }
}