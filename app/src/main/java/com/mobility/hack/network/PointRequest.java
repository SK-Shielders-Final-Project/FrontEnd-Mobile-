package com.mobility.hack.network;

public class PointRequest {
    private int hoursToUse;
    private int bikeId;

    public PointRequest(int hoursToUse, int bikeId) {
        this.hoursToUse = hoursToUse;
        this.bikeId = bikeId;
    }
}
