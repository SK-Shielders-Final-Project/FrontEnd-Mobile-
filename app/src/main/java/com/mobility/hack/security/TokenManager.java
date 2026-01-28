package com.mobility.hack.security;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {

    private static final String PREFS_NAME = "secure_auth_prefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";

    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveAuthToken(String token) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_JWT_TOKEN, token);
        // [수정] 비동기 apply() 대신 동기 commit()을 사용하여 즉시 저장되도록 보장
        editor.commit();
    }

    public void saveUserId(long userId) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_USER_ID, userId);
        // [수정] 비동기 apply() 대신 동기 commit()을 사용하여 즉시 저장되도록 보장
        editor.commit();
    }

    public String fetchAuthToken() {
        return prefs.getString(KEY_JWT_TOKEN, null);
    }

    public long fetchUserId() {
        return prefs.getLong(KEY_USER_ID, 0L);
    }

    public void clearData() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        // [수정] 비동기 apply() 대신 동기 commit()을 사용하여 즉시 삭제되도록 보장
        editor.commit();
    }
}
