package com.mobility.hack.network;

public class ChatResponse {
    private long userId; // JSON의 "userId"와 철자/대소문자까지 같아야 함
    private String assistantMessage;
    private String model;

    public String getAssistantMessage() {
        return assistantMessage;
    }
}