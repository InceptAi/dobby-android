package com.inceptai.dobby.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.google.common.net.InetAddresses;
import com.google.gson.Gson;
import com.inceptai.dobby.BuildConfig;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.MainActivity;
import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.database.ActionRecord;
import com.inceptai.dobby.speedtest.BandwidthTestCodes;
import com.inceptai.dobby.ui.WifiDocActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.inceptai.dobby.expert.ExpertChatService.INTENT_NOTIF_SOURCE;

/**
 * Utils class.
 */

public class Utils {
    public static final String EMPTY_STRING = "";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String TRUE_STRING = "true";
    public static final String FALSE_STRING = "false";
    public static final String ZERO_POINT_ZERO = "0.0";
    public static final String WIFIDOC_FLAVOR = "wifidoc";
    public static final String WIFIEXPERT_FLAVOR = "dobby";
    public static final String PREFERENCES_FILE = BuildConfig.FLAVOR + "_" + BuildConfig.BUILD_TYPE + "_settings";
    public static final String USER_UUID = "userUuid";

    private static final int READ_TIMEOUT_MS = 5000;
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    public static final String UNKNOWN_LATENCY_STRING = "--";
    private static final int MAX_SSID_LENGTH = 30;
    public static final String WIFI_MONITORING_SERVICE_STATE = "WifiMonitoringServiceState";



    private static Random random = new Random();

    private Utils() {
    }

    public static String timeSecondsToString(long totalSecs) {
        int hours = (int) totalSecs / 3600;
        int minutes = (int) (totalSecs % 3600) / 60;
        int seconds = (int) totalSecs % 60;

        if (hours > 0) {
            return String.format("%02d hours and %02d minutes.", hours, minutes);
        }

        if (minutes > 0) {
            return String.format("%02d minutes.", minutes);
        }

        return String.format("%02d seconds.", seconds);
    }

    public static String fetchUuid(Context context) {
        String uuid = Utils.readSharedSetting(context, USER_UUID, EMPTY_STRING);
        if (EMPTY_STRING.equals(uuid)) {
            uuid = UUID.randomUUID().toString();
            Utils.saveSharedSetting(context, USER_UUID, uuid);
        }
        return uuid;
    }

