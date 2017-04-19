package com.inceptai.dobby.ai;

import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Created by arunesh on 4/18/17.
 */

public class PossibleConditions {
    public static final PossibleConditions NOOP_CONDITION = new PossibleConditions();

    private HashMap<Integer, Double> inclusionMap;
    private Set<Integer> exclusionSet;

    PossibleConditions() {
        inclusionMap = new HashMap<>();
        exclusionSet = new HashSet<>();
    }

    void include(@InferenceMap.Condition int condition, double weight) {
        inclusionMap.put(condition, weight);
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
            Double incumbentWeight = inclusionMap.get(condition);
            Double totalWeight = mergeMap.get(condition);
            if (incumbentWeight != null) {
                totalWeight += incumbentWeight;
            }
            inclusionMap.put(condition, totalWeight);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PossibleConditions dump.\n");
        builder.append("Exclusion conditions : " + exclusionSet.size() + "\n");
        for (int condition : exclusionSet) {
            builder.append("  Condition: "
                    + InferenceMap.conditionString(condition)
                    + "\n");
        }
        builder.append("Inclusion conditions: " + inclusionMap().size());
        for (HashMap.Entry<Integer, Double> entry : inclusionMap.entrySet()) {
            @InferenceMap.Condition
            int condition = entry.getKey();
            builder.append("  Condition: "
            + InferenceMap.conditionString(condition) + ", weight: " + entry.getValue());
        }
        return builder.toString();
    }

    public void normalizeWeights() {
        double sum = 0.0;
        for (Double weight : inclusionMap.values()) {
            sum += weight;
        }
        for (HashMap.Entry<Integer, Double> entry : inclusionMap.entrySet()) {
            entry.setValue(entry.getValue() / sum);
        }
    }
}