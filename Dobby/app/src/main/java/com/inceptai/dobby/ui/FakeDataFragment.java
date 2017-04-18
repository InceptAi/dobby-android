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
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.R;
import com.inceptai.dobby.connectivity.ConnectivityAnalyzer;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.fake.FakePingAnalyzer;
import com.inceptai.dobby.fake.FakeSpeedTestSocket;
import com.inceptai.dobby.wifi.WifiState;

import java.util.ArrayList;

import javax.inject.Inject;

import static com.inceptai.dobby.DobbyApplication.TAG;
import static com.inceptai.dobby.fake.FakeConnectivityAnalyzer.fakeWifiConnectivityMode;
import static com.inceptai.dobby.fake.FakeConnectivityAnalyzer.setFakeWifiConnectivityMode;
import static com.inceptai.dobby.fake.FakePingAnalyzer.PingStatsMode.DEFAULT_WORKING_STATE;
import static com.inceptai.dobby.fake.FakeWifiAnalyzer.FAKE_WIFI_SCAN_CONFIG;

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


    // Fake ping UI:
    private Spinner pingStatsModeSpinner;

    // Fake connectivity selector
    private Spinner connectivityModeSpinner;

    //Fake Wifi Problem State UI
    private Spinner wifiProblemStateSpinner;

    @Inject
    NetworkLayer networkLayer;
    @Inject
    DobbyThreadpool threadpool;
    @Inject
    DobbyEventBus eventBus;

    public FakeDataFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ((DobbyApplication) getActivity().getApplication()).getProdComponent().inject(this);
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
        populateSpinnerWithNumbers(chanOneNumApsSpinner, 15, FAKE_WIFI_SCAN_CONFIG.numApsChannelOne, 0);
        setRssiSelection(chanOneRssiSpinner, FAKE_WIFI_SCAN_CONFIG.signalZoneChannelOne);

        chanSixNumApsSpinner = (Spinner) view.findViewById(R.id.num_aps_chan_6_spinner);
        chanSixRssiSpinner = (Spinner) view.findViewById(R.id.rssi_level_chan_6_spinner);
        populateSpinnerWithNumbers(chanSixNumApsSpinner, 15, FAKE_WIFI_SCAN_CONFIG.numApsChannelSix, 0);
        setRssiSelection(chanSixRssiSpinner, FAKE_WIFI_SCAN_CONFIG.signalZoneChannelSix);

        chanElevenNumApsSpinner = (Spinner) view.findViewById(R.id.num_aps_chan_11_spinner);
        chanElevenRssiSpinner = (Spinner) view.findViewById(R.id.rssi_level_chan_11_spinner);
        populateSpinnerWithNumbers(chanElevenNumApsSpinner, 15, FAKE_WIFI_SCAN_CONFIG.numApsChannelEleven, 0);
        setRssiSelection(chanElevenRssiSpinner, FAKE_WIFI_SCAN_CONFIG.signalZoneChannelEleven);

        mainApChannelSpinner = (Spinner) view.findViewById(R.id.main_ap_channel_spinner);
        mainApRssiSpinner = (Spinner) view.findViewById(R.id.rssi_level_main_ap_spinner);
        setRssiSelection(mainApRssiSpinner, FAKE_WIFI_SCAN_CONFIG.signalZoneMainAp);
        populateSpinnerWithNumbers(mainApChannelSpinner, 11, FAKE_WIFI_SCAN_CONFIG.mainApChannelNumber, 1);

        pingStatsModeSpinner = (Spinner) view.findViewById(R.id.ping_stats_mode_selector);
        populateSpinnerWithPingModes(pingStatsModeSpinner, DEFAULT_WORKING_STATE);

        connectivityModeSpinner = (Spinner) view.findViewById(R.id.connectivity_mode_selector);
        populateSpinnerWithConnectivityModes(connectivityModeSpinner, ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_ONLINE);

        wifiProblemStateSpinner = (Spinner) view.findViewById(R.id.wifiproblem_mode_selector);
        populateSpinnerWithWifiProblemModes(wifiProblemStateSpinner, WifiState.WifiLinkMode.NO_PROBLEM_DEFAULT_STATE);

        return view;
    }


    /**
     * Populates from 0 .. max - 1
     * @param spinner
     * @param selection
     */
    private void populateSpinnerWithPingModes(Spinner spinner, @FakePingAnalyzer.PingStatsMode int selection) {
        ArrayList<String> modeList = new ArrayList<>();
        for (@FakePingAnalyzer.PingStatsMode int i = 0; i < FakePingAnalyzer.PingStatsMode.MAX_STATES; i++) {
            modeList.add(FakePingAnalyzer.getPingStatsModeName(i));
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, modeList);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(selection, true);
    }


    /**
     * Populates from 0 .. max - 1
     * @param spinner
     * @param selection
     */
    private void populateSpinnerWithConnectivityModes(Spinner spinner, @ConnectivityAnalyzer.WifiConnectivityMode int selection) {
        ArrayList<String> modeList = new ArrayList<>();
        for (@ConnectivityAnalyzer.WifiConnectivityMode int modeIndex = 0;
             modeIndex < ConnectivityAnalyzer.WifiConnectivityMode.MAX_MODES; modeIndex++) {
            modeList.add(ConnectivityAnalyzer.getConnecitivyStateName(modeIndex));
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, modeList);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(selection, true);
    }

    /**
     * Populates from 0 .. max - 1
     * @param spinner
     * @param selection
     */
    private void populateSpinnerWithWifiProblemModes(Spinner spinner, @WifiState.WifiLinkMode int selection) {
        ArrayList<String> modeList = new ArrayList<>();
        for (@WifiState.WifiLinkMode int modeIndex = 0;
             modeIndex < WifiState.WifiLinkMode.MAX_MODES; modeIndex++) {
            modeList.add(WifiState.getWifiStatsModeName(modeIndex));
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, modeList);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(selection, true);
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

    private static void setRssiSelection(Spinner spinner, @WifiState.SignalStrengthZones int rssiZone) {
        if (rssiZone == WifiState.SignalStrengthZones.HIGH) {
            spinner.setSelection(0, true);
        } else if (rssiZone == WifiState.SignalStrengthZones.MEDIUM) {
            spinner.setSelection(1, true);
        }  else if (rssiZone == WifiState.SignalStrengthZones.LOW) {
            spinner.setSelection(2, true);
        }  else if (rssiZone == WifiState.SignalStrengthZones.FRINGE) {
            spinner.setSelection(3, true);
        }
    }

    @WifiState.SignalStrengthZones
    private static int getRssiZone(Spinner spinner) {
        if (spinner.getSelectedItemPosition() == 0) {
            return WifiState.SignalStrengthZones.HIGH;
        } else if (spinner.getSelectedItemPosition() == 1) {
            return WifiState.SignalStrengthZones.MEDIUM;
        }  else if (spinner.getSelectedItemPosition() == 2) {
            return WifiState.SignalStrengthZones.LOW;
        }  else if (spinner.getSelectedItemPosition() == 3) {
            return WifiState.SignalStrengthZones.FRINGE;
        }
        return WifiState.SignalStrengthZones.HIGH;
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
        boolean hasFakeDataSwichBeenToggled = false;
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
            hasFakeDataSwichBeenToggled = true;
        }
        if (saveWifiSettingsIfChanged()) {
            hasChanged = true;
            changedMessage = changedMessage + " Wifi settings. ";
        }
        if (savePingSettingsIfChanged()) {
            hasChanged = true;
            changedMessage = changedMessage + " Ping settings changed. ";
        }
        if (saveConnectivitySettingsIfChanged()) {
            hasChanged = true;
            changedMessage = changedMessage + " Connectivity settings changed. ";
        }
        if (saveWifiProblemModeSettings()) {
            hasChanged = true;
            changedMessage = changedMessage + " Wifi problem mode settings changed. ";
        }
        if (hasFakeDataSwichBeenToggled) {
            //To cleanup the old instance and get the new instance.
            //Make sure this goes at the end so we can get all the changes.
            if (networkLayer != null) {
                networkLayer.getWifiAnalyzerInstance();
            }
        }
        if (hasChanged) {
            Toast.makeText(getContext(), changedMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean saveWifiProblemModeSettings() {
        boolean hasChanged = false;
        @WifiState.WifiLinkMode int selectedMode = wifiProblemStateSpinner.getSelectedItemPosition();
        if (selectedMode != FAKE_WIFI_SCAN_CONFIG.fakeWifiProblemMode) {
            hasChanged = true;
            //noinspection ResourceType
            FAKE_WIFI_SCAN_CONFIG.fakeWifiProblemMode = selectedMode;
            Log.i(TAG, "FAKE Setting Wifi Problem mode to : " +
                    WifiState.getWifiStatsModeName(selectedMode));
        }
        return hasChanged;
    }

    private boolean saveConnectivitySettingsIfChanged() {
        boolean hasChanged = false;
        @ConnectivityAnalyzer.WifiConnectivityMode  int selectedMode = connectivityModeSpinner.getSelectedItemPosition();
        if (selectedMode != fakeWifiConnectivityMode) {
            hasChanged = true;
            //noinspection ResourceType
            setFakeWifiConnectivityMode(selectedMode);
            Log.i(TAG, "FAKE Setting Connectivity mode to : " +
                    ConnectivityAnalyzer.getConnecitivyStateName(fakeWifiConnectivityMode));
        }
        return hasChanged;
    }

    private boolean savePingSettingsIfChanged() {
        boolean hasChanged = false;
        @FakePingAnalyzer.PingStatsMode int selectedMode = pingStatsModeSpinner.getSelectedItemPosition();
        if (selectedMode != FakePingAnalyzer.pingStatsMode) {
            hasChanged = true;
            //noinspection ResourceType
            FakePingAnalyzer.pingStatsMode = selectedMode;
            Log.i(TAG, "FAKE Setting ping stats mode to : " + FakePingAnalyzer.getPingStatsModeName(FakePingAnalyzer.pingStatsMode));
        }
        return hasChanged;
    }

    private boolean saveWifiSettingsIfChanged() {
        boolean hasChanged = false;
        if (chanOneNumApsSpinner.getSelectedItemPosition() != FAKE_WIFI_SCAN_CONFIG.numApsChannelOne) {
            hasChanged = true;
            FAKE_WIFI_SCAN_CONFIG.numApsChannelOne = chanOneNumApsSpinner.getSelectedItemPosition();
            Log.i(TAG, "Setting num Aps for Channel 1 to : " + chanOneNumApsSpinner.getSelectedItemPosition());
        }

        if (chanSixNumApsSpinner.getSelectedItemPosition() != FAKE_WIFI_SCAN_CONFIG.numApsChannelSix) {
            hasChanged = true;
            FAKE_WIFI_SCAN_CONFIG.numApsChannelSix = chanSixNumApsSpinner.getSelectedItemPosition();
            Log.i(TAG, "Setting num Aps for Channel 6 to : " + chanSixNumApsSpinner.getSelectedItemPosition());
        }

        if (chanElevenNumApsSpinner.getSelectedItemPosition() != FAKE_WIFI_SCAN_CONFIG.numApsChannelEleven) {
            hasChanged = true;
            FAKE_WIFI_SCAN_CONFIG.numApsChannelEleven = chanElevenNumApsSpinner.getSelectedItemPosition();
            Log.i(TAG, "Setting num Aps for Channel 11 to : " + chanElevenNumApsSpinner.getSelectedItemPosition());
        }

        int zone = getRssiZone(chanOneRssiSpinner);
        if (zone != FAKE_WIFI_SCAN_CONFIG.signalZoneChannelOne) {
            hasChanged = true;
            FAKE_WIFI_SCAN_CONFIG.signalZoneChannelOne = zone;
        }

        zone = getRssiZone(chanSixRssiSpinner);
        if (zone != FAKE_WIFI_SCAN_CONFIG.signalZoneChannelSix) {
            hasChanged = true;
            FAKE_WIFI_SCAN_CONFIG.signalZoneChannelSix = zone;
        }

        zone = getRssiZone(chanElevenRssiSpinner);
        if (zone != FAKE_WIFI_SCAN_CONFIG.signalZoneChannelEleven) {
            hasChanged = true;
            FAKE_WIFI_SCAN_CONFIG.signalZoneChannelEleven = zone;
        }

        zone = getRssiZone(mainApRssiSpinner);
        if (zone != FAKE_WIFI_SCAN_CONFIG.signalZoneMainAp) {
            hasChanged = true;
            FAKE_WIFI_SCAN_CONFIG.signalZoneMainAp = zone;
        }

        int mainApSelectedChannel = mainApChannelSpinner.getSelectedItemPosition() + 1;
        if (mainApSelectedChannel != FAKE_WIFI_SCAN_CONFIG.mainApChannelNumber) {
            hasChanged = true;
            FAKE_WIFI_SCAN_CONFIG.mainApChannelNumber = mainApSelectedChannel;
        }
        return hasChanged;
    }

}
