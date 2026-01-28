package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class InquiryModifyRequest {
    @SerializedName("user_id")
    private Long userId;

    @SerializedName("inquiry_id")
    private Long inquiryId;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("file_id")
    private Long fileId;

    public InquiryModifyRequest(Long userId, Long inquiryId, String title, String content, Long fileId) {
        this.userId = userId;
        this.inquiryId = inquiryId;
        this.title = title;
        this.content = content;
        this.fileId = fileId;
    }
}