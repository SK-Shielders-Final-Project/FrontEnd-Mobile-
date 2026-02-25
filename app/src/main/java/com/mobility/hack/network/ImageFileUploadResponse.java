package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

// 이미지 업로드 응답을 위한 별도의 클래스
public class ImageFileUploadResponse {

    @SerializedName("result")
    private String result;

    @SerializedName("file_id")
    private int fileId;

    public String getResult() {
        return result;
    }

    public int getFileId() {
        return fileId;
    }
}
