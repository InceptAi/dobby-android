package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.support.annotation.Nullable;

import com.inceptai.wifimonitoringservice.utils.ServiceLog;
import com.inceptai.wifimonitoringservice.utils.Utils;

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

    //Default serverInformation if any
    private ServerInformation serverInformation;
    private Context context;
    private int defaultXmlListId;

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

    public ParseServerInformation(String urlString1, String urlString2,
                                  int defaultXmlListId,
                                  Context context,
                                  @Nullable ResultsCallback resultsCallback) {
        this.serverListUrl1 = urlString1;
        this.serverListUrl2 = urlString2;
        this.resultsCallback = resultsCallback;
        this.context = context;
        this.defaultXmlListId = defaultXmlListId;
    }

    public ParseServerInformation(@Nullable ResultsCallback resultsCallback) {
        this.serverListUrl1 = defaultServerListUrl1;
        this.serverListUrl2 = defaultServerListUrl2;
        this.resultsCallback = resultsCallback;
    }

    public ParseServerInformation(int defaultXmlListId,
                                  Context context,
                                  @Nullable ResultsCallback resultsCallback) {
        this.serverListUrl1 = defaultServerListUrl1;
        this.serverListUrl2 = defaultServerListUrl2;
        this.resultsCallback = resultsCallback;
        this.context = context;
        this.defaultXmlListId = defaultXmlListId;
    }

    private void initializeServerInformation(int xmlFileId) {
        XmlResourceParser xmlResourceParser = this.context.getResources().getXml(xmlFileId);
        this.serverInformation = new ServerInformation(xmlResourceParser);
    }

    ServerInformation fetchServerInfo() {
        ServerInformation info = null;
        try {
            info = downloadAndParseServerInformation(defaultServerListUrl1);
        } catch (IOException e) {
            ServiceLog.v("Exception thrown while fetching server information try 1: " + e);
        }
        if (info == null) {
            try {
                info = downloadAndParseServerInformation(defaultServerListUrl2);
            } catch (IOException e) {
                String exceptionString = "Exception thrown while fetching server information try 2: " + e;
                ServiceLog.v(exceptionString);
                if (this.resultsCallback != null) {
                    this.resultsCallback.onServerInformationFetchError(exceptionString);
                }
            }
        }
        if (this.resultsCallback != null) {
            this.resultsCallback.onServerInformationFetch(info);
        }
        if (info != null) {
            this.serverInformation = info;
        }
        return info;
    }

    ServerInformation getServerInfo(boolean enableFetchIfNeeded) {
        //No fetch, just return what we have
        initializeServerInformation(defaultXmlListId);
        if (! enableFetchIfNeeded || serverInformation != null) {
            return serverInformation;
        }
        return fetchServerInfo();
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
