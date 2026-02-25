package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class ReturnRequest {

    @SerializedName("SERIAL_NUMBER")
    private String serialNumber;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("fileId")
    private Long fileId;

    public ReturnRequest(String serialNumber, double latitude, double longitude, Long fileId) {
        this.serialNumber = serialNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.fileId = fileId;
    }

    @Override
    public String toString() {
        return "ReturnRequest{" +
                "SERIAL_NUMBER='" + serialNumber + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", fileId=" + fileId +
                '}';
    }
}
