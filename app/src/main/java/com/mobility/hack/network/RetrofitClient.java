package com.mobility.hack.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    public static final String BASE_URL = "http://insecure-api.mobilityhack.com/";
    private static RetrofitClient instance = null;
    private static Retrofit retrofit = null;

    private RetrofitClient() {
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public ApiService getApiService() {
        return retrofit.create(ApiService.class);
    }

    // 하위 호환성을 위해 유지 (필요 시)
    public static Retrofit getClient() {
        return getInstance().getRetrofit();
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }
}