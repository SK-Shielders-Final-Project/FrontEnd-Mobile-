package com.mobility.hack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.community.CustomerCenterActivity;
import androidx.appcompat.widget.Toolbar;
import com.mobility.hack.auth.CheckPasswordActivity;
import com.mobility.hack.auth.LoginActivity;
import com.mobility.hack.security.SecurityEngine;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- [보안 강화] 토큰 인증 확인 로직 추가 ---
        // MainApplication.getTokenManager()가 초기화되었는지, 그리고 저장된 토큰이 있는지 확인
        if (MainApplication.getTokenManager() == null || MainApplication.getTokenManager().fetchAuthToken() == null) {
            Toast.makeText(this, "로그인이 필요합니다. 로그인 페이지로 이동합니다.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            // 이전 액티비티 스택을 모두 지우고, 새로운 태스크를 시작합니다.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // 현재 MainActivity를 완전히 종료
            return;   // 아래의 UI 생성 코드를 실행하지 않음
        }
        // --- 로직 끝 ---

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SecurityEngine engine = new SecurityEngine();
        engine.initAntiDebug();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_my_info) {
            Intent intent = new Intent(this, CheckPasswordActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_logout) {
            MainApplication.getTokenManager().clearData();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
        setContentView(R.layout.activity_main);

        // 왼쪽 상단 메뉴 버튼 연결
        ImageButton btnMenu = findViewById(R.id.btn_menu);

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                // 클릭 시 고객센터(파일 업로드 취약점 실습지)로 이동
                Intent intent = new Intent(MainActivity.this, CustomerCenterActivity.class);
                startActivity(intent);
            });
        }
    }
}
