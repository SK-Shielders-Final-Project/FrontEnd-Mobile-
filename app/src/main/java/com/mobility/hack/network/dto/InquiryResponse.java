package com.mobility.hack.network.dto;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class InquiryResponse implements Serializable {
    @SerializedName("id")
    private Long id;
    @SerializedName("title")
    private String title;
    @SerializedName("content")
    private String content;
    @SerializedName("storedName")
    private String storedName;
    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getStoredName() {
        return storedName;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
