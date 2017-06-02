
package com.inceptai.dobby.ai.suggest;

import android.content.Context;
import android.support.annotation.Nullable;

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

        Summary bandwidthSummary = bandwidth(dataSummary.bandwidthGrade);
        return new SnippetLocalizer(context).localize(bandwidthSummary);
    }

    private static Summary bandwidth(DataInterpreter.BandwidthGrade grade) {
        if (grade.getUploadBandwidthMetric() == DataInterpreter.MetricType.UNKNOWN &&
                grade.getDownloadBandwidthMetric() == DataInterpreter.MetricType.UNKNOWN) {
            DobbyLog.i("Returning snippet of UNKNOWN type.");
            return Summary.ofType(Summary.Type.TYPE_BW_UNKNOWN);
        }
        Summary download = download(grade);
        Summary upload = upload(grade);
        return Summary.ofType(Summary.Type.TYPE_OVERALL_BANDWIDTH_OK, download, upload);
    }

    // Can return a SNIPPET. The Summary class can have a more text and a basic text.
    // Browsing, streaming (video/audio) and file downloads. Twitter, facebook etc, 100KBs.
    // SnippetType class that
    private static Summary download(DataInterpreter.BandwidthGrade grade) {

        if (grade.getDownloadBandwidthMetric() == DataInterpreter.MetricType.UNKNOWN) {
            return Summary.ofType(Summary.Type.TYPE_DOWNLOAD_BW_UNKNOWN);
        }

        double bwMbps = Utils.toMbps(grade.getDownloadBandwidth());
        double bwKbps = Utils.toKbps(grade.getDownloadBandwidth());

        if (bwKbps < 40.00) {
            return Summary.ofType(Summary.Type.TYPE_DOWNLOAD_BW_LT40KBPS, bwKbps);
        }

        if (bwKbps < 100.0) {
            Summary.ofType(Summary.Type.TYPE_DOWNLOAD_BW_LT100KBPS, bwKbps);
        }

        if (bwKbps < 500.0) {
            Summary.ofType(Summary.Type.TYPE_DOWNLOAD_BW_LT500KBPS, bwKbps);
        }

        if (bwMbps < 0.95) {
            return Summary.ofType(Summary.Type.TYPE_DOWNLOAD_BW_LT_1MBPS, bwMbps);
        }

        if (bwMbps < 0.95) {
            return Summary.ofType(Summary.Type.TYPE_DOWNLOAD_BW_LT_1MBPS, bwMbps);
        }

        if (bwMbps < 1.8) {
            return Summary.ofType(Summary.Type.TYPE_DOWNLOAD_BW_1MBPS, bwMbps);
        }

        if (bwMbps < 2.6) {
            return Summary.ofType(Summary.Type.TYPE_DOWNLOAD_BW_2MBPS, bwMbps);
        }

        if (bwMbps < 5.0) {
            return Summary.ofType(Summary.Type.TYPE_DOWNLOAD_BW_4MBPS, bwMbps);
        }

        if (bwMbps < 8.5) {
            return Summary.ofType(Summary.Type.TYPE_DOWNLOAD_BW_6TO8, bwMbps);
        }

        if (bwMbps < 22.0) {
            return Summary.ofType(Summary.Type.TYPE_DOWNLOAD_BW_8TO20, bwMbps);
        }

        if (bwMbps > 22.0) {
            return Summary.ofType(Summary.Type.TYPE_DOWNLOAD_BW_ABOVE20, bwMbps);
        }
        return Summary.ofType(Summary.Type.TYPE_DOWNLOAD_BW_UNKNOWN);
    }

    private static Summary upload(DataInterpreter.BandwidthGrade grade) {

        if (grade.getUploadBandwidthMetric() == DataInterpreter.MetricType.UNKNOWN) {
            return Summary.ofType(Summary.Type.TYPE_UPLOAD_BW_UNKNOWN);
        }

        double uploadBwMbps = Utils.toMbps(grade.getUploadBandwidthMetric());
        double downloadBwMbps = Utils.toMbps(grade.getDownloadBandwidth());
        double ratio = uploadBwMbps / downloadBwMbps;

        if (ratio < 0.005) {
            return Summary.ofType(Summary.Type.TYPE_UPLOAD_BW_RATIO_VERY_POOR, uploadBwMbps);
        }

        if (ratio < 0.001) {
            return Summary.ofType(Summary.Type.TYPE_UPLOAD_BW_RATIO_LT_1PERCENT, uploadBwMbps);
        }
        if (ratio < 0.095) {
            return Summary.ofType(Summary.Type.TYPE_UPLOAD_BW_RATIO_LT_10PERCENT, uploadBwMbps);
        }
        if (ratio < 0.25) {
            return Summary.ofType(Summary.Type.TYPE_UPLOAD_BW_RATIO_LT_25PERCENT, uploadBwMbps);

        } else if (ratio > 0.24) {
            return Summary.ofType(Summary.Type.TYPE_UPLOAD_BW_RATIO_GT_25PERCENT, uploadBwMbps);
        }
        return Summary.ofType(Summary.Type.TYPE_UPLOAD_BW_UNKNOWN);
    }
}
