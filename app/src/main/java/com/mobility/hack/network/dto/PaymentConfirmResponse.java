package com.example.mobilityhack.network.dto; // 패키지 이름 변경

public class PaymentConfirmResponse {
    private long paymentId;
    private int userId;
    private int totalPoint;
    private String paymentKey;

    public long getPaymentId() {
        return paymentId;
    }

    public int getUserId() {
        return userId;
    }

    public int getTotalPoint() {
        return totalPoint;
    }

    public String getPaymentKey() {
        return paymentKey;
    }
}
