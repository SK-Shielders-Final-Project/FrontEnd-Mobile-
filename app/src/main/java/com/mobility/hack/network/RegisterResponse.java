package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse {

    @SerializedName("user_id")
    private final Long userId;

    private final String username;
    private final String name;
    private final String email;
    private final String phone;

    @SerializedName("admin_lev")
    private final Integer adminLev;

    @SerializedName("total_point")
    private final Integer totalPoint;

    @SerializedName("created_at")
    private final String createdAt;

    @SerializedName("updated_at")
    private final String updatedAt;

    public RegisterResponse(Long userId, String username, String name, String email, String phone, Integer adminLev, Integer totalPoint, String createdAt, String updatedAt) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.adminLev = adminLev;
        this.totalPoint = totalPoint;
        this.createdAt = createdAt;
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

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Integer getAdminLev() {
        return adminLev;
    }

    public Integer getTotalPoint() {
        return totalPoint;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
