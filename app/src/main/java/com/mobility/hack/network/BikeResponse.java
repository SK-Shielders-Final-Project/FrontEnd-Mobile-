package com.mobility.hack.network;

public class BikeResponse {
    private String bike_id;
    private String serial_number;
    private String model_name;
    private String status;
    private String latitude;
    private String longitude;

    // Getter 메서드들
    public String getBikeId() { return bike_id; }
    public String getSerialNumber() { return serial_number; }
    public String getModelName() { return model_name; }
    public String getStatus() { return status; }
    public String getLatitude() { return latitude; }
    public String getLongitude() { return longitude; }
}