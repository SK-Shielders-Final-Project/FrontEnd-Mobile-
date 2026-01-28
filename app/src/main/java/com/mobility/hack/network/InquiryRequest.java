package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class InquiryRequest {
    @SerializedName("user_id")
    private Long userId;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("file_id")
    private Long fileId;

    public InquiryRequest(Long userId, String title, String content, Long fileId) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.fileId = fileId;
    }
}