package com.inceptai.dobby.ai.suggest;

import android.support.annotation.Nullable;

import com.google.common.primitives.UnsignedInteger;
import com.inceptai.dobby.ai.DataInterpreter;

/**
 * Created by arunesh on 5/15/17.
 */

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

    // Can return a SNIPPET. The Snippet class can have a more text and a basic text.
    // Browsing, streaming (video/audio) and file downloads. Twitter, facebook etc, 100KBs.
    // SnippetType class that
    String getSummary(DataInterpreter.BandwidthGrade grade) {

        if (grade.getUploadBandwidthMetric() == DataInterpreter.MetricType.UNKNOWN &&
                grade.getDownloadBandwidthMetric() == DataInterpreter.MetricType.UNKNOWN) {

        }
        return null;
    }
}
