package com.inceptai.wifiexpert.expertSystem.inferencing;

import com.google.common.collect.Sets;
import com.inceptai.wifiexpert.utils.DobbyLog;
import com.inceptai.wifiexpert.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by arunesh on 4/18/17.
 */

public class PossibleConditions {
    public static final PossibleConditions NOOP_CONDITION = new PossibleConditions();

    private Map<Integer, Double> inclusionMap;
    private Set<Integer> exclusionSet;

    PossibleConditions() {
        inclusionMap = new ConcurrentHashMap();
        exclusionSet = Sets.newConcurrentHashSet();
    }

    void clearConditions() {
        inclusionMap.clear();
        exclusionSet.clear();
    }

    void excludeEverythingElse() {
        exclusionSet.addAll(InferenceMap.getAllConditions());
        for (int condition : inclusionMap.keySet()) {
            exclusionSet.remove(condition);
        }
    }

    void include(@InferenceMap.Condition int condition, double weight) {
        inclusionMap.put(condition, weight);
        exclusionSet.remove(condition);
    }

    void exclude(@InferenceMap.Condition int condition) {
        exclusionSet.add(condition);
    }

    void exclude(@InferenceMap.Condition int[] conditions) {
        for (int index=0; index < conditions.length; index++) {
            exclusionSet.add(conditions[index]);
        }
    }

    Set<Integer> inclusionSet() {
        return inclusionMap.keySet();
    }

    Set<Integer> exclusionSet() {
        return exclusionSet;
    }

    Map<Integer, Double> inclusionMap() {
        return inclusionMap;
    }

    boolean isNoop() {
        return inclusionMap.isEmpty() && exclusionSet.isEmpty();
    }

    void mergeIn(PossibleConditions mergeFrom) {
        DobbyLog.i("Removing conditions: " + InferenceMap.toString(mergeFrom.exclusionSet()));
        exclusionSet.addAll(mergeFrom.exclusionSet());
        Map<Integer, Double> mergeMap = mergeFrom.inclusionMap();
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

    public HashMap<Integer, Double> finalizeInference() {
        HashMap<Integer, Double> inclusionCopy = new HashMap<Integer, Double>(inclusionMap);
        for (Integer key : exclusionSet) {
            inclusionCopy.remove(key);
        }
        return inclusionCopy;
    }

    public HashMap<Integer, Double> sortConditions() {
        Map<Integer, Double> sortedHashMap = Utils.sortHashMapByValueDescending(inclusionMap);
        return (HashMap)sortedHashMap;
    }

    public List<Integer> getTopConditions(int maxConditionsToReturn, double maxGap) {
        List<Integer> topConditionsList = new ArrayList<>();
        normalizeWeights();
        Map<Integer, Double> sortedHashMap = Utils.sortHashMapByValueDescending(finalizeInference());
        double maxWeight = 0;
        for (HashMap.Entry<Integer, Double> entry : sortedHashMap.entrySet()) {
            if (maxWeight == 0) {
                maxWeight = entry.getValue();
            }
            if (maxWeight - entry.getValue() < maxGap && topConditionsList.size() <= maxConditionsToReturn) {
                topConditionsList.add(entry.getKey());
            }
        }
        return topConditionsList;
    }

    public HashMap<Integer, Double> getTopConditionsMap(int maxConditionsToReturn, double maxGap) {
        HashMap<Integer, Double> topConditionsMap = new HashMap<>();
        normalizeWeights();
        Map<Integer, Double> sortedHashMap = Utils.sortHashMapByValueDescending(finalizeInference());
        double maxWeight = 0;
        for (HashMap.Entry<Integer, Double> entry : sortedHashMap.entrySet()) {
            if (maxWeight == 0) {
                maxWeight = entry.getValue();
            }
            if (maxWeight - entry.getValue() < maxGap && topConditionsMap.size() <= maxConditionsToReturn) {
                topConditionsMap.put(entry.getKey(), entry.getValue());
            }
        }
        return topConditionsMap;
    }

}