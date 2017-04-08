package com.inceptai.dobby.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.inceptai.dobby.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class DebugFragment extends Fragment implements View.OnClickListener {
    public static final String FRAGMENT_TAG = "DebugFragment";
    private static final String TAG_BW_TEST = "bw test";
    private static final String TAG_WIFI_SCAN = "wifi scan";
    private static final String TAG_PING = "ping";

    private Button bwTestButton;
    private Button wifiScanButton;
    private Button pingTestButton;
    private SwitchCompat uploadSwitchButton;
    private SwitchCompat downloadSwitchButton;
    private TextView consoleTv;

    public DebugFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mainView =  inflater.inflate(R.layout.fragment_debug, container, false);
        bwTestButton = (Button) mainView.findViewById(R.id.bw_test_button);
        bwTestButton.setTag(TAG_BW_TEST);
        bwTestButton.setOnClickListener(this);

        wifiScanButton = (Button) mainView.findViewById(R.id.wifi_scan_button);
        wifiScanButton.setTag(TAG_WIFI_SCAN);
        wifiScanButton.setOnClickListener(this);

        pingTestButton = (Button) mainView.findViewById(R.id.ping_button);
        pingTestButton.setTag(TAG_PING);
        pingTestButton.setOnClickListener(this);

        uploadSwitchButton = (SwitchCompat) mainView.findViewById(R.id.upload_switch_button);
        downloadSwitchButton = (SwitchCompat) mainView.findViewById(R.id.download_switch_button);

        consoleTv = (TextView) mainView.findViewById(R.id.console_textview);

        return mainView;
    }

    @Override
    public void onClick(View v) {
        String tag = (String) v.getTag();
        if (TAG_BW_TEST.equals(tag)) {
            String config = "";
            if (uploadSwitchButton.isChecked()) {
                config = "Upload ";
            }
            if (downloadSwitchButton.isChecked()) {
                config += "Download ";
            }

            if (!config.isEmpty()) {
                addConsoleText("Clicked on Bandwidth test with : { " + config + " }.");
            }

        } else if (TAG_PING.equals(tag)) {
            addConsoleText("Clicked on Ping.");

        } else if (TAG_WIFI_SCAN.equals(tag)) {
            addConsoleText("Clicked on Wifi scan.");
        }

    }

    public void addConsoleText(String text) {
        String existingText = consoleTv.getText().toString();
        consoleTv.setText(existingText + "\n" + text);
    }
}
