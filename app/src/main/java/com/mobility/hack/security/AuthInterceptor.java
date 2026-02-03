package com.mobility.hack.security;

import androidx.annotation.NonNull;
import com.mobility.hack.security.TokenManager;
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

        String token = tokenManager.fetchAuthToken();
        if (token != null) {
            // .header()를 사용하여 기존 헤더를 덮어쓰고, 없으면 추가합니다.
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        Request newRequest = requestBuilder.build();
        return chain.proceed(newRequest);
    }
}
