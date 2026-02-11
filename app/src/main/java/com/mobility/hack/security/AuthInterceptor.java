// AuthInterceptor.java
package com.mobility.hack.security;

import androidx.annotation.NonNull;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final TokenManager tokenManager;

    public AuthInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder requestBuilder = originalRequest.newBuilder();

        // ===== 1. Authorization 헤더 (기존 로직) =====
        String token = tokenManager.fetchAuthToken();
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        // ===== 2. X-Device-Id 헤더 (모든 요청에 추가) =====
        String deviceId = tokenManager.getOrCreateDeviceId();
        requestBuilder.header("X-Device-Id", deviceId);

        // ===== 3. X-Integrity-Token 헤더 (로그인/리프레시 시에만) =====
        String url = originalRequest.url().toString();

        // 로그인 API에만 Integrity Token 추가
        if (url.contains("/api/user/auth/login")) {
            String integrityToken = tokenManager.getIntegrityToken();
            if (integrityToken != null) {
                requestBuilder.header("X-Integrity-Token", integrityToken);
            }
        }

        Request newRequest = requestBuilder.build();
        return chain.proceed(newRequest);
    }
}