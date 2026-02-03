package com.mobility.hack.network;

import com.mobility.hack.GiftHistory;
import com.mobility.hack.UsageHistory;

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
import retrofit2.http.Query;

public interface ApiService {
    // ... (기존 로그인 관련 코드는 그대로 두세요) ...
    @POST("/api/user/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/api/auth/refresh") // /api/user/auth/refresh라고 되어있었음
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
    Call<UserInfoResponse> updateUserInfo(@Body UpdateUserRequest request); // 클래스명 확인 필요 (UpdateInfoRequest 인지 UpdateUserRequest 인지)

    @POST("/api/checkpw")
    Call<CheckPasswordResponse> checkPassword(@Body CheckPasswordRequest request);

    @PUT("/api/user/auth/changepw")
    Call<UserInfoResponse> changePassword(@Body ChangePasswordRequest request);

    // -----------------------------------------------------------
    // [수정 포인트 1] 상세 조회: 이름(Details) 맞추고, 토큰 파라미터 제거
    // -----------------------------------------------------------
    // ApiService 인터페이스 내부
    @GET("api/user/inquiry/{inquiry_id}")
    Call<InquiryDetailResponseDto> getInquiryDetails(@Path("inquiry_id") long inquiryId);

    // -----------------------------------------------------------
    // [수정 포인트 2] 삭제: 반환 타입을 InquiryDeleteResponse로 변경
    // -----------------------------------------------------------
    @POST("/api/user/inquiry/delete")
    Call<InquiryDeleteResponse> deleteInquiry(@Body InquiryDeleteRequest request);

    // [작성]
    @POST("api/user/inquiry/write")
    Call<InquiryResponse> writeInquiry(@Body InquiryWriteRequest request);

    // [목록]
    @GET("/api/user/inquiry")
    Call<List<InquiryResponse>> getInquiryList(@Query("user_id") long userId);

    // [수정] - CommonResultResponse가 있다면 유지, 없으면 InquiryModifyResponse로 변경
    @PUT("api/user/inquiry/modify")
    Call<InquiryModifyResponse> modifyInquiry(@Body InquiryModifyRequest request);

    // [파일 업로드]
    @Multipart
    @POST("api/files/upload")
    Call<FileUploadResponse> uploadFile(@Part MultipartBody.Part file);

    // ... (나머지 메서드들 유지) ...
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

    @POST("/api/chat")
    Call<ChatResponse> sendChatMessage(@Body ChatRequest request);

    // 앱 무결성 검증 API
    @POST("/api/app/verify-integrity")
    Call<IntegrityResponse> checkIntegrity(@Body IntegrityRequest request);

    @GET("/api/user/crypto/public-key")
    Call<PublicKeyResponse> getPublicKey();

    @POST("/api/user/crypto/exchange-key")
    Call<Void> exchangeKeys(@Body ExchangeRequest request);

    @POST("/api/user/point/gift")
    Call<PointGiftResponse> giftPoint(@Body PointGiftRequest request);
}
