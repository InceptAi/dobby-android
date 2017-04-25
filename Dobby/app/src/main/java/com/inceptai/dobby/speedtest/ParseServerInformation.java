package com.inceptai.dobby.speedtest;

import android.support.annotation.Nullable;

import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by vivek on 4/1/17.
 */

public class ParseServerInformation {
    private final String defaultServerListUrl1 = "http://www.speedtest.net/speedtest-servers-static.php";
    private final String defaultServerListUrl2 = "http://c.speedtest.net/speedtest-servers-static.php";

    private String serverListUrl1;
    private String serverListUrl2;

    //Results callback
    private ResultsCallback resultsCallback;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {
        void onServerInformationFetch(ServerInformation serverInformation);
        void onServerInformationFetchError(String error);
    }

    public ParseServerInformation(String urlString1, String urlString2,
                                  @Nullable ResultsCallback resultsCallback) {
        this.serverListUrl1 = urlString1;
        this.serverListUrl2 = urlString2;
        this.resultsCallback = resultsCallback;
    }

    public ParseServerInformation(@Nullable ResultsCallback resultsCallback) {
        this.serverListUrl1 = defaultServerListUrl1;
        this.serverListUrl2 = defaultServerListUrl2;
        this.resultsCallback = resultsCallback;
    }

    public ServerInformation getServerInfo() {
        ServerInformation info = null;
        try {
            info = downloadAndParseServerInformation(defaultServerListUrl1);
        } catch (IOException e) {
            DobbyLog.v("Exception thrown while fetching config: " + e);
        }
        if (info == null) {
            try {
                info = downloadAndParseServerInformation(defaultServerListUrl2);
            } catch (IOException e) {
                String exceptionString = "Exception thrown while fetching config: " + e;
                DobbyLog.v(exceptionString);
                if (this.resultsCallback != null) {
                    this.resultsCallback.onServerInformationFetchError(exceptionString);
                }
            }
        }
        if (this.resultsCallback != null) {
            this.resultsCallback.onServerInformationFetch(info);
        }
        return info;
    }


    public ServerInformation getServerInfoFromUrlString (String urlString) {
        ServerInformation info = null;
        try {
            info = downloadAndParseServerInformation(urlString);
        } catch (IOException e) {
            System.out.println("Exception thrown  :" + e);
        } finally {
            return info;
        }
    }

    public ServerInformation downloadAndParseServerInformation(String urlString) throws IOException {
        InputStream stream = null;
        ServerInformation info = null;
        try {
            stream = Utils.getStreamFromUrl(urlString);
            info = new ServerInformation(stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        return info;
    }
}
