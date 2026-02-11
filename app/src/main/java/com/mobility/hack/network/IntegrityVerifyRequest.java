package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class IntegrityVerifyRequest {
    @SerializedName("nonce")
    private String nonce;

    @SerializedName("binaryHash") // 서버 DTO의 binaryHash와 매핑
    private String binary_hash;

    @SerializedName("signatureHash") // 서버 DTO의 signatureHash와 매핑
    private String signature_hash;

    public IntegrityVerifyRequest(String nonce, String binaryHash, String signatureHash) {
        this.nonce = nonce;
        this.binary_hash = binaryHash;
        this.signature_hash = signatureHash;
    }

    // Getters
    public String getNonce() { return nonce; }
    public String getBinary_hash() { return binary_hash; }
    public String getSignature_hash() { return signature_hash; }
}