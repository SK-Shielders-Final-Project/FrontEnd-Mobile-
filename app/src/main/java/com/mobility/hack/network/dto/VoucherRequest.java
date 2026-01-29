package com.mobility.hack.network.dto;

public class VoucherRequest {
    private String voucherCode;

    public VoucherRequest(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }
}
