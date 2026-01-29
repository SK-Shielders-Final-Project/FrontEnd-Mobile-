package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class UpdateInfoResponse {

    @SerializedName("user_id")
    private final Long userId;

    private final String username;
    private final String name;

    @SerializedName("updated_at")
    private final String updatedAt;

    public UpdateInfoResponse(Long userId, String username, String name, String updatedAt) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.updatedAt = updatedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
