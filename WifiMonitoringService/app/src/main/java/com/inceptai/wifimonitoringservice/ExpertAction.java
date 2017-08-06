package com.inceptai.wifimonitoringservice;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by vivek on 8/5/17.
 */

public class ExpertAction {

    public static class ActionRequest {
        @ActionType private int actionType;
        private int networkId;
        private long actionTimeOutMs;

        //Additional parameters
        public ActionRequest(@ActionType int actionType) {
            this.actionType = actionType;
            this.networkId = 0;
            this.actionTimeOutMs = ACTION_TIMEOUT_MS;
        }

        public ActionRequest(@ActionType int actionType, long actionTimeOutMs) {
            this.actionType = actionType;
            this.networkId = 0;
            this.actionTimeOutMs = actionTimeOutMs;
        }

        public ActionRequest(@ActionType int actionType, int networkId) {
            this.actionType = actionType;
            this.networkId = networkId;
            this.actionTimeOutMs = ACTION_TIMEOUT_MS;
        }


        public ActionRequest(@ActionType int actionType, int networkId, long actionTimeOutMs) {
            this.actionType = actionType;
            this.networkId = 0;
            this.actionTimeOutMs = actionTimeOutMs;
        }

        @ActionType public int getActionType() {
            return actionType;
        }

        public int getNetworkId() {
            return networkId;
        }

        public long getActionTimeOutMs() {
            return actionTimeOutMs;
        }
    }


}
