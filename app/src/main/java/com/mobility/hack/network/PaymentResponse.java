package com.mobility.hack.network;

public class PaymentResponse {

    private long userId;
    private long amount;
    private String orderId;
    private String paymentKey;
    private String paymentMethod;
    private String status;

    // Getters
    public long getUserId() {
        return userId;
    }

    public long getAmount() {
        return amount;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getStatus() {
        return status;
    }
}
