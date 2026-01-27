package com.example.mobilityhack.network.dto; // 패키지 이름 변경

public class PaymentConfirmRequest {
    private String paymentKey;
    private String orderId;
    private int amount;
    private int userId;

    public PaymentConfirmRequest(String paymentKey, String orderId, int amount, int userId) {
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.amount = amount;
        this.userId = userId;
    }
}
