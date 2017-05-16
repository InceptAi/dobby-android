package com.inceptai.dobby.ai.suggest;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.inceptai.dobby.utils.Utils;

import java.util.ArrayList;

/**
 * Created by arunesh on 5/16/17.
 */

public class LocalSnippet {

    private Snippet snippet;

    private ArrayList<Pair<String, String>> stringList;

    LocalSnippet(@NonNull Snippet snippet) {
        this.snippet = snippet;
        stringList = new ArrayList<>();
    }

    public void addString(String mainString) {
        stringList.add(new Pair<String, String>(mainString, Utils.EMPTY_STRING));
    }

    public void addString(String mainString, String moreString) {
        stringList.add(new Pair<String, String>(mainString, moreString));
    }
}
