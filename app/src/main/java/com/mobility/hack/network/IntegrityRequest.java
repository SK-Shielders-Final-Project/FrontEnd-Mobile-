package com.mobility.hack.network;

public class IntegrityRequest {
    private String signature_hash;
    private String binary_hash;

    public IntegrityRequest(String signature_hash, String binary_hash) {
        this.signature_hash = signature_hash;
        this.binary_hash = binary_hash;
    }
}