package com.mobility.hack.network;

import com.mobility.hack.network.request.RefreshTokenRequest;
import com.mobility.hack.network.response.AccessTokenResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("api/auth/refresh")
    Call<AccessTokenResponse> refreshAccessToken(@Body RefreshTokenRequest request);

}
