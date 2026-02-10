package com.mobility.hack.network;

import android.content.Context;
import android.os.Build;
import com.mobility.hack.security.AuthInterceptor;
import com.mobility.hack.security.TokenManager;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;
import com.mobility.hack.BuildConfig;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = BuildConfig.BASE_URL;

    // Retrofit 인스턴스를 저장할 static 변수 (싱글턴 구현)
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context, TokenManager tokenManager) {
        // 이미 만들어진 인스턴스가 있으면 재사용
        if (retrofit == null) {
            // 1. 로깅 인터셉터
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 2. 쿠키 매니저 (앱이 켜져 있는 동안 쿠키 유지)
            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

            // 3. User-Agent
            String userAgent = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36";

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .cookieJar(new JavaNetCookieJar(cookieManager)) // 쿠키 핸들러 추가
                    .connectTimeout(30, TimeUnit.SECONDS) // 타임아웃 설정
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new AuthInterceptor(tokenManager))
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder requestBuilder = original.newBuilder()
                                .header("User-Agent", userAgent)
                                .header("Origin", "http://localhost:8080");
                        Request request = requestBuilder.build();
                        return chain.proceed(request);
                    })
                    .addInterceptor(loggingInterceptor)
                    .build();

            // Retrofit 객체를 생성하여 static 변수에 저장
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }

    public static ApiService getApiService(Context context, TokenManager tokenManager) {
        return getClient(context, tokenManager).create(ApiService.class);
    }
}
