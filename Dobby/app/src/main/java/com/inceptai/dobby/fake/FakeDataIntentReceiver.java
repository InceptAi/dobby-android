
package com.inceptai.dobby.fake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.apache.logging.log4j.util.Strings;

/**
 * Created by arunesh on 5/18/17.
 */

public class FakeDataIntentReceiver extends BroadcastReceiver {
    private static final String KEY_DOWNLOAD_BW = "download";
    private static final String KEY_UPLOAD_BW = "upload";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Extract parameters from intent and set them !.
        Bundle extras = intent.getExtras();
        if (extras != null) {
            parseParameters(extras, context);
        }
    }

    void parseParameters(Bundle bundle, Context context) {
        String downloadBw = bundle.getString(KEY_DOWNLOAD_BW);
        if (Strings.isNotBlank(downloadBw)) {
            Toast.makeText(context, "Download Bandwidth = " + downloadBw, Toast.LENGTH_SHORT).show();
        }

        String uploadBw = bundle.getString(KEY_UPLOAD_BW);
        if (Strings.isNotBlank(uploadBw)) {
            Toast.makeText(context, "Upload Bandwidth = " + uploadBw, Toast.LENGTH_SHORT).show();
        }
    }
}