    public static String limitSsid(String ssid) {
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

    public static Random getRandom() {
        return random;
    }

    public static class HTTPReturnCodeException extends IOException {
        public int httpReturnCode = 0;

        public HTTPReturnCodeException(int httpReturnCode) {
            this.httpReturnCode = httpReturnCode;
        }
    }

    public static Fragment setupFragment(AppCompatActivity appCompatActivity, Class fragmentClass, String tag, int resourceId) {

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = appCompatActivity.getSupportFragmentManager();
        Fragment existingFragment = fragmentManager.findFragmentByTag(tag);
        if (existingFragment == null) {
            try {
                existingFragment = (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                DobbyLog.e("Unable to create fragment: " + fragmentClass.getCanonicalName());
                return null;
            }
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(resourceId, existingFragment, tag);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        return existingFragment;
    }

    public static class PercentileStats {
        public double median;
        public double max;
        public double min;
        public double percentile90;
        public double getPercentile10;
        public int samples;

        public PercentileStats() {
            median = 0;
            max = 0;
            min = 0;
            percentile90 = 0;
            getPercentile10 = 0;
            samples = 0;
        }

        public PercentileStats(List<Double> list) {
            Collections.sort(list);
            median = computePercentileFromSortedList(list, 50);
            max = computePercentileFromSortedList(list, 100);
            min = computePercentileFromSortedList(list, 0);
            percentile90 = computePercentileFromSortedList(list, 90);
            getPercentile10 = computePercentileFromSortedList(list, 10);
            samples = list.size();
        }

        public String toJson() {
            Gson gson = new Gson();
            String json = gson.toJson(this);
            return json;
        }

        @Override
        public String toString() {
            return toJson();
        }
    }

    /**
     * Given a string url, connects and returns an input stream
     *
     * @param urlString       string to fetch
     * @param maxStringLength maximum length of the string in bytes
     * @return
     * @throws IOException
     */
    public static String getDataFromUrl(String urlString, int maxStringLength) throws IOException {
        return getDataFromUrlWithTimeouts(urlString, maxStringLength,
                READ_TIMEOUT_MS, CONNECTION_TIMEOUT_MS);
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

    /**
     * Closes the input stream and disconnects
     *
     * @param stream     input stream to close
     * @param connection connection to close
     * @throws IOException
     */
    public static void closeInputStreamAndDisconnect(InputStream stream, HttpURLConnection connection) throws IOException {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                throw e;
            }
        }
        if (connection != null) {
            connection.disconnect();
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
     * Run linux system command
     *
     * @param command
     */
    public static Process getPingProcess(String command) throws Exception {
        try {
            return Runtime.getRuntime().exec(command);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * Run linux system command with timeout
     *
     * @param command
     */
    public static String runSystemCommand(final String command,
                                          ScheduledExecutorService scheduledExecutorService,
                                          int timeoutMs) throws Exception {
        StringBuilder outputStringBuilder = new StringBuilder();
        final Process pingProcess = getPingProcess(command);
        ScheduledFuture<?> pingCommandFuture = scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                DobbyLog.v("Utils: KILLING process " + pingProcess.toString() + " for command " + command);
                pingProcess.destroy();
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);

        try {
            BufferedReader inputStream = new BufferedReader(
                    new InputStreamReader(pingProcess.getInputStream()));
            String s;
            // reading output stream of the command
            while ((s = inputStream.readLine()) != null) {
                outputStringBuilder.append(s);
                DobbyLog.v("Ping RunSystemCommand: " + s);
            }
        } finally {
            DobbyLog.v("Utils: Cancelling pingCommandFuture");
            pingCommandFuture.cancel(true);
        }
        return outputStringBuilder.toString();
    }

    /**
     * Run linux system command
     *
     * @param command
     */
    public static String runSystemCommand(String command) throws Exception {
        StringBuilder outputStringBuilder = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader inputStream = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));

            String s;
            // reading output stream of the command
            while ((s = inputStream.readLine()) != null) {
                outputStringBuilder.append(s);
                //DobbyLog.v("ping response: " + s);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            return outputStringBuilder.toString();
        }
    }

    /**
     * Run linux system command
     *
     * @param command
     */
    public static Reader getReaderForSystemCommand(String command) throws Exception {
        StringBuilder outputStringBuilder = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            return bufferedReader;
        } catch (IOException e) {
            DobbyLog.e(e.getStackTrace().toString());
            throw e;
        }
    }

    //Int to IP
    public static String intToIp(int ipAddress) {
        String returnValue = ((ipAddress & 0xFF) + "." +
                ((ipAddress >>>= 8) & 0xFF) + "." +
                ((ipAddress >>>= 8) & 0xFF) + "." +
                ((ipAddress >>>= 8) & 0xFF));
        boolean isValid = InetAddresses.isInetAddress(returnValue);
        if (!isValid) {
            return EMPTY_STRING;
        }
        return returnValue;
    }

    public static AlertDialog buildSimpleDialog(Context context, String title, String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
            }
        });
        return builder.show();
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

    public static int convertSignalDbmToPercent(int signalDbm) {
        final double MAX_SIGNAL_DBM = -30.0;
        final double MIN_SIGNAL_DBM = -110.0;
        double percent = (((double) signalDbm - MIN_SIGNAL_DBM) / (MAX_SIGNAL_DBM - MIN_SIGNAL_DBM)) * 100.0;
        if (percent > 100) {
            percent = 100;
        } else if (percent < 0) {
            percent = 1;
        }
        return (int) percent;
    }

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

