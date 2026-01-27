package com.example.mobilityhack.network; 

import com.example.mobilityhack.network.dto.PaymentHistoryItem;
import com.example.mobilityhack.network.dto.RentalRequest;
import com.example.mobilityhack.network.dto.RentalResponse;
import com.example.mobilityhack.network.dto.VoucherRequest;
import com.example.mobilityhack.network.dto.VoucherResponse;
import com.example.mobilityhack.network.dto.PaymentConfirmRequest;
import com.example.mobilityhack.network.dto.PaymentConfirmResponse;

import java.util.List; // List import 추가
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET; // GET import 추가
import retrofit2.http.POST;
import retrofit2.http.Path; // Path import 추가

public interface ApiService {

    @POST("/api/payment/confirm")
    Call<PaymentConfirmResponse> confirmPayment(@Body PaymentConfirmRequest request);

    @POST("/api/vouchers/redeem")
    Call<VoucherResponse> redeemVoucher(@Body VoucherRequest request);

    @POST("/api/rentals/start")
    Call<RentalResponse> rentBike(@Body RentalRequest request);

    /**
     * 특정 사용자의 모든 결제 내역을 조회합니다.
     */
    @GET("/api/payments/{userId}")
    Call<List<PaymentHistoryItem>> getPaymentHistory(@Path("userId") int userId);
}
