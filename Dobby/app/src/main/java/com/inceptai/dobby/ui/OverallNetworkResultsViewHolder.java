package com.inceptai.dobby.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.utils.Utils;

import static com.inceptai.dobby.ai.DataInterpreter.MetricType.ABYSMAL;
import static com.inceptai.dobby.ai.DataInterpreter.MetricType.AVERAGE;
import static com.inceptai.dobby.ai.DataInterpreter.MetricType.EXCELLENT;
import static com.inceptai.dobby.ai.DataInterpreter.MetricType.GOOD;
import static com.inceptai.dobby.ai.DataInterpreter.MetricType.POOR;
import static com.inceptai.dobby.utils.Utils.setImage;
/**
 * Created by arunesh on 5/22/17.
 */

public class OverallNetworkResultsViewHolder extends RecyclerView.ViewHolder {

    private TextView wifiSsidTv;
    private TextView wifiSignalValueTv;
    private ImageView wifiSignalIconIv;
    private TextView routerIpTv;
    private TextView ispNameTv;
    private View rootView;

    public OverallNetworkResultsViewHolder(View rootView) {
        super(rootView);
        fetchViewInstances(rootView);
        this.rootView = rootView;
    }

    private void fetchViewInstances(View rootView) {
        // Populate wifi card views
        wifiSsidTv = (TextView) rootView.findViewById(R.id.wifi_ssid_tv);

        wifiSignalValueTv = (TextView) rootView.findViewById(R.id.value_tv);
        wifiSignalIconIv = (ImageView) rootView.findViewById(R.id.icon_iv);
        ispNameTv = (TextView) rootView.findViewById(R.id.isp_name_tv);
        routerIpTv = (TextView) rootView.findViewById(R.id.router_ip_tv);

    }

    public void setResults(Context context, DataInterpreter.WifiGrade wifiGrade, String ispName, String routerIp) {
        String ssid = wifiGrade.getPrimaryApSsid();
        if (ssid != null && !ssid.isEmpty()) {
            ssid = Utils.limitSsid(ssid);
            wifiSsidTv.setText(ssid);
        }
        setWifiResult(context, wifiSignalValueTv, String.valueOf(wifiGrade.getPrimaryApSignal()),
                wifiSignalIconIv, wifiGrade.getPrimaryApSignalMetric());
        ispNameTv.setText(ispName);
        routerIpTv.setText(routerIp);
        rootView.requestLayout();
    }

    private void setWifiResult(Context context, TextView valueTv, String value, ImageView gradeIv, @DataInterpreter.MetricType int grade) {
        valueTv.setText(value);
        switch (grade) {
            case EXCELLENT:
                setImage(context, gradeIv, R.drawable.signal_5);
                break;
            case GOOD:
                setImage(context, gradeIv, R.drawable.signal_4);
                break;
            case AVERAGE:
                setImage(context, gradeIv, R.drawable.signal_3);
                break;
            case POOR:
                setImage(context, gradeIv, R.drawable.signal_2);
                break;
            case ABYSMAL:
                setImage(context, gradeIv, R.drawable.signal_1);
                break;
            default:
                setImage(context, gradeIv, R.drawable.signal_disc);
                break;

        }
    }
}
