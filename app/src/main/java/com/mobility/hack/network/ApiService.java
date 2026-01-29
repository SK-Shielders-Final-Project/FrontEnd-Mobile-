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

    @POST("/api/user/auth/refresh")
    Call<LoginResponse> refresh(@Body RefreshRequest request);

    @POST("/api/user/auth/signup")
    Call<Void> register(@Body RegisterRequest request);

    @POST("/api/user/verify-password")
    Call<Void> verifyPassword(@Body PasswordRequest request);

    @POST("/api/auth/password-reset/request")
    Call<Void> requestPasswordReset(@Header("Host") String host, @Body Map<String, String> emailPayload);

    @POST("/api/auth/password-reset/reset")
    Call<Void> resetPassword(@Body ResetPasswordRequest resetPayload);

    @GET("/api/user/info/{userId}")
    Call<UserInfoResponse> getUserInfo(@Path("userId") long userId);

    @GET("/api/user/info")
    Call<UserInfoResponse> getUserInfo();

    @PUT("/api/user/info")
    Call<UserInfoResponse> updateUserInfo(@Body UpdateUserRequest request);

    @POST("/api/checkpw")
    Call<CheckPasswordResponse> checkPassword(@Body CheckPasswordRequest request);

    @POST("/api/user/auth/changepw")
    Call<UserInfoResponse> changePassword(@Body Map<String, String> body);

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

    @POST("/api/payment/confirm")
    Call<PaymentResponse> confirmPayment(@Body PaymentRequest request);

    @POST("/api/bikes")
    Call<List<BikeResponse>> getBikes();

    @POST("/api/chat")
    Call<ChatResponse> sendChatMessage(@Body ChatRequest request);
}
