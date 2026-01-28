package com.mobility.hack.auth;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

public class MenuActivity extends AppCompatActivity {

    // 1. 변수 선언
    private String baseUrl;
    private String userInfoUrl;

    private TextView textUserPoint;
    private TextView textUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // 2. onCreate 내부에서 strings.xml의 주소를 가져와 조합합니다.
        String baseUrl = getString(R.string.server_url);
        userInfoUrl = baseUrl + "/api/user/info";

        textUserPoint = findViewById(R.id.textUserPoint);
        textUsername = findViewById(R.id.textUsername);

        ImageButton btnClose = findViewById(R.id.btnClose);
        TextView btnLogout = findViewById(R.id.btnLogout);

        btnClose.setOnClickListener(v -> finish());
        btnLogout.setOnClickListener(v -> performLogout());

        // 서버에서 정보 가져오기 시작
        fetchUserInfo();
    }

    /**
     * HttpURLConnection을 사용하여 직접 서버와 통신하고 JSON을 파싱합니다.
     */
    private void fetchUserInfo() {
        // 네트워크 작업은 메인 스레드에서 할 수 없으므로 새 스레드 생성
        new Thread(() -> {
            try {
                // 1. SharedPreferences에서 저장된 JWT 토큰 읽기
                SharedPreferences sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                String token = sharedPreferences.getString("jwt_token", null);

                // 서버 연결 설정
                URL url = new URL(userInfoUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);

                // 2. 헤더에 Bearer 토큰 추가
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                } else {
                    // 토큰이 없으면 정보 조회가 불가능하므로 예외 처리
                    throw new Exception("인증 정보가 없습니다.");
                }

                //

                // 3. 서버 응답 확인
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    final String name = jsonResponse.getString("username");
                    final int point = jsonResponse.getInt("total_point");

                    new Handler(Looper.getMainLooper()).post(() -> {
                        updateUserInterface(name, point);
                    });
                } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // 401 에러 시 인증 실패 처리
                    throw new Exception("인증이 만료되었습니다.");
                }

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(MenuActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void updateUserInterface(String name, int point) {
        if (name != null) {
            textUsername.setText(name + " >");
        }
        DecimalFormat formatter = new DecimalFormat("###,###");
        String formattedPoint = formatter.format(point);
        textUserPoint.setText("현재 이용권 잔액: " + formattedPoint + "원");
    }

    private void performLogout() {
        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
