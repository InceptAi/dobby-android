package com.inceptai.dobby.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.inceptai.dobby.R;
import com.inceptai.dobby.fake.FakeSpeedTestSocket;
import com.inceptai.dobby.speedtest.SpeedTestSocketFactory;
import com.inceptai.dobby.utils.Utils;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * A simple {@link Fragment} subclass.
 */
public class FakeDataFragment extends Fragment implements View.OnClickListener, TextView.OnEditorActionListener, View.OnFocusChangeListener {
    public static final String FRAGMENT_TAG = "FakeDataFragment";
    private static final String TOGGLE_TAG = "ToggleTag";
    private static final String UPLOAD_ET_TAG = "Upload";
    private static final String DOWNLOAD_ET_TAG = "Download";
    private static final String SAVE_BUTTON_TAG = "SaveButton";

    private EditText uploadBwEt;
    private EditText downloadBwEt;
    private ToggleButton fakeDataToggle;
    private Button saveButton;

    public FakeDataFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_fake_data, container, false);

        saveButton = (Button) view.findViewById(R.id.save_button);
        saveButton.setOnClickListener(this);
        saveButton.setTag(SAVE_BUTTON_TAG);

        uploadBwEt = (EditText) view.findViewById(R.id.upload_bw_et);
        uploadBwEt.setTag(UPLOAD_ET_TAG);
        uploadBwEt.setText(String.valueOf(FakeSpeedTestSocket.DEFAULT_FAKE_CONFIG.getMaxUploadBandwidthMbps()));

        downloadBwEt = (EditText) view.findViewById(R.id.download_bw_et);
        downloadBwEt.setTag(DOWNLOAD_ET_TAG);
        downloadBwEt.setText(String.valueOf(FakeSpeedTestSocket.DEFAULT_FAKE_CONFIG.getMaxDownloadBandwidthMbps()));

        fakeDataToggle = (ToggleButton) view.findViewById(R.id.fake_data_toggle);
        fakeDataToggle.setChecked(SpeedTestSocketFactory.useFakes());

        fakeDataToggle.setOnClickListener(this);
        fakeDataToggle.setTag(TOGGLE_TAG);
        return view;
    }

    @Override
    public void onClick(View v) {
        if (SAVE_BUTTON_TAG.equals(v.getTag())) {
            saveChanges();
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

    private void saveChanges() {
        boolean hasChanged = false;
        String changedMessage = "Saving changes: ";
        String newDownloadBw = downloadBwEt.getText().toString();
        if (!newDownloadBw.isEmpty()) {
            double dbValue = Double.valueOf(newDownloadBw);
            if (dbValue != FakeSpeedTestSocket.DEFAULT_FAKE_CONFIG.getMaxDownloadBandwidthMbps()) {
                hasChanged = true;
                changedMessage = changedMessage + "Download to " + newDownloadBw + " Mbps.";
                FakeSpeedTestSocket.DEFAULT_FAKE_CONFIG.setMaxDownloadBandwidthMbps(dbValue);
            }
        }

        String newUploadBw = uploadBwEt.getText().toString();
        if (!newUploadBw.isEmpty()) {
            double dbValue = Double.valueOf(newUploadBw);
            if (dbValue != FakeSpeedTestSocket.DEFAULT_FAKE_CONFIG.getMaxUploadBandwidthMbps()) {
                changedMessage = changedMessage + " Upload to " + newUploadBw + " Mbps.";
                hasChanged = true;
                FakeSpeedTestSocket.DEFAULT_FAKE_CONFIG.setMaxUploadBandwidthMbps(dbValue);
            }
        }
        if (SpeedTestSocketFactory.useFakes() != fakeDataToggle.isChecked()) {
            changedMessage = changedMessage + " Fake data to " + (fakeDataToggle.isChecked() ? "ON." : "OFF.");
            SpeedTestSocketFactory.setUseFakes(fakeDataToggle.isChecked());
            hasChanged = true;
        }
        if (hasChanged) {
            Toast.makeText(getContext(), changedMessage, Toast.LENGTH_SHORT).show();
        }
    }
}
