package com.mobility.hack.chatbot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.ChatRequest;
import com.mobility.hack.network.ChatResponse;
import com.mobility.hack.security.TokenManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ImageButton buttonClose;

    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // MainApplication에서 ApiService 및 TokenManager 인스턴스 가져오기
        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        initViews();
        setupRecyclerView();
        loadWelcomeMessage();

        buttonSend.setOnClickListener(v -> sendMessage());
        buttonClose.setOnClickListener(v -> finish());
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
        String welcomeText = "안녕하세요,\n🌲복잡한 도심 속, 원하는 곳 어디든 자유롭게 이동하세요.\n스마트한 자전거 공유 서비스 작당모빌 🚲 입니다.";
        addMessageToChat(welcomeText, ChatMessage.VIEW_TYPE_BOT);
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString();
        if (messageText.isEmpty()) return;

        addMessageToChat(messageText, ChatMessage.VIEW_TYPE_USER);
        editTextMessage.setText("");

        // Retrofit을 사용한 서버 통신
        requestBotResponse(messageText);
    }

    private void requestBotResponse(String userMessage) {
        long userId = tokenManager.fetchUserId();

        ChatRequest request = new ChatRequest(userId, userMessage);

        apiService.sendChatMessage(request).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                // 화면이 이미 닫혔다면 UI 업데이트 중단 (안정성)
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    addMessageToChat(response.body().getAssistantMessage(), ChatMessage.VIEW_TYPE_BOT);
                } else {
                    String error = (response.code() == 401) ? "로그인이 필요합니다." : "서버 오류";
                    addMessageToChat(error, ChatMessage.VIEW_TYPE_BOT);
                }
            }

            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                addMessageToChat("연결 실패: " + t.getMessage(), ChatMessage.VIEW_TYPE_BOT);
            }
        });
    }

    private void addMessageToChat(String message, int viewType) {
        String currentTime = new SimpleDateFormat("a h:mm", Locale.KOREA).format(new Date());
        ChatMessage chatMsg = new ChatMessage(message, viewType, currentTime);
        chatAdapter.addMessage(chatMsg);
        recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }
}
