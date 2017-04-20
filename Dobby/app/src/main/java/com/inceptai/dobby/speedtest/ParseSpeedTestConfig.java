package com.inceptai.dobby.speedtest;

import android.support.annotation.Nullable;

import com.inceptai.dobby.utils.Utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by vivek on 3/31/17.
 */

public class ParseSpeedTestConfig {
    private final String defaultConfigUrl = "www.speedtest.net/speedtest-config.php";

    public SpeedTestConfig speedTestConfig;

    //Results callback
    private ResultsCallback resultsCallback;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {
        void onConfigFetch(SpeedTestConfig config);
        void onConfigFetchError(String error);
    }

    public ParseSpeedTestConfig(String urlString, @Nullable ResultsCallback resultsCallback) {
        this.speedTestConfig = null;
        this.resultsCallback = resultsCallback;
    }


    public ParseSpeedTestConfig(@Nullable ResultsCallback resultsCallback) {
        speedTestConfig = null;
        this.resultsCallback = resultsCallback;
    }


    public SpeedTestConfig getConfig(@Nullable  String mode) {
        String configUrl = null;
        if (mode == null || mode == "http") {
            configUrl = "http://" + defaultConfigUrl;
        } else {
            configUrl = "https://" + defaultConfigUrl;
        }
        return getConfigFromUrlString(configUrl);
    }


    public SpeedTestConfig getConfigFromUrlString (String urlString) {
        SpeedTestConfig config = null;
        try {
            config = downloadAndParseConfig(urlString);
            if (resultsCallback != null) {
                this.resultsCallback.onConfigFetch(config);
            }
        } catch (IOException e) {
            String outputErrorMessage = "Exception thrown  :" + e;
            System.out.println(outputErrorMessage);
            if (this.resultsCallback != null) {
                this.resultsCallback.onConfigFetchError(outputErrorMessage);
            }
        } finally {
            return config;
        }
    }

    public SpeedTestConfig downloadAndParseConfig(String urlString) throws IOException {
        InputStream stream = null;
        SpeedTestConfig configToReturn = null;
        try {
            stream = Utils.getStreamFromUrl(urlString);
            configToReturn = new SpeedTestConfig(stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        return configToReturn;
    }

    public String getClientISP() {
        if (speedTestConfig != null && speedTestConfig.clientConfig != null) {
            return speedTestConfig.clientConfig.isp;
        }
        return null;
    }

    public String getClientip() {
        if (speedTestConfig != null && speedTestConfig.clientConfig != null) {
            return speedTestConfig.clientConfig.ip;
        }
        return null;
    }


}

