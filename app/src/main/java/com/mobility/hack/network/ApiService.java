package com.mobility.hack.network;

import com.mobility.hack.GiftHistory;
import com.mobility.hack.UsageHistory;
import com.mobility.hack.network.LinkPreviewResponse;

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
import retrofit2.http.Streaming;

public interface ApiService {
    // ... (기존 로그인 관련 코드) ...
    @POST("/api/user/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/api/auth/refresh")
    Call<LoginResponse> refresh(@Body RefreshRequest request);

    @POST("/api/user/auth/signup")
    Call<Void> register(@Body RegisterRequest request);

    @POST("/api/user/verify-password")
    Call<Void> verifyPassword(@Body PasswordRequest request);

    @POST("/api/auth/password-reset/request")
    Call<Void> requestPasswordReset(@Header("Host") String host, @Body Map<String, String> emailPayload);

    @POST("/api/auth/password-reset/reset")
    Call<Void> resetPassword(@Body ResetPasswordRequest resetPayload);


    @GET("/api/user/info")
    Call<UserInfoResponse> getUserInfo();

    @PUT("/api/user/info")
    Call<UserInfoResponse> updateUserInfo(@Body UpdateUserRequest request);

    @POST("/api/checkpw")
    Call<CheckPasswordResponse> checkPassword(@Body CheckPasswordRequest request);

    @PUT("/api/user/auth/changepw")
    Call<UserInfoResponse> changePassword(@Body ChangePasswordRequest request);

    // -----------------------------------------------------------
    // [게시판 관련 API]
    // -----------------------------------------------------------
    @GET("/api/user/inquiry/{inquiry_id}")
    Call<InquiryDetailResponseDto> getInquiryDetails(@Path("inquiry_id") long inquiryId);

    @POST("/api/user/inquiry/delete")
    Call<InquiryDeleteResponse> deleteInquiry(@Body InquiryDeleteRequest request);

    @POST("/api/user/inquiry/write")
    Call<InquiryResponse> writeInquiry(@Body InquiryWriteRequest request);

    @GET("/api/user/inquiry")
    Call<List<InquiryResponse>> getInquiryList(@Query("user_id") long userId);

    @PUT("/api/user/inquiry/modify")
    Call<InquiryModifyResponse> modifyInquiry(@Body InquiryModifyRequest request);

    @Multipart
    @POST("/api/files/upload")
    Call<FileUploadResponse> uploadFile(@Part MultipartBody.Part file);


    // ... (나머지 비즈니스 로직) ...
    @Streaming
    @POST("/api/coupon/redeem")
    Call<VoucherResponse> redeemVoucher(@Body VoucherRequest request);

    @POST("/api/payments/user/confirm")
    Call<PaymentResponse> confirmPayment(@Body PaymentRequest request);

    @GET("/api/payments/user")
    Call<List<com.mobility.hack.Payment>> getPaymentHistory();

    @POST("/api/user/point")
    Call<PointResponse> usePoint(@Body PointRequest request);

    @GET("/api/user/point")
    Call<List<UsageHistory>> getUsageHistory();

    @GET("/api/user/point/gift/history")
    Call<List<GiftHistory>> getGiftHistory();

    @GET("/api/user/files/download")
    Call<ResponseBody> downloadFile(@Query("file") String filename);

    @POST("/api/bikes")
    Call<List<BikeResponse>> getBikes();

    @GET("/api/user/bikes/{serial_number}")
    Call<ResponseBody> getBikeImage(@Path("serial_number") String serialNumber);

    @POST("/api/bikes/return")
    Call<Void> returnBike(@Body ReturnRequest request);

    @POST("/api/chat")
    Call<ChatResponse> sendChatMessage(@Body ChatRequest request);

    // 앱 무결성 검증 API
    @POST("/api/app/verify-integrity")
    Call<IntegrityResponse> checkIntegrity(@Body IntegrityRequest request);

    @GET("/api/auth/public-key")
    Call<PublicKeyResponse> getAuthPublicKey();

    @GET("/api/user/crypto/public-key")
    Call<PublicKeyResponse> getPublicKey();

    @POST("api/user/crypto/exchange-key")
    Call<Void> exchangeKeys(@Body ExchangeRequest request);

    @POST("/api/user/point/gift")
    Call<PointGiftResponse> giftPoint(@Body PointGiftRequest request);

    // -----------------------------------------------------------
    // [SSRF 취약점 테스트용 API] - (수정됨)
    // 명세서에 맞춰 GET 방식 + Query 파라미터로 변경
    // -----------------------------------------------------------
    @GET("/api/scrap")
    Call<LinkPreviewResponse> getLinkPreview(@Query("url") String url);

    //추가할 API 엔드포인트
    @GET("/api/security/challenge")
    Call<NonceResponse> getNonce();

    @POST("/api/security/verify")
    Call<IntegrityTokenResponse> verifyIntegrity(@Body IntegrityVerifyRequest request);

    @POST("/api/bike/rental")
    Call<Void> startBikeRental(@Body RentalRequest request);

}