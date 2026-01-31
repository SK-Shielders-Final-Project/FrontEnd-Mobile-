package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

/**
 * [API 최적화] 문의하기 요청 DTO
 */
public class InquiryRequest {
    @SerializedName("user_id")
    private long userId;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("file_id")
    private Integer fileId; // null 허용을 위해 Integer 사용

    public InquiryRequest(long userId, String title, String content, Integer fileId) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        // 파일이 없으면 0을 기본값으로 설정
        this.fileId = (fileId != null) ? fileId : 0;
    }
}
