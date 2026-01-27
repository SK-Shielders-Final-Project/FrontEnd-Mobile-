package com.example.mobilityhack.ride;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.example.mobilityhack.R;
import com.example.mobilityhack.ride.PaymentHistoryAdapter; // import 경로 변경
import com.example.mobilityhack.network.ApiService;
import com.example.mobilityhack.network.RetrofitClient;
import com.example.mobilityhack.network.dto.PaymentHistoryItem;
import com.example.mobilityhack.util.Constants;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserHistoryActivity extends AppCompatActivity {

    private static final String TAG = "UserHistoryActivity";
    private RecyclerView recyclerView;
    private PaymentHistoryAdapter adapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_history);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.history_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        apiService = RetrofitClient.getApiService(this);

        fetchPaymentHistory();
    }

    private void fetchPaymentHistory() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        int userId = prefs.getInt(Constants.KEY_USER_ID, -1);

        if (userId == -1) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getPaymentHistory(userId).enqueue(new Callback<List<PaymentHistoryItem>>() {
            @Override
            public void onResponse(Call<List<PaymentHistoryItem>> call, Response<List<PaymentHistoryItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter = new PaymentHistoryAdapter(response.body());
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(UserHistoryActivity.this, "내역을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<PaymentHistoryItem>> call, Throwable t) {
                Toast.makeText(UserHistoryActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to fetch payment history", t);
            }
        });
    }
}
