package com.inceptai.wifiexpertsystem.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.inceptai.wifiexpertsystem.R;


/**
 * Created by arunesh on 3/29/17.
 */

public class ExpertChatViewHolder extends RecyclerView.ViewHolder {
    private TextView expertInitialsTv;
    private TextView expertChatTv;


    public ExpertChatViewHolder(View itemView) {
        super(itemView);
        expertInitialsTv = (TextView) itemView.findViewById(R.id.expert_initials_tv);
        expertChatTv = (TextView) itemView.findViewById(R.id.expert_text_tv);
    }

    public TextView getInitialsTextView() {
        return expertInitialsTv;
    }

    public TextView getExpertChatTextView() {
        return expertChatTv;
    }
}
