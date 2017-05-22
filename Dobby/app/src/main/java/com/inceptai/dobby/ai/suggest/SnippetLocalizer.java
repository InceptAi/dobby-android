package com.inceptai.dobby.ai.suggest;

import android.content.Context;
import android.content.res.Resources;

import com.inceptai.dobby.R;

import static com.inceptai.dobby.ai.suggest.Snippet.Type.TYPE_BW_UNKNOWN;
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
 * Created by arunesh on 5/16/17.
 */

public class SnippetLocalizer {

    private Context context;

    public SnippetLocalizer(Context context) {
        this.context = context;
    }

    public LocalSnippet localize(Snippet snippet) {
        if (snippet.typeBandwidth()) {
            return typeBandwidth(snippet, context.getResources());
        }
        return null;
    }

    private static LocalSnippet typeBandwidth(Snippet snippet, Resources resources) {
        LocalSnippet localSnippet = new LocalSnippet(snippet);
        switch (snippet.getType()) {
            case TYPE_BW_UNKNOWN:
                localSnippet.addString(resources.getString(R.string.suggest_bw_unknown),
                        resources.getString(R.string.suggest_bw_unknown_more));
                break;
            case TYPE_DOWNLOAD_BW_UNKNOWN:
                localSnippet.addString(resources.getString(R.string.suggest_download_bw_unknown),
                        resources.getString(R.string.suggest_download_bw_unknown_more));
                break;
            case TYPE_DOWNLOAD_BW_LT_1MBPS:
                localSnippet.addString(resources.getString(R.string.suggest_download_bw_lt1mbps, snippet.getBandwidth()),
                        resources.getString(R.string.suggest_download_bw_1mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_1MBPS:
                localSnippet.addString(resources.getString(R.string.suggest_download_bw_1mbps, snippet.getBandwidth()),
                        resources.getString(R.string.suggest_download_bw_1mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_2MBPS:
                localSnippet.addString(resources.getString(R.string.suggest_download_bw_2mbps, snippet.getBandwidth()),
                        resources.getString(R.string.suggest_download_bw_2mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_4MBPS:
                localSnippet.addString(resources.getString(R.string.suggest_download_bw_4mbps, snippet.getBandwidth()),
                        resources.getString(R.string.suggest_download_bw_4mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_6TO8:
                localSnippet.addString(resources.getString(R.string.suggest_download_bw_6to8mbps, snippet.getBandwidth()),
                        resources.getString(R.string.suggest_download_bw_6to8mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_8TO20:
                localSnippet.addString(resources.getString(R.string.suggest_download_bw_8to20mbps, snippet.getBandwidth()),
                        resources.getString(R.string.suggest_download_bw_8to20mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_ABOVE20:
                localSnippet.addString(resources.getString(R.string.suggest_download_bw_above20mbps, snippet.getBandwidth()),
                        resources.getString(R.string.suggest_download_bw_above20mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_LT500KBPS:
                localSnippet.addString(resources.getString(R.string.suggest_download_bw_lt500kbps, snippet.getBandwidth()),
                        resources.getString(R.string.suggest_download_bw_lt500kbps_more));
                break;
            case TYPE_DOWNLOAD_BW_LT100KBPS:
                localSnippet.addString(resources.getString(R.string.suggest_download_bw_lt100kbps, snippet.getBandwidth()),
                        resources.getString(R.string.suggest_download_bw_lt100kbps_more));
                break;
            case TYPE_DOWNLOAD_BW_LT40KBPS:
                localSnippet.addString(resources.getString(R.string.suggest_download_bw_lt40kbps, snippet.getBandwidth()),
                        resources.getString(R.string.suggest_download_bw_lt40kbps_more));
                break;

            case TYPE_UPLOAD_BW_UNKNOWN:
                localSnippet.addString(resources.getString(R.string.suggest_upload_bw_unknown), resources.getString(R.string.suggest_upload_bw_unknown_more));
                break;
            case TYPE_UPLOAD_BW_RATIO_VERY_POOR:
                break;
            case TYPE_UPLOAD_BW_RATIO_LT_1PERCENT:
                break;
            case TYPE_UPLOAD_BW_RATIO_LT_10PERCENT:
                break;
            case TYPE_UPLOAD_BW_RATIO_LT_25PERCENT:
                break;
            case TYPE_UPLOAD_BW_RATIO_GT_25PERCENT:
                break;

            case TYPE_OVERALL_BANDWIDTH_GOOD:
                break;
            case TYPE_OVERALL_BANDWIDTH_OK:
                localSnippet.addString(resources.getString(R.string.suggest_bandwidth_overall_ok), resources.getString(R.string.suggest_bandwidth_overall_ok_more));
                break;
            case TYPE_OVERALL_BANDWIDTH_POOR:
                break;
        }
        return localSnippet;
    }

}
