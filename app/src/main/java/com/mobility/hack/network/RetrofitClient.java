package com.mobility.hack.network;

import com.mobility.hack.MainApplication;
import com.mobility.hack.security.AuthInterceptor;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // 실제 서버 IP 주소 (끝에 / 반드시 포함)
    private static final String BASE_URL = "http://43.203.51.77:8080/";
    private static Retrofit retrofit = null;
    private static RetrofitClient instance = null;

    private RetrofitClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor);

        // MainApplication에서 초기화된 전역 Context를 사용해 인터셉터 적용
        if (MainApplication.getInstance() != null) {
            httpClient.addInterceptor(new AuthInterceptor(MainApplication.getInstance()));
        }

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient.build())
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
        if (retrofit == null) return null;
        return retrofit.create(ApiService.class);
    }

    public static Retrofit getClient() {
        return getInstance().getRetrofit();
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }
}
