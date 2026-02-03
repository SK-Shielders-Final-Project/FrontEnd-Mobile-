package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class PublicKeyResponse {
    @SerializedName("publicKey")
    private String publicKey;

    public String getPublicKey() {
        return publicKey;
    }
}
