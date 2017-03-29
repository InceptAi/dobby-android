package com.inceptai.dobby.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;



public class RecyclerViewSimpleTextViewHolder extends RecyclerView.ViewHolder {

    private TextView label1;


    public RecyclerViewSimpleTextViewHolder(View v) {
        super(v);
        label1 = (TextView) v.findViewById(android.R.id.text1);
    }

    public TextView getLabel() {
        return label1;
    }

    public void setLabel1(TextView label1) {
        this.label1 = label1;
    }
}