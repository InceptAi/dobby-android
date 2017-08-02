package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest;

import android.util.Xml;

import com.inceptai.wifimonitoringservice.utils.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by vivek on 3/31/17.
 */

public class SpeedTestConfig {
    private static final String ns = null;
    public ClientConfig clientConfig = null;
    public ServerConfig serverConfig = null;
    public DownloadConfig downloadConfig = null;
    public UploadConfig uploadConfig = null;

    public SpeedTestConfig(ClientConfig clientConfig, ServerConfig serverConfig,
                           DownloadConfig downloadConfig, UploadConfig uploadConfig) {
        this.clientConfig = clientConfig;
        this.serverConfig = serverConfig;
        this.downloadConfig = downloadConfig;
        this.uploadConfig = uploadConfig;
    }

    public SpeedTestConfig(InputStream in) {
        SpeedTestConfig newConfig = parseSpeedTestConfig(in);
        if (newConfig != null) {
            this.clientConfig = newConfig.clientConfig;
            this.serverConfig = newConfig.serverConfig;
            this.downloadConfig = newConfig.downloadConfig;
            this.uploadConfig = newConfig.uploadConfig;
        }
    }


    //Client config
    public class ClientConfig {
        public final String ip;
        public final String isp;
        public final double lat;
        public final double lon;

        public ClientConfig(String ip, String isp, double lat, double lon) {
            this.ip = ip;
            this.isp = isp;
            this.lat = lat;
            this.lon = lon;
        }
    }

    //Server config
    public class ServerConfig {
        public final int threadCount;
        public final int[] ignoreIds;

        public ServerConfig(int threadCount, int[] ignoreIds) {
            this.threadCount = threadCount;
            this.ignoreIds = ignoreIds;
        }
    }

    //Download config
    public class DownloadConfig {
        public final int testLength;
        public final int threadsPerUrl;

        public DownloadConfig(int testLength, int threadsPerUrl) {
            this.testLength = testLength;
            this.threadsPerUrl = threadsPerUrl;
        }
    }

    //Upload config
    public class UploadConfig {
        public final int testLength;
        public final int threads;
        public final int maxChunkCount;
        public final int ratio;

        public UploadConfig(int testLength, int threads, int maxChunkCount, int ratio) {
            this.testLength = testLength;
            this.threads = threads;
            this.maxChunkCount = maxChunkCount;
            this.ratio = ratio;
        }
    }

    // Parses the contents of an client config. If it encounters a ip, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    private SpeedTestConfig readSpeedTestConfig(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "settings");
        boolean all_configs = false;
        ClientConfig clientConfig = null;
        ServerConfig serverConfig = null;
        DownloadConfig downloadConfig = null;
        UploadConfig uploadConfig = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("client")) {
                clientConfig = readClientConfig(parser);
                parser.nextTag();
            } else if (name.equals("server-config")) {
                serverConfig = readServerConfig(parser);
                parser.nextTag();
            } else if (name.equals("download")) {
                downloadConfig = readDownloadConfig(parser);
                parser.nextTag();
            } else if (name.equals("upload")) {
                uploadConfig = readUploadConfig(parser);
                parser.nextTag();
            } else {
                Utils.skip(parser);
            }
            if (clientConfig != null && serverConfig != null && downloadConfig != null && uploadConfig != null) {
                break;
            }
        }
        return new SpeedTestConfig(clientConfig, serverConfig, downloadConfig, uploadConfig);
    }


    //<link rel="alternate" href="http://stackoverflow.com/questions/9439999/where-is-my-data-file" />

    // Processes link tags in the feed.
    private ClientConfig readClientConfig(XmlPullParser parser) throws IOException, XmlPullParserException {
        ClientConfig config = null;
        parser.require(XmlPullParser.START_TAG, ns, "client");
        String ip = parser.getAttributeValue(null, "ip");
        String latString = parser.getAttributeValue(null, "lat");
        String lonString = parser.getAttributeValue(null, "lon");
        String isp = parser.getAttributeValue(null, "isp");
        double lat = Utils.parseDoubleWithDefault(0, latString);
        double lon = Utils.parseDoubleWithDefault(0, lonString);
        return new ClientConfig(ip, isp, lat, lon);
    }


    // Processes link tags in the feed.
    private ServerConfig readServerConfig(XmlPullParser parser) throws IOException, XmlPullParserException {
        ServerConfig config = null;
        parser.require(XmlPullParser.START_TAG, ns, "server-config");
        int threadCount = Utils.parseIntWithDefault(-1, parser.getAttributeValue(null, "threadcount"));
        String ignoreIdsString = parser.getAttributeValue(null, "ignoreids");
        int ignoreIds[] = new int[0];
        if (ignoreIdsString != null) {
            String[] IdsSplitString = ignoreIdsString.split(",");
            ignoreIds = new int[IdsSplitString.length];
            for(int index=0; index < IdsSplitString.length; index++)
            {
                ignoreIds[index] = Utils.parseIntWithDefault(0, IdsSplitString[index]);
            }
        }
        return new ServerConfig(threadCount, ignoreIds);
    }

    // Processes link tags in the feed.
    private DownloadConfig readDownloadConfig(XmlPullParser parser) throws IOException, XmlPullParserException {
        DownloadConfig config = null;
        parser.require(XmlPullParser.START_TAG, ns, "download");
        int testLength = Utils.parseIntWithDefault(-1, parser.getAttributeValue(null, "testlength"));
        int threadsPerUrl = Utils.parseIntWithDefault(-1, parser.getAttributeValue(null, "threadsperurl"));
        return new DownloadConfig(testLength, threadsPerUrl);
    }

    // Processes link tags in the feed.
    private UploadConfig readUploadConfig(XmlPullParser parser) throws IOException, XmlPullParserException {
        UploadConfig config = null;
        parser.require(XmlPullParser.START_TAG, ns, "upload");
        int testLength = Utils.parseIntWithDefault(-1, parser.getAttributeValue(null, "testlength"));
        int threads = Utils.parseIntWithDefault(-1, parser.getAttributeValue(null, "threads"));
        int maxChunkCount = Utils.parseIntWithDefault(-1, parser.getAttributeValue(null, "maxchunkcount"));
        int ratio = Utils.parseIntWithDefault(-1, parser.getAttributeValue(null, "ratio"));
        return new UploadConfig(testLength, threads, maxChunkCount, ratio);
    }

    public SpeedTestConfig parseSpeedTestConfig(InputStream in) {
        SpeedTestConfig configToReturn = null;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            configToReturn = readSpeedTestConfig(parser);
        } catch (XmlPullParserException|IOException e){
            System.out.println("Exception thrown  :" + e);
        }
        try {
            in.close();
        } catch (IOException e) {
            System.out.println("Exception thrown  :" + e);
        }
        return configToReturn;
    }


}