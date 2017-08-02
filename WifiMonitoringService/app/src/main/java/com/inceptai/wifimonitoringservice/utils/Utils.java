package com.inceptai.wifimonitoringservice.utils;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;

import com.inceptai.wifimonitoringservice.WifiMonitoringService;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

/**
 * Created by vivek on 7/11/17.
 */

public class Utils {
    public static String EMPTY_STRING = "";
    private static final int MAX_SSID_LENGTH = 30;
    private static final int READ_TIMEOUT_MS = 5000;
    private static final int CONNECTION_TIMEOUT_MS = 5000;

    public static int computeMovingAverageSignal(int currentSignal, int previousSignal, long currentSeen, long previousSeen, int maxAge) {
        if (currentSeen == 0) {
            currentSeen = System.currentTimeMillis();
        }
        long age = currentSeen - previousSeen;
        if (previousSeen > 0 && age > 0 && age < maxAge / 2) {
            // Average the RSSI with previously seen instances of this scan result
            double alpha = 0.5 - (double) age / (double) maxAge;
            currentSignal = (int) ((double) currentSignal * (1 - alpha) + (double) previousSignal * alpha);
        }
        return currentSignal;
    }

    /**
     * Checks whether the "Avoid poor networks" setting (named "Auto network switch" on
     * some Samsung devices) is enabled, which can in some instances interfere with Wi-Fi.
     *
     * @return true if the "Avoid poor networks" or "Auto network switch" setting is enabled
     */
    public static boolean isPoorNetworkAvoidanceEnabled (Context ctx) {
        final int SETTING_UNKNOWN = -1;
        final int SETTING_ENABLED = 1;
        final String AVOID_POOR = "wifi_watchdog_poor_network_test_enabled";
        final String WATCHDOG_CLASS = "android.net.wifi.WifiWatchdogStateMachine";
        final String DEFAULT_ENABLED = "DEFAULT_POOR_NETWORK_AVOIDANCE_ENABLED";
        final ContentResolver cr = ctx.getContentResolver();

        int result;

        if (SDK_INT >= JELLY_BEAN_MR1) {
            //Setting was moved from Secure to Global as of JB MR1
            result = Settings.Global.getInt(cr, AVOID_POOR, SETTING_UNKNOWN);
        } else if (SDK_INT >= ICE_CREAM_SANDWICH_MR1) {
            result = Settings.Secure.getInt(cr, AVOID_POOR, SETTING_UNKNOWN);
        } else {
            //Poor network avoidance not introduced until ICS MR1
            //See android.provider.Settings.java
            return false;
        }

        //Exit here if the setting value is known
        if (result != SETTING_UNKNOWN) {
            return (result == SETTING_ENABLED);
        }

        //Setting does not exist in database, so it has never been changed.
        //It will be initialized to the default value.
        if (SDK_INT >= JELLY_BEAN_MR1) {
            //As of JB MR1, a constant was added to WifiWatchdogStateMachine to determine
            //the default behavior of the Avoid Poor Networks setting.
            try {
                //In the case of any failures here, take the safe route and assume the
                //setting is disabled to avoid disrupting the user with false information
                Class wifiWatchdog = Class.forName(WATCHDOG_CLASS);
                Field defValue = wifiWatchdog.getField(DEFAULT_ENABLED);
                if (!defValue.isAccessible()) defValue.setAccessible(true);
                return defValue.getBoolean(null);
            } catch (IllegalAccessException ex) {
                return false;
            } catch (NoSuchFieldException ex) {
                return false;
            } catch (ClassNotFoundException ex) {
                return false;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        } else {
            //Prior to JB MR1, the default for the Avoid Poor Networks setting was
            //to enable it unless explicitly disabled
            return true;
        }
    }

    /** Gets a String resource from another installed application */
    public static String getExternalString(Context ctx, String namespace,
                                           String key, String defVal) {
        int resId = getExternalIdentifier(ctx, namespace, key, "string");
        if (resId != 0) {
            Resources res = getExternalResources(ctx, namespace);
            return res.getString(resId);
        } else {
            return defVal;
        }
    }

    //Broadcast notification info
    public static void sendNotificationInfo(Context context, String title, String body, int notificationId) {
        ServiceLog.v("Broadcasting message with title/body " + title + " / " + body);
        Intent intent = new Intent(WifiMonitoringService.NOTIFICATION_INFO_INTENT_VALUE);
        intent.putExtra(WifiMonitoringService.EXTRA_NOTIFICATION_TITLE, title);
        intent.putExtra(WifiMonitoringService.EXTRA_NOTIFICATION_BODY, body);
        intent.putExtra(WifiMonitoringService.EXTRA_NOTIFICATION_ID, notificationId);
        // You can also include some extra data.
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    //Private stuff
    /** Gets the resources of another installed application */
    private static Resources getExternalResources(Context ctx, String namespace) {
        PackageManager pm = ctx.getPackageManager();
        try {
            return (pm == null) ? null : pm.getResourcesForApplication(namespace);
        } catch (PackageManager.NameNotFoundException ex) {
            return null;
        }
    }

    /** Gets a resource ID from another installed application */
    private static int getExternalIdentifier(Context ctx, String namespace,
                                             String key, String type) {
        Resources res = getExternalResources(ctx, namespace);
        return (res == null) ? 0 : res.getIdentifier(key, type, namespace);
    }



    /*
     * Disables component so we don't get alarms
     * respects disabled state
     */
    public static void setComponentEnabled(Context context,
                                           Class<?> cls, Boolean state) {
        PackageManager pm = context.getPackageManager();
        ComponentName service = new ComponentName(context, cls);
        if (state)
            pm.setComponentEnabledSetting(service,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        else {
            pm.setComponentEnabledSetting(service,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    public static void disableBootReceiver(Context context, Class<?> cls) {
        updateBootReceiver(context, cls, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    public static void enableBootReceiver(Context context, Class<?> cls) {
        updateBootReceiver(context, cls, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    public static String convertMillisecondsToTimeForNotification(long currentTimeMillis) {
        DateFormat formatter = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
        return formatter.format(new Date(currentTimeMillis));
    }

    public static void updateBootReceiver(Context context, Class<?> cls, int flag) {
        ComponentName component = new ComponentName(context, cls);
        context.getPackageManager().setComponentEnabledSetting(component, flag, PackageManager.DONT_KILL_APP);
    }

    public static String limitSSID(String ssid) {
        if (ssid != null && !ssid.isEmpty()) {
            if (ssid.length() > MAX_SSID_LENGTH) {
                ssid = ssid.substring(0, MAX_SSID_LENGTH);
                if (ssid.startsWith("\"") || ssid.startsWith("'")) {
                    ssid = ssid + ssid.substring(0, 1);
                }
            }
        }
        return ssid;
    }

    public static String userReadableRepairSummary(boolean repairSuccessful,
                                                   boolean toggleSuccessful,
                                                   WifiInfo wifiInfo) {
        StringBuilder sb = new StringBuilder();
        if (repairSuccessful) {
            if (wifiInfo != null) {
                sb.append("Hooray ! You are connected and online via wifi network: " + wifiInfo.getSSID() + ". ");
                sb.append(" Run full tests to see what speeds are you currently getting !");
            } else {
                //Never happens
                sb.append("Repair was successful and you are now online via WiFi !");
                sb.append("Run full tests to see what speeds are you currently getting !");
            }
        } else {
            if (wifiInfo != null) {
                sb.append("You are connected to wifi network: " + wifiInfo.getSSID() + " but we can't reach Internet through this network. " +
                        "This could be an issue with the router your Internet provider or you could be behind " +
                        "a captive portal which requires sign-in for access.");
                sb.append("Run full tests to see what could be the issue here.");
            } else {
                if (toggleSuccessful) {
                    sb.append("Sorry, we were unable to repair your WiFi connection. " +
                            "We were unable to find a good WiFi network to connect to. " +
                            "Try running the full tests to see whats going on. ");
                } else {
                    sb.append("Sorry, we were unable to repair your WiFi connection. " +
                            "Specifically, if your WiFi won't turn on, it could be a memory issue, so make sure to clean unused " +
                            "apps and have enough RAM on your device.");
                }
            }
        }
        return sb.toString();
    }

    //Common utils as dobby
    /**
     * converts string to int with default value if unsuccessful
     *
     * @param defaultValue
     * @param inputString
     * @return
     */
    public static int parseIntWithDefault(int defaultValue, String inputString) {
        int valueToReturn = defaultValue;
        if (inputString != null) {
            valueToReturn = Integer.parseInt(inputString);
        }
        return valueToReturn;
    }

    /**
     * converts string to double with default value if unsuccessful
     *
     * @param defaultValue
     * @param inputString
     * @return
     */
    public static double parseDoubleWithDefault(double defaultValue, String inputString) {
        double valueToReturn = defaultValue;
        if (inputString != null) {
            valueToReturn = Double.parseDouble(inputString);
        }
        return valueToReturn;
    }

    /**
     * Skips one xml tag -- handles tags with nested tags
     *
     * @param parser
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }


    /**
     * Computes distance
     *
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return
     */
    public static double computeDistance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = (Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))) +
                (Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta)));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    /**
     * This function converts decimal degrees to radians
     *
     * @param deg
     * @return
     */
    public static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /**
     * This function converts radians to decimal degrees
     *
     * @param rad
     * @return
     */
    public static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    //math utilities
    public static double computeAverage(double in[]) throws IllegalArgumentException {
        if (in.length == 0) {
            throw new IllegalArgumentException("Array length is 0");
        }
        double sum = 0;
        for (int i = 0; i < in.length; i++) {
            sum += in[i];
        }
        return sum / (double) (in.length);
    }

    /**
     * Given a string url, connects and returns an input stream
     *
     * @param urlString       string to fetch
     * @param maxStringLength maximum length of the string in bytes
     * @return
     * @throws IOException
     */
    public static String getDataFromUrlWithTimeouts(String urlString, int maxStringLength,
                                                    int readTimeOutMs, int connectionTimeOutMs)
            throws IOException {
        final boolean DEFAULT_URL_REDIRECT = true;
        final boolean DEFAULT_USE_CACHES = true;
        return getDataFromUrlWithOptions(urlString, maxStringLength,
                readTimeOutMs, connectionTimeOutMs,
                DEFAULT_URL_REDIRECT, DEFAULT_USE_CACHES);
    }


    public static class HTTPReturnCodeException extends IOException {
        public int httpReturnCode = 0;

        public HTTPReturnCodeException(int httpReturnCode) {
            this.httpReturnCode = httpReturnCode;
        }
    }

    /**
     * Converts the contents of an InputStream to a String.
     *
     * @param stream
     * @param maxLength
     * @return
     * @throws IOException
     */
    public static String readStream(InputStream stream, int maxLength) throws IOException {
        String result = Utils.EMPTY_STRING;
        // Read InputStream using the UTF-8 charset.
        InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
        // Create temporary buffer to hold Stream data with specified max length.
        char[] buffer = new char[maxLength];
        // Populate temporary buffer with Stream data.
        int numChars = 0;
        int readSize = 0;
        while (numChars < maxLength && readSize != -1) {
            numChars += readSize;
            int pct = (100 * numChars) / maxLength;
            //publishProgress(DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS, pct);
            readSize = reader.read(buffer, numChars, buffer.length - numChars);
        }
        if (numChars != -1) {
            // The stream was not empty.
            // Create String that is actual length of response body if actual length was less than
            // max length.
            numChars = Math.min(numChars, maxLength);
            result = new String(buffer, 0, numChars);
        }
        return result;
    }


    /**
     * Given a string url, connects and returns an input stream
     *
     * @param urlString       string to fetch
     * @param maxStringLength maximum length of the string in bytes
     * @return
     * @throws IOException
     */
    public static String getDataFromUrlWithOptions(String urlString,
                                                   int maxStringLength,
                                                   int readTimeOutMs,
                                                   int connectionTimeOutMs,
                                                   boolean urlRedirect,
                                                   boolean useCaches) throws IOException {
        String outputString = null;
        InputStream stream = null;
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
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new HTTPReturnCodeException(responseCode);
        }
        stream = connection.getInputStream();
        if (stream == null) {
            throw new IOException("Could not get Input Stream from the URL" + urlString);
        }
        outputString = readStream(stream, maxStringLength);
        if (stream != null) {
            stream.close();
        }
        if (connection != null) {
            connection.disconnect();
        }
        return outputString;
    }


    /**
     * Given a string url, connects and returns an input stream
     *
     * @param urlString string to fetch
     * @return
     * @throws IOException
     */
    public static InputStream getStreamFromUrl(String urlString) throws IOException {
        String outputString = null;
        InputStream stream = null;
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(READ_TIMEOUT_MS /* milliseconds */);
        connection.setConnectTimeout(CONNECTION_TIMEOUT_MS /* milliseconds */);
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        // Starts the query
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error code: " + responseCode);
        }
        stream = connection.getInputStream();
        return stream;
    }

    public static double computePercentileFromSortedList(List<Double> sortedList, int percentile)  {
        int size = sortedList.size();
        if (size == 0) {
            return 0;
        }
        int index = (percentile * size) / 100;
        //special case for median
        if (percentile == 50) {
            int index1 = index + 1;
            if (index1 < size) {
                double median = (sortedList.get(index) + sortedList.get(index1)) / 2;
                return median;
            }
        } else if (percentile == 100) {
            return sortedList.get(size - 1);
        } else if (percentile == 0) {
            return sortedList.get(0);
        }
        return sortedList.get(index);
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

}
