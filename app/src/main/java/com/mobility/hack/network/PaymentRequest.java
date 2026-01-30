package com.mobility.hack.network;

public class PaymentRequest {
    private String paymentKey;
    private String orderId;
    private Long amount;
    private long userId;

    public PaymentRequest(String paymentKey, String orderId, Long amount, long userId) {
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.amount = amount;
        this.userId = userId;
    }

}
