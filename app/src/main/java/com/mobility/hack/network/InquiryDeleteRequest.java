package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class InquiryDeleteRequest {
    @SerializedName("user_id")
    private Long userId;

    @SerializedName("inquiry_id")
    private Long inquiryId;

    public InquiryDeleteRequest(Long userId, Long inquiryId) {
        this.userId = userId;
        this.inquiryId = inquiryId;
    }
}