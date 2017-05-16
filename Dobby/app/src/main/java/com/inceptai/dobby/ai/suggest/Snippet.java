package com.inceptai.dobby.ai.suggest;

import android.os.Bundle;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_BW_MAX;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_BW_MIN;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_BW_UNKNOWN;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_COMPOUND_GENERIC;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_DOWNLOAD_BW_1MBPS;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_DOWNLOAD_BW_2MBPS;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_DOWNLOAD_BW_4MBPS;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_DOWNLOAD_BW_6TO8;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_DOWNLOAD_BW_8TO20;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_DOWNLOAD_BW_ABOVE20;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_DOWNLOAD_BW_LT100KBPS;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_DOWNLOAD_BW_LT40KBPS;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_DOWNLOAD_BW_LT500KBPS;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_DOWNLOAD_BW_LT_1MBPS;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_DOWNLOAD_BW_UNKNOWN;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_NONE;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_OVERALL_BANDWIDTH_GOOD;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_OVERALL_BANDWIDTH_OK;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_OVERALL_BANDWIDTH_POOR;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_UPLOAD_BW_RATIO_GT_25PERCENT;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_UPLOAD_BW_RATIO_LT_10PERCENT;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_UPLOAD_BW_RATIO_LT_1PERCENT;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_UPLOAD_BW_RATIO_LT_25PERCENT;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_UPLOAD_BW_RATIO_VERY_POOR;
import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_UPLOAD_BW_UNKNOWN;

/**
 * Created by arunesh on 5/15/17.
 */

public class Snippet {
    public static final double INVALID_BW = -1.0;
    @IntDef({TYPE_NONE, TYPE_BW_UNKNOWN, TYPE_DOWNLOAD_BW_UNKNOWN, TYPE_UPLOAD_BW_UNKNOWN, TYPE_COMPOUND_GENERIC,
            TYPE_OVERALL_BANDWIDTH_GOOD, TYPE_OVERALL_BANDWIDTH_OK, TYPE_OVERALL_BANDWIDTH_POOR, TYPE_DOWNLOAD_BW_1MBPS, TYPE_DOWNLOAD_BW_LT_1MBPS,
            TYPE_DOWNLOAD_BW_LT500KBPS, TYPE_DOWNLOAD_BW_LT100KBPS, TYPE_DOWNLOAD_BW_LT40KBPS,
            TYPE_DOWNLOAD_BW_2MBPS, TYPE_DOWNLOAD_BW_4MBPS, TYPE_DOWNLOAD_BW_6TO8, TYPE_DOWNLOAD_BW_8TO20, TYPE_DOWNLOAD_BW_ABOVE20,
            TYPE_UPLOAD_BW_RATIO_VERY_POOR, TYPE_UPLOAD_BW_RATIO_LT_1PERCENT, TYPE_UPLOAD_BW_RATIO_LT_10PERCENT,
            TYPE_UPLOAD_BW_RATIO_LT_25PERCENT, TYPE_UPLOAD_BW_RATIO_GT_25PERCENT})
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
        int TYPE_UPLOAD_BW_RATIO_VERY_POOR = 1051;
        int TYPE_UPLOAD_BW_RATIO_LT_1PERCENT = 1052;
        int TYPE_UPLOAD_BW_RATIO_LT_10PERCENT = 1053;
        int TYPE_UPLOAD_BW_RATIO_LT_25PERCENT = 1054;
        int TYPE_UPLOAD_BW_RATIO_GT_25PERCENT = 1055;

        int TYPE_OVERALL_BANDWIDTH_GOOD = 1100;
        int TYPE_OVERALL_BANDWIDTH_OK = 1101;
        int TYPE_OVERALL_BANDWIDTH_POOR = 1102;
        int TYPE_BW_MAX = 1500;
    }

    private static final String TAG_BANDWIDTH = "bandwidth";
    @Type
    private int snippetType;
    private Bundle dataBundle;
    private Snippet[] snippetList;

    private Snippet(@Type int snippetType) {
        this.snippetType = snippetType;
        dataBundle = new Bundle();
    }

    private Snippet(Snippet[] snippetList) {
        this.snippetType = TYPE_COMPOUND_GENERIC;
        this.snippetList = snippetList;
    }

    private Snippet(@Type int snippetType, Snippet[] snippetList) {
        this.snippetType = snippetType;
        this.snippetList = snippetList;
    }

    // Factory constructors:
    public static Snippet ofType(@Type int snippetType) {
        return new Snippet(snippetType);
    }

    public static Snippet ofType(@Type int snippetType, double bandwidth) {
        Snippet snippet = new Snippet(snippetType);
        snippet.putDouble(TAG_BANDWIDTH, bandwidth);
        return snippet;
    }

    public static Snippet combine(Snippet ... list) {
        return new Snippet(list);
    }

    public static Snippet ofType(@Type int snippetType, Snippet ... list) {
        return new Snippet(snippetType, list);
    }

    @Type
    public int getType() {
        return snippetType;
    }

    public boolean typeBandwidth() {
        return snippetType > TYPE_BW_MIN && snippetType < TYPE_BW_MAX;
    }

    public double getBandwidth() {
        return dataBundle.getDouble(TAG_BANDWIDTH, INVALID_BW);
    }

    private void putDouble(String key, double value) {
        dataBundle.putDouble(key, value);
    }

}
