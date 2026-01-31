package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class PointResponse {
    private long user_id;
    @SerializedName("currentPoint") // 백엔드 응답의 "currentPoint" 필드를 매핑
    private int currentPoint;
    private int admin_lev;
    private String updated_at;

    public long getUserId() {
        return user_id;
    }

    public int getCurrentPoint() { // currentPoint의 getter
        return currentPoint;
    }

    public int getAdmin_lev() {
        return admin_lev;
    }

    public String getUpdated_at() {
        return updated_at;
    }
}
