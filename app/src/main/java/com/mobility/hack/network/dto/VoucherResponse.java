package com.mobility.hack.network.dto;

public class VoucherResponse {
    private String message;
    private int rechargedPoint;
    private int totalPoint;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getRechargedPoint() {
        return rechargedPoint;
    }

    public void setRechargedPoint(int rechargedPoint) {
        this.rechargedPoint = rechargedPoint;
    }

    public int getTotalPoint() {
        return totalPoint;
    }

    public void setTotalPoint(int totalPoint) {
        this.totalPoint = totalPoint;
    }
}
