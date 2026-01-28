package com.mobility.hack.network;

import com.example.mobilityhack.network.ApiService; // import 경로 변경
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.content.Context;
import com.mobility.hack.security.AuthInterceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit = null;
    // 안드로이드 에뮬레이터에서 localhost에 접근하려면 10.0.2.2를 사용합니다.
    // 실제 서버 배포 시에는 해당 서버의 도메인이나 IP 주소로 변경해야 합니다.
    private static final String BASE_URL = "http://10.0.2.2:8080";

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static com.example.mobilityhack.network.ApiService getApiService() {
        return getClient().create(com.example.mobilityhack.network.ApiService.class);
    }
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
