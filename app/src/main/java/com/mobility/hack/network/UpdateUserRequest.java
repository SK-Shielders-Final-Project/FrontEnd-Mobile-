package com.mobility.hack.network;

public class UpdateUserRequest {
    private String username;
    private String name;
    private String password;
    private String email;
    private String phone;
    private int admin_lev;

    public UpdateUserRequest(String username, String name, String password, String email, String phone, int admin_lev) {
        this.username = username;
        this.name = name;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.admin_lev = admin_lev;
    }

    // Getters and setters (optional, but good practice)
}
