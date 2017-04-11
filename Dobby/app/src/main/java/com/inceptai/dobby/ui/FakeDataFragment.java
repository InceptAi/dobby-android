package com.inceptai.dobby.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.inceptai.dobby.R;
import com.inceptai.dobby.fake.FakeSpeedTestSocket;
import com.inceptai.dobby.speedtest.SpeedTestSocketFactory;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * A simple {@link Fragment} subclass.
 */
public class FakeDataFragment extends Fragment implements View.OnClickListener, TextView.OnEditorActionListener, View.OnFocusChangeListener {
    public static final String FRAGMENT_TAG = "FakeDataFragment";
    private static final String TOGGLE_TAG = "ToggleTag";
    private static final String UPLOAD_ET_TAG = "Upload";
    private static final String DOWNLOAD_ET_TAG = "Download";

    private EditText uploadBwEt;
    private EditText downloadBwEt;
    private ToggleButton fakeDataToggle;

    public FakeDataFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_fake_data, container, false);

        uploadBwEt = (EditText) view.findViewById(R.id.upload_bw_et);
        uploadBwEt.setTag(UPLOAD_ET_TAG);
        uploadBwEt.setOnEditorActionListener(this);
        uploadBwEt.setOnFocusChangeListener(this);

        downloadBwEt = (EditText) view.findViewById(R.id.download_bw_et);
        downloadBwEt.setTag(DOWNLOAD_ET_TAG);
        downloadBwEt.setOnEditorActionListener(this);
        downloadBwEt.setOnFocusChangeListener(this);

        fakeDataToggle = (ToggleButton) view.findViewById(R.id.fake_data_toggle);

        fakeDataToggle.setOnClickListener(this);
        fakeDataToggle.setTag(TOGGLE_TAG);
        return view;
    }

    @Override
    public void onClick(View v) {
        if (TOGGLE_TAG.equals(v.getTag())) {
            SpeedTestSocketFactory.setUseFakes(fakeDataToggle.isChecked());
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        String text = v.getText().toString();
        Log.i(TAG, "Action ID: " + actionId);
        if (event != null) {
            Log.i(TAG, "key event: " + event.toString());
        }
        if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
            Log.i(TAG, "ENTER 1");
            processBandwidthChange(v, text);
            return true;
        } else if (actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_NEXT) {
            Log.i(TAG, "ENTER 2");
            processBandwidthChange(v, text);
            return true;
        }
        return false;
    }


    private void processBandwidthChange(View v, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        double dbValue = Double.valueOf(value);
        if (UPLOAD_ET_TAG.equals(v.getTag())) {
            FakeSpeedTestSocket.DEFAULT_FAKE_CONFIG.setMaxUploadBandwidthMbps(dbValue);
            Toast.makeText(getContext(), "Upload bandwidth set to " + value + " Mbps.", Toast.LENGTH_SHORT).show();
        } else if (DOWNLOAD_ET_TAG.equals(v.getTag())) {
            FakeSpeedTestSocket.DEFAULT_FAKE_CONFIG.setMaxDownloadBandwidthMbps(dbValue);
            Toast.makeText(getContext(), "Download bandwidth set to " + value + " Mbps.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            processBandwidthChange(v, ((TextView) v).getText().toString());
        }
    }
}
