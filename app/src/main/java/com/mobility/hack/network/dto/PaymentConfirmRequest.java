package com.mobility.hack.network.dto;

import com.google.gson.annotations.SerializedName;

public class PaymentConfirmRequest {
    @SerializedName("paymentKey")
    private String paymentKey;
    @SerializedName("orderId")
    private String orderId;
    @SerializedName("amount")
    private int amount;

    public PaymentConfirmRequest(String paymentKey, String orderId, int amount) {
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.amount = amount;
    }
}
