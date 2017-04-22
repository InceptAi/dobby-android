package com.inceptai.dobby.ai;

/**
 * Created by arunesh on 4/21/17.
 */

/**
 * Represents the result of a ComposableOperation. One of the possible values of T above.
 */
public class OperationResult {
    public static final int SUCCESS = 0;
    public static final int FAILED_TO_START = 1;

    private int status;
    private Object payload;

    public OperationResult(int status) {
        this.status = status;
    }

    public OperationResult(int status, Object payload) {
        this.status = status;
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public int getStatus() {
        return status;
    }
}
