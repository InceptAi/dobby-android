package com.inceptai.dobby.analytics;

import android.os.Bundle;

/**
 * Created by vivek on 7/3/17.
 */

public abstract class AnalyticsBackend {
    abstract public void logEvent(String eventType, Bundle bundle);
    abstract public void setUserProperty(String propertyName, String propertyValue);
}
