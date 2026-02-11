package com.example.mobile.network;

import com.example.mobile.network.request.RefreshTokenRequest;
import com.example.mobile.network.response.AccessTokenResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {

    /**
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.
     * @param request refreshToken을 포함하는 요청 객체
     * @return accessToken을 포함하는 응답 객체
     */
    @POST("/api/auth/refresh")
    Call<AccessTokenResponse> refreshAccessToken(@Body RefreshTokenRequest request);

}
