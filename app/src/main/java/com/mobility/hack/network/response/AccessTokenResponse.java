package com.mobility.hack.network.response;

import com.google.gson.annotations.SerializedName;

public class AccessTokenResponse {

    @SerializedName("accessToken")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }
}
