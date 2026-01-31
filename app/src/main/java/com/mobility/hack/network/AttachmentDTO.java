package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class AttachmentDTO implements Serializable {
    @SerializedName("fileId")
    private Long fileId;

    @SerializedName("originalFilename")
    private String originalFilename;

    @SerializedName("fileDownloadUri")
    private String fileDownloadUri;

    @SerializedName("fileViewUri")
    private String fileViewUri;

    @SerializedName("ext")
    private String ext;

    public Long getFileId() { return fileId; }
    public String getOriginalFilename() { return originalFilename; }
    public String getFileDownloadUri() { return fileDownloadUri; }
    public String getFileViewUri() { return fileViewUri; }
    public String getExt() { return ext; }
}
