package com.mobility.hack.network.dto;

import com.google.gson.annotations.SerializedName;

public class UpdateInfoResponse {
    @SerializedName("username")
    private String username;
    @SerializedName("name")
    private String name;

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }
}
