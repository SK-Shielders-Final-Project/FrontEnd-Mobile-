package com.mobility.hack.community;

import android.content.Intent;
import android.os.Bundle;
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
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.network.dto.InquiryResponse;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryListActivity extends AppCompatActivity {

    private RecyclerView rvInquiries;
    private InquiryAdapter adapter;
    private List<InquiryResponse> inquiryList = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_list);

        apiService = RetrofitClient.getApiService(((MainApplication) getApplication()).getTokenManager());

        rvInquiries = findViewById(R.id.rv_inquiries);
        rvInquiries.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InquiryAdapter(inquiryList);
        rvInquiries.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        loadInquiries();
    }

    private void loadInquiries() {
        String token = ((MainApplication) getApplication()).getTokenManager().fetchAuthToken();
        if (token == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getInquiries("Bearer " + token).enqueue(new Callback<List<InquiryResponse>>() {
            @Override
            public void onResponse(Call<List<InquiryResponse>> call, Response<List<InquiryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    inquiryList.clear();
                    inquiryList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(InquiryListActivity.this, "목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
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
            holder.tvStatus.setText("접수중"); // 상태값 바인딩
            holder.tvDate.setText(item.getCreatedAt()); // 날짜 바인딩

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(InquiryListActivity.this, InquiryDetailActivity.class);
                intent.putExtra("inquiry", item); // InquiryResponse 객체 전체를 전달
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
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvDate = itemView.findViewById(R.id.tv_date);
            }
        }
    }
}
