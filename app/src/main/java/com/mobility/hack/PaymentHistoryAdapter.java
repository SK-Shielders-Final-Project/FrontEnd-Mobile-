package com.mobility.hack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.PaymentViewHolder> {

    private final List<Payment> payments;

    public PaymentHistoryAdapter(List<Payment> payments) {
        this.payments = payments;
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_history, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        Payment payment = payments.get(position);
        holder.amountText.setText(String.format("%,d원", payment.getAmount()));
        holder.paymentMethodText.setText("결제 수단: " + payment.getPaymentMethod());
        holder.statusText.setText("상태: " + payment.getStatus());
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        TextView amountText;
        TextView paymentMethodText;
        TextView statusText;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            amountText = itemView.findViewById(R.id.amount_text);
            paymentMethodText = itemView.findViewById(R.id.payment_method_text);
            statusText = itemView.findViewById(R.id.status_text);
        }
    }
}
