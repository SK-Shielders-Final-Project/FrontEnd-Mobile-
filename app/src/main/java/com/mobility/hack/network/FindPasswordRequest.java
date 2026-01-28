package com.mobility.hack.network;

public class FindPasswordRequest {

    private final String username;
    private final String email;

    public FindPasswordRequest(String username, String email) {
        this.username = username;
        this.email = email;
    }
}
