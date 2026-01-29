package com.mobility.hack.network;

public class PaymentRequest {
    private String paymentKey;
    private String orderId;
    private Long amount;

    public PaymentRequest(String paymentKey, String orderId, Long amount) {
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.amount = amount;
    }

}
