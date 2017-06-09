
package com.inceptai.dobby.ai.suggest;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.constraint.solver.SolverVariable;

import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

public class NewSuggestions {
    public static class DataSummary {
        private DataInterpreter.BandwidthGrade bandwidthGrade;
        private DataInterpreter.WifiGrade wifiGrade;
        private DataInterpreter.PingGrade pingGrade;
        private DataInterpreter.HttpGrade httpGrade;

        public DataSummary(@Nullable DataInterpreter.BandwidthGrade bandwidthGrade,
                           DataInterpreter.WifiGrade wifiGrade, DataInterpreter.PingGrade pingGrade,
                           DataInterpreter.HttpGrade httpGrade) {
            this.bandwidthGrade = bandwidthGrade;
            this.wifiGrade = wifiGrade;
            this.pingGrade = pingGrade;
            this.httpGrade = httpGrade;
        }
    }

    private DataSummary dataSummary;
    private Context context;

    public NewSuggestions(DataSummary dataSummary, Context context) {
        this.dataSummary = dataSummary;
        this.context = context;
    }

    public LocalSummary getSuggestions() {

        Summary overallSummary = overall(dataSummary.bandwidthGrade);
        DobbyLog.w("New summary: " + overallSummary);
        return new SnippetLocalizer(context).localize(overallSummary);
    }

    private static Summary overall(DataInterpreter.BandwidthGrade grade) {
        if (grade.getUploadBandwidthMetric() == DataInterpreter.MetricType.UNKNOWN &&
                grade.getDownloadBandwidthMetric() == DataInterpreter.MetricType.UNKNOWN) {
            DobbyLog.i("Returning snippet of UNKNOWN type.");
            return new Summary(Summary.Type.TYPE_BW_UNKNOWN, Summary.Type.TYPE_UPLOAD_BW_UNKNOWN,
                    Summary.Type.TYPE_DOWNLOAD_BW_UNKNOWN, grade);
        }
        @Summary.Type
        int download = download(grade);

        @Summary.Type
        int upload = upload(grade);

        @Summary.Type
        int overall = Summary.Type.TYPE_BW_UNKNOWN;

        if ((upload == Summary.Type.TYPE_UPLOAD_BW_POOR || upload == Summary.Type.TYPE_UPLOAD_BW_AND_RATIO_POOR) &&
                (download == Summary.Type.TYPE_DOWNLOAD_BW_LT40KBPS ||
                download == Summary.Type.TYPE_DOWNLOAD_BW_LT100KBPS ||
                download == Summary.Type.TYPE_DOWNLOAD_BW_LT500KBPS)) {

            overall = Summary.Type.TYPE_OVERALL_BANDWIDTH_POOR;
        } else if ((upload == Summary.Type.TYPE_UPLOAD_BW_OK_RATIO_POOR) &&
                (download == Summary.Type.TYPE_DOWNLOAD_BW_LT_1MBPS ||
                        download == Summary.Type.TYPE_DOWNLOAD_BW_1MBPS ||
                        download == Summary.Type.TYPE_DOWNLOAD_BW_2MBPS)) {
            overall = Summary.Type.TYPE_OVERALL_BANDWIDTH_OK;
        } else if ((upload == Summary.Type.TYPE_UPLOAD_BW_AND_RATIO_OK) &&
                (download == Summary.Type.TYPE_DOWNLOAD_BW_4MBPS ||
                download == Summary.Type.TYPE_DOWNLOAD_BW_6TO8 ||
                download == Summary.Type.TYPE_DOWNLOAD_BW_8TO20 ||
                download == Summary.Type.TYPE_DOWNLOAD_BW_ABOVE20)) {
            overall = Summary.Type.TYPE_OVERALL_BANDWIDTH_GOOD;
        }

        return new Summary(overall, upload, download, grade);
    }

    @Summary.Type
    private static int download(DataInterpreter.BandwidthGrade grade) {

        if (grade.getDownloadBandwidthMetric() == DataInterpreter.MetricType.UNKNOWN) {
            return Summary.Type.TYPE_DOWNLOAD_BW_UNKNOWN;
        }

        double bwMbps = Utils.toMbps(grade.getDownloadBandwidth());
        double bwKbps = Utils.toKbps(grade.getDownloadBandwidth());

        if (bwKbps < 40.00) {
            return Summary.Type.TYPE_DOWNLOAD_BW_LT40KBPS;
        }

        if (bwKbps < 100.0) {
            return Summary.Type.TYPE_DOWNLOAD_BW_LT100KBPS;
        }

        if (bwKbps < 500.0) {
            return Summary.Type.TYPE_DOWNLOAD_BW_LT500KBPS;
        }

        if (bwMbps < 0.95) {
            return Summary.Type.TYPE_DOWNLOAD_BW_LT_1MBPS;
        }

        if (bwMbps < 1.8) {
            return Summary.Type.TYPE_DOWNLOAD_BW_1MBPS;
        }

        if (bwMbps < 2.6) {
            return Summary.Type.TYPE_DOWNLOAD_BW_2MBPS;
        }

        if (bwMbps < 5.0) {
            return Summary.Type.TYPE_DOWNLOAD_BW_4MBPS;
        }

        if (bwMbps < 8.5) {
            return Summary.Type.TYPE_DOWNLOAD_BW_6TO8;
        }

        if (bwMbps < 22.0) {
            return Summary.Type.TYPE_DOWNLOAD_BW_8TO20;
        }

        if (bwMbps > 22.0) {
            return Summary.Type.TYPE_DOWNLOAD_BW_ABOVE20;
        }
        return Summary.Type.TYPE_DOWNLOAD_BW_UNKNOWN;
    }

    @Summary.Type
    private static int upload(DataInterpreter.BandwidthGrade grade) {

        if (grade.getUploadBandwidthMetric() == DataInterpreter.MetricType.UNKNOWN) {
            return Summary.Type.TYPE_UPLOAD_BW_UNKNOWN;
        }

        double uploadBwMbps = Utils.toMbps(grade.getUploadBandwidthMetric());
        double downloadBwMbps = Utils.toMbps(grade.getDownloadBandwidth());
        double ratio = uploadBwMbps / downloadBwMbps;

        DobbyLog.e("uploadMbps: " + uploadBwMbps + " ratio:" + ratio);
        if (ratio < 0.095) {
            return (uploadBwMbps <= 0.5) ? Summary.Type.TYPE_UPLOAD_BW_AND_RATIO_POOR :
                    Summary.Type.TYPE_UPLOAD_BW_OK_RATIO_POOR;
        }

        return (uploadBwMbps <= 0.5) ? Summary.Type.TYPE_UPLOAD_BW_POOR : Summary.Type.TYPE_UPLOAD_BW_AND_RATIO_OK;
    }
}
