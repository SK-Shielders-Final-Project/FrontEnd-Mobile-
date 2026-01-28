package com.mobility.hack.network;

public class RegisterRequest {

    private final String username;
    private final String name;
    private final String password;
    private final String email;
    private final String phone;

    public RegisterRequest(String username, String name, String password, String email, String phone) {
        this.username = username;
        this.name = name;
        this.password = password;
        this.email = email;
        this.phone = phone;
    }
}
