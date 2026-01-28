package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class InquiryResponse implements Serializable {
    @SerializedName("inquiry_id") // Swagger 명세에 맞춘 필드명
    private Long inquiryId;

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("file_id")
    private String fileId;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("created_at") // Swagger JSON 명세 반영
    private String createdAt;

    public Long getInquiryId() { return inquiryId; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getFileId() { return fileId; }
    public String getImageUrl() { return imageUrl; }
    
    // [중요] 요구하신 getCreatedAt() 메서드 추가
    public String getCreatedAt() { return createdAt; }

    // [에러 방지] 기존 어댑터 로직 호환을 위해 추가
    public String getStoredName() { return title; }
}