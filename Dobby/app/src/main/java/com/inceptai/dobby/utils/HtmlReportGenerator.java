package com.inceptai.dobby.utils;

import android.content.Context;

import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.DataInterpreter;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * Created by arunesh on 6/5/17.
 */

public class HtmlReportGenerator {

    private static final String HTML_BREAK = "<br/>";
    private static final String HTML_REPORT_FALLBACK = "Wifi Tester Speedtest Results" + HTML_BREAK +
    "Dated: %s." + HTML_BREAK + HTML_BREAK +
    "Test configuration:" + HTML_BREAK + "Test Duration: 10 seconds upload, 10 seconds download." + HTML_BREAK + HTML_BREAK +
    "  Test server location: %s " + HTML_BREAK +
    "  Latency to test server: %s ms." + HTML_BREAK + HTML_BREAK + "Bandwidth Results:" + HTML_BREAK +
            " Router IP: %s, ISP: %s" + HTML_BREAK +
    "  Download: %s Mbps  Upload: %s Mbps" + HTML_BREAK + HTML_BREAK + "Wifi Statistics:" + HTML_BREAK +
    "  SSID: %s Signal: %s dbM" + HTML_BREAK + HTML_BREAK + "Ping Statistics:" + HTML_BREAK +
    "Router: %s ms    Google: %s ms" + HTML_BREAK +
    "Your DNS: %s ms  Alt DNS: %s " + HTML_BREAK + "Contact: hello@obiai.tech for further support." + HTML_BREAK +
    "Download Wifi Tester at the Google Play Store.";

    private HtmlReportGenerator(){}

    public static String createHtmlFor(Context context, String testServerLocation, String testServerLatencyMs, String routerIp, String ispName,
                                       double uploadBw, double downloadBw, DataInterpreter.PingGrade pingGrade, DataInterpreter.WifiGrade wifiGrade) {
        InputStream is = context.getResources().openRawResource(R.raw.html_report);
        String downloadMbps = Utils.toMbpsString(downloadBw);
        String uploadMbps = Utils.toMbpsString(uploadBw);
        String date = new Date().toString();
        String htmlReport = HTML_REPORT_FALLBACK;
        String routerLatencyMs = Utils.doubleToString(pingGrade.getRouterLatencyMs());
        String googleLatencyMs = Utils.doubleToString(pingGrade.getExternalServerLatencyMs());
        String yourDnsLatencyMs = Utils.doubleToString(pingGrade.getDnsServerLatencyMs());
        String altDnsLatencyMs = Utils.doubleToString(pingGrade.getAlternativeDnsLatencyMs());
        String wifiSignalDbm = String.valueOf(wifiGrade.getPrimaryApSignal());
        String SSID = "\"" + wifiGrade.getPrimaryApSsid() + "\"";

        try {
            htmlReport = IOUtils.toString(is);
        } catch (IOException e) {
            DobbyLog.w("Exception attempting to read HTML report file.");
        }
        IOUtils.closeQuietly(is);
        String formattedReport = String.format(htmlReport, date, testServerLocation, testServerLatencyMs, routerIp, ispName, downloadMbps,
                uploadMbps, SSID, wifiSignalDbm, routerLatencyMs, googleLatencyMs, yourDnsLatencyMs, altDnsLatencyMs);
        return formattedReport;
    }
}
