package com.mobility.hack.network;

public class ChatRequest {
    private Message message;

    public ChatRequest(long userId, String content) {
        this.message = new Message(userId, content);
    }

    public static class Message {
        private long user_id; // 서버와 변수명을 맞추면 SerializedName 생략 가능
        private String role = "user"; // 고정값
        private String content;

        public Message(long userId, String content) {
            this.user_id = userId;
            this.content = content;
        }
    }
}