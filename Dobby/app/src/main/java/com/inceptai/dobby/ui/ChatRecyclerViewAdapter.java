package com.inceptai.dobby.ui;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.inceptai.dobby.ChatEntry;
import com.inceptai.dobby.R;

import java.util.List;

/**
 * Created by arunesh on 3/28/17.
 */

public class ChatRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatEntry> entryList;
    private Context context;

    public ChatRecyclerViewAdapter(Context context, List<ChatEntry> entryList) {
        this.context = context;
        this.entryList = entryList;
    }

    @Override
    public int getItemViewType(int position) {
        ChatEntry entry = entryList.get(position);
        return entry.getEntryType();
    }

    public void addEntryAtBottom(ChatEntry entry) {
        entryList.add(entry);
        notifyItemChanged(entryList.size() - 1);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == ChatEntry.DOBBY_CHAT) {
            View v = inflater.inflate(R.layout.dobby_chat, parent, false);
            viewHolder = new DobbyChatViewHolder(v);
        } else if (viewType == ChatEntry.USER_CHAT) {
            View v = inflater.inflate(R.layout.user_chat, parent, false);
            viewHolder = new UserChatViewHolder(v);
        } else {
            View v = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            viewHolder = new RecyclerViewSimpleTextViewHolder(v);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      switch (holder.getItemViewType()) {
          case ChatEntry.DOBBY_CHAT:
              DobbyChatViewHolder vh1 = (DobbyChatViewHolder) holder;
              configureDobbyViewHolder(vh1, position);
              break;
          case ChatEntry.USER_CHAT:
              UserChatViewHolder vh2 = (UserChatViewHolder) holder;
              configureUserViewHolder(vh2, position);
              break;
          default:
              RecyclerViewSimpleTextViewHolder vh = (RecyclerViewSimpleTextViewHolder) holder;
              configureDefaultViewHolder(vh, position);
              break;
      }
    }

    private void configureDefaultViewHolder(RecyclerViewSimpleTextViewHolder holder, int position) {
        holder.getLabel().setText("This is a dummy message");
    }

    private void configureDobbyViewHolder(DobbyChatViewHolder dobbyChatViewHolder, int position) {
        dobbyChatViewHolder.getDobbyChatTextView().setText(entryList.get(position).getText());
    }

    private void configureUserViewHolder(UserChatViewHolder userChatViewHolder, int position) {
        userChatViewHolder.getUserChatTv().setText(entryList.get(position).getText());
    }

    @Override
    public int getItemCount() {
        return entryList.size();
    }
}
