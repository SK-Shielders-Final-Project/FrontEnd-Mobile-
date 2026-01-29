package com.mobility.hack.chatbot;

public class ChatMessage {
    public static final int VIEW_TYPE_USER = 1;
    public static final int VIEW_TYPE_BOT = 2;

    private String message;
    private int viewType;
    private String time;

    public ChatMessage(String message, int viewType, String time) {
        this.message = message;
        this.viewType = viewType;
        this.time = time;
    }

    public String getMessage() { return message; }
    public int getViewType() { return viewType; }
    public String getTime() { return time; }
}