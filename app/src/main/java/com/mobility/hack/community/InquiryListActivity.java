package com.mobility.hack.community;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
// [중요] 실제 InquiryResponse.java가 있는 정확한 패키지 경로 하나만 사용하세요.
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.security.TokenManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryListActivity extends AppCompatActivity {
    private static final String TAG = "InquiryList";
    private RecyclerView rvInquiries;
    private InquiryAdapter adapter;
    private List<InquiryResponse> inquiryList = new ArrayList<>();
    private TokenManager tokenManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_list);

        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        rvInquiries = findViewById(R.id.rv_inquiry_list);
        rvInquiries.setLayoutManager(new LinearLayoutManager(this));

        adapter = new InquiryAdapter(inquiryList);
        rvInquiries.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
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

        apiService.getInquiryList(userId).enqueue(new Callback<List<InquiryResponse>>() {
            @Override
            public void onResponse(Call<List<InquiryResponse>> call, Response<List<InquiryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    inquiryList.clear();
                    inquiryList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(InquiryListActivity.this, "목록을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<InquiryResponse>> call, Throwable t) {
                Toast.makeText(InquiryListActivity.this, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}