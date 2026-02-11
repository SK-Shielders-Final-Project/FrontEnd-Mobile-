package com.mobility.hack.network;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.mobility.hack.BuildConfig;
import com.mobility.hack.security.AuthInterceptor;
import com.mobility.hack.security.TokenManager;
import com.mobility.hack.security.SslGuard; // [중요]

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context, TokenManager tokenManager) {
        if (retrofit == null) {

            Log.e("RetrofitClient", "🚀 [1] Retrofit 생성 시작...");

            // 1. 기본 설정
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

            String userAgent = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36";

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .cookieJar(new JavaNetCookieJar(cookieManager))
                    .connectTimeout(30, TimeUnit.SECONDS)
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
                    .addInterceptor(loggingInterceptor);

            // =========================================================
            // [핵심] SslGuard 강제 연결 (여기서 실패하면 앱 죽임)
            // =========================================================
            try {
                Log.e("RetrofitClient", "🔐 [2] SSL Pinning 적용 시도...");

                SslGuard sslGuard = new SslGuard();
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{sslGuard}, new SecureRandom());

                // ▼ 이 부분이 검문소 설치하는 코드입니다.
                clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), sslGuard);

                Log.e("RetrofitClient", "✅ [3] SSL Pinning 적용 성공! (이제 Burp 켜면 막힙니다)");

            } catch (Exception e) {
                Log.e("RetrofitClient", "🚨 [FATAL] SSL 설정 실패! 앱을 종료합니다.", e);
                // 설정 실패하면 그냥 앱을 죽여서라도 알려줌
                throw new RuntimeException("SSL Pinning 설정 실패", e);
            }
            // =========================================================

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(clientBuilder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }

    public static ApiService getApiService(Context context, TokenManager tokenManager) {
        return getClient(context, tokenManager).create(ApiService.class);
    }
}