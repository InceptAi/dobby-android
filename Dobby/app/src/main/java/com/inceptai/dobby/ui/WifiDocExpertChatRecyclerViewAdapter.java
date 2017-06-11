package com.inceptai.dobby.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.inceptai.dobby.R;
import com.inceptai.dobby.expert.ExpertChat;
import com.inceptai.dobby.utils.DobbyLog;

import org.w3c.dom.Text;

import java.util.List;

public class WifiDocExpertChatRecyclerViewAdapter extends Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<ExpertChat> expertChatList;

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout fromExpertLayout;
        LinearLayout fromUserLayout;
        TextView userChatTv;
        TextView expertChatTv;

        public ChatViewHolder(View rootView) {
            super(rootView);
            fromExpertLayout = (LinearLayout) rootView.findViewById(R.id.wd_expert_chat_ll);
            fromUserLayout = (LinearLayout) rootView.findViewById(R.id.wd_user_chat_ll);
            userChatTv = (TextView) rootView.findViewById(R.id.wd_user_chat_tv);
            expertChatTv = (TextView) rootView.findViewById(R.id.wd_expert_chat_tv);
        }
    }

    public WifiDocExpertChatRecyclerViewAdapter(Context context, List<ExpertChat> expertChatList) {
        this.context = context;
        this.expertChatList = expertChatList;
    }

    public void addChatEntry(ExpertChat expertChatEntry) {
        expertChatList.add(expertChatEntry);
        notifyItemChanged(expertChatList.size() - 1);
    }

    public void clear() {
        expertChatList.clear();
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.expert_chat_message_item, parent, false);
        return new ChatViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ChatViewHolder chatViewHolder = (ChatViewHolder) holder;
        configureChatViewHolder(chatViewHolder, position);
    }

    @Override
    public int getItemCount() {
        return expertChatList.size();
    }

    private void configureChatViewHolder(ChatViewHolder viewHolder, int position) {
        if (position >= expertChatList.size()) {
            DobbyLog.e("Attempt to show chat entry index beyond adapter size.");
            return;
        }
        ExpertChat expertChat = expertChatList.get(position);
        if (ExpertChat.isExpertChat(expertChat)) {
            viewHolder.fromUserLayout.setVisibility(View.GONE);
            viewHolder.fromExpertLayout.setVisibility(View.VISIBLE);
            viewHolder.expertChatTv.setText(expertChat.getText());
        } else if (ExpertChat.isUserChat(expertChat)) {
            viewHolder.fromExpertLayout.setVisibility(View.GONE);
            viewHolder.fromUserLayout.setVisibility(View.VISIBLE);
            viewHolder.userChatTv.setText(expertChat.getText());
        }
    }
}
