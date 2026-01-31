package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class InquiryDeleteResponse {
    @SerializedName("result")
    private String result;

    public String getResult() {
        return result;
    }
}