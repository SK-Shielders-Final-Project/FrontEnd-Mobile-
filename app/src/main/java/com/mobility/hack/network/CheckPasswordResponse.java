package com.mobility.hack.network;

public class CheckPasswordResponse {

    private final String status;

    public CheckPasswordResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
