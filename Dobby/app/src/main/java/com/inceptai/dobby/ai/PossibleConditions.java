package com.inceptai.dobby.ai;

import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Created by arunesh on 4/18/17.
 */

public class PossibleConditions {
    public static final int TYPE_EXCLUSION = 1;
    public static final int TYPE_INCLUSION = 2;
    public static final int TYPE_NOOP = 3;
    public static final PossibleConditions NOOP_CONDITION = new PossibleConditions();

    private HashMap<Integer, Double> inclusionMap;
    private Set<Integer> exclusionSet;

    PossibleConditions() {
        inclusionMap = new HashMap<>();
        exclusionSet = new HashSet<>();
    }

    void include(@InferenceMap.Condition int condition, double probability) {
        inclusionMap.put(condition, probability);
    }

    void exclude(@InferenceMap.Condition int condition) {
        exclusionSet.add(condition);
    }

    Set<Integer> inclusionSet() {
        return inclusionMap.keySet();
    }

    Set<Integer> exclusionSet() {
        return exclusionSet;
    }

    HashMap<Integer, Double> inclusionMap() {
        return inclusionMap;
    }

    boolean isNoop() {
        return inclusionMap.isEmpty() && exclusionSet.isEmpty();
    }

    void mergeIn(PossibleConditions mergeFrom) {
        Log.i(TAG, "Removing conditions: " + InferenceMap.toString(mergeFrom.exclusionSet()));
        inclusionMap.remove(mergeFrom.exclusionSet());
        HashMap<Integer, Double> mergeMap = mergeFrom.inclusionMap();
        for (int condition : mergeMap.keySet()) {
            Double incumbentProb = inclusionMap.get(condition);
            if (incumbentProb != null) {

            }
        }
    }
}