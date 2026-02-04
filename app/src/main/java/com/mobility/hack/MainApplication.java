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

    static {
        System.loadLibrary("mobile");
    }

    private TokenManager tokenManager;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    "secret_shared_prefs",
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
        // RetrofitClient.getApiService가 Context를 요구하므로 'this' (Application Context)를 전달
        return RetrofitClient.getApiService(this, tokenManager);
    }
}
