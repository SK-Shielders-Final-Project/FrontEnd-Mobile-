package com.mobility.hack;

import android.app.Application;
import android.util.Log;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.TokenManager;
import retrofit2.Retrofit;

public class MainApplication extends Application {
    private static final String TAG = "CRASH_CHECK";
    private static MainApplication instance;
    private static TokenManager tokenManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "MainApplication: onCreate Started"); // 프로세스 시작 로그
        instance = this;
        
        try {
            // TokenManager 초기화 중 에러가 나도 프로세스가 죽지 않게 보호
            tokenManager = new TokenManager(this);
            Log.e(TAG, "MainApplication: TokenManager initialized");
        } catch (Exception e) {
            Log.e(TAG, "MainApplication: TokenManager initialization FAILED: " + e.getMessage());
        }
    }

    public static MainApplication getInstance() {
        return instance;
    }

    public static TokenManager getTokenManager() {
        return tokenManager;
    }

    // Retrofit 인스턴스를 안전하게 가져오는 메서드
    public static Retrofit getRetrofit() {
        try {
            return RetrofitClient.getInstance().getRetrofit();
        } catch (Exception e) {
            Log.e(TAG, "MainApplication: Retrofit getClient FAILED: " + e.getMessage());
            return null;
        }
    }
}
