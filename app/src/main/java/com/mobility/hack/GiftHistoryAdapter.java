package com.mobility.hack;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobility.hack.security.TokenManager;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;

public class GiftHistoryAdapter extends RecyclerView.Adapter<GiftHistoryAdapter.ViewHolder> {

    private final List<GiftHistory> giftHistoryList;
    private final String currentUsername;

    private final DateTimeFormatter parser = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true) // Allow up to 9 fractional digits
            .optionalEnd()
            .toFormatter();

    public GiftHistoryAdapter(List<GiftHistory> giftHistoryList, String currentUsername) {
        this.giftHistoryList = giftHistoryList;
        this.currentUsername = currentUsername;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gift_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GiftHistory history = giftHistoryList.get(position);
        boolean isSender = history.getSenderName().equals(currentUsername);

        DecimalFormat formatter = new DecimalFormat("###,###P");
        String formattedAmount = formatter.format(history.getAmount());

        if (isSender) {
            holder.description.setText(String.format(Locale.getDefault(), "%s님에게 선물", history.getReceiverName()));
            holder.amount.setText("-" + formattedAmount);
            holder.amount.setTextColor(Color.BLUE);
        } else {
            holder.description.setText(String.format(Locale.getDefault(), "%s님으로부터 선물", history.getSenderName()));
            holder.amount.setText("+" + formattedAmount);
            holder.amount.setTextColor(Color.RED);
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(history.getCreatedAt(), parser);
            DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            holder.date.setText(dateTime.format(displayFormatter));
        } catch (Exception e) {
            holder.date.setText("날짜 형식 오류");
        }
    }

    @Override
    public int getItemCount() {
        return giftHistoryList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView description;
        TextView date;
        TextView amount;

        ViewHolder(View itemView) {
            super(itemView);
            description = itemView.findViewById(R.id.tv_gift_description);
            date = itemView.findViewById(R.id.tv_gift_date);
            amount = itemView.findViewById(R.id.tv_gift_amount);
        }
    }
}
