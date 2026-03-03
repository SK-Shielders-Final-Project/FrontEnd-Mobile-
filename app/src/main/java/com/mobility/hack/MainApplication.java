package com.mobility.hack;

import android.app.Application;
import android.content.Context;

import com.mobility.hack.network.ApiService;
import com.mobility.hack.security.TokenManager;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MainApplication extends Application {

    private static Context appContext;

    // Hilt가 생성해준 싱글톤 인스턴스를 주입받습니다.
    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    static {
        System.loadLibrary("mobile");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
    }

    // 기존 수동 호출 코드들과의 호환성을 유지합니다.
    public ApiService getApiService() {
        return apiService;
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }

    public static Context getAppContext() {
        return appContext;
    }
}
