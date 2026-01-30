package com.mobility.hack.community;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.security.TokenManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryListActivity extends AppCompatActivity {
    private static final String TAG = "InquiryList";
    private RecyclerView rvInquiries;
    private InquiryAdapter adapter;
    private List<InquiryResponse> inquiryList = new ArrayList<>(); // 메모리 주소 고정
    private TokenManager tokenManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_list);

        // 초기화
        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        // 리사이클러뷰 설정
        rvInquiries = findViewById(R.id.rv_inquiry_list);
        rvInquiries.setLayoutManager(new LinearLayoutManager(this));

        // 어댑터 생성 및 연결 (빈 리스트 먼저 연결)
        adapter = new InquiryAdapter(inquiryList);
        rvInquiries.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면에 들어올 때마다 목록을 새로고침합니다.
        loadInquiryList();
    }

    private void loadInquiryList() {
        String authToken = tokenManager.fetchAuthToken();
        long userId = tokenManager.fetchUserId();

        if (authToken == null || userId <= 0) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = "Bearer " + authToken;
        Log.d(TAG, "Request UserID: " + userId);

        // 수정된 API 호출 (Map을 쓰지 않고 userId만 보냄)
        apiService.getInquiryList(token, userId).enqueue(new Callback<List<InquiryResponse>>() {
            @Override
            public void onResponse(Call<List<InquiryResponse>> call, Response<List<InquiryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    inquiryList.clear();
                    inquiryList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "Success: " + inquiryList.size() + " items");
                } else {
                    // 500이나 405 에러가 여기서 찍혔을 것입니다.
                    Log.e(TAG, "Error Code: " + response.code());
                    Toast.makeText(InquiryListActivity.this, "목록 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<InquiryResponse>> call, Throwable t) {
                Log.e(TAG, "Network Error: " + t.getMessage());
            }
        });
    }
}