    public static <K, V extends Comparable<? super V>> Map<K, V>
    sortHashMapByValueDescending(Map<K, V> map) {
        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V>
    sortHashMapByValueAscending(Map<K, V> map) {
        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V>
    sortHashMapByValue1(Map<K, V> map) {
        List<Map.Entry<K, V>> list =
                new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    //Wifi channel stuff

    public static int[] get2GHzChannelList() {
        final int MAX_CHANNELS_2GHZ = 11;
        final int CHANNEL_1_CENTER_FREQUENCY = 2412;
        final int GAP_BETWEEN_CHANNELS = 5;
        int[] channelList = new int[MAX_CHANNELS_2GHZ];
        for (int channelIndex = 0; channelIndex < MAX_CHANNELS_2GHZ; channelIndex++) {
            channelList[channelIndex] = CHANNEL_1_CENTER_FREQUENCY + (GAP_BETWEEN_CHANNELS * channelIndex);
        }
        return channelList;
    }

    public static int[] get5GHzChannelList() {
        return new int[]{5180, 5200, 5220, 5240, 5260, 5280, 5300, 5320,
                5500, 5520, 5540, 5560, 5580, 5600, 5620, 5640, 5660, 5680, 5700, 5745, 5765,
                5785, 5805, 5825};
    }

    public static int[] get2GHzAnd5GHzChannelList() {
        int[] channelList2GHz = Utils.get2GHzChannelList();
        int[] channelList5GHz = Utils.get5GHzChannelList();
        int[] combinedChannelList = new int[channelList2GHz.length + channelList5GHz.length];
        System.arraycopy(channelList2GHz, 0, combinedChannelList, 0, channelList2GHz.length);
        System.arraycopy(channelList5GHz, 0, combinedChannelList, channelList2GHz.length, channelList5GHz.length);
        return combinedChannelList;
    }

    public static int[] get2GHzNonOverlappingChannelList() {
        return new int[]{2412, 2437, 2462};
    }

    public static int convertCenterFrequencyToChannelNumber(int centerFrequency) {
        final int CHANNEL_11_CENTER_FREQUENCY = 2462;
        final int CHANNEL_1_CENTER_FREQUENCY = 2417;
        final int GAP_BETWEEN_CHANNELS = 5;
        if (centerFrequency > CHANNEL_11_CENTER_FREQUENCY || centerFrequency < CHANNEL_1_CENTER_FREQUENCY) {
            return -1;
        }
        int gap = (centerFrequency - CHANNEL_1_CENTER_FREQUENCY);
        if (gap % GAP_BETWEEN_CHANNELS != 0) {
            return -1;
        }
        return (gap / GAP_BETWEEN_CHANNELS) + 1;
    }

    public static void setImage(Context context, ImageView view, int resourceId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setImageDrawable(context.getResources().getDrawable(resourceId, context.getApplicationContext().getTheme()));
        } else {
            view.setImageDrawable(context.getResources().getDrawable(resourceId));
        }
    }

    public static String readSharedSetting(Context ctx, String settingName, String defaultValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(settingName, defaultValue);
    }

    public static void saveSharedSetting(Context ctx, String settingName, String settingValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(settingName, settingValue);
        editor.apply();
    }

    public static long readSharedSetting(Context ctx, String settingName, long defaultValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getLong(settingName, defaultValue);
    }

    public static void saveSharedSetting(Context ctx, String settingName, long settingValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(settingName, settingValue);
        editor.apply();
    }

    public static String convertIntegerDoubleHashMapToJsonString(HashMap<Integer, Double> integerDoubleHashMap) {
        Gson gson = new Gson();
        return gson.toJson(integerDoubleHashMap);
    }

    public static String convertHashMapToJson(Map<?, ?> hashMap) {
        Gson gson = new Gson();
        return gson.toJson(hashMap);
    }

    public static float dpToPixelsX(Context context, float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (metrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static float dpToPixelsY(Context context, float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (metrics.ydpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static float pixelsToDpX(Context context, float px) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return Math.round(px / (metrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static float pixelsToDpY(Context context, float px) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return Math.round(px / (metrics.ydpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }

    public static double toMbps(double bwBytes) {
        return bwBytes * 1.0e-6;
    }

    public static String toMbpsString(double bwBytes) {
        return String.format("%2.1f", toMbps(bwBytes));
    }

    public static String doubleToString(double value) {
        return String.format("%2.1f", value);
    }

    public static double toKbps(double bwBytes) {
        return bwBytes * 1.0e-3;
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public static boolean grepForString(String stringToGrep, List<String> stringList) {
        for (String stringToCheck: stringList) {
            if (stringToCheck == null || stringToGrep == null) {
                continue;
            }
            if (stringToCheck.toLowerCase().contains(stringToGrep.toLowerCase()) || stringToGrep.toLowerCase().contains(stringToCheck.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static double nonLinearBwScale(double input) {
        // 0 .. 5 maps to 0 .. 50
        if (input <= 5.0) {
            return input * 10.;
        }
        // 5 to 10 maps to 50 .. 62.5
        if (input <= 10.0) {
            return 12.5 * (input - 5.0) / 5.0 + 50.0;
        }

        // 10 to 20 maps to 62.5 .. 75.
        if (input < 20.0) {
            return 12.5 * (input - 10.0) / 10.0 + 62.5;
        }

        // 20 to 50 maps to 75 to 87.5
        if (input < 50.0) {
            return 12.5 * (input - 20.0) / 30.0 + 75;
        }

        // Upper bound by 100
        input = Math.min(100.0, input);
        // 50 to 100 maps to 87.5 to 100
        return 12.5 * (input - 50.0) / 50.0 + 87.5;
    }

    public static void safeSleep(int sleepTimeMs) {
        try {
            Thread.sleep(sleepTimeMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static boolean areSystemAnimationsEnabled(Context context) {
        float duration = 1;
        float transition = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            duration = Settings.Global.getFloat(
                    context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1);
            transition = Settings.Global.getFloat(
                    context.getContentResolver(),
                    Settings.Global.TRANSITION_ANIMATION_SCALE, 1);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            duration = Settings.System.getFloat(
                    context.getContentResolver(),
                    Settings.System.ANIMATOR_DURATION_SCALE, 1);
            transition = Settings.System.getFloat(
                    context.getContentResolver(),
                    Settings.System.TRANSITION_ANIMATION_SCALE, 1);
        }
        DobbyLog.v("Animation duration == " + duration + " and transition = " + transition);
        return (duration != 0 && transition != 0);
    }

    public static void printXmlResourceParser(XmlResourceParser xmlResourceParser) {
        try {
            int eventType = xmlResourceParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        DobbyLog.v("Start document");
                        break;
                    case XmlPullParser.START_TAG:
                        DobbyLog.v("Start tag " + xmlResourceParser.getName());
                        break;
                    case XmlPullParser.END_TAG:
                        DobbyLog.v("End tag " + xmlResourceParser.getName());
                        break;
                    case XmlPullParser.TEXT:
                        DobbyLog.v("Text " + xmlResourceParser.getText());
                        break;
                    default:
                        DobbyLog.e("Unexpected eventType = " + eventType);
                }
                eventType = xmlResourceParser.next();
            }
        } catch (XmlPullParserException|IOException e) {
            DobbyLog.e("Error while parsing xml " + e);
        }
    }

    public static class BandwidthValue {
        @BandwidthTestCodes.TestMode
        public int mode;
        public double value;
        public static BandwidthValue from(int mode, double value) {
            BandwidthValue bandwidthValue = new BandwidthValue();
            bandwidthValue.mode = mode;
            bandwidthValue.value = value;
            return bandwidthValue;
        }
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    public static String getDeviceDetails() {
        HashMap<String, String> phoneInfo = new HashMap<>();
        phoneInfo.put("manufacturer", Build.MANUFACTURER);
        phoneInfo.put("model", Build.MODEL);
        phoneInfo.put("release", Build.VERSION.RELEASE);
        phoneInfo.put("sdk", Integer.toString(Build.VERSION.SDK_INT));
        phoneInfo.put("hardware", Build.HARDWARE);
        phoneInfo.put("product", Build.PRODUCT);
        return convertHashMapToJson(phoneInfo);
    }

    public static String getWifiManufacturer(String mac) {
        final int dataLengthToReadMs = 500;
        final int connectionTimeOutMs = 1000;
        final int readTimeOutMs = 1000;
        final String macToVendorUrl = "http://api.macvendors.com/" + mac;
        String manufacturer = Utils.EMPTY_STRING;
        try {
            manufacturer = Utils.getDataFromUrlWithTimeouts(macToVendorUrl, dataLengthToReadMs,
                    readTimeOutMs, connectionTimeOutMs);
        } catch (IOException e) {
            DobbyLog.v("Exception while getting manufacturer");
        }
        return manufacturer;
    }

    public static PendingIntent getPendingIntentForNotification(Context context, String source) {
        boolean isWifiTester = false;

        if (BuildConfig.FLAVOR.equals(WIFIDOC_FLAVOR)) {
            isWifiTester = true;
        } else if (!(BuildConfig.FLAVOR.equals(WIFIEXPERT_FLAVOR))) {
            DobbyLog.e("Unknown build flavor: " + BuildConfig.FLAVOR);
            return null;
        }
        Intent intent = null;

        if (isWifiTester) {
            intent = new Intent(context, WifiDocActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else {
            intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        if (source != null) {
            intent.putExtra(INTENT_NOTIF_SOURCE, source);
        }

        PendingIntent pendingIntent = null;
        if (isWifiTester) {
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            // Adds the back stack
            stackBuilder.addParentStack(WifiDocActivity.class);
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(intent);
            // Gets a PendingIntent containing the entire back stack
            pendingIntent =
                    stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            //pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        }

        return pendingIntent;
    }

    public static void saveWifiMonitoringEnabled(Context context) {
        saveSharedSetting(context, WIFI_MONITORING_SERVICE_STATE, TRUE_STRING);
    };

    public static void saveWifiMonitoringDisabled(Context context) {
        saveSharedSetting(context, WIFI_MONITORING_SERVICE_STATE, FALSE_STRING);
    };

    public static boolean checkIsWifiMonitoringEnabled(Context context, boolean defaultState) {
        String defaultStateString = defaultState ? Utils.TRUE_STRING : Utils.FALSE_STRING;
        return Boolean.valueOf(readSharedSetting(context, WIFI_MONITORING_SERVICE_STATE, defaultStateString));
    }

    public static String userReadableRepairSummary(boolean repairSuccessful, boolean toggleSuccessful, WifiInfo wifiInfo) {
       StringBuilder sb = new StringBuilder();
        if (repairSuccessful) {
            if (wifiInfo != null) {
                sb.append("Hooray ! You are connected and online via wifi network: " + wifiInfo.getSSID() + ". ");
                sb.append(DataInterpreter.convertSignalToReadableMessage(wifiInfo.getRssi()));
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

//    public static void launchWifiExpertMainActivity(Context context) {
//        Intent i = new Intent(context, MainActivity.class);
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(i);
//    }

    public static void launchWifiExpertMainActivity(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // You need this if starting
        //  the activity from a service
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        context.startActivity(intent);
    }

    public static void resumeWifiExpertMainActivity(Context context) {
        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    public static ActionRecord createActionRecord(String userId, String phoneInfo,
                                            @Nullable DataInterpreter.BandwidthGrade bandwidthGrade,
                                            @Nullable DataInterpreter.WifiGrade wifiGrade,
                                            @Nullable DataInterpreter.PingGrade pingGrade,
                                            @Nullable DataInterpreter.HttpGrade httpGrade,
                                            @Nullable Location location) {
        ActionRecord actionRecord = new ActionRecord();
        actionRecord.uid = userId;
        actionRecord.phoneInfo = phoneInfo;
        actionRecord.appVersion = DobbyApplication.getAppVersion();
        actionRecord.actionType = "UNKNOWN";

        int count = 0;
        //Assign all the grades
        if (bandwidthGrade != null) {
            actionRecord.bandwidthGradeJson = bandwidthGrade.toJson();
            actionRecord.actionType = "BWTEST";
            count++;
        }

        if (wifiGrade != null) {
            actionRecord.wifiGradeJson = wifiGrade.toJson();
            actionRecord.actionType = "WIFI";
            count++;
        }

        if (pingGrade != null) {
            actionRecord.pingGradeJson = pingGrade.toJson();
            actionRecord.actionType = "PING";
            count++;
        }

        if (httpGrade != null) {
            actionRecord.httpGradeJson = httpGrade.toJson();
            actionRecord.actionType = "HTTP";
            count++;
        }

        if (count > 1) {
            actionRecord.actionType = "MIXED";
        }

        if (location != null) {
            actionRecord.lat = location.getLatitude();
            actionRecord.lon = location.getLongitude();
            actionRecord.accuracy = location.getAccuracy();
        }
        //Assign the timestamp
        actionRecord.timestamp = System.currentTimeMillis();
        return actionRecord;
    }


    public static boolean nullOrEmpty(String target) {
        return target == null || target.isEmpty() || target.equals("null");
    }

}
