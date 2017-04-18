package com.inceptai.dobby.ai;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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

    boolean isNoop() {
        return inclusionMap.isEmpty() && exclusionSet.isEmpty();
    }

    void mergeIn(PossibleConditions mergeFrom) {
        inclusionMap.remove(mergeFrom.inclusionSet());
    }
}