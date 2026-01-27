package com.example.mobilityhack.network.dto;

import com.google.gson.annotations.SerializedName;

public class PaymentHistoryItem {

    @SerializedName("userId")
    private int userId;

    @SerializedName("amount")
    private int amount;

    @SerializedName("orderId")
    private String orderId;

    @SerializedName("paymentKey")
    private String paymentKey;

    @SerializedName("paymentMethod")
    private String paymentMethod;

    @SerializedName("createAt")
    private String createAt; // 날짜/시간 형식이므로 String으로 받습니다.

    @SerializedName("status")
    private String status;

    @SerializedName("remainAmount")
    private int remainAmount;

    // Getters
    public int getUserId() {
        return userId;
    }

    public int getAmount() {
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

    public String getCreateAt() {
        return createAt;
    }

    public String getStatus() {
        return status;
    }

    public int getRemainAmount() {
        return remainAmount;
    }
}
