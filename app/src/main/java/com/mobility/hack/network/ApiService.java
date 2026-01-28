package com.mobility.hack.network;

import com.mobility.hack.network.dto.ChangePasswordRequest;
import com.mobility.hack.network.dto.CheckPasswordRequest;
import com.mobility.hack.network.dto.CheckPasswordResponse;
import com.mobility.hack.network.dto.InquiryResponse;
import com.mobility.hack.network.dto.LoginRequest;
import com.mobility.hack.network.dto.LoginResponse;
import com.mobility.hack.network.dto.PaymentConfirmRequest;
import com.mobility.hack.network.dto.PaymentConfirmResponse;
import com.mobility.hack.network.dto.PaymentHistoryItem;
import com.mobility.hack.network.dto.RegisterResponse;
import com.mobility.hack.network.dto.RentalRequest;
import com.mobility.hack.network.dto.RentalResponse;
import com.mobility.hack.network.dto.UpdateInfoRequest;
import com.mobility.hack.network.dto.UpdateInfoResponse;
import com.mobility.hack.network.dto.VoucherRequest;
import com.mobility.hack.network.dto.VoucherResponse;
import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("api/auth/signup")
    Call<RegisterResponse> signup(@Body Map<String, Object> request);

    @POST("api/rentals/start")
    Call<RentalResponse> rentBike(@Body RentalRequest request);

    @POST("api/vouchers/redeem")
    Call<VoucherResponse> redeemVoucher(@Body VoucherRequest request);

    @GET("api/users/me")
    Call<RegisterResponse> getUserInfo(@Header("Authorization") String token);

    @PUT("api/users/me")
    Call<UpdateInfoResponse> updateUserInfo(@Header("Authorization") String token, @Body UpdateInfoRequest request);

    @POST("api/auth/change-password")
    Call<Void> changePassword(@Header("Authorization") String token, @Body ChangePasswordRequest request);

    @POST("api/auth/check-password")
    Call<CheckPasswordResponse> checkPassword(@Header("Authorization") String token, @Body CheckPasswordRequest request);

    @POST("api/auth/request-password-reset")
    Call<Void> requestPasswordReset(@Body Map<String, String> payload);

    @POST("api/auth/reset-password")
    Call<Void> resetPassword(@Body Map<String, String> payload);

    @GET("api/inquiries")
    Call<List<InquiryResponse>> getInquiries(@Header("Authorization") String token);

    @Multipart
    @POST("api/inquiries")
    Call<InquiryResponse> uploadInquiry(@Header("Authorization") String token, @PartMap Map<String, RequestBody> partMap, @Part MultipartBody.Part file);

    @GET("api/inquiries/download/{filename}")
    Call<ResponseBody> downloadFile(@Header("Authorization") String token, @Path("filename") String filename);
    
    @GET("api/payments/history")
    Call<List<PaymentHistoryItem>> getPaymentHistory(@Header("Authorization") String token);
    
    @POST("api/payment/confirm")
    Call<PaymentConfirmResponse> confirmPayment(@Header("Authorization") String token, @Body PaymentConfirmRequest request);
}
