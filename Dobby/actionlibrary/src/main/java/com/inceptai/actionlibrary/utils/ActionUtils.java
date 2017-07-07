package com.inceptai.actionlibrary.utils;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;

import com.google.common.base.CharMatcher;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    public static WifiConfiguration findWifiConfigurationForGivenScanResult(
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

    public static boolean matchWifiConfigurationWithScanResult(WifiConfiguration wifiConfiguration,
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

    public static boolean compareSSIDs(String ssid1, String ssid2) {
        String cleanSSID1 = CharMatcher.is('\"').trimFrom(ssid1);
        String cleanSSID2 = CharMatcher.is('\"').trimFrom(ssid2);
        return cleanSSID1.equals(cleanSSID2);
    }

}
