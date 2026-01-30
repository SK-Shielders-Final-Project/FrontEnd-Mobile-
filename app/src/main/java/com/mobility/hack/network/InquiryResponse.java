package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * [상세 조회 명세 반영] 문의사항 응답 데이터 모델
 */
public class InquiryResponse implements Serializable {
    @SerializedName("inquiryId")
    private long inquiryId;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("authorName")
    private String authorName;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("adminReply")
    private String adminReply;

    @SerializedName("updatedAt")
    private String updatedAt;

    @SerializedName("attachment")
    private AttachmentDTO attachment; // 명세서의 attachment 객체 매핑

    // --- Inner Class: AttachmentDTO ---
    public static class AttachmentDTO implements Serializable {
        @SerializedName("fileId")
        private int fileId;

        @SerializedName("originalFilename")
        private String originalFilename;

        @SerializedName("fileDownloadUri")
        private String fileDownloadUri;

        @SerializedName("fileViewUri")
        private String fileViewUri; // Glide에서 사용할 이미지 경로

        @SerializedName("ext")
        private String ext;

        // Getters
        public int getFileId() { return fileId; }
        public String getOriginalFilename() { return originalFilename; }
        public String getFileDownloadUri() { return fileDownloadUri; }
        public String getFileViewUri() { return fileViewUri; }
        public String getExt() { return ext; }
    }

    // --- Getters ---
    public long getInquiryId() { return inquiryId; }
    public String getTitle() { return title != null ? title : ""; }
    public String getContent() { return content != null ? content : ""; }
    public String getAuthorName() { return authorName; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public String getAdminReply() { return adminReply; }
    public String getUpdatedAt() { return updatedAt; }
    public AttachmentDTO getAttachment() { return attachment; }
}