package com.mobility.hack;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class HistoryFragmentAdapter extends FragmentStateAdapter {

    public HistoryFragmentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new PaymentHistoryFragment(); // 결제 내역
            case 1:
                return new UsageHistoryFragment();   // 이용 내역
            case 2:
                return new GiftHistoryFragment();    // 선물 내역
            default:
                return new PaymentHistoryFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // 탭 개수
    }
}
