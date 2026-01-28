package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("userId")
    private final Long userId;

    @SerializedName("jwttoken")
    private final String jwtToken;

    public LoginResponse(Long userId, String jwtToken) {
        this.userId = userId;
        this.jwtToken = jwtToken;
    }

    public Long getUserId() {
        return userId;
    }

    public String getJwtToken() {
        return jwtToken;
    }
}
