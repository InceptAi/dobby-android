package com.inceptai.dobby.ai.suggest;

import android.support.annotation.IntDef;

import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_BW_UNKNOWN;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_COMPOUND_GENERIC;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_DOWNLOAD_BW_1MBPS;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_DOWNLOAD_BW_2MBPS;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_DOWNLOAD_BW_4MBPS;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_DOWNLOAD_BW_6TO8;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_DOWNLOAD_BW_8TO20;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_DOWNLOAD_BW_ABOVE20;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_DOWNLOAD_BW_LT100KBPS;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_DOWNLOAD_BW_LT40KBPS;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_DOWNLOAD_BW_LT500KBPS;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_DOWNLOAD_BW_LT_1MBPS;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_DOWNLOAD_BW_UNKNOWN;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_NONE;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_OVERALL_BANDWIDTH_GOOD;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_OVERALL_BANDWIDTH_OK;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_OVERALL_BANDWIDTH_POOR;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_UPLOAD_BW_AND_RATIO_OK;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_UPLOAD_BW_AND_RATIO_POOR;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_UPLOAD_BW_OK_RATIO_POOR;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_UPLOAD_BW_POOR;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_UPLOAD_BW_UNKNOWN;


public class Summary {
    public static final double INVALID_BW = -1.0;
    @IntDef({TYPE_NONE, TYPE_BW_UNKNOWN, TYPE_DOWNLOAD_BW_UNKNOWN, TYPE_UPLOAD_BW_UNKNOWN, TYPE_COMPOUND_GENERIC,
            TYPE_OVERALL_BANDWIDTH_GOOD, TYPE_OVERALL_BANDWIDTH_OK, TYPE_OVERALL_BANDWIDTH_POOR, TYPE_DOWNLOAD_BW_1MBPS, TYPE_DOWNLOAD_BW_LT_1MBPS,
            TYPE_DOWNLOAD_BW_LT500KBPS, TYPE_DOWNLOAD_BW_LT100KBPS, TYPE_DOWNLOAD_BW_LT40KBPS,
            TYPE_DOWNLOAD_BW_2MBPS, TYPE_DOWNLOAD_BW_4MBPS, TYPE_DOWNLOAD_BW_6TO8, TYPE_DOWNLOAD_BW_8TO20, TYPE_DOWNLOAD_BW_ABOVE20,
    TYPE_UPLOAD_BW_AND_RATIO_OK, TYPE_UPLOAD_BW_AND_RATIO_POOR, TYPE_UPLOAD_BW_OK_RATIO_POOR, TYPE_UPLOAD_BW_POOR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
        int TYPE_NONE = 0;

        int TYPE_COMPOUND_GENERIC = 10;

        // Bandwidth related messages:
        // Bandwidth ladder from: https://www.soundandvision.com/content/how-much-bandwidth-do-you-need-streaming-video
        int TYPE_BW_MIN = 999;
        int TYPE_BW_UNKNOWN = 1000;
        int TYPE_DOWNLOAD_BW_UNKNOWN = 1001;
        int TYPE_DOWNLOAD_BW_LT_1MBPS = 1002;
        int TYPE_DOWNLOAD_BW_1MBPS = 1003;
        int TYPE_DOWNLOAD_BW_2MBPS = 1004;
        int TYPE_DOWNLOAD_BW_4MBPS = 1005;
        int TYPE_DOWNLOAD_BW_6TO8 = 1006;
        int TYPE_DOWNLOAD_BW_8TO20 = 1007;
        int TYPE_DOWNLOAD_BW_ABOVE20 = 1008;
        int TYPE_DOWNLOAD_BW_LT500KBPS = 1009;
        int TYPE_DOWNLOAD_BW_LT100KBPS = 1010;
        int TYPE_DOWNLOAD_BW_LT40KBPS = 1011;

        int TYPE_UPLOAD_BW_UNKNOWN = 1050;
        int TYPE_UPLOAD_BW_AND_RATIO_POOR = 1051;
        int TYPE_UPLOAD_BW_OK_RATIO_POOR = 1052;
        int TYPE_UPLOAD_BW_AND_RATIO_OK = 1053;
        int TYPE_UPLOAD_BW_POOR = 1054;

        int TYPE_OVERALL_BANDWIDTH_GOOD = 1100;
        int TYPE_OVERALL_BANDWIDTH_OK = 1101;
        int TYPE_OVERALL_BANDWIDTH_POOR = 1102;
        int TYPE_BW_MAX = 1500;
    }

    @Type
    private int overall;

    @Type
    private int download;

    @Type
    private int upload;

    private DataInterpreter.BandwidthGrade bandwidthGrade;

    public Summary(@Type int overall, @Type int upload, @Type int download, DataInterpreter.BandwidthGrade bandwidthGrade) {
        this.overall = overall;
        this.upload = upload;
        this.download = download;
        this.bandwidthGrade = bandwidthGrade;
    }

    public int getOverallType() {
        return overall;
    }

    public int getDownloadType() {
        return download;
    }

    public int getUploadType() {
        return upload;
    }

    public double getDownloadMbps() {
        return Utils.toMbps(bandwidthGrade.getDownloadBandwidth());
    }

    public double getUploadMbps() {
        return Utils.toMbps(bandwidthGrade.getUploadBandwidth());
    }

    @Override
    public String toString() {
        return "Overall: " + overall + " upload: " + upload + " download: " + download;
    }
}
