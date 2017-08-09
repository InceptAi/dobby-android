package com.inceptai.wifiexpert.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.inceptai.wifiexpert.R;
import com.inceptai.wifiexpert.expertSystem.inferencing.DataInterpreter;
import com.inceptai.wifiexpert.utils.DobbyLog;
import com.inceptai.wifiexpert.utils.Utils;

import static com.inceptai.wifiexpert.expertSystem.inferencing.DataInterpreter.MetricType.ABYSMAL;
import static com.inceptai.wifiexpert.expertSystem.inferencing.DataInterpreter.MetricType.AVERAGE;
import static com.inceptai.wifiexpert.expertSystem.inferencing.DataInterpreter.MetricType.EXCELLENT;
import static com.inceptai.wifiexpert.expertSystem.inferencing.DataInterpreter.MetricType.GOOD;
import static com.inceptai.wifiexpert.expertSystem.inferencing.DataInterpreter.MetricType.POOR;
import static com.inceptai.wifiexpert.utils.Utils.setImage;


/**
 * Created by arunesh on 5/22/17.
 */

public class PingResultsViewHolder extends RecyclerView.ViewHolder {

    private Context context;

    private TextView pingRouterTitleTv;
    private TextView pingRouterValueTv;
    private ImageView pingRouterGradeIv;

    private TextView pingDnsPrimaryTitleTv;
    private TextView pingDnsPrimaryValueTv;
    private ImageView pingDnsPrimaryGradeIv;

    private TextView pingDnsSecondTitleTv;
    private TextView pingDnsSecondValueTv;
    private ImageView pingDnsSecondGradeIv;

    private TextView pingWebTitleTv;
    private TextView pingWebValueTv;
    private ImageView pingWebGradeIv;

    public PingResultsViewHolder(View rootView) {
        super(rootView);
        fetchViewInstances(rootView);
    }


    private void fetchViewInstances(View rootView) {
        View row1View = rootView.findViewById(R.id.ping_latency_row_inc1);
        pingRouterTitleTv = (TextView) row1View.findViewById(R.id.left_title_tv);
        pingRouterValueTv = (TextView) row1View.findViewById(R.id.left_value_tv);
        pingRouterGradeIv = (ImageView) row1View.findViewById(R.id.left_grade_iv);

        pingWebTitleTv = (TextView) row1View.findViewById(R.id.right_title_tv);
        pingWebValueTv = (TextView) row1View.findViewById(R.id.right_value_tv);
        pingWebGradeIv = (ImageView) row1View.findViewById(R.id.right_grade_iv);

        View row2View = rootView.findViewById(R.id.ping_latency_row_inc2);
        pingDnsPrimaryTitleTv = (TextView) row2View.findViewById(R.id.left_title_tv);
        pingDnsPrimaryValueTv = (TextView) row2View.findViewById(R.id.left_value_tv);
        pingDnsPrimaryGradeIv = (ImageView) row2View.findViewById(R.id.left_grade_iv);

        pingDnsSecondTitleTv = (TextView) row2View.findViewById(R.id.right_title_tv);
        pingDnsSecondValueTv = (TextView) row2View.findViewById(R.id.right_value_tv);
        pingDnsSecondGradeIv = (ImageView) row2View.findViewById(R.id.right_grade_iv);

        pingRouterTitleTv.setText(R.string.router_ping);
        pingDnsPrimaryTitleTv.setText(R.string.dns_primary_ping);
        pingDnsSecondTitleTv.setText(R.string.dns_second_ping);
        pingWebTitleTv.setText(R.string.web_ping);
    }

    public void setPingResults(Context context, DataInterpreter.PingGrade pingGrade) {
        DobbyLog.v("Ping grade available.");
        if (pingGrade == null) {
            DobbyLog.w("Null ping grade.");
            return;
        }

        setPingResult(context, pingRouterValueTv, String.format("%02.1f", pingGrade.getRouterLatencyMs()),
                pingRouterGradeIv, pingGrade.getRouterLatencyMetric());

        setPingResult(context, pingDnsPrimaryValueTv, String.format("%02.1f", pingGrade.getDnsServerLatencyMs()),
                pingDnsPrimaryGradeIv, pingGrade.getDnsServerLatencyMetric());

        setPingResult(context, pingDnsSecondValueTv,  String.format("%02.1f", pingGrade.getAlternativeDnsLatencyMs()),
                pingDnsSecondGradeIv, pingGrade.getAlternativeDnsMetric());

        setPingResult(context, pingWebValueTv, String.format("%02.1f", pingGrade.getExternalServerLatencyMs()),
                pingWebGradeIv, pingGrade.getExternalServerLatencyMetric());
    }

    private void setPingResult(Context context, TextView valueTv, String value, ImageView gradeIv, @DataInterpreter.MetricType int grade) {
        valueTv.setText(value);
        switch (grade) {
            case EXCELLENT:
                setImage(context, gradeIv, R.drawable.double_tick);
                break;
            case GOOD:
                setImage(context, gradeIv, R.drawable.tick_mark);
                break;
            case AVERAGE:
                setImage(context, gradeIv, R.drawable.yellow_tick_mark);
                break;
            case POOR:
                setImage(context, gradeIv, R.drawable.poor_icon);
                break;
            case ABYSMAL:
                setImage(context, gradeIv, R.drawable.poor_icon);
                break;
            default:
                setImage(context, gradeIv, R.drawable.non_functional);
                valueTv.setText(Utils.UNKNOWN_LATENCY_STRING);
                break;

        }
    }

}
