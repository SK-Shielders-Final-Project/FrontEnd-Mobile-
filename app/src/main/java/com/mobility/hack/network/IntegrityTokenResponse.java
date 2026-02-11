package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class IntegrityTokenResponse {
    @SerializedName("integrityToken") // 서버 DTO의 integrityToken과 매핑
    private String integrity_token;

    public String getIntegrityToken() {
        return integrity_token;
    }

    public void setIntegrityToken(String integrityToken) {
        this.integrity_token = integrityToken;
    }
}