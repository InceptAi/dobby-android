package com.inceptai.dobby.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.inceptai.dobby.R;

import static com.inceptai.dobby.utils.Utils.nonLinearBwScale;

/**
 * Created by arunesh on 5/22/17.
 */

public class BandwidthResultsCardViewHolder extends RecyclerView.ViewHolder {
    private CircularGauge downloadCircularGauge;
    private TextView downloadGaugeTv;
    private TextView downloadGaugeTitleTv;

    private CircularGauge uploadCircularGauge;
    private TextView uploadGaugeTv;
    private TextView uploadGaugeTitleTv;


    public BandwidthResultsCardViewHolder(View itemView) {
        super(itemView);
        fetchViewInstances(itemView);
    }

    private void fetchViewInstances(View rootView) {
        View downloadView = rootView.findViewById(R.id.cg_download);
        downloadCircularGauge = (CircularGauge) downloadView.findViewById(R.id.bw_gauge);
        downloadGaugeTv = (TextView) downloadView.findViewById(R.id.gauge_tv);
        downloadGaugeTitleTv = (TextView) downloadView.findViewById(R.id.title_tv);
        downloadGaugeTitleTv.setText(R.string.download_bw);

        View uploadView = rootView.findViewById(R.id.cg_upload);
        uploadCircularGauge = (CircularGauge) uploadView.findViewById(R.id.bw_gauge);
        uploadGaugeTv = (TextView) uploadView.findViewById(R.id.gauge_tv);
        uploadGaugeTitleTv = (TextView) uploadView.findViewById(R.id.title_tv);
        uploadGaugeTitleTv.setText(R.string.upload_bw);
    }

    public void showResults(double uploadMbps, double downloadMbps) {
        uploadCircularGauge.setValue((int) nonLinearBwScale(uploadMbps));
        uploadGaugeTv.setText(String.format("%2.2f", uploadMbps));
        downloadCircularGauge.setValue((int) nonLinearBwScale(downloadMbps));
        downloadGaugeTv.setText(String.format("%2.2f", downloadMbps));
    }
}
