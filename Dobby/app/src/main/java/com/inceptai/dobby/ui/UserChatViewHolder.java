package com.inceptai.dobby.ui;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.TextView;

import com.inceptai.dobby.R;

/**
 * Created by arunesh on 3/29/17.
 */

public class UserChatViewHolder extends ViewHolder {
    private TextView userChatTv;
    private TextView userInitialsTv;

    public UserChatViewHolder(View itemView) {
        super(itemView);
        userChatTv = (TextView) itemView.findViewById(R.id.user_text_tv);
        userInitialsTv = (TextView) itemView.findViewById(R.id.user_initials_tv);
    }

    public TextView getUserChatTv() {
        return userChatTv;
    }

    public TextView getUserInitialsTv() {
        return userInitialsTv;
    }
}
