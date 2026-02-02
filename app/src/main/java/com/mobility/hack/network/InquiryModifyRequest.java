package com.mobility.hack.network;

public class InquiryModifyRequest {
    private Long user_id;
    private Long inquiry_id;
    private String title;
    private String content;
    private Long file_id; // [중요] long 대신 Long 사용 (null 허용)

    public InquiryModifyRequest(Long user_id, Long inquiry_id, String title, String content, Long file_id) {
        this.user_id = user_id;
        this.inquiry_id = inquiry_id;
        this.title = title;
        this.content = content;
        this.file_id = file_id;
    }
}