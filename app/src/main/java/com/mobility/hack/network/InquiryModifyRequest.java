package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class InquiryModifyRequest {
    @SerializedName("user_id")
    private long userId;

    @SerializedName("inquiry_id")
    private long inquiryId;

    private String title;
    private String content;

    @SerializedName("file_id")
    private Long fileId;

    public InquiryModifyRequest(long userId, long inquiryId, String title, String content, Long fileId) {
        this.userId = userId;
        this.inquiryId = inquiryId;
        this.title = title;
        this.content = content;
        this.fileId = fileId;
    }
}
