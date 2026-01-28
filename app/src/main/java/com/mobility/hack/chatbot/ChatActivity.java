package com.mobility.hack.chatbot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobility.hack.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    // 1. 변수 선언
    private String baseUrl;
    private String chatApiUrl;

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ImageButton buttonClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 2. onCreate 내부에서 값을 할당합니다.
        baseUrl = getString(R.string.server_url); // 이제 여기서 호출 가능!
        chatApiUrl = baseUrl + "/api/chat";

        initViews();
        setupRecyclerView();
        loadWelcomeMessage();

        buttonSend.setOnClickListener(v -> sendMessage());

        // 닫기 버튼 클릭 시 MapActivity로 이동
        buttonClose.setOnClickListener(v -> {
            // 1. Intent 객체 생성 (context와 목적지 클래스 명시)
            android.content.Intent intent = new android.content.Intent(ChatActivity.this, com.mobility.hack.ride.MapActivity.class);

            // 2. 보안 및 리소스 관리를 위한 플래그 설정
            // FLAG_ACTIVITY_CLEAR_TOP: 스택에 기존 MapActivity가 있으면 그 위의 것들을 모두 제거
            // FLAG_ACTIVITY_SINGLE_TOP: 기존 MapActivity를 재사용 (새로 생성 X)
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);

            // 3. 이동 및 현재 화면 종료
            startActivity(intent);
            finish();
        });
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewChat);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonClose = findViewById(R.id.buttonClose);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);
    }

    private void loadWelcomeMessage() {
        String welcomeText = "안녕하세요,\n🌲서울을 즐기는 가장 친환경적인 방법\n서울자전거 작당모빌 🚲 입니다.\n\n이용 중 궁금한 점이나 불편 사항이 있으신가요?";
        addMessageToChat(welcomeText, ChatMessage.VIEW_TYPE_BOT);
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString();

        if (messageText.isEmpty()) {
            Toast.makeText(this, "메시지를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        addMessageToChat(messageText, ChatMessage.VIEW_TYPE_USER);
        editTextMessage.setText("");

        // 백엔드 명세 규격에 맞춘 서버 통신 시작
        requestBotResponse(messageText);
    }

    /**
     * 백엔드 팀 명세 규격 반영 (API Key 제거 버전)
     * Request: { "userId": 1, "message": "..." }
     * Response: { "assistantMessage": "..." }
     */
    /**
     * 서버에 챗봇 메시지를 요청하는 메서드
     * 인증을 위해 SharedPreferences에서 JWT 토큰을 꺼내 Header에 Bearer 토큰을 추가합니다.
     */
    private void requestBotResponse(String userMessage) {
        new Thread(() -> {
            try {
                // 1. SharedPreferences에서 JWT 토큰 가져오기
                // 저장 시 설정한 파일명("auth_prefs")과 키값("jwt_token")을 확인하세요.
                SharedPreferences sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                String token = sharedPreferences.getString("jwt_token", null);

                URL url = new URL(chatApiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                // 2. Authorization 헤더 추가 (Bearer 방식)
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                } else {
                    // 보안: 토큰이 없으면 서버에 요청하지 않고 즉시 예외 발생
                    throw new Exception("인증 토큰이 없습니다. 다시 로그인 해주세요.");
                }

                conn.setDoOutput(true);

                // 3. Request Body 생성 (JSON)
                JSONObject jsonInput = new JSONObject();
                jsonInput.put("userId", 1);
                jsonInput.put("message", userMessage);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // 4. 서버 응답 처리
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 정상 응답 (200 OK)
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                    }
                    br.close();

                    // JSON 파싱
                    JSONObject jsonObj = new JSONObject(response.toString());
                    String botResponse = jsonObj.getString("assistantMessage");

                    // UI 업데이트
                    new Handler(Looper.getMainLooper()).post(() -> {
                        addMessageToChat(botResponse, ChatMessage.VIEW_TYPE_BOT);
                    });

                } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // 인증 실패 (401 Unauthorized)
                    throw new Exception("세션이 만료되었습니다. 다시 로그인 하세요.");
                } else {
                    // 그 외 에러 (500, 404 등)
                    throw new Exception("서버 에러 발생 (Code: " + responseCode + ")");
                }

            } catch (Exception e) {
                // 5. 모든 예외 상황(네트워크 에러, 토큰 없음, 401 등)을 UI에 표시
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    addMessageToChat("오류: " + e.getMessage(), ChatMessage.VIEW_TYPE_BOT);
                });
            }
        }).start();
    }
    private void addMessageToChat(String message, int viewType) {
        String currentTime = new SimpleDateFormat("a h:mm", Locale.KOREA).format(new Date());
        ChatMessage chatMessage = new ChatMessage(message, viewType, currentTime);

        chatAdapter.addMessage(chatMessage);
        recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }
}