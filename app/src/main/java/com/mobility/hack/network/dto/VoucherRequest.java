package com.mobility.hack.network.dto;

import com.google.gson.annotations.SerializedName;

public class VoucherRequest {

    @SerializedName("couponCode")
    private String couponCode;

    public VoucherRequest(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}
