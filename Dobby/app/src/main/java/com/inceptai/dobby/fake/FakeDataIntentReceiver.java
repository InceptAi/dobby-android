
package com.inceptai.dobby.fake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.inceptai.dobby.ai.suggest.LocalSnippet;
import com.inceptai.dobby.ui.WifiDocActivity;
import com.inceptai.dobby.utils.DobbyLog;

import org.apache.logging.log4j.util.Strings;

/**
 * Created by arunesh on 5/18/17.
 */

public class FakeDataIntentReceiver extends BroadcastReceiver {
    public static final String FAKE_DATA_INTENT = "com.inceptai.dobby.wifi.fake.FAKE_DATA";
    private static final String KEY_DOWNLOAD_BW = "download";
    private static final String KEY_UPLOAD_BW = "upload";
    private static final String KEY_SHOW_SUGGESTIONS = "show_suggestions";

    private WifiDocActivity wifiDocActivity;

    public FakeDataIntentReceiver(WifiDocActivity wifiDocActivity) {
        this.wifiDocActivity = wifiDocActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Received broadcast intent", Toast.LENGTH_SHORT).show();
        // Extract parameters from intent and set them !.
        Bundle extras = intent.getExtras();
        if (extras != null) {
            parseParameters(extras, context);
        }
    }

    void parseParameters(Bundle bundle, Context context) {
        String downloadBw = bundle.getString(KEY_DOWNLOAD_BW);
        if (Strings.isNotBlank(downloadBw)) {
            DobbyLog.i("Setting fake download Bandwidth = " + downloadBw);
        }

        String uploadBw = bundle.getString(KEY_UPLOAD_BW);
        if (Strings.isNotBlank(uploadBw)) {
            DobbyLog.i("Setting fake upload Bandwidth = " + uploadBw);
        }
        if (Strings.isNotBlank(uploadBw) && Strings.isNotBlank(downloadBw)) {
            BetaInferenceEngine.get().setBandwidth(Double.valueOf(uploadBw), Double.valueOf(downloadBw));
        }

        String showSuggestionsString = bundle.getString(KEY_SHOW_SUGGESTIONS);
        if (Strings.isNotBlank(showSuggestionsString)) {
            if (Boolean.valueOf(showSuggestionsString)) {
                LocalSnippet snippet = BetaInferenceEngine.get().doInference(context);
                wifiDocActivity.showFakeSuggestionsUi(snippet);
            }
        }
    }

}
