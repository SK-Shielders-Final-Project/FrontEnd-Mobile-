package com.mobility.hack.auth;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.MainApplication;
import com.mobility.hack.network.ApiService;

/**
 * 모든 액티비티의 부모 클래스
 * 공통 비즈니스 로직(API 서비스 초기화 등)을 한 곳에서 관리
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected ApiService apiService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // [효율성] 자식 액티비티들이 초기화할 필요 없이 여기서 일괄 생성
        if (MainApplication.getRetrofit() != null) {
            apiService = MainApplication.getRetrofit().create(ApiService.class);
        }
    }
}
