package com.example.mobilityhack.ride; // 패키지 위치 변경

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilityhack.R;
import com.example.mobilityhack.network.dto.PaymentHistoryItem;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder> {

    private List<PaymentHistoryItem> historyItems;

    public PaymentHistoryAdapter(List<PaymentHistoryItem> historyItems) {
        this.historyItems = historyItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentHistoryItem item = historyItems.get(position);

        String formattedAmount = NumberFormat.getCurrencyInstance(Locale.KOREA).format(item.getAmount());
        holder.amountText.setText(formattedAmount);
        holder.orderIdText.setText("주문 ID: " + item.getOrderId());
        holder.statusText.setText("상태: " + item.getStatus());
    }

    @Override
    public int getItemCount() {
        return historyItems == null ? 0 : historyItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView amountText;
        TextView orderIdText;
        TextView statusText;

        ViewHolder(View itemView) {
            super(itemView);
            amountText = itemView.findViewById(R.id.amount_text);
            orderIdText = itemView.findViewById(R.id.order_id_text);
            statusText = itemView.findViewById(R.id.status_text);
        }
    }
}
