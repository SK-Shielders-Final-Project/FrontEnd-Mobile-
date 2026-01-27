package com.mobility.hack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.community.CustomerCenterActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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