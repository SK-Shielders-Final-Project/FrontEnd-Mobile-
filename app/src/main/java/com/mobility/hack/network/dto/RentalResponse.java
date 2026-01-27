package com.example.mobilityhack.network.dto; // 패키지 이름 변경

import com.google.gson.annotations.SerializedName;

public class RentalResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }
}
