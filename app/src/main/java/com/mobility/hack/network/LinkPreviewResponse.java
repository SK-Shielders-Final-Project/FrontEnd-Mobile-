package com.mobility.hack.network;

// 웹 팀이 준 JSON 필드명(image, description, title...)과 똑같아야 합니다.
public class LinkPreviewResponse {
    private String title;
    private String description;
    private String image;
    private String url;
    private String content;

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImage() { return image; }
}