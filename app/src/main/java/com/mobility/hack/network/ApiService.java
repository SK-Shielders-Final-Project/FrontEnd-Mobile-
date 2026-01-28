package com.mobility.hack.network;

import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @Multipart
    @POST("upload_inquiry.php")
    Call<InquiryResponse> uploadInquiry(
        @Part("title") RequestBody title,
        @Part("content") RequestBody content,
        @Part MultipartBody.Part file
    );

    @POST("/api/user/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/api/user/auth/signup")
    Call<RegisterResponse> signup(@Body Map<String, Object> request);

    // [취약점 적용] Host 헤더 인젝션을 위해 Host 헤더를 파라미터로 받고, 엔드포인트 및 요청 데이터 변경
    @POST("/api/auth/password-reset/request")
    Call<Void> requestPasswordReset(@Header("Host") String host, @Body Map<String, String> emailPayload);

    // [취약점 적용] 비밀번호 재설정 실행을 위한 새로운 엔드포인트 추가
    @POST("/api/auth/password-reset/reset")
    Call<Void> resetPassword(@Body Map<String, String> resetPayload);
    @GET("get_inquiries.php")
    Call<List<InquiryResponse>> getInquiries();

    @GET("download.php")
    Call<ResponseBody> downloadFile(@Query("filename") String filename);
    @GET("/api/user/info/{userId}")
    Call<RegisterResponse> getUserInfo(@Path("userId") long userId);

    @POST("validate_token.php")
    Call<Void> validateToken(@Header("Authorization") String token);
    @PUT("/api/user/info")
    Call<UpdateInfoResponse> updateUserInfo(@Body UpdateInfoRequest request);

    @POST("/api/checkpw")
    Call<CheckPasswordResponse> checkPassword(@Body CheckPasswordRequest request);

    @PUT("/api/user/auth/changepw")
    Call<Void> changePassword(@Body ChangePasswordRequest request);

    // 사용되지 않는 findPassword 메소드는 주석 처리 또는 삭제
    // @PUT("/api/user/auth/findpw")
    // Call<Void> findPassword(@Body FindPasswordRequest request);
}
