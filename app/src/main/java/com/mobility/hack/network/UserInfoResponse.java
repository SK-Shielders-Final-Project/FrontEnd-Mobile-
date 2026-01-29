package com.mobility.hack.network;

public class UserInfoResponse {
    private String username;
    private String name;
    private String email;
    private String phone;
    private int rideCount;
    private int total_point;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getRideCount() {
        return rideCount;
    }

    public void setRideCount(int rideCount) {
        this.rideCount = rideCount;
    }

    public int getTotalPoint() {
        return total_point;
    }

    public void setTotalPoint(int total_point) {
        this.total_point = total_point;
    }
}
