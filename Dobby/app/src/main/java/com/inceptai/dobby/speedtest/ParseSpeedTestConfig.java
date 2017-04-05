package com.inceptai.dobby.speedtest;

import com.inceptai.dobby.utils.Utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by vivek on 3/31/17.
 */

public class ParseSpeedTestConfig {
    public final SpeedTestConfig speedTestConfig;

    public ParseSpeedTestConfig(String urlString) {
        this.speedTestConfig = getConfigFromUrlString(urlString);
    }

    public static SpeedTestConfig getConfig(String mode) {
        final String httpConfigUrl = "https://www.speedtest.net/speedtest-config.php";
        final String httpsConfigUrl = "http://www.speedtest.net/speedtest-config.php";
        if (mode.contains("https")) {
            return getConfigFromUrlString(httpsConfigUrl);
        } else {
            return getConfigFromUrlString(httpConfigUrl);
        }
    }


    public static SpeedTestConfig getConfigFromUrlString (String urlString) {
        SpeedTestConfig config = null;
        try {
            config = downloadAndParseConfig(urlString);
        } catch (IOException e) {
            System.out.println("Exception thrown  :" + e);
        } finally {
            return config;
        }
    }

    public static SpeedTestConfig downloadAndParseConfig(String urlString) throws IOException {
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

}

