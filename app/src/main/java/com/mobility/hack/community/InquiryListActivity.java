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

        Map<String, Long> params = new HashMap<>();
        params.put("user_id", userId);

        String token = "Bearer " + authToken;
        Log.d(TAG, "Request: " + params.toString()); // 요청 데이터 확인 로그

        apiService.getInquiryList(token, params).enqueue(new Callback<List<InquiryResponse>>() {
            @Override
            public void onResponse(Call<List<InquiryResponse>> call, Response<List<InquiryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 데이터 갱신 로직
                    inquiryList.clear();
                    inquiryList.addAll(response.body());
                    adapter.notifyDataSetChanged(); // UI 업데이트 강제 실행

                    Log.d(TAG, "Response Success: " + inquiryList.size() + " items");

                    if (inquiryList.isEmpty()) {
                        Toast.makeText(InquiryListActivity.this, "등록된 문의 내역이 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Error Code: " + response.code());
                    Toast.makeText(InquiryListActivity.this, "목록을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<InquiryResponse>> call, Throwable t) {
                Log.e(TAG, "Network Error: " + t.getMessage());
                Toast.makeText(InquiryListActivity.this, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}