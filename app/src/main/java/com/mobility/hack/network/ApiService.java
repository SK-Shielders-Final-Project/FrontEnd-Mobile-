package com.mobility.hack.network;

import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
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

    @GET("get_inquiries.php")
    Call<List<InquiryResponse>> getInquiries();

    @GET("download.php")
    Call<ResponseBody> downloadFile(@Query("filename") String filename);

    @POST("validate_token.php")
    Call<Void> validateToken(@Header("Authorization") String token);
}