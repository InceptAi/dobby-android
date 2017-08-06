package com.inceptai.wifiexpert.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.inceptai.wifiexpert.R;


/**
 * Created by arunesh on 3/29/17.
 */

public class ExpertChatViewHolder extends RecyclerView.ViewHolder {
    private TextView expertInitialsTv;
    private TextView expertChatTv;


    public ExpertChatViewHolder(View itemView) {
        super(itemView);
        expertInitialsTv = (TextView) itemView.findViewById(R.id.expertInitialsTv);
        expertChatTv = (TextView) itemView.findViewById(R.id.expertTextTv);
    }

    public TextView getInitialsTextView() {
        return expertInitialsTv;
    }

    public TextView getExpertChatTextView() {
        return expertChatTv;
    }
}
