package com.mobility.hack.network.dto;

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
}
