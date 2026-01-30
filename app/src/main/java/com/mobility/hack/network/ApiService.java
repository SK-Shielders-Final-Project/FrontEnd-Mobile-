package com.mobility.hack.network;

import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
    // --- [1] 인증 및 회원관리 ---
    @POST("api/auth/register")
    Call<Void> register(@Body RegisterRequest request);

    @POST("/api/user/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("/api/user/info/{userId}")
    Call<UserInfoResponse> getUserInfo(@Path("userId") long userId);

    @POST("api/auth/verify-password")
    Call<Void> verifyPassword(@Body PasswordRequest request);

    // --- [2] 문의사항 처리 (백엔드 스웨거 명세 반영) ---

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
    @Multipart
    @POST("/api/files/upload")
    Call<FileUploadResponse> uploadFile(
            @Header("Authorization") String token,
            @Part MultipartBody.Part file
    );

    @GET("/api/user/files/download")
    @Streaming
    Call<ResponseBody> downloadFile(
            @Header("Authorization") String token,
            @Query("file") String filename
    );

    @GET("/api/bikes")
    Call<List<BikeResponse>> getBikes();

    @POST("api/chatbot/message")
    Call<ChatResponse> sendChatMessage(@Body ChatRequest request);
}
