package com.inceptai.dobby.speedtest;

import android.util.Xml;

import com.inceptai.dobby.utils.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vivek on 4/1/17.
 */

public class ServerInformation {
    private static final String ns = null;
    public List<ServerDetails> serverList = new ArrayList<ServerDetails>();

    public ServerInformation(List<ServerDetails> serverList) {
        this.serverList = serverList;
    }

    public ServerInformation(InputStream in) {
        ServerInformation info = parseServerInformation(in);
        if (info != null) {
            this.serverList = info.serverList;
        }
    }


    public class ServerDetails {
        public int serverId;
        public double lat;
        public double lon;
        public URL url;
        public String host;
        public String country;
        public double distance;

        public ServerDetails(int serverId, double lat, double lon,
                             URL url, String host, String country) {
            this.serverId = serverId;
            this.lat = lat;
            this.lon = lon;
            this.url = url;
            this.host = host;
            this.country = country;
            this.distance = 0;
        }
    }
    // Parses the contents of an client config. If it encounters a ip, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    private ServerInformation readServerInformation(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "settings");
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, ns, "servers");
        List<ServerDetails> serverList = new ArrayList<ServerDetails>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("server")) {
                ServerDetails serverDetails = readServerDetails(parser);
                serverList.add(serverDetails);
                parser.nextTag();
            } else {
                Utils.skip(parser);
            }
        }
        return new ServerInformation(serverList);
    }

    // Processes link tags in the feed.
    public ServerDetails readServerDetails(XmlPullParser parser) throws IOException, XmlPullParserException {
        ServerDetails serverDetails = null;
        parser.require(XmlPullParser.START_TAG, ns, "server");
        String serverIdString = parser.getAttributeValue(null, "id");
        String urlString = parser.getAttributeValue(null, "url");
        String latString = parser.getAttributeValue(null, "lat");
        String lonString = parser.getAttributeValue(null, "lon");
        String country = parser.getAttributeValue(null, "country");
        String host = parser.getAttributeValue(null, "host");
        int serverId = Utils.parseIntWithDefault(0, serverIdString);
        double lat = Utils.parseDoubleWithDefault(0, latString);
        double lon = Utils.parseDoubleWithDefault(0, lonString);
        URL url = new URL(urlString);
        return new ServerDetails(serverId, lat, lon, url, host, country);
    }

    public ServerInformation parseServerInformation(InputStream in) {
        ServerInformation info = null;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            info = readServerInformation(parser);
        } catch (XmlPullParserException|IOException e){
            System.out.println("Exception thrown  :" + e);
        }
        try {
            in.close();
        } catch (IOException e) {
            System.out.println("Exception thrown  :" + e);
        }
        return info;
    }
}
