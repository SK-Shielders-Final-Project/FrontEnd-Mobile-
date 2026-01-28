package com.mobility.hack.community;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.network.RetrofitClient;
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
    private List<InquiryResponse> inquiryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_list);

        rvInquiries = findViewById(R.id.rv_inquiry_list);
        rvInquiries.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new InquiryAdapter(inquiryList, item -> {
            // [3] 클릭 시 상세 화면으로 이동하며 ID 전달
            Intent intent = new Intent(this, InquiryDetailActivity.class);
            intent.putExtra("inquiry_id", item.getInquiryId());
            startActivity(intent);
        });
        rvInquiries.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        loadInquiries();
    }

    /**
     * [3] 내 아이디로 문의 목록 요청 (POST /api/user/inquiry)
     */
    private void loadInquiries() {
        long userId = 0L;
        if (MainApplication.getTokenManager() != null) {
            userId = MainApplication.getTokenManager().fetchUserId();
        }

        if (userId == 0L) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Map<String, Long> requestMap = new HashMap<>();
        requestMap.put("user_id", userId);

        apiService.getInquiryList(requestMap).enqueue(new Callback<List<InquiryResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<InquiryResponse>> call, @NonNull Response<List<InquiryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    inquiryList.clear();
                    inquiryList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e(TAG, "목록 로드 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<InquiryResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "API Error: " + t.getMessage());
                Toast.makeText(InquiryListActivity.this, "서버 연결 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class InquiryAdapter extends RecyclerView.Adapter<InquiryAdapter.ViewHolder> {
        private List<InquiryResponse> items;
        private OnItemClickListener listener;

        public interface OnItemClickListener { void onItemClick(InquiryResponse item); }

        public InquiryAdapter(List<InquiryResponse> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inquiry, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            InquiryResponse item = items.get(position);
            holder.tvTitle.setText(item.getTitle());
            holder.tvStatus.setText("접수중"); 
            holder.tvDate.setText(item.getCreatedAt() != null ? item.getCreatedAt().split("T")[0] : "");
            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvStatus, tvDate;
            ViewHolder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tv_item_title);
                tvStatus = v.findViewById(R.id.tv_item_status);
                tvDate = v.findViewById(R.id.tv_item_date);
            }
        }
    }
}
