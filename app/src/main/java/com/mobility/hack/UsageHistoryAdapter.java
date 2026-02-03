package com.mobility.hack;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;

public class UsageHistoryAdapter extends RecyclerView.Adapter<UsageHistoryAdapter.ViewHolder> {

    private static final String TAG = "UsageHistoryAdapter";
    private final List<UsageHistory> usageHistoryList;

    // Create a flexible parser once
    private final DateTimeFormatter parser = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            // Handle optional fractional seconds from 1 to 6 digits
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 6, true)
            .optionalEnd()
            .toFormatter();

    public UsageHistoryAdapter(List<UsageHistory> usageHistoryList) {
        this.usageHistoryList = usageHistoryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usage_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UsageHistory history = usageHistoryList.get(position);

        holder.bikeInfo.setText(String.format(Locale.getDefault(), "%d번 bike", history.getBikeId()));

        try {
            LocalDateTime startTime = LocalDateTime.parse(history.getStartTime(), parser);
            LocalDateTime endTime = LocalDateTime.parse(history.getEndTime(), parser);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            holder.usagePeriod.setText(String.format("%s ~ %s", startTime.format(formatter), endTime.format(formatter)));

            Duration duration = Duration.between(startTime, endTime);
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;

            if (hours > 0) {
                holder.duration.setText(String.format(Locale.getDefault(), "%d시간 이용", hours));
            } else {
                holder.duration.setText(String.format(Locale.getDefault(), "%d분 이용", minutes));
            }

        } catch (DateTimeParseException e) {
            Log.e(TAG, "Failed to parse date: " + history.getStartTime() + " or " + history.getEndTime(), e);
            holder.usagePeriod.setText("날짜 형식 오류");
            holder.duration.setText("-");
        }
    }

    @Override
    public int getItemCount() {
        return usageHistoryList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView bikeInfo;
        TextView usagePeriod;
        TextView duration;

        ViewHolder(View itemView) {
            super(itemView);
            bikeInfo = itemView.findViewById(R.id.tv_bike_info);
            usagePeriod = itemView.findViewById(R.id.tv_usage_period);
            duration = itemView.findViewById(R.id.tv_duration);
        }
    }
}
