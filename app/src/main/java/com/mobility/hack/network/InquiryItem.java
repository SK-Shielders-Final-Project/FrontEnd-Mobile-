package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class InquiryItem {
    @SerializedName("id") private String id;
    @SerializedName("title") private String title;
    @SerializedName("date") private String date;
    @SerializedName("content") private String content;
    @SerializedName("file_name") private String fileName;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getContent() { return content; }
    public String getFileName() { return fileName; }
}