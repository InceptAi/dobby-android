package com.inceptai.dobby;

import android.content.Context;
import android.util.Log;

import com.inceptai.dobby.apiai.ApiAiClient;
import com.inceptai.dobby.speedtest.BestServerSelector;
import com.inceptai.dobby.speedtest.DownloadManager;
import com.inceptai.dobby.speedtest.ParseServerInformation;
import com.inceptai.dobby.speedtest.ParseSpeedTestConfig;
import com.inceptai.dobby.speedtest.PingAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;
import com.inceptai.dobby.speedtest.SpeedTestTask;
import com.inceptai.dobby.speedtest.UploadManager;

import java.util.LinkedList;
import java.util.List;

import ai.api.model.Result;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * This class is responsible for managing the user queries and showing the responses by working
 * with the main activity.
 * It can be made to send queries to API AI's server or use a local/another AI system.
 */

public class DobbyChatManager implements ApiAiClient.ResultListener {
    private static final String CANNED_RESPONSE = "We are working on it.";

    private Context context;
    private DobbyThreadpool threadpool;
    private ApiAiClient apiAiClient;
    private ResponseCallback responseCallback;
    private SpeedTestTask speedTestTask;


    public interface ResponseCallback {
        void showResponse(String text);
    }

    public DobbyChatManager(Context context, DobbyThreadpool threadpool, ResponseCallback callback) {
        this.context = context;
        this.threadpool = threadpool;
        this.responseCallback = callback;
        //Why is this not this.apiAiClient
        apiAiClient = new ApiAiClient(context, threadpool);
        apiAiClient.connect();
        this.speedTestTask = new SpeedTestTask();
    }

    @Override
    public void onResult(Result result) {
        String response = result.getFulfillment().getSpeech();
        if (response == null || response.isEmpty()) {
            response = CANNED_RESPONSE;
        }
        responseCallback.showResponse(response);
        Log.i(TAG, "Got response Action: " + result.toString());
        if (result.toString().contains("test")) {
            //Vivek--testing best server code.
            PingAnalyzer pingAnalyzer = new PingAnalyzer();
            PingAnalyzer.PingStats routerPingStats = pingAnalyzer.pingAndReturnStats("192.168.1.1");
            SpeedTestConfig config = ParseSpeedTestConfig.getConfig("https");
            ServerInformation info = ParseServerInformation.getServerInfo();
            ServerInformation.ServerDetails bestServer = BestServerSelector.getBestServerId(BestServerSelector.getClosestServers(config, info));
            DownloadManager downloadManager = new DownloadManager(config.downloadConfig, bestServer, null, null);
            downloadManager.downloadTestWithOneThread();
            UploadManager uploadManager = new UploadManager(config.uploadConfig, bestServer, null, null);
            uploadManager.uploadTestWithOneThread();
            /*
            threadpool.submit(new Runnable() {
                @Override
                public void run() {
                    speedTestSocket.startDownload("2.testdebit.info", "/fichiers/1Mo.dat");
                }
            });
            speedTestTask.doInBackground();
            */
        }
    }

    public void startMic() {

    }

    public void sendQuery(String text) {
        apiAiClient.sendTextQuery(text, this);
    }
}
