package com.mobility.hack.chatbot;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.ChatRequest;
import com.mobility.hack.network.ChatResponse;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.TokenManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ChipGroup chipGroup;
    private ImageButton buttonClose;

    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.recyclerViewChat);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        chipGroup = findViewById(R.id.layoutQuick);
        buttonClose = findViewById(R.id.buttonClose);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);

        tokenManager = new TokenManager(this);
        apiService = RetrofitClient.getClient(tokenManager).create(ApiService.class);

        addWelcomeMessage();

        buttonSend.setOnClickListener(v -> sendMessage());
        buttonClose.setOnClickListener(v -> finish());

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            chip.setOnClickListener(view -> {
                String quickQuestion = chip.getText().toString();
                editTextMessage.setText(quickQuestion);
                sendMessage();
            });
        }
    }

    private void addWelcomeMessage() {
        String welcomeText = "안녕하세요,\n🌲복잡한 도심 속, 원하는 곳 어디든 자유롭게 이동하세요.\n스마트한 자전거 공유 서비스 작당모빌 🚲 입니다.";
        messageList.add(new Message(welcomeText, false));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (!messageText.isEmpty()) {
            messageList.add(new Message(messageText, true));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);
            editTextMessage.setText("");

            long userId = tokenManager.fetchUserId();
            apiService.sendChatMessage(new ChatRequest(userId, messageText)).enqueue(new Callback<ChatResponse>() {
                @Override
                public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String botResponse = response.body().getReply();
                        messageList.add(new Message(botResponse, false));
                        chatAdapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    } else {
                        messageList.add(new Message("죄송합니다. 오류가 발생했습니다.", false));
                        chatAdapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                }

                @Override
                public void onFailure(Call<ChatResponse> call, Throwable t) {
                    messageList.add(new Message("네트워크 연결을 확인해주세요.", false));
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            });
        }
    }
}
