package com.mobility.hack.network.dto;

import com.google.gson.annotations.SerializedName;

public class VoucherResponse {

    @SerializedName("userId")
    private int userId;

    @SerializedName("totalPoint")
    private int totalPoint;

    @SerializedName("rechargedPoint")
    private int rechargedPoint;

    public int getUserId() {
        return userId;
    }

    public int getTotalPoint() {
        return totalPoint;
    }

    public int getRechargedPoint() {
        return rechargedPoint;
    }
}
