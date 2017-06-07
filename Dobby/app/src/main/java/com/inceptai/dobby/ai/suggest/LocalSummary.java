package com.inceptai.dobby.ai.suggest;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.inceptai.dobby.utils.Utils;

import java.util.ArrayList;

/**
 * Created by arunesh on 5/16/17.
 */

public class LocalSummary {

    private Summary summary;

    private String overall;
    private String overallDetailed;
    private String upload;
    private String uploadDetailed;
    private String download;
    private String downloadDetailed;

    LocalSummary(@NonNull Summary summary) {
        this.summary = summary;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setOverall(String overall, String overallDetailed) {
        this.overall = overall;
        this.overallDetailed = overallDetailed;
    }

    public void setUpload(String upload, String uploadDetailed) {
        this.upload = upload;
        this.uploadDetailed = uploadDetailed;
    }

    public void setDownload(String download, String downloadDetailed) {
        this.download = download;
        this.downloadDetailed = downloadDetailed;
    }

    public String getOverall() {
        return overall;
    }

    public String getOverallDetailed() {
        return overallDetailed;
    }

    public String getUpload() {
        return upload;
    }

    public String getUploadDetailed() {
        return uploadDetailed;
    }

    public String getDownload() {
        return download;
    }

    public String getDownloadDetailed() {
        return downloadDetailed;
    }
}
