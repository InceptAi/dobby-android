package com.inceptai.wifimonitoringservice.actionlibrary.utils;

import android.annotation.TargetApi;
import android.net.Network;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;

import com.google.common.base.CharMatcher;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Utils class.
 */

public class ActionUtils {
    public static final String EMPTY_STRING = "";

    private ActionUtils() {}


    public static WifiConfiguration findBestConfiguredNetworkFromScanResult(
            List<WifiConfiguration> wifiConfigurationList, List<ScanResult> scanResultList) {
        WifiConfiguration wifiConfiguration;
        if (scanResultList == null || wifiConfigurationList == null) {
            return null;
        }
        Collections.sort(scanResultList, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult s1, ScanResult s2) {
                return (s2.level - s1.level);
            }
        });
        for (ScanResult scanResult: scanResultList) {
            wifiConfiguration = findWifiConfigurationForGivenScanResult(wifiConfigurationList, scanResult);
            if (wifiConfiguration != null) {
                return wifiConfiguration;
            }
        }
        return null;
    }

    private static WifiConfiguration findWifiConfigurationForGivenScanResult(
            List<WifiConfiguration> wifiConfigurationList, ScanResult scanResult) {
        if (wifiConfigurationList == null || scanResult == null) {
            return null;
        }
        for (WifiConfiguration wifiConfiguration: wifiConfigurationList) {
            if (matchWifiConfigurationWithScanResult(wifiConfiguration, scanResult)) {
                return wifiConfiguration;
            }
        }
        return null;
    }

    private static boolean matchWifiConfigurationWithScanResult(WifiConfiguration wifiConfiguration,
                                                               ScanResult scanResult) {
        if (wifiConfiguration == null || scanResult == null) {
            return false;
        }

        if (wifiConfiguration.BSSID != null && !wifiConfiguration.BSSID.equals("any") && scanResult.BSSID != null) {
            return wifiConfiguration.BSSID.equals(scanResult.BSSID);
        }

        if (wifiConfiguration.SSID != null && scanResult.SSID != null) {
            return ActionUtils.compareSSIDs(wifiConfiguration.SSID, scanResult.SSID);
        }

        return false;
    }

    private static boolean compareSSIDs(String ssid1, String ssid2) {
        String cleanSSID1 = CharMatcher.is('\"').trimFrom(ssid1);
        String cleanSSID2 = CharMatcher.is('\"').trimFrom(ssid2);
        return cleanSSID1.equals(cleanSSID2);
    }

    public static boolean checkIfWifiConfigurationMatchesListOfRouterIds(WifiConfiguration wifiConfiguration, List<String> routerIds) {
        List<String> routerBSSIDs = new ArrayList<>();
        List<String> routerSSIDs = new ArrayList<>();
        if (wifiConfiguration == null || routerIds == null) {
            return false;
        }

        for (String routerId: routerIds) {
            String routerBSSID = getValueFromRouterId(routerId, 0);
            String routerSSID = getValueFromRouterId(routerId, 1);
            routerBSSIDs.add(getValueFromRouterId(routerId, 0));
            routerSSIDs.add(getValueFromRouterId(routerId, 1));
        }

        if (wifiConfiguration.BSSID != null && !wifiConfiguration.BSSID.equals("any")) {
            return routerBSSIDs.contains(wifiConfiguration.BSSID);
        } else if (wifiConfiguration.SSID != null){
            for (String routerSSID: routerSSIDs) {
                if (compareSSIDs(wifiConfiguration.SSID, routerSSID)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static String getValueFromRouterId(String routerId, int position) {
        if (position != 0 && position != 1) {
            return EMPTY_STRING;
        }
        final String[] tokens = routerId.split(",");
        if (tokens.length != 2) {
            return ActionUtils.EMPTY_STRING;
        }
        final String[] valueTokens = tokens[position].split("-");
        if (valueTokens.length != 2) {
            return ActionUtils.EMPTY_STRING;
        }
        return valueTokens[1];
    }

    /**
     * Given a string url, connects and returns response code
     *
     * @param urlString       string to fetch
     * @param readTimeOutMs       read time out
     * @param connectionTimeOutMs       connection time out
     * @param urlRedirect       should use urlRedirect
     * @param useCaches       should use cache
     * @return httpResponseCode http response code
     * @throws IOException
     */

    public static int checkUrlWithOptions(String urlString,
                                          int readTimeOutMs,
                                          int connectionTimeOutMs,
                                          boolean urlRedirect,
                                          boolean useCaches) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(readTimeOutMs /* milliseconds */);
        connection.setConnectTimeout(connectionTimeOutMs /* milliseconds */);
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(urlRedirect);
        connection.setUseCaches(useCaches);
        // Starts the query
        connection.connect();
        int responseCode = connection.getResponseCode();
        connection.disconnect();
        return responseCode;
    }


    /**
     * Given a string url, connects and returns response code
     *
     * @param urlString       string to fetch
     * @param network       network
     * @param readTimeOutMs       read time out
     * @param connectionTimeOutMs       connection time out
     * @param urlRedirect       should use urlRedirect
     * @param useCaches       should use cache
     * @return httpResponseCode http response code
     * @throws IOException
     */

    @TargetApi(LOLLIPOP)
    public static int checkUrlWithOptionsOverNetwork(String urlString,
                                                     Network network,
                                                     int readTimeOutMs,
                                                     int connectionTimeOutMs,
                                                     boolean urlRedirect,
                                                     boolean useCaches) throws IOException {
        if (network == null) {
            return -1;
        }
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) network.openConnection(url);
        connection.setReadTimeout(readTimeOutMs /* milliseconds */);
        connection.setConnectTimeout(connectionTimeOutMs /* milliseconds */);
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(urlRedirect);
        connection.setUseCaches(useCaches);
        // Starts the query
        connection.connect();
        int responseCode = connection.getResponseCode();
        connection.disconnect();
        return responseCode;
    }

    //

}
