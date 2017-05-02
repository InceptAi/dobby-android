package com.inceptai.dobby.ping;

import com.inceptai.dobby.DobbyThreadpool;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 * Created by arunesh on 5/2/17.
 */

public class InputStreamAccumulator implements Runnable {

    Thread internalThread;
    List<InputStream> inputStreamList;
    List<StringBuilder> stringBuilderList;

    InputStreamAccumulator(List<InputStream> inputStreamList, DobbyThreadpool threadpool) {

    }

    @Override
    public void run() {
        try {
            for (InputStream inputStream : inputStreamList) {
                if (inputStream.available()> 0) {

                }
            }
        } catch (IOException e)
        {}
    }

    public HashMap<InputStream, String> getResult() {
        return null;
    }
}

