package com.inceptai.dobby.ai.suggest;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by arunesh on 5/15/17.
 */

public class Snippet {

    @IntDef({})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
        int TYPE_NONE = 0;
    }
}
