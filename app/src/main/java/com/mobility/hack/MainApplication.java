package com.mobility.hack;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.TokenManager;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class MainApplication extends Application {

    private static Context appContext;

    static {
        System.loadLibrary("mobile");
    }

    private TokenManager tokenManager;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    "AuthPrefs", // SharedPreferences 이름 변경
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            tokenManager = new TokenManager(sharedPreferences);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Could not create EncryptedSharedPreferences", e);
        }
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }

    public ApiService getApiService() {
        return RetrofitClient.getApiService(this, tokenManager);
    }

    public static Context getAppContext() {
        return appContext;
    }
}
