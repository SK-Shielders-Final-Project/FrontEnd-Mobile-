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
    // --- [1] 인증 및 유저 관리 ---
    @POST("api/user/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/user/auth/signup")
    Call<RegisterResponse> signup(@Body RegisterRequest request);

    // [에러 해결] AuthInterceptor에서 사용하는 리프레시 토큰 API 추가
    @POST("api/auth/refresh")
    Call<LoginResponse> refreshToken(@Body Map<String, String> refreshTokenMap);

    @GET("api/user/info/{userId}")
    Call<RegisterResponse> getUserInfo(@Path("userId") long userId);

    @PUT("api/user/info")
    Call<UpdateInfoResponse> updateUserInfo(@Body UpdateInfoRequest request);

    // --- [2] 문의사항 처리 (Swagger 명세 반영) ---
    @POST("api/user/inquiry/write")
    Call<InquiryResponse> writeInquiry(@Body InquiryRequest request);

    @POST("api/user/inquiry")
    Call<List<InquiryResponse>> getInquiryList(@Body Map<String, Long> userIdMap);

    @GET("api/user/inquiry/{inquiryId}")
    Call<InquiryResponse> getInquiryDetail(@Path("inquiryId") Long inquiryId);

    // --- [3] 보안 실습 및 호환성 ---
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
    Call<ResponseBody> resetPassword(@Body Map<String, String> resetMap);

    @POST("api/auth/validate-token")
    Call<Void> validateToken(@Header("Authorization") String authHeader);

    @Multipart
    @POST("api/user/inquiry/upload")
    Call<InquiryResponse> uploadInquiry(
        @Part("title") RequestBody title,
        @Part("content") RequestBody content,
        @Part MultipartBody.Part file
    );

    @GET("api/user/inquiry/download")
    Call<ResponseBody> downloadFile(@Query("filename") String filename);

    @GET("get_inquiries.php")
    Call<List<InquiryResponse>> getInquiries();
}
