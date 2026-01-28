package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * [보안 강화] 서버 문의사항 응답 DTO
 * Swagger 상세 조회 명세 반영
 */
public class InquiryResponse implements Serializable {
    @SerializedName("inquiry_id")
    private Long inquiryId;

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("file_id")
    private Long fileId;

    @SerializedName("admin_lev")
    private Integer adminLev;

    @SerializedName("admin_reply")
    private String adminReply;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // --- Getters ---
    public Long getInquiryId() { return inquiryId; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title != null ? title : ""; }
    public String getContent() { return content != null ? content : ""; }
    public String getImageUrl() { return imageUrl; }
    public Long getFileId() { return fileId; }
    public Integer getAdminLev() { return adminLev; }
    public String getAdminReply() { return adminReply; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public String getUpdatedAt() { return updatedAt; }

    // 기존 코드 호환성 유지용 (취약점 실습에서 사용)
    public String getStoredName() { return (fileId != null) ? String.valueOf(fileId) : null; }
}
