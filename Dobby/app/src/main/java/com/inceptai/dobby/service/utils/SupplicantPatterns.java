package com.inceptai.dobby.service.utils;

import android.net.wifi.SupplicantState;

import java.util.Arrays;
import java.util.List;

/**
 * Created by vivek on 7/10/17.
 */

/*
* Lists of patterns which indicate high confidence bugged supplicant state
*/
public class SupplicantPatterns {
    public static final SupplicantPattern SCAN_BOUNCE_1 = new SupplicantPatterns().new SupplicantPattern(Arrays
            .asList(SupplicantState.DISCONNECTED,
                    SupplicantState.SCANNING, SupplicantState.FOUR_WAY_HANDSHAKE,
                    SupplicantState.SCANNING, SupplicantState.DISCONNECTED,
                    SupplicantState.FOUR_WAY_HANDSHAKE, SupplicantState.DISCONNECTED,
                    SupplicantState.SCANNING));

    public static final SupplicantPattern SCAN_BOUNCE_2 = new SupplicantPatterns().new SupplicantPattern(Arrays
            .asList(SupplicantState.DISCONNECTED,
                    SupplicantState.INACTIVE, SupplicantState.SCANNING,
                    SupplicantState.DISCONNECTED, SupplicantState.INACTIVE,
                    SupplicantState.SCANNING));

    public static final SupplicantPattern CONNECT_FAIL_1 = new SupplicantPatterns().new SupplicantPattern(
            Arrays.asList(SupplicantState.ASSOCIATED,
                    SupplicantState.FOUR_WAY_HANDSHAKE,
                    SupplicantState.DISCONNECTED, SupplicantState.ASSOCIATED,
                    SupplicantState.FOUR_WAY_HANDSHAKE,
                    SupplicantState.DISCONNECTED));

    public static List<SupplicantPattern> getSupplicantPatterns() {
        return Arrays
                .asList(SCAN_BOUNCE_1, SCAN_BOUNCE_2, CONNECT_FAIL_1);
    }

    public class SupplicantPattern {
        List<SupplicantState> list;

        SupplicantPattern(List<SupplicantState> pattern) {
            list = pattern;
        }

        public List<SupplicantState> getPattern() {
            return list;
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder();
            for (SupplicantState s : list) {
                out.append(s.name());
                out.append(",");
            }
            return out.toString();
        }
    }

}
