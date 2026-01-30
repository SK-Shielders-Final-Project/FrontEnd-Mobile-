package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class FileUploadResponse {
    // 백엔드: private Long file_id;
    @SerializedName("file_id")
    private Long fileId;

    @SerializedName("original_name")
    private String originalName;

    private String path;
    private String uuid;
    private String ext;

    @SerializedName("stored_name")
    private String storedName;

    // Getter들
    public Long getFile_id() { return fileId; } // 기존 액티비티 코드 호환을 위해 유지
    public String getOriginalName() { return originalName; }
    public String getPath() { return path; }
    public String getUuid() { return uuid; }
    public String getExt() { return ext; }
    public String getStoredName() { return storedName; }
}