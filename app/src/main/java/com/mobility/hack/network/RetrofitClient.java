package com.mobility.hack.network;

import com.mobility.hack.security.AuthInterceptor;
import com.mobility.hack.security.TokenManager;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "http://43.203.51.77:8080";

    public static Retrofit getClient(TokenManager tokenManager) {
        // 로깅 인터셉터 추가
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(tokenManager))
                .addInterceptor(loggingInterceptor) // 로깅 인터셉터 등록
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static ApiService getApiService(TokenManager tokenManager) {
        return getClient(tokenManager).create(ApiService.class);
    }
}
