package com.inceptai.dobby.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.inceptai.dobby.R;

/**
 * Created by arunesh on 3/29/17.
 */

public class DobbyChatViewHolder extends RecyclerView.ViewHolder {
    private TextView dobbyInitialsTv;
    private TextView dobbyChatTv;

    public DobbyChatViewHolder(View itemView) {
        super(itemView);
        dobbyInitialsTv = (TextView) itemView.findViewById(R.id.dobbyInitialsTv);
        dobbyChatTv = (TextView) itemView.findViewById(R.id.dobbyTextTv);
    }

    public TextView getInitialsTextView() {
        return dobbyInitialsTv;
    }

    public TextView getDobbyChatTextView() {
        return dobbyChatTv;
    }
}
