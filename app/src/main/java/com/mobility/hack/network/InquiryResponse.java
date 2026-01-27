package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class InquiryResponse {
    @SerializedName("file_id")
    private String fileId;

    @SerializedName("stored_name")
    private String storedName;

    @SerializedName("path")
    private String path;

    public String getFileId() { return fileId; }
    public String getStoredName() { return storedName; }
    public String getPath() { return path; }
}