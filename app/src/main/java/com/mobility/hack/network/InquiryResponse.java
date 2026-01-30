package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * 백엔드 InquiryResponse 규격에 맞춘 통합 응답 모델
 */
public class InquiryResponse implements Serializable {

    @SerializedName("inquiry_id") // 백엔드 필드명과 일치시켜야 500 에러가 안 납니다.
    private long inquiryId;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("author_name")
    private String authorName;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("admin_reply")
    private String adminReply;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("file_id") // 문의 작성 직후 반환되는 파일 ID
    private Long fileId;

    @SerializedName("attachment")
    private AttachmentDTO attachment; // 상세 조회 시 포함되는 첨부파일 정보

    // --- Inner Class: AttachmentDTO ---
    public static class AttachmentDTO implements Serializable {
        @SerializedName("file_id")
        private int fileId;

        @SerializedName("original_filename")
        private String originalFilename;

        @SerializedName("file_download_uri")
        private String fileDownloadUri;

        @SerializedName("file_view_uri")
        private String fileViewUri;

        @SerializedName("ext")
        private String ext;

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
    public Long getFileId() { return fileId; }
    public AttachmentDTO getAttachment() { return attachment; }
}