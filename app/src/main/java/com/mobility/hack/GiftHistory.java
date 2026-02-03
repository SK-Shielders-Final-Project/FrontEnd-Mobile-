package com.mobility.hack;

import com.google.gson.annotations.SerializedName;

public class GiftHistory {

    @SerializedName("senderName")
    private String senderName;

    @SerializedName("receiverName")
    private String receiverName;

    @SerializedName("amount")
    private int amount;

    @SerializedName("createdAt")
    private String createdAt;

    public String getSenderName() {
        return senderName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public int getAmount() {
        return amount;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
