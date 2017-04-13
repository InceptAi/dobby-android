package com.inceptai.dobby.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.R;
import com.inceptai.dobby.fake.FakeSpeedTestSocket;
import com.inceptai.dobby.fake.FakeWifiAnalyzer;
import com.inceptai.dobby.speedtest.SpeedTestSocketFactory;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.dobby.wifi.WifiStats;

import java.util.ArrayList;

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

    // Fake wifi scan UI:

    private Spinner chanOneNumApsSpinner;
    private Spinner chanOneRssiSpinner;

    private Spinner chanSixNumApsSpinner;
    private Spinner chanSixRssiSpinner;

    private Spinner chanElevenNumApsSpinner;
    private Spinner chanElevenRssiSpinner;

    private Spinner mainApChannelSpinner;
    private Spinner mainApRssiSpinner;


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
        fakeDataToggle.setChecked(DobbyApplication.USE_FAKES.get());

        fakeDataToggle.setOnClickListener(this);
        fakeDataToggle.setTag(TOGGLE_TAG);

        chanOneNumApsSpinner = (Spinner) view.findViewById(R.id.num_aps_chan_1_spinner);
        chanOneRssiSpinner = (Spinner) view.findViewById(R.id.rssi_level_chan_1_spinner);
        populateSpinnerWithNumbers(chanOneNumApsSpinner, 15, FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.numApsChannelOne, 0);
        setRssiSelection(chanOneRssiSpinner, FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.signalZoneChannelOne);

        chanSixNumApsSpinner = (Spinner) view.findViewById(R.id.num_aps_chan_6_spinner);
        chanSixRssiSpinner = (Spinner) view.findViewById(R.id.rssi_level_chan_6_spinner);
        populateSpinnerWithNumbers(chanSixNumApsSpinner, 15, FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.numApsChannelSix, 0);
        setRssiSelection(chanSixRssiSpinner, FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.signalZoneChannelSix);

        chanElevenNumApsSpinner = (Spinner) view.findViewById(R.id.num_aps_chan_11_spinner);
        chanElevenRssiSpinner = (Spinner) view.findViewById(R.id.rssi_level_chan_11_spinner);
        populateSpinnerWithNumbers(chanElevenNumApsSpinner, 15, FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.numApsChannelEleven, 0);
        setRssiSelection(chanElevenRssiSpinner, FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.signalZoneChannelEleven);

        mainApChannelSpinner = (Spinner) view.findViewById(R.id.main_ap_channel_spinner);
        mainApRssiSpinner = (Spinner) view.findViewById(R.id.rssi_level_main_ap_spinner);
        setRssiSelection(mainApRssiSpinner, FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.signalZoneMainAp);
        populateSpinnerWithNumbers(mainApChannelSpinner, 11, FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.mainApChannelNumber, 1);

        return view;
    }

    /**
     * Populates from 0 .. max - 1
     * @param spinner
     * @param max
     * @param selection
     */
    private void populateSpinnerWithNumbers(Spinner spinner, int max, int selection, int offset) {
        ArrayList<String> numberList = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            numberList.add(String.valueOf(i + offset));
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, numberList);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(selection, true);
    }

    private static void setRssiSelection(Spinner spinner, @WifiStats.SignalStrengthZones int rssiZone) {
        if (rssiZone == WifiStats.SignalStrengthZones.HIGH) {
            spinner.setSelection(0, true);
        } else if (rssiZone == WifiStats.SignalStrengthZones.MEDIUM) {
            spinner.setSelection(1, true);
        }  else if (rssiZone == WifiStats.SignalStrengthZones.LOW) {
            spinner.setSelection(2, true);
        }  else if (rssiZone == WifiStats.SignalStrengthZones.FRINGE) {
            spinner.setSelection(3, true);
        }
    }

    @WifiStats.SignalStrengthZones
    private static int getRssiZone(Spinner spinner) {
        if (spinner.getSelectedItemPosition() == 0) {
            return WifiStats.SignalStrengthZones.HIGH;
        } else if (spinner.getSelectedItemPosition() == 1) {
            return WifiStats.SignalStrengthZones.MEDIUM;
        }  else if (spinner.getSelectedItemPosition() == 2) {
            return WifiStats.SignalStrengthZones.LOW;
        }  else if (spinner.getSelectedItemPosition() == 3) {
            return WifiStats.SignalStrengthZones.FRINGE;
        }
        return 0;
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
        if (DobbyApplication.USE_FAKES.get() != fakeDataToggle.isChecked()) {
            changedMessage = changedMessage + " Fake data to " + (fakeDataToggle.isChecked() ? "ON." : "OFF.");
            DobbyApplication.USE_FAKES.set(fakeDataToggle.isChecked());
            hasChanged = true;
        }
        if (saveWifiSettingsIfChanged()) {
            hasChanged = true;
            changedMessage = changedMessage + " Wifi settings. ";
        }
        if (hasChanged) {
            Toast.makeText(getContext(), changedMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean saveWifiSettingsIfChanged() {
        boolean hasChanged = false;
        if (chanOneNumApsSpinner.getSelectedItemPosition() != FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.numApsChannelOne) {
            hasChanged = true;
            FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.numApsChannelOne = chanOneNumApsSpinner.getSelectedItemPosition();
            Log.i(TAG, "Setting num Aps for Channel 1 to : " + chanOneNumApsSpinner.getSelectedItemPosition());
        }

        if (chanSixNumApsSpinner.getSelectedItemPosition() != FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.numApsChannelSix) {
            hasChanged = true;
            FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.numApsChannelSix = chanSixNumApsSpinner.getSelectedItemPosition();
            Log.i(TAG, "Setting num Aps for Channel 6 to : " + chanSixNumApsSpinner.getSelectedItemPosition());
        }

        if (chanElevenNumApsSpinner.getSelectedItemPosition() != FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.numApsChannelEleven) {
            hasChanged = true;
            FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.numApsChannelEleven = chanElevenNumApsSpinner.getSelectedItemPosition();
            Log.i(TAG, "Setting num Aps for Channel 11 to : " + chanElevenNumApsSpinner.getSelectedItemPosition());
        }

        int zone = getRssiZone(chanOneRssiSpinner);
        if (zone != FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.signalZoneChannelOne) {
            hasChanged = true;
            FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.signalZoneChannelOne = zone;
        }

        zone = getRssiZone(chanSixRssiSpinner);
        if (zone != FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.signalZoneChannelSix) {
            hasChanged = true;
            FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.signalZoneChannelSix = zone;
        }

        zone = getRssiZone(chanElevenRssiSpinner);
        if (zone != FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.signalZoneChannelEleven) {
            hasChanged = true;
            FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.signalZoneChannelEleven = zone;
        }

        zone = getRssiZone(mainApRssiSpinner);
        if (zone != FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.signalZoneMainAp) {
            hasChanged = true;
            FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.signalZoneMainAp = zone;
        }

        int mainApSelectedChannel = mainApChannelSpinner.getSelectedItemPosition() + 1;
        if (mainApSelectedChannel != FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.mainApChannelNumber) {
            hasChanged = true;
            FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG.mainApChannelNumber = mainApSelectedChannel;
        }
        return hasChanged;
    }
}
