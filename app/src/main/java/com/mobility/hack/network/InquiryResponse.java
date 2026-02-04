package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * 백엔드 InquiryResponse 규격에 맞춘 통합 응답 모델
 */
public class InquiryResponse implements Serializable {

    @SerializedName("inquiry_id")
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

    @SerializedName("file_id")
    private Long fileId;

    @SerializedName("attachment")
    private AttachmentDTO attachment;

    // --- Inner Class: AttachmentDTO ---
    public static class AttachmentDTO implements Serializable {
        @SerializedName("fileId")
        private int fileId;

        @SerializedName("path") // [중요] 서버가 주는 경로 (예: INQUIRY/20260130)
        private String path;

        @SerializedName("fileName") // [중요] 서버 저장 파일명 (예: f5b03f...)
        private String fileName;

        @SerializedName("ext") // [중요] 확장자 (예: jpg)
        private String ext;

        @SerializedName("originalName") // 원본 파일명
        private String originalName;

        public int getFileId() { return fileId; }
        public String getPath() { return path; }
        public String getFileName() { return fileName; }
        public String getExt() { return ext; }
        public String getOriginalName() { return originalName; }
    }

    // --- [핵심] 다운로드 URL 생성 (8080 포트 강제 사용) ---
    public String getDownloadUrl() {
        if (attachment == null) return null;

        // 규칙: path + "/" + fileName + "." + ext
        String fullPath = attachment.getPath() + "/" + attachment.getFileName() + "." + attachment.getExt();

        // 다운로드 서버 포트인 8080으로 주소 완성
        return "http://43.203.51.77:8080/api/user/files/download?file=" + fullPath;
    }

    // --- Getters ---
    public long getInquiryId() { return inquiryId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthorName() { return authorName; }
    public String getCreatedAt() { return createdAt; }
    public String getAdminReply() { return adminReply; }
    public String getUpdatedAt() { return updatedAt; }
    public Long getFileId() { return fileId; }
    public AttachmentDTO getAttachment() { return attachment; }
}

