package com.mobility.hack.network;

public class InquiryDeleteRequest {
    private Long user_id;    // 삭제 요청자 ID
    private Long inquiry_id; // 삭제할 글 ID

    public InquiryDeleteRequest(Long user_id, Long inquiry_id) {
        this.user_id = user_id;
        this.inquiry_id = inquiry_id;
    }
}