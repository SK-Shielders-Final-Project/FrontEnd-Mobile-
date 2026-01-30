package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class InquiryResponse implements Serializable {
    // 백엔드: inquiry_id
    @SerializedName("inquiry_id")
    private long inquiryId;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    // 백엔드: author_name (수정됨)
    @SerializedName("author_name")
    private String authorName;

    // 백엔드: created_at
    @SerializedName("created_at")
    private String createdAt;

    // 백엔드: admin_reply
    @SerializedName("admin_reply")
    private String adminReply;

    // 백엔드: updated_at
    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("attachment")
    private AttachmentDTO attachment;

    public static class AttachmentDTO implements Serializable {
        // 백엔드: file_id
        @SerializedName("file_id")
        private int fileId;

        // 백엔드: original_filename
        @SerializedName("original_filename")
        private String originalFilename;

        // 백엔드: file_download_uri
        @SerializedName("file_download_uri")
        private String fileDownloadUri;

        // 백엔드: file_view_uri
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

    public long getInquiryId() { return inquiryId; }
    public String getTitle() { return title != null ? title : ""; }
    public String getContent() { return content != null ? content : ""; }
    public String getAuthorName() { return authorName; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public String getAdminReply() { return adminReply; }
    public String getUpdatedAt() { return updatedAt; }
    public AttachmentDTO getAttachment() { return attachment; }
}