package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class ChatResponse {
    private long userId;
    private String assistantMessage;
    private String model;

    @SerializedName("reply")
    private String reply;

    public String getAssistantMessage() {
        return assistantMessage;
    }

    public String getReply() {
        return reply;
    }
}
