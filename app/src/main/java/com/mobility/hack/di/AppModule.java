package com.mobility.hack.di;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.TokenManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            return EncryptedSharedPreferences.create(
                    "AuthPrefs",
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e("AppModule", "Failed to create EncryptedSharedPreferences, falling back to normal", e);
            return context.getSharedPreferences("AuthPrefs_Fallback", Context.MODE_PRIVATE);
        }
    }

    @Provides
    @Singleton
    public TokenManager provideTokenManager(SharedPreferences sharedPreferences) {
        return new TokenManager(sharedPreferences);
    }

    @Provides
    @Singleton
    public ApiService provideApiService(@ApplicationContext Context context, TokenManager tokenManager) {
        return RetrofitClient.getApiService(context, tokenManager);
    }
}
