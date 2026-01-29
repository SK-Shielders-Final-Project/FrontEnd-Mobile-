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
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

public interface ApiService {
    @POST("/api/user/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/api/user/auth/signup")
    Call<Void> register(@Body RegisterRequest request);

    @POST("/api/user/verify-password")
    Call<Void> verifyPassword(@Body PasswordRequest request);

    // [취약점 적용] Host 헤더 인젝션을 위해 Host 헤더를 파라미터로 받고, 엔드포인트 및 요청 데이터 변경
    @POST("/api/auth/password-reset/request")
    Call<Void> requestPasswordReset(@Header("Host") String host, @Body Map<String, String> emailPayload);

    // [취약점 적용] 비밀번호 재설정 실행을 위한 새로운 엔드포인트 추가
    @POST("/api/auth/password-reset/reset")
    Call<Void> resetPassword(@Body ResetPasswordRequest resetPayload);

    @GET("/api/user/info/{userId}")
    Call<UserInfoResponse> getUserInfo(@Path("userId") long userId);

    @GET("/api/user/info")
    Call<UserInfoResponse> getUserInfo();

    @PUT("/api/user/info")
    Call<UpdateInfoResponse> updateUserInfo(@Body UpdateInfoRequest request);

    @POST("/api/checkpw")
    Call<CheckPasswordResponse> checkPassword(@Body CheckPasswordRequest request);

    @PUT("/api/user/auth/changepw")
    Call<Void> changePassword(@Body ChangePasswordRequest request);

    @GET("/api/inquiries")
    Call<List<InquiryResponse>> getInquiries(@Header("Authorization") String token);

    @Multipart
    @POST("/api/inquiries")
    Call<InquiryResponse> uploadInquiry(@Header("Authorization") String token, @PartMap Map<String, RequestBody> partMap, @Part MultipartBody.Part file);

    @Streaming
    @GET("/api/inquiries/download/{filename}")
    Call<ResponseBody> downloadFile(@Header("Authorization") String token, @Path("filename") String filename);

    @POST("/api/voucher/redeem")
    Call<VoucherResponse> redeemVoucher(@Body VoucherRequest request);

    @POST("/api/payments/user/confirm")
    Call<PaymentResponse> confirmPayment(@Body PaymentRequest request);


    // 사용되지 않는 findPassword 메소드는 주석 처리 또는 삭제
    // @PUT("/api/user/auth/findpw")
    // Call<Void> findPassword(@Body FindPasswordRequest request);
}
