package com.inceptai.wifiexpertsystem.expertSystem.messages;

import android.support.annotation.IntDef;

import com.inceptai.wifiexpertsystem.expertSystem.inferencing.DataInterpreter;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest.BandwidthResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.wifi.WifiNetworkOverview;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.Action;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Created by vivek on 8/7/17.
 */

public class ExpertMessage {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ExpertMessageType.FREE_FORM, ExpertMessageType.STRUCTURED_LIST,
            ExpertMessageType.ACTION_STARTED, ExpertMessageType.ACTION_FINISHED,
            ExpertMessageType.SHOW_WIFI_INFO, ExpertMessageType.SHOW_PING_INFO,
            ExpertMessageType.SHOW_BANDWIDTH_INFO})
    public @interface ExpertMessageType {
        int FREE_FORM = 0;
        int STRUCTURED_LIST = 1;
        int ACTION_STARTED = 2;
        int ACTION_FINISHED = 3;
        int SHOW_WIFI_INFO = 4;
        int SHOW_PING_INFO = 5;
        int SHOW_BANDWIDTH_INFO = 6;
    }

    private String message;
    private Action expertAction;
    private ActionResult actionResult;
    private List<StructuredUserResponse> userResponseOptionsToShow;
    private DataInterpreter.PingGrade pingGrade;
    private WifiNetworkOverview wifiNetworkOverview;
    private BandwidthResult bandwidthResult;
    @ExpertMessageType private int expertMessageType;

    private ExpertMessage(@ExpertMessageType int expertMessageType,
                          String message, Action action,
                          ActionResult actionResult,
                          List<StructuredUserResponse> userResponseOptionsToShow,
                          DataInterpreter.PingGrade pingGrade,
                          WifiNetworkOverview wifiNetworkOverview,
                          BandwidthResult bandwidthResult) {
        this.expertMessageType = expertMessageType;
        this.message = message;
        this.actionResult = actionResult;
        this.expertAction = action;
        this.userResponseOptionsToShow = userResponseOptionsToShow;
        this.pingGrade = pingGrade;
        this.wifiNetworkOverview = wifiNetworkOverview;
        this.bandwidthResult = bandwidthResult;
    }

    /**
     * Factory constructor to create an instance
     */
    public static ExpertMessage createMessageForActionStarted(Action action) {
        return new ExpertMessage(ExpertMessageType.ACTION_STARTED, action.getName() + " Started", action, null, null, null, null, null);
    }

    public static ExpertMessage createMessageForActionEnded(Action action, ActionResult actionResult) {
        return new ExpertMessage(ExpertMessageType.ACTION_FINISHED, action.getName() + " Finished", action, actionResult, null, null, null, null);
    }

    public static ExpertMessage createMessageForActionEndedWithoutSuccess(Action action, ActionResult actionResult) {
        String messageToDisplay = action.getName() + " Finished with error ";
        if (actionResult != null) {
            messageToDisplay += actionResult.getStatusString();
        }
        return new ExpertMessage(ExpertMessageType.ACTION_FINISHED, messageToDisplay, action, actionResult, null, null, null, null);
    }

    public static ExpertMessage createPingActionCompleted(Action action, DataInterpreter.PingGrade pingGrade) {
        return new ExpertMessage(ExpertMessageType.SHOW_PING_INFO, null, action, null,  null, pingGrade, null, null);
    }

    public static ExpertMessage createBandwidthActionCompleted(Action action, BandwidthResult bandwidthResult) {
        return new ExpertMessage(ExpertMessageType.SHOW_BANDWIDTH_INFO, null, action, null, null, null, null, bandwidthResult);
    }

    public static ExpertMessage createShowNetworkOverview(Action action, WifiNetworkOverview wifiNetworkOverview) {
        return new ExpertMessage(ExpertMessageType.SHOW_WIFI_INFO, null, action, null,  null, null, wifiNetworkOverview, null);
    }

    public String getMessage() {
        return message;
    }

    public Action getExpertAction() {
        return expertAction;
    }

    public ActionResult getActionResult() {
        return actionResult;
    }

    public List<StructuredUserResponse> getUserResponseOptionsToShow() {
        return userResponseOptionsToShow;
    }

    public int getExpertMessageType() {
        return expertMessageType;
    }

    public DataInterpreter.PingGrade getPingGrade() {
        return pingGrade;
    }

    public WifiNetworkOverview getWifiNetworkOverview() {
        return wifiNetworkOverview;
    }

    public BandwidthResult getBandwidthResult() {
        return bandwidthResult;
    }
}
