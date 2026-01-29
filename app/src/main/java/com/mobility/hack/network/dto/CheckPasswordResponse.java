package com.mobility.hack.network.dto;

import com.google.gson.annotations.SerializedName;

public class CheckPasswordResponse {
    @SerializedName("status")
    private String status;

    public String getStatus() {
        return status;
    }
}
