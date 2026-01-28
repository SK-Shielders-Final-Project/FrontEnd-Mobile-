package com.mobility.hack;

import android.app.Application;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.TokenManager;
import retrofit2.Retrofit;

public class MainApplication extends Application {

    private static MainApplication instance;
    private static Retrofit retrofit;
    private static TokenManager tokenManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        retrofit = RetrofitClient.getClient(this);
        tokenManager = new TokenManager(this);
    }

    public static MainApplication getInstance() {
        return instance;
    }

    public static Retrofit getRetrofit() {
        return retrofit;
    }

    public static TokenManager getTokenManager() {
        return tokenManager;
    }
}
