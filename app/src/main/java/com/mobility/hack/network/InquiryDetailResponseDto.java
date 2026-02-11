package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import com.mobility.hack.BuildConfig;

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

    public String getDownloadUrl() {
        if (attachment == null) return null;
        String fullPath = attachment.getPath() + "/" + attachment.getFileName() + "." + attachment.getExt();
        return BuildConfig.BASE_URL + "/api/user/files/download?file=" + fullPath;
    }

    // Getters
    public long getInquiryId() { return inquiryId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
    public Long getFileId() { return fileId; }
    public AttachmentDTO getAttachment() { return attachment; }
}