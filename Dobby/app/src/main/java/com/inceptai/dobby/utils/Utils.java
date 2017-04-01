package com.inceptai.dobby.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by vivek on 4/1/17.
 */

public class Utils {
    private static int readTimeout = 10000;
    private static int connectionTimeout = 15000;
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
        connection.setReadTimeout(readTimeout /* milliseconds */);
        connection.setConnectTimeout(connectionTimeout /* milliseconds */);
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
        connection.setReadTimeout(readTimeout /* milliseconds */);
        connection.setConnectTimeout(connectionTimeout /* milliseconds */);
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
}
