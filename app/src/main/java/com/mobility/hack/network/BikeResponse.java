package com.mobility.hack.network;

public class BikeResponse {

    private int bike_id;
    private String serial_number;
    private String model_name;
    private int status_code;
    private String status;
    private double latitude;
    private double longitude;

    public int getBikeId() { return bike_id; }
    public String getSerialNumber() { return serial_number; }
    public String getModelName() { return model_name; }
    public int getStatusCode() { return status_code; }
    public String getStatus() { return status; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
