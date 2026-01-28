package com.mobility.hack.network;

import com.mobility.hack.security.AuthInterceptor;
import com.mobility.hack.util.TokenManager;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static volatile Retrofit retrofit = null;
    private static final String BASE_URL = "http://43.203.51.77:8080";

    public static Retrofit getClient(TokenManager tokenManager) {
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(new AuthInterceptor(tokenManager))
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofit;
    }

    public static ApiService getApiService(TokenManager tokenManager) {
        return getClient(tokenManager).create(ApiService.class);
    }
}
