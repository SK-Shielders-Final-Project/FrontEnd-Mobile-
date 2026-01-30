package com.mobility.hack.community;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mobility.hack.R;
import com.mobility.hack.network.InquiryResponse;
import java.util.List;

public class InquiryAdapter extends RecyclerView.Adapter<InquiryAdapter.ViewHolder> {
    private List<InquiryResponse> inquiryList;

    public InquiryAdapter(List<InquiryResponse> inquiryList) {
        this.inquiryList = inquiryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inquiry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InquiryResponse item = inquiryList.get(position);
        
        holder.tvTitle.setText(item.getTitle());
        holder.tvDate.setText(item.getCreatedAt());
        
        String reply = item.getAdminReply();
        if (reply == null || reply.trim().isEmpty()) {
            holder.tvStatus.setText("접수중");
            holder.tvStatus.setTextColor(Color.parseColor("#888888"));
        } else {
            holder.tvStatus.setText("답변완료");
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
        }

        // 아이템 클릭 시 상세 페이지로 이동하며 객체 전달
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), InquiryDetailActivity.class);
            intent.putExtra("inquiry_data", item); 
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return inquiryList.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvStatus;
        ViewHolder(View v) {
            super(v);
            // [1] 리소스 ID 수정: item_inquiry.xml의 실제 ID와 매칭
            tvTitle = v.findViewById(R.id.tv_title);
            tvDate = v.findViewById(R.id.tv_date);
            tvStatus = v.findViewById(R.id.tv_item_status);
        }
    }
}
