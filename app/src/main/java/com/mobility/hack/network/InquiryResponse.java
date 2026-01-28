package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class InquiryResponse implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("file_id")
    private String fileId;

    @SerializedName("stored_name")
    private String storedName;

    @SerializedName("path")
    private String path;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getFileId() { return fileId; }
    public String getStoredName() { return storedName; }
    public String getPath() { return path; }
    public String getCreatedAt() { return createdAt; }
}