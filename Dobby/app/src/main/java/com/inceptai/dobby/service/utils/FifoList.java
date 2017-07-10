package com.inceptai.dobby.service.utils;

/**
 * Created by vivek on 7/10/17.
 */

import android.net.wifi.SupplicantState;

import java.util.ArrayList;
import java.util.List;

public class FifoList extends ArrayList<Object> {
    /*
     * Behaves as fixed-size FIFO
     */
    private final int length;

    public FifoList(int s) {
        length = s;
    }

    @Override
    public boolean add(Object object) {
        if (this.size() < length)
            this.add(0, object);
        else {
            this.remove(this.size() - 1);
            this.add(0, object);
        }
        return true;
    }

    public boolean containsPattern(List<SupplicantState> collection) {
        if (this.size() < collection.size())
            return false;
        int chash = hashSum(collection);
        int sum;
        for (int n = 0; n < this.size() - collection.size(); n++) {
            sum = 0;
            for (int c = 0; c < collection.size(); c++) {
                sum += this.get(n + c).hashCode();
                if (sum == chash)
                    return true;
            }
        }
        return false;
    }

    private static int hashSum(List<SupplicantState> collection) {
        int sum = 0;
        for (int n = 0; n < collection.size(); n++) {
            sum += collection.get(n).hashCode();
        }
        return sum;
    }

    public SupplicantPatterns.SupplicantPattern containsPatterns(
            List<SupplicantPatterns.SupplicantPattern> patterns) {
        for (SupplicantPatterns.SupplicantPattern n : patterns) {
            if (this.containsPattern(n.getPattern()))
                return n;
        }
        return null;
    }
}

