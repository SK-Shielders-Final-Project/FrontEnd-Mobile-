package com.mobility.hack.network;

public class ExchangeRequest {
    private String encryptedSymmetricKey;

    public ExchangeRequest(String encryptedSymmetricKey) {
        this.encryptedSymmetricKey = encryptedSymmetricKey;
    }
}
