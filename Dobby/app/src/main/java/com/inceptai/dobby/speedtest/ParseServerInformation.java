package com.inceptai.dobby.speedtest;

import com.inceptai.dobby.utils.Utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by vivek on 4/1/17.
 */

public class ParseServerInformation {
    public ServerInformation serverInformation;

    public ParseServerInformation(String urlString) {
        this.serverInformation = getServerInfoFromUrlString(urlString);
    }

    public static ServerInformation getServerInfo() {
        final String serverListUrl1 = "http://www.speedtest.net/speedtest-servers-static.php";
        final String serverListUrl2 = "http://c.speedtest.net/speedtest-servers-static.php";
        ServerInformation info = getServerInfoFromUrlString(serverListUrl1);
        if (info == null) {
            info = getServerInfoFromUrlString(serverListUrl2);
        }
        return info;
    }


    public static ServerInformation getServerInfoFromUrlString (String urlString) {
        ServerInformation info = null;
        try {
            info = downloadAndParseServerInformation(urlString);
        } catch (IOException e) {
            System.out.println("Exception thrown  :" + e);
        } finally {
            return info;
        }
    }

    public static ServerInformation downloadAndParseServerInformation(String urlString) throws IOException {
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
