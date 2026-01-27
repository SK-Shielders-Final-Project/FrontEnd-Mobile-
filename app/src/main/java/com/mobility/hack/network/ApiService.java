package com.mobility.hack.network;

import com.example.mobilityhack.network.dto.RentalRequest;
import com.example.mobilityhack.network.dto.RentalResponse;
import com.example.mobilityhack.network.dto.VoucherRequest;
import com.example.mobilityhack.network.dto.VoucherResponse;
import com.example.mobilityhack.network.dto.PaymentConfirmRequest;
import com.example.mobilityhack.network.dto.PaymentConfirmResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("/api/payment/confirm")
    Call<PaymentConfirmResponse> confirmPayment(@Body PaymentConfirmRequest request);

    @POST("/api/vouchers/redeem")
    Call<VoucherResponse> redeemVoucher(@Body VoucherRequest request);

    @POST("/api/rentals/start")
    Call<RentalResponse> rentBike(@Body RentalRequest request);
}
