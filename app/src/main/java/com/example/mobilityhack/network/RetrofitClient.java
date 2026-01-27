package com.example.mobilityhack.network;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mobilityhack.util.Constants;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit = null;
    // 안드로이드 에뮬레이터에서 localhost에 접근하려면 10.0.2.2를 사용합니다.
    // 실제 서버 배포 시에는 해당 서버의 도메인이나 IP 주소로 변경해야 합니다.
    private static final String BASE_URL = "http://10.0.2.2:8080";

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(new AuthInterceptor(context));
            OkHttpClient client = httpClient.build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService(Context context) {
        return getClient(context).create(ApiService.class);
    }

    private static class AuthInterceptor implements Interceptor {
        private Context context;

        public AuthInterceptor(Context context) {
            this.context = context;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request.Builder requestBuilder = chain.request().newBuilder();
            SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            String token = prefs.getString("jwt_token", null); // "jwt_token" should be a constant
            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer " + token);
            }
            return chain.proceed(requestBuilder.build());
        }
    }
}
