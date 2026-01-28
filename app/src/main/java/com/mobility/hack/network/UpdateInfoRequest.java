package com.mobility.hack.network;

import com.google.gson.annotations.SerializedName;

public class UpdateInfoRequest {
    @SerializedName("username") private String username;
    @SerializedName("name") private String name;
    @SerializedName("password") private String password;
    @SerializedName("email") private String email;
    @SerializedName("phone") private String phone;
    @SerializedName("admin_lev") private Integer adminLev;

    // [에러 해결] 5개의 인자만 받는 생성자 추가 (adminLev 기본값 0 설정)
    public UpdateInfoRequest(String username, String name, String password, String email, String phone) {
        this(username, name, password, email, phone, 0);
    }

    public UpdateInfoRequest(String username, String name, String password, String email, String phone, Integer adminLev) {
        this.username = username;
        this.name = name;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.adminLev = adminLev;
    }
}
