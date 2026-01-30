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
    private int rideCount;

    @SerializedName("total_point") // 서버가 보내주는 JSON 필드 이름으로 수정
    private int totalPoint;

    @SerializedName("name")
    private String name;


    @SerializedName("admin_lev")
    private int adminLev;

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

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public void setTotalPoint(int totalPoint) {
        this.totalPoint = totalPoint;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
