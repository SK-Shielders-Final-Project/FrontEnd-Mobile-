package com.mobility.hack;

import android.app.Application;

import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.TokenManager;

public class MainApplication extends Application {

    private TokenManager tokenManager;
    private ApiService apiService;

    @Override
    public void onCreate() {
        super.onCreate();
        tokenManager = new TokenManager(this);
        apiService = RetrofitClient.getApiService(tokenManager);
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }

    public ApiService getApiService() {
        return apiService;
    }
}
