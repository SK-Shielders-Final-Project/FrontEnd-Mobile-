package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class InquiryWriteRequest {
    @SerializedName("user_id") // 백엔드 getUser_id()와 매칭
    private Long user_id;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("file_id") // 백엔드 getFile_id()와 매칭
    private Long file_id;

    public InquiryWriteRequest(Long user_id, String title, String content, Long fileId) {
        this.user_id = user_id;
        this.title = title;
        this.content = content;
        this.file_id = (fileId != null) ? fileId : 0L;
    }
}