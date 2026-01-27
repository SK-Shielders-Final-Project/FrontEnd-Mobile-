package com.example.mobilityhack.network.dto; // 패키지 이름 변경

import com.google.gson.annotations.SerializedName;

public class VoucherRequest {

    @SerializedName("voucher_code")
    private String voucherCode;

    @SerializedName("user_id")
    private int userId;

    public VoucherRequest(String voucherCode, int userId) {
        this.voucherCode = voucherCode;
        this.userId = userId;
    }
}
