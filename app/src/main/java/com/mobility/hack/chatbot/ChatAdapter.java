package com.mobility.hack.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobility.hack.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<ChatMessage> chatList = new ArrayList<>();

    public void addMessage(ChatMessage message) {
        chatList.add(message);
        notifyItemInserted(chatList.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return chatList.get(position).getViewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessage.VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatList.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).textMessage.setText(message.getMessage());
            ((UserViewHolder) holder).textTime.setText(message.getTime());
        } else {
            ((BotViewHolder) holder).textMessage.setText(message.getMessage());
        }
    }

    @Override
    public int getItemCount() { return chatList.size(); }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;
        UserViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        BotViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
        }
    }
}