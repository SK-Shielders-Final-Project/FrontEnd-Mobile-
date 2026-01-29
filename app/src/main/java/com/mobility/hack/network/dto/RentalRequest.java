package com.mobility.hack.network.dto;

import com.google.gson.annotations.SerializedName;

public class RentalRequest {

    @SerializedName("bike_id")
    private String bikeId;

    @SerializedName("user_id")
    private int userId;

    public RentalRequest(String bikeId, int userId) {
        this.bikeId = bikeId;
        this.userId = userId;
    }
}
