package com.inceptai.dobby.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.google.common.net.InetAddresses;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Utils class.
 */

public class Utils {
    public static final String EMPTY_STRING = "";
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int CONNECTION_TIMEOUT_MS = 15000;

    private Utils() {}
    /**
     * Given a string url, connects and returns an input stream
     * @param urlString string to fetch
     * @param maxStringLength maximum length of the string in bytes
     * @return
     * @throws IOException
     */
    public static String getDataFromUrl(String urlString, int maxStringLength) throws IOException {
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
     * @param stream input stream to close
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
     * @param stream
     * @param maxLength
     * @return
     * @throws IOException
     */
    public static String readStream(InputStream stream, int maxLength) throws IOException {
        String result = null;
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
     * @param deg
     * @return
     */
    public static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /**
     * This function converts radians to decimal degrees
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
        return sum/(double)(in.length);
    }

    /**
     * Run linux system command
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
                Log.i(TAG, "ping response: " + s);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            return outputStringBuilder.toString();
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

    public static double computePercentileFromSortedList(List<Double> sortedList, int percentile) {
        int size = sortedList.size();
        int index = (percentile * size) / 100;
        //special case for median
        if (percentile == 50) {
            int index1 = index + 1;
            if (index1 < size) {
                double median = (sortedList.get(index) + sortedList.get(index1)) / 2;
                return median;
            }
        }
        return sortedList.get(index);
    }
}
