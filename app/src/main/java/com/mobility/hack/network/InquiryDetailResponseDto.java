package com.mobility.hack.network;

import java.io.Serializable;

public class InquiryDetailResponseDto implements Serializable {
    private Long inquiry_id;
    private Long user_id;
    private String title;
    private String content;
    private Long file_id;
    private String admin_reply;
    private String created_at;
    private String updated_at;

    // Getters
    public Long getInquiryId() { return inquiry_id; }
    public Long getUserId() { return user_id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Long getFileId() { return file_id; }
    public String getAdminReply() { return admin_reply; }
    public String getCreatedAt() { return created_at; }
}