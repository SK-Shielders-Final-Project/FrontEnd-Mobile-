package com.mobility.hack.network;

import android.content.Context;
import com.mobility.hack.security.AuthInterceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // [수정] 개발용 로컬 주소에서 실제 서버 주소(Public IP)로 변경
    private static final String BASE_URL = "http://43.203.51.77:8080";
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // AuthInterceptor를 사용하는 OkHttpClient 생성
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(context))
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
