package com.mobility.hack;
import android.util.Log;
import com.mobility.hack.security.SecurityBridge;
import com.mobility.hack.security.SecurityEngine;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.auth.LoginActivity;
import com.mobility.hack.R;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // onCreate 안에 추가
        SecurityBridge sb = new SecurityBridge();
        int result = sb.detectRooting(this);

        Log.d("SecurityTest", "루팅 탐지 결과: " + Integer.toHexString(result));
        Toast.makeText(this, "보안 엔진 가동 중... 결과: " + result, Toast.LENGTH_SHORT).show();
        // 보안 엔진 초기화 및 체크
        SecurityEngine engine = new SecurityEngine();
        engine.initAntiDebug();
        
        // 로그인 화면으로 이동
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}

