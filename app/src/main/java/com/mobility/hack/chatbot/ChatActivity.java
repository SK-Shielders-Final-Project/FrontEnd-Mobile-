package com.mobility.hack.chatbot;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private List<ChatMessage> messages = new ArrayList<>();
    private ChatHistoryManager historyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // [15] 프롬프트 인젝션 실습용 UI
        // [16] 대화 내역 평문 저장 취약점 포함
        historyManager = new ChatHistoryManager(this);
        
        // UI 초기화 로직 (실제 레이아웃 파일 필요)
        // setupChat();
    }

    private void sendMessage(String userText) {
        messages.add(new ChatMessage(userText, true));
        historyManager.saveChatHistory(userText); // [16] 평문 저장 호출
        
        // LLM 응답 시뮬레이션 및 프롬프트 인젝션 포인트
        String botResponse = "AI: " + userText + " 에 대한 답변입니다.";
        messages.add(new ChatMessage(botResponse, false));
    }
}