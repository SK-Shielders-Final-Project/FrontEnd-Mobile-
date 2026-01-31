package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class InquiryModifyResponse {
    @SerializedName("result")
    private String result; // "Y" or "N"

    public String getResult() {
        return result;
    }
}