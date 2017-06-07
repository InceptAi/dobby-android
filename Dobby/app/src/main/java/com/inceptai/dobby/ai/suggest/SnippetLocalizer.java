package com.inceptai.dobby.ai.suggest;

import android.content.Context;
import android.content.res.Resources;

import com.inceptai.dobby.R;

import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_BW_UNKNOWN;
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
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_OVERALL_BANDWIDTH_GOOD;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_OVERALL_BANDWIDTH_OK;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_OVERALL_BANDWIDTH_POOR;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_UPLOAD_BW_AND_RATIO_OK;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_UPLOAD_BW_AND_RATIO_POOR;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_UPLOAD_BW_OK_RATIO_POOR;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_UPLOAD_BW_POOR;
import static com.inceptai.dobby.ai.suggest.Summary.Type.TYPE_UPLOAD_BW_UNKNOWN;

/**
 * Created by arunesh on 5/16/17.
 */

public class SnippetLocalizer {

    private Context context;

    public SnippetLocalizer(Context context) {
        this.context = context;
    }

    public LocalSummary localize(Summary summary) {
        Resources resources = context.getResources();
        LocalSummary localSummary = new LocalSummary(summary);
        switch (summary.getOverallType()) {

            case TYPE_OVERALL_BANDWIDTH_GOOD:
                localSummary.setOverall(resources.getString(R.string.suggest_bandwidth_overall_good), resources.getString(R.string.suggest_bandwidth_overall_good_more));
                break;
            case TYPE_OVERALL_BANDWIDTH_OK:
                localSummary.setOverall(resources.getString(R.string.suggest_bandwidth_overall_ok), resources.getString(R.string.suggest_bandwidth_overall_ok_more));
                break;
            case TYPE_OVERALL_BANDWIDTH_POOR:
                localSummary.setOverall(resources.getString(R.string.suggest_bandwidth_overall_poor), resources.getString(R.string.suggest_bandwidth_overall_poor_more));
                break;
            case TYPE_BW_UNKNOWN:
                localSummary.setDownload(resources.getString(R.string.suggest_bw_unknown),
                        resources.getString(R.string.suggest_bw_unknown_more));
                break;
        }

        switch (summary.getDownloadType()) {
            case TYPE_DOWNLOAD_BW_UNKNOWN:
                localSummary.setDownload(resources.getString(R.string.suggest_download_bw_unknown),
                        resources.getString(R.string.suggest_download_bw_unknown_more));
                break;
            case TYPE_DOWNLOAD_BW_LT_1MBPS:
                localSummary.setDownload(resources.getString(R.string.suggest_download_bw_lt1mbps, summary.getDownloadMbps()),
                        resources.getString(R.string.suggest_download_bw_1mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_1MBPS:
                localSummary.setDownload(resources.getString(R.string.suggest_download_bw_1mbps, summary.getDownloadMbps()),
                        resources.getString(R.string.suggest_download_bw_1mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_2MBPS:
                localSummary.setDownload(resources.getString(R.string.suggest_download_bw_2mbps, summary.getDownloadMbps()),
                        resources.getString(R.string.suggest_download_bw_2mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_4MBPS:
                localSummary.setDownload(resources.getString(R.string.suggest_download_bw_4mbps, summary.getDownloadMbps()),
                        resources.getString(R.string.suggest_download_bw_4mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_6TO8:
                localSummary.setDownload(resources.getString(R.string.suggest_download_bw_6to8mbps, summary.getDownloadMbps()),
                        resources.getString(R.string.suggest_download_bw_6to8mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_8TO20:
                localSummary.setDownload(resources.getString(R.string.suggest_download_bw_8to20mbps, summary.getDownloadMbps()),
                        resources.getString(R.string.suggest_download_bw_8to20mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_ABOVE20:
                localSummary.setDownload(resources.getString(R.string.suggest_download_bw_above20mbps, summary.getDownloadMbps()),
                        resources.getString(R.string.suggest_download_bw_above20mbps_more));
                break;
            case TYPE_DOWNLOAD_BW_LT500KBPS:
                localSummary.setDownload(resources.getString(R.string.suggest_download_bw_lt500kbps, summary.getDownloadMbps()),
                        resources.getString(R.string.suggest_download_bw_lt500kbps_more));
                break;
            case TYPE_DOWNLOAD_BW_LT100KBPS:
                localSummary.setDownload(resources.getString(R.string.suggest_download_bw_lt100kbps, summary.getDownloadMbps()),
                        resources.getString(R.string.suggest_download_bw_lt100kbps_more));
                break;
            case TYPE_DOWNLOAD_BW_LT40KBPS:
                localSummary.setDownload(resources.getString(R.string.suggest_download_bw_lt40kbps, summary.getDownloadMbps()),
                        resources.getString(R.string.suggest_download_bw_lt40kbps_more));
                break;
        }
        switch (summary.getUploadType()) {
            case TYPE_UPLOAD_BW_UNKNOWN:
                localSummary.setUpload(resources.getString(R.string.suggest_upload_bw_unknown), resources.getString(R.string.suggest_upload_bw_unknown_more));
                break;
            case TYPE_UPLOAD_BW_AND_RATIO_OK:
                localSummary.setUpload(resources.getString(R.string.suggest_upload_bw_and_ratio_ok), resources.getString(R.string.suggest_upload_bw_and_ratio_ok_more));
                break;
            case TYPE_UPLOAD_BW_AND_RATIO_POOR:
                localSummary.setUpload(resources.getString(R.string.suggest_upload_bw_and_ratio_poor), resources.getString(R.string.suggest_upload_bw_and_ratio_poor_more));
                break;
            case TYPE_UPLOAD_BW_OK_RATIO_POOR:
                localSummary.setUpload(resources.getString(R.string.suggest_upload_bw_ok_ratio_poor), resources.getString(R.string.suggest_upload_bw_ok_ratio_poor_more));
                break;
            case TYPE_UPLOAD_BW_POOR:
                localSummary.setUpload(resources.getString(R.string.suggest_upload_bw_poor), resources.getString(R.string.suggest_upload_bw_poor_more));
                break;
        }
        return localSummary;
    }

}
