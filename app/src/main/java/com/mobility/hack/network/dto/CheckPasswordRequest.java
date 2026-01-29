package com.mobility.hack.network.dto;

import com.google.gson.annotations.SerializedName;

public class CheckPasswordRequest {
    @SerializedName("password")
    private String password;

    public CheckPasswordRequest(String password) {
        this.password = password;
    }
}
