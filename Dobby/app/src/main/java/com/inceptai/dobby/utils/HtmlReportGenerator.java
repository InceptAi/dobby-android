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

    private static final String HTML_REPORT_FALLBACK = "Wifi Tester Speedtest Results\n" +
    "Dated: %s.\n\n" +
    "Test configuration:\n" + "Test Duration: 10 seconds upload, 10 seconds download.\n" +
    "  Test server location: %s \n" +
    "  Latency to test server: %s ms.\n\n" + "Bandwidth Results:\n" +
            " Router IP: %s, ISP: %s\n" +
    "  Download: %s Mbps  Upload: %s Mbps\n\n" + "Wifi Statistics:\n" +
    "  SSID: %s Signal: %s dbM\n\n" + "Ping Statistics:\n" +
    "Router: %s ms    Google: %s ms\n" +
    "Your DNS: %s ms  Alt DNS: %s \n\n" + "Contact: hello@obiai.tech for further support.\n" +
    "Download Wifi Tester at the Google Play Store.\n";

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
