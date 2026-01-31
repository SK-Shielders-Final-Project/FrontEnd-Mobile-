package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class InquiryDetailResponseDto implements Serializable {
    @SerializedName("inquiryId")
    private long inquiryId;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("fileId")
    private Long fileId;

    @SerializedName("attachment")
    private AttachmentDTO attachment;

    public static class AttachmentDTO implements Serializable {
        @SerializedName("path")
        private String path; //

        @SerializedName("fileName")
        private String fileName; //

        @SerializedName("ext")
        private String ext; //

        @SerializedName("originalName")
        private String originalName;

        public String getPath() { return path; }
        public String getFileName() { return fileName; }
        public String getExt() { return ext; }
        public String getOriginalName() { return originalName; }
    }

    // [핵심] 질문하신 다운로드 URL 생성 메서드
    public String getDownloadUrl() {
        if (attachment == null) return null;
        // 규칙: path + "/" + fileName + "." + ext
        String fullPath = attachment.getPath() + "/" + attachment.getFileName() + "." + attachment.getExt();
        // 다운로드 서버 포트인 8080 고정
        return "http://43.203.51.77:8080/api/user/files/download?file=" + fullPath;
    }

    // Getters
    public long getInquiryId() { return inquiryId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
    public Long getFileId() { return fileId; }
    public AttachmentDTO getAttachment() { return attachment; }
}