package com.mobility.hack.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobility.hack.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;

    private List<Message> messageList;

    public ChatAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.isUser()) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_BOT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            ((UserViewHolder) holder).bind(message);
        } else {
            ((BotViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // User's message ViewHolder
    private static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        UserViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textMessage);
        }

        void bind(Message message) {
            messageText.setText(message.getText());
        }
    }

    // Bot's message ViewHolder
    private static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        BotViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textMessage);
        }

        void bind(Message message) {
            messageText.setText(message.getText());
        }
    }
}
