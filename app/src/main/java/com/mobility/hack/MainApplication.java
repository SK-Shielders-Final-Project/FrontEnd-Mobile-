package com.mobility.hack;

import android.app.Application;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.util.TokenManager;

public class MainApplication extends Application {

    private TokenManager tokenManager;
    private ApiService apiService;

    @Override
    public void onCreate() {
        super.onCreate();
        // 1. 앱에서 유일한 TokenManager 인스턴스를 생성합니다.
        tokenManager = new TokenManager(this);
        // 2. 생성된 TokenManager를 사용하여 ApiService 인스턴스를 생성합니다.
        apiService = RetrofitClient.getApiService(tokenManager);
    }

    // 3. 다른 클래스에서 인스턴스를 가져갈 수 있도록 getter를 제공합니다.
    public TokenManager getTokenManager() {
        return tokenManager;
    }

    public ApiService getApiService() {
        return apiService;
    }
}
