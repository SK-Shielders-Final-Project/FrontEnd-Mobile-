package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

/**
 * [보안 강화] 서버 로그인 응답 DTO
 * Swagger 명세 반영 및 Null 방어 로직 추가
 */
public class LoginResponse {

    @SerializedName("userId")
    private final Long userId;

    @SerializedName("accessToken")
    private final String accessToken;

    @SerializedName("refreshToken")
    private final String refreshToken;

    public LoginResponse(Long userId, String accessToken, String refreshToken) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public Long getUserId() {
        return userId != null ? userId : 0L;
    }

    public String getAccessToken() {
        return accessToken != null ? accessToken : "";
    }

    public String getRefreshToken() {
        return refreshToken != null ? refreshToken : "";
    }
}
