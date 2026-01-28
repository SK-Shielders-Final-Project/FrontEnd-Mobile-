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
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.network.RetrofitClient;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryListActivity extends AppCompatActivity {

    private RecyclerView rvInquiries;
    private InquiryAdapter adapter;
    private List<InquiryResponse> inquiryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_list);

        // [3] 리소스 ID 수정: rv_inquiry_list -> rv_inquiries (XML과 일치시킴)
        rvInquiries = findViewById(R.id.rv_inquiries);
        rvInquiries.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InquiryAdapter(inquiryList);
        rvInquiries.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        loadInquiries();
    }

    private void loadInquiries() {
        // [1] RetrofitClient 호출부 수정: 인자 제거
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        apiService.getInquiryList().enqueue(new Callback<List<InquiryResponse>>() {
            @Override
            public void onResponse(Call<List<InquiryResponse>> call, Response<List<InquiryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    inquiryList.clear();
                    inquiryList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e("API_ERROR", "목록 로드 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<InquiryResponse>> call, Throwable t) {
                Toast.makeText(InquiryListActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class InquiryAdapter extends RecyclerView.Adapter<InquiryAdapter.ViewHolder> {
        private List<InquiryResponse> items;

        InquiryAdapter(List<InquiryResponse> items) {
            this.items = items;
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
            
            String rawDate = item.getCreatedAt();
            if (rawDate != null && rawDate.contains("T")) {
                holder.tvDate.setText(rawDate.split("T")[0]);
            } else {
                holder.tvDate.setText(rawDate != null ? rawDate : "2026-01-27");
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(InquiryListActivity.this, InquiryDetailActivity.class);
                intent.putExtra("inquiry_id", item.getInquiryId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvStatus, tvDate;

            ViewHolder(View itemView) {
                super(itemView);
                // [3] 리소스 ID 수정: item_inquiry.xml의 실제 ID와 매칭
                tvTitle = itemView.findViewById(R.id.tv_item_title);
                tvStatus = itemView.findViewById(R.id.tv_item_status);
                tvDate = itemView.findViewById(R.id.tv_item_date);
            }
        }
    }
}