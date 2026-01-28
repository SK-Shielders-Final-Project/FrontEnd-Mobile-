package com.mobility.hack.network;

import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    // --- [1] 인증 및 유저 관리 (Swagger 명세 반영) ---

    // [보안] 로그인 API 경로: /api/user/auth/login
    @POST("api/user/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // [보안] 회원가입 API 경로: /api/user/auth/signup
    @POST("api/user/auth/signup")
    Call<RegisterResponse> signup(@Body RegisterRequest request);

    // [보안] 비밀번호 확인 API 추가
    @POST("api/user/verify-password")
    Call<ResponseBody> verifyPassword(@Body Map<String, String> passwordMap);

    @GET("api/user/info/{userId}")
    Call<RegisterResponse> getUserInfo(@Path("userId") long userId);

    // [보안] 유저 정보 수정 API 경로: /api/user/info
    @PUT("api/user/info")
    Call<UpdateInfoResponse> updateUserInfo(@Body UpdateInfoRequest request);

    @POST("api/auth/refresh")
    Call<LoginResponse> refreshToken(@Body Map<String, String> refreshTokenMap);

    @GET(".well-known/jwks.json")
    Call<ResponseBody> getJwks();

    // --- [2] 문의사항 처리 ---

    // [보안] 문의하기 API 추가
    @POST("api/user/inquiry/write")
    Call<InquiryResponse> writeInquiry(@Body InquiryRequest request);

    @GET("api/user/inquiry/list")
    Call<List<InquiryResponse>> getInquiryList();

    @Multipart
    @POST("api/user/inquiry/upload")
    Call<InquiryResponse> uploadInquiry(
        @Part("title") RequestBody title,
        @Part("content") RequestBody content,
        @Part MultipartBody.Part file
    );

    @GET("api/user/inquiry/download")
    Call<ResponseBody> downloadFile(@Query("filename") String filename);

    // --- [3] 기타 보안 및 호환성 ---
    @POST("api/auth/check-password")
    Call<CheckPasswordResponse> checkPassword(@Body CheckPasswordRequest request);

    @PUT("api/auth/change-password")
    Call<ResponseBody> changePassword(@Body ChangePasswordRequest request);

    @POST("api/auth/reset-password-request")
    Call<ResponseBody> requestPasswordReset(
        @Header("X-Forwarded-Host") String host, 
        @Body Map<String, String> payload
    );

    @POST("api/auth/reset-password")
    Call<ResponseBody> resetPassword(@Body Map<String, String> payload);

    @POST("api/auth/validate-token")
    Call<Void> validateToken(@Header("Authorization") String authHeader);

    @GET("get_inquiries.php")
    Call<List<InquiryResponse>> getInquiries();
}
