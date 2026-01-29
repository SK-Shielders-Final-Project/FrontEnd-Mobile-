package com.mobility.hack.network.dto;

import com.google.gson.annotations.SerializedName;

public class UpdateInfoRequest {
    @SerializedName("username")
    private String username;
    @SerializedName("name")
    private String name;
    @SerializedName("password")
    private String password;
    @SerializedName("email")
    private String email;
    @SerializedName("phone")
    private String phone;

    public UpdateInfoRequest(String username, String name, String password, String email, String phone) {
        this.username = username;
        this.name = name;
        this.password = password;
        this.email = email;
        this.phone = phone;
    }
}
