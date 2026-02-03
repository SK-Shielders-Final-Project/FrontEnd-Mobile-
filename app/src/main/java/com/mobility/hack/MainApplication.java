package com.mobility.hack;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.TokenManager;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class MainApplication extends Application {

    private TokenManager tokenManager;
    // private ApiService apiService; // ApiService 인스턴스 캐싱 제거

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

        // 앱 시작 시 ApiService를 미리 생성하지 않음
        // apiService = RetrofitClient.getApiService(tokenManager);
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }

    /**
     * ApiService 인스턴스를 요청할 때마다 새로 생성하여 반환합니다.
     * 이렇게 하면 AuthInterceptor가 항상 최신 토큰을 사용하여 API를 요청하게 됩니다.
     * @return 최신 인증 정보가 적용된 ApiService 인스턴스
     */
    public ApiService getApiService() {
        return RetrofitClient.getApiService(tokenManager);
    }
}
