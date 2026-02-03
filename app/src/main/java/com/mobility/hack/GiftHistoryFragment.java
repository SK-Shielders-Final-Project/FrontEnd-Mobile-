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
import com.mobility.hack.network.UserInfoResponse;
import com.mobility.hack.security.TokenManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GiftHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private GiftHistoryAdapter adapter;
    private ApiService apiService;
    private TokenManager tokenManager;

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
        tokenManager = ((MainApplication) getActivity().getApplication()).getTokenManager();
        fetchCurrentUsernameAndHistory();
    }

    private void fetchCurrentUsernameAndHistory() {
        // ✨ [수정] TokenManager에서 userId를 가져와서 API를 호출합니다.
        long userId = tokenManager.fetchUserId();
        if (userId == 0L) {
            Toast.makeText(getContext(), "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getUserInfoById(userId).enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String currentUsername = response.body().getName();
                    fetchGiftHistory(currentUsername);
                } else {
                    Toast.makeText(getContext(), "사용자 정보 조회 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                Toast.makeText(getContext(), "사용자 정보 조회 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchGiftHistory(String currentUsername) {
        apiService.getGiftHistory().enqueue(new Callback<List<GiftHistory>>() {
            @Override
            public void onResponse(Call<List<GiftHistory>> call, Response<List<GiftHistory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter = new GiftHistoryAdapter(response.body(), currentUsername);
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(getContext(), "선물 내역을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<GiftHistory>> call, Throwable t) {
                Toast.makeText(getContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
