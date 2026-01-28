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
    private static OkHttpClient okHttpClient = null;
    private static final String BASE_URL = "http://43.203.51.77:8080";

    public static Retrofit getClient(Context context) {
        if (okHttpClient == null) {
            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
            // Use application context to avoid memory leaks
            httpClientBuilder.addInterceptor(new AuthInterceptor(context.getApplicationContext()));
            okHttpClient = httpClientBuilder.build();
        }

        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
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
            String token = prefs.getString("jwt_token", null);
            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer " + token);
            }
            return chain.proceed(requestBuilder.build());
        }
    }
}
