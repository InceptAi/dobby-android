package com.inceptai.dobby.fake;

/**
 * Created by arunesh on 5/19/17.
 */

import android.content.Context;

import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.ai.suggest.LocalSummary;
import com.inceptai.dobby.ai.suggest.NewSuggestions;

/**
 * This does real inferencing but using the new suggestions. We use this in a special test mode to
 * build the new suggestions.
 */
public class BetaInferenceEngine {
    public static String FAKE_ISP = "Comcast";
    public static String FAKE_EXTERNAL_IP = "192.56.23.4";
    public static BetaInferenceEngine INSTANCE = new BetaInferenceEngine();

    private DataInterpreter.BandwidthGrade bandwidthGrade;

    public static BetaInferenceEngine get() {
        return INSTANCE;
    }

    BetaInferenceEngine() {
    }

    public void setBandwidth(double upload, double download) {
        bandwidthGrade = DataInterpreter.interpret(download, upload, FAKE_ISP, FAKE_EXTERNAL_IP, 0);
    }

    LocalSummary doInference(Context context) {
        NewSuggestions.DataSummary dataSummary = new NewSuggestions.DataSummary(bandwidthGrade, null, null, null);
        NewSuggestions newSuggestions = new NewSuggestions(dataSummary, context);
        return newSuggestions.getSuggestions();
    }
}
