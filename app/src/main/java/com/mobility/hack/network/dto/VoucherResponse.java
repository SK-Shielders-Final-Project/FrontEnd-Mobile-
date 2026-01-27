package com.example.mobilityhack.network.dto; // 패키지 이름 변경

import com.google.gson.annotations.SerializedName;

public class VoucherResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private SuccessData data;

    @SerializedName("error_code")
    private String errorCode;

    @SerializedName("message")
    private String message;

    public String getStatus() {
        return status;
    }

    public SuccessData getData() {
        return data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    public static class SuccessData {
        @SerializedName("recharged_amount")
        private int rechargedAmount;

        @SerializedName("total_point")
        private int totalPoint;

        @SerializedName("message")
        private String message;

        public int getRechargedAmount() {
            return rechargedAmount;
        }

        public int getTotalPoint() {
            return totalPoint;
        }

        public String getMessage() {
            return message;
        }
    }
}
