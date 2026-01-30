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
import retrofit2.http.Query;

public interface ApiService {
    @POST("/api/user/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/api/user/auth/refresh")
    Call<LoginResponse> refresh(@Body RefreshRequest request);

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
    Call<UserInfoResponse> updateUserInfo(@Body UpdateUserRequest request);

    @POST("/api/checkpw")
    Call<CheckPasswordResponse> checkPassword(@Body CheckPasswordRequest request);

    @PUT("/api/user/auth/changepw")
    Call<UserInfoResponse> changePassword(@Body ChangePasswordRequest request);

    @GET("/api/inquiries")
    Call<List<InquiryResponse>> getInquiries(@Header("Authorization") String token);

    @Multipart
    @POST("/api/inquiries")
    Call<InquiryResponse> uploadInquiry(@Header("Authorization") String token, @PartMap Map<String, RequestBody> partMap, @Part MultipartBody.Part file);

    @Streaming
    @POST("/api/coupon/redeem")
    Call<VoucherResponse> redeemVoucher(@Body VoucherRequest request);

    @POST("/api/payments/user/confirm")
    Call<PaymentResponse> confirmPayment(@Body PaymentRequest request);

    @GET("/api/payments/user")
    Call<List<com.mobility.hack.Payment>> getPaymentHistory();

    @POST("/api/user/point")
    Call<PointResponse> usePoint(@Body PointRequest request);
    // 목록 조회: POST /api/user/inquiry (Body에 user_id 포함)
    @POST("/api/user/inquiry")
    Call<List<InquiryResponse>> getInquiryList(
            @Header("Authorization") String token,
            @Body Map<String, Long> params
    );
    // 상세 조회: GET /api/user/inquiry/{inquiryId}
    @GET("/api/user/inquiry/{inquiryId}")
    Call<InquiryResponse> getInquiryDetail(
            @Header("Authorization") String token,
            @Path("inquiryId") long inquiryId
    );
    // 문의 작성: POST /api/user/inquiry/write
    @POST("/api/user/inquiry/write")
    Call<InquiryResponse> writeInquiry(
            @Header("Authorization") String token,
            @Header("User-ID") String userIdHeader,
            @Body InquiryWriteRequest request
    );
    // 문의 수정: PUT /api/user/inquiry/modify
    @PUT("/api/user/inquiry/modify")
    Call<CommonResultResponse> modifyInquiry(
            @Header("Authorization") String token,
            @Body InquiryModifyRequest request
    );
    // 문의 삭제: POST /api/user/inquiry/delete
    @POST("/api/user/inquiry/delete")
    Call<CommonResultResponse> deleteInquiry(
            @Header("Authorization") String token,
            @Body InquiryDeleteRequest request
    );

    // --- [3] 파일 및 기타 서비스 ---
    @POST("/api/files/upload")
    Call<FileUploadResponse> uploadFile(
            @Header("Authorization") String token,
            @Part MultipartBody.Part file
    );
    @GET("/api/user/files/download")
    Call<ResponseBody> downloadFile(
            @Header("Authorization") String token,
            @Query("file") String filename
    );

    // 자전거 목록 불러오기
    @POST("/api/bikes")
    Call<List<BikeResponse>> getBikes();

    // 챗봇
    @POST("/api/chat")
    Call<ChatResponse> sendChatMessage(@Body ChatRequest request);

    // 사용되지 않는 findPassword 메소드는 주석 처리 또는 삭제
    // @PUT("/api/user/auth/findpw")
    // Call<Void> findPassword(@Body FindPasswordRequest request);
}
