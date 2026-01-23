package com.mobility.hack;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.auth.LoginActivity;
import com.mobility.hack.security.SecurityEngine;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 보안 엔진 초기화 및 체크
        SecurityEngine engine = new SecurityEngine();
        engine.initAntiDebug();
        
        // 로그인 화면으로 이동
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}