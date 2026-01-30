package com.mobility.hack.network.dto;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class InquiryResponse implements Serializable {

    @SerializedName("inquiry_id")
    private Long id;

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("file_id")
    private Long fileId;

    @SerializedName("admin_reply")
    private String adminReply;

    @SerializedName("admin_level")
    private Integer adminLevel;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }

    public Integer getAdminLevel() { return adminLevel; }
    public void setAdminLevel(Integer adminLevel) { this.adminLevel = adminLevel; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}