package com.mobility.hack.chatbot;

import android.content.Context;
import java.io.FileOutputStream;

public class ChatHistoryManager {
    private Context context;

    public ChatHistoryManager(Context context) {
        this.context = context;
    }

    // [16] 취약점: AI와의 민감한 대화 내역을 기기 내 평문(txt)으로 저장
    public void saveChatHistory(String message) {
        try (FileOutputStream fos = context.openFileOutput("chat_history.txt", Context.MODE_APPEND)) {
            fos.write((message + "\n").getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}