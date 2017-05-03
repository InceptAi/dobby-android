package com.inceptai.dobby.ping;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by arunesh on 5/2/17.
 */

public class BufferedReaderAccumulator implements Runnable {

    private Thread internalThread;
    private HashMap <String, Reader> readerHashMap;
    private HashMap <String, StringBuilder> stringStringBuilderHashMap;
    private ExecutorService executorService;
    private List<String> pingAddressList;

    BufferedReaderAccumulator(List<String> pingAddressList, ExecutorService executorService) {
        this.executorService = executorService;
        this.pingAddressList = pingAddressList;
    }

    @Override
    public void run() {
        try {
            for (Reader bufferedReader : readerHashMap.values()) {
                if (bufferedReader.ready()) {

                }
            }
        } catch (IOException e)
        {}
    }

    public HashMap<InputStream, String> getResult() {
        return null;
    }
}

