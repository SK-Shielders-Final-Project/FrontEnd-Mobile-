package com.mobility.hack.security;

import android.content.Context;
import androidx.annotation.NonNull;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.LoginResponse;
import com.mobility.hack.network.RetrofitClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final TokenManager tokenManager;

    public AuthInterceptor(Context context) {
        this.tokenManager = new TokenManager(context);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder builder = originalRequest.newBuilder();

        // 1. 모든 요청에 accessToken 자동 추가
        String accessToken = tokenManager.fetchAuthToken();
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }

        Response response = chain.proceed(builder.build());

        // 2. 401 Unauthorized 발생 시 리프레시 로직 실행
        if (response.code() == 401) {
            synchronized (this) {
                String refreshToken = tokenManager.fetchRefreshToken();
                if (refreshToken != null) {
                    // 동기 방식으로 토큰 갱신 시도
                    String newAccessToken = handleTokenRefresh(refreshToken);
                    if (newAccessToken != null) {
                        response.close();
                        // 새 토큰으로 기존 요청 재시도
                        return chain.proceed(originalRequest.newBuilder()
                                .header("Authorization", "Bearer " + newAccessToken)
                                .build());
                    }
                }
            }
        }
        return response;
    }

    private String handleTokenRefresh(String refreshToken) throws IOException {
        // 별도의 Retrofit 인스턴스나 ApiService를 사용하여 갱신 요청
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", refreshToken);

        retrofit2.Response<LoginResponse> refreshResponse = apiService.refreshToken(body).execute();
        if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
            String newAccess = refreshResponse.body().getAccessToken();
            String newRefresh = refreshResponse.body().getRefreshToken();
            tokenManager.saveTokens(newAccess, newRefresh);
            return newAccess;
        }
        return null;
    }
}
