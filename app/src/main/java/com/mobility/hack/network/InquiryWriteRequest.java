package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class InquiryWriteRequest {
    @SerializedName("user_id") // 웹팀의 {user_id: 1} 규격 일치
    private Long userId;

    private String title;
    private String content;

    @SerializedName("file_id") // 웹팀의 {file_id: 1} 규격 일치
    private Long fileId;

    public InquiryWriteRequest() {}

    public InquiryWriteRequest(Long userId, String title, String content, Long fileId) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.fileId = fileId;
    }

    // Getter/Setter 생략 가능 (필요시 추가)
}