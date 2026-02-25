package com.mobility.hack.network;

public class RentalRequest {
    private String serialNumber;

    public RentalRequest(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
}
