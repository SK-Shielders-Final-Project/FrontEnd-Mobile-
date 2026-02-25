package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

// 자전거 반납 이미지 업로드 전용 응답 클래스
public class ImageUploadResponse {

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
