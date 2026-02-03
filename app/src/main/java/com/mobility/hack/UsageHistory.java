package com.mobility.hack;

import com.google.gson.annotations.SerializedName;

public class UsageHistory {
    @SerializedName("userId")
    private long userId;
    @SerializedName("bikeId")
    private int bikeId;
    @SerializedName("startTime")
    private String startTime;
    @SerializedName("endTime")
    private String endTime;

    public long getUserId() { return userId; }
    public int getBikeId() { return bikeId; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
}
