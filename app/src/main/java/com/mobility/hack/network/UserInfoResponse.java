package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class UserInfoResponse {

    @SerializedName("user_id")
    private long userId;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("admin_lev")
    private int adminLev;

    @SerializedName("total_point")
    private int totalPoint;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public int getAdminLev() {
        return adminLev;
    }

    public int getTotalPoint() {
        return totalPoint;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
