package com.mobility.hack.network.dto;

import com.google.gson.annotations.SerializedName;

public class PaymentHistoryItem {
    @SerializedName("paymentDate")
    private String paymentDate;

    @SerializedName("amount")
    private int amount;

    @SerializedName("status")
    private String status;

    public String getPaymentDate() {
        return paymentDate;
    }

    public int getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }
}
