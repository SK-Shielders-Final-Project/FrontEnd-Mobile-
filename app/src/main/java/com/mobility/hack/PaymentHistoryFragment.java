package com.mobility.hack;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mobility.hack.network.ApiService;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private PaymentHistoryAdapter adapter;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        recyclerView = view.findViewById(R.id.rv_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ((MainApplication) getActivity().getApplication()).getApiService();
        fetchPaymentHistory();
    }

    private void fetchPaymentHistory() {
        apiService.getPaymentHistory().enqueue(new Callback<List<Payment>>() {
            @Override
            public void onResponse(Call<List<Payment>> call, Response<List<Payment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter = new PaymentHistoryAdapter(response.body());
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(getContext(), "결제 내역을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Payment>> call, Throwable t) {
                Toast.makeText(getContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
