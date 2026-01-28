package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    @SerializedName("username")
    private final String username;

    @SerializedName("name")
    private final String name;

    @SerializedName("password")
    private final String password;

    @SerializedName("email")
    private final String email;

    @SerializedName("phone")
    private final String phone;

    public RegisterRequest(String username, String name, String password, String email, String phone) {
        this.username = username;
        this.name = name;
        this.password = password;
        this.email = email;
        this.phone = phone;
    }
}
