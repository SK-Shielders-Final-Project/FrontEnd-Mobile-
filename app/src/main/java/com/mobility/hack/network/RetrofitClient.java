package com.mobility.hack.network;

import android.content.Context;
import android.os.Build;
import com.mobility.hack.security.AuthInterceptor;
import com.mobility.hack.security.TokenManager;

import java.net.CookieManager;
import java.net.CookiePolicy;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // [명세 반영] 백엔드 서버 IP 주소 고정
    private static final String BASE_URL = "http://43.203.51.77:8080/";

    public static Retrofit getClient(Context context, TokenManager tokenManager) {
        // 로깅 인터셉터 추가
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // 쿠키 관리를 위한 CookieManager 설정
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        // User-Agent 문자열 동적 생성
        String userAgent = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36";

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieManager)) // ⭐️ 쿠키 핸들러 추가
                .addInterceptor(new AuthInterceptor(tokenManager))
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("User-Agent", userAgent)
                            .header("Origin", "http://localhost:8080"); // ⭐️ Origin 헤더 추가
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                })
                .addInterceptor(loggingInterceptor) // 로깅 인터셉터 등록
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static ApiService getApiService(Context context, TokenManager tokenManager) {
        return getClient(context, tokenManager).create(ApiService.class);
    }
}
