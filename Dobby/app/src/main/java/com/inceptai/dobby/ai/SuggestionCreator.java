package com.inceptai.dobby.ai;

import com.inceptai.dobby.ai.InferenceMap.Condition;
import com.inceptai.dobby.connectivity.ConnectivityAnalyzer;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.dobby.wifi.WifiState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.inceptai.dobby.utils.Utils.convertSignalDbmToPercent;

/**
 * Created by vivek on 4/19/17.
 */
public class SuggestionCreator {
    private static final String MULTIPLE_CONDITIONS_PREFIX = "There a few things which can be causing problems for your network.";
    private static final String NO_CONDITION_MESSAGE = "We performed speed tests, DNS pings and wifi tests on your network and did not see anything amiss.";

    public static class Suggestion {
        String title;
        List<String> longSuggestionList;
        List<String> shortSuggestionList;
        HashMap<Integer, Double> conditionsMap;
        SuggestionCreatorParams suggestionCreatorParams;
        long creationTimestampMs;

        Suggestion() {
            title = Utils.EMPTY_STRING;
            longSuggestionList = new ArrayList<>();
            shortSuggestionList = new ArrayList<>();
            conditionsMap = new HashMap<>();
            suggestionCreatorParams = new SuggestionCreatorParams();
            creationTimestampMs = System.currentTimeMillis();
        }

        public String getTitle() {
            return title;
        }

        public long getCreationTimestampMs() {
            return creationTimestampMs;
        }

        public ArrayList<String> getLongSuggestionList() {
            return new ArrayList<>(longSuggestionList);
        }

        public List<String> getShortSuggestionList() {
            return shortSuggestionList;
        }

        public String getLongSuggestionListString() {
            return convertListToStringMessage(true);
        }

        public String getShortSuggestionListString() {
            return convertListToStringMessage(false);
        }

        private String convertListToStringMessage(boolean useLongSuggestions) {
            //Use short suggestion by default.
            final String CONDITIONS_PREFIX = "Here are the details";
            List<String> suggestionList;
            StringBuilder sb = new StringBuilder();
            if (useLongSuggestions) {
                suggestionList = longSuggestionList;
            } else {
                suggestionList = shortSuggestionList;
            }
            if (suggestionList.size() == 0) {
                sb.append(getNoConditionMessageList(suggestionCreatorParams));
            } else if (suggestionList.size() == 1) {
                sb.append(suggestionList.get(0));
            } else {
                //Multiple conditions
                sb.append(CONDITIONS_PREFIX);
                int index = 1;
                for(String suggestion: suggestionList) {
                    sb.append("\n");
                    sb.append(String.valueOf(index));
                    sb.append(". ");
                    sb.append(suggestion);
                    index++;
                }
            }
            return sb.toString();
        }

        public String getIsp() {
            return suggestionCreatorParams.bandwidthGrade.isp;
        }

        public String getExternalIp() {
            return suggestionCreatorParams.bandwidthGrade.externalIP;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("TITLE: ");
            sb.append(getTitle());
            sb.append("\n");
            sb.append("DETAIL LIST: ");
            //Use long suggestion by default.
            sb.append(convertListToStringMessage(true));
            sb.append("\n");
            sb.append("SHORT LIST: ");
            //Use long suggestion by default.
            sb.append(convertListToStringMessage(false));
            sb.append("\n");
            sb.append("CONDITIONS: ");
            sb.append(conditionsMap.toString());
            sb.append("\n");
            return sb.toString();
        }
    }

    public static class SuggestionCreatorParams {
        DataInterpreter.BandwidthGrade bandwidthGrade;
        DataInterpreter.WifiGrade wifiGrade;
        DataInterpreter.PingGrade pingGrade;
        DataInterpreter.HttpGrade httpGrade;
        SuggestionCreatorParams() {
            bandwidthGrade = new DataInterpreter.BandwidthGrade();
            wifiGrade = new DataInterpreter.WifiGrade();
            pingGrade = new DataInterpreter.PingGrade();
            httpGrade = new DataInterpreter.HttpGrade();
        }
    }

    private static String convertChannelFrequencyToString(int channelFrequency) {
        int channelNumber = Utils.convertCenterFrequencyToChannelNumber(channelFrequency);
        String channelString = Utils.EMPTY_STRING;
        if (channelNumber > 0) {
            channelString = Integer.toString(channelNumber);
        } else {
            channelString = Integer.toString(channelNumber);
        }
        return channelString;
    }

    static Suggestion get(HashMap<Integer, Double> conditionMap, SuggestionCreatorParams params) {
        Suggestion suggestionToReturn = new Suggestion();
        suggestionToReturn.suggestionCreatorParams = params;
        suggestionToReturn.conditionsMap = conditionMap;
        List<Integer> conditionList = new ArrayList<>(conditionMap.keySet());
        suggestionToReturn.title = getTitle(conditionList, params);
        if (conditionList.size() > 0) {
            for (int index = 0; index < conditionList.size(); index++) {
                @Condition int condition = conditionList.get(index);
                suggestionToReturn.longSuggestionList.add(getSuggestionForCondition(condition, params));
                suggestionToReturn.shortSuggestionList.add(getShortSuggestionForCondition(condition, params));
            }
        } else {
            suggestionToReturn.longSuggestionList = getNoConditionMessageList(params);
            suggestionToReturn.shortSuggestionList = getNoConditionMessageList(params);
        }
        DobbyLog.v("Returning suggestion " + suggestionToReturn.toString());
        return suggestionToReturn;
    }

    private static List<String> getNoConditionMessageList(SuggestionCreatorParams params) {
        List<String> noConditionMessageList = new ArrayList<>();
        noConditionMessageList.add("We performed speed tests, pings and wifi scans on your network. ");

        if (DataInterpreter.isNonFunctionalOrUnknown(params.bandwidthGrade.getDownloadBandwidthMetric()) ||
                DataInterpreter.isNonFunctionalOrUnknown(params.bandwidthGrade.getUploadBandwidthMetric()) ||
                DataInterpreter.isUnknown(params.wifiGrade.primaryApSignalMetric)) {
            //Shouldn't have happened, but we are here.
            noConditionMessageList.add(getShortNoConditionMessage(params));
            return noConditionMessageList;
        }

        noConditionMessageList.add("You are getting " + String.format("%.2f", params.bandwidthGrade.getDownloadBandwidth()) +
                " Mbps download and " + String.format("%.2f", params.bandwidthGrade.getUploadBandwidth()) +
                " Mbps upload speed on your phone, which is pretty good. ");
        noConditionMessageList.add("You connection to your wifi is also strong at about " +
                Utils.convertSignalDbmToPercent(params.wifiGrade.getPrimaryApSignal()) +
                "% strength (100% means very high signal, usually when you " +
                "are right next to wifi router).");
        noConditionMessageList.add("Since wifi network problems are sometimes transient, " +
                "it might be good if you run this test a few times so we can catch an issue " +
                "if it shows up. Hope this helps :)");
        return noConditionMessageList;

    }

    private static String getShortNoConditionMessage(SuggestionCreatorParams params) {
        String output = Utils.EMPTY_STRING;
        if (DataInterpreter.isUnknown(params.bandwidthGrade.getDownloadBandwidthMetric()) ||
                DataInterpreter.isUnknown(params.bandwidthGrade.getUploadBandwidthMetric()) || DataInterpreter.isUnknown(params.wifiGrade.getPrimaryApSignalMetric())) {
            output += "We were unable to run bandwidth tests as your phone is unable to reach the Internet right now. ";
            if (!(params.wifiGrade.getPrimaryApSsid() == null || params.wifiGrade.getPrimaryApSsid().equals(""))) {
                output += "Make sure " + params.wifiGrade.getPrimaryApSsid() + " is the right wifi network you want to use. ";
            }
           output += "Your wifi doesn't seem to be connected properly for us to estimate the bandwidth. " +
                        "Try rebooting the router if possible or contact your administrator if this is a managed wifi network. ";
            return output;
        }
        return "We don't see any key issues with your wifi network at the moment. " +
                "Your Internet speeds are good and your wifi network is working " +
                "well as per our tests.";
    }

    public static String getSuggestionString(List<Integer> conditionList,
                                              SuggestionCreatorParams params,
                                              boolean getLongSuggestions) {
        final String NO_CONDITION_MESSAGE = getShortNoConditionMessage(params);

        List<String> suggestionList = new ArrayList<>();

        for (int index=0; index < conditionList.size(); index++) {
            @Condition int condition  = conditionList.get(index);
            if (getLongSuggestions) {
                suggestionList.add(getSuggestionForCondition(condition, params));
            } else {
                suggestionList.add(getShortSuggestionForCondition(condition, params));
            }
        }

        StringBuilder sb = new StringBuilder();
        if (suggestionList.size() == 0) {
            sb.append(NO_CONDITION_MESSAGE);
        } else if (suggestionList.size() == 1) {
            sb.append(suggestionList.get(0));
        } else {
            //Multiple conditions
            sb.append(MULTIPLE_CONDITIONS_PREFIX);
            sb.append("\n");
            int index = 1;
            for(String suggestion: suggestionList) {
                sb.append(index);
                sb.append(". ");
                sb.append(suggestion);
                sb.append("\n");
                index++;
            }
        }

        return sb.toString();

    }

    private static String getTitle(List<Integer> conditionList,
                                  SuggestionCreatorParams params) {
        String titleToReturn = Utils.EMPTY_STRING;
        if (conditionList.size() > 0) {
            @Condition int condition = conditionList.get(0);
            titleToReturn = getTitleForCondition(condition, params);
        } else {
            titleToReturn = getShortNoConditionMessage(params);
        }
        return titleToReturn;
    }

    private static boolean isAlternateDNSBetter(SuggestionCreatorParams params) {
        return (params.pingGrade.hasValidData() && DataInterpreter.compareMetric(params.pingGrade.alternativeDnsMetric,
                params.pingGrade.dnsServerLatencyMetric) > 0);
    }

    private static String getAlternateDNSMessage(SuggestionCreatorParams params) {
        if (isAlternateDNSBetter(params)) {
            return "Try changing your primary DNS to " +  params.pingGrade.getAlternativeDns() +
                    " which has much lower latency (" + params.pingGrade.getAlternativeDnsLatencyMs() +
                    " ms vs. " + params.pingGrade.getDnsServerLatencyMs() + " ms) " +
                    "and see if things are faster to load. ";
        }
        return Utils.EMPTY_STRING;
    }

    private static String getSuggestionForCondition(@InferenceMap.Condition int condition,
                                                   SuggestionCreatorParams params) {
        StringBuilder suggestionToReturn = new StringBuilder();
        String baseMessage = Utils.EMPTY_STRING;
        String conditionalMessage = Utils.EMPTY_STRING;
        switch (condition) {
            case Condition.WIFI_CHANNEL_CONGESTION:
                baseMessage = "Your wifi is operating on Channel " +
                        convertChannelFrequencyToString(params.wifiGrade.getPrimaryApChannel()) +
                        " which is congested. This means there a lot of other Wifi networks near " +
                        "you which are also operating on the same channel as yours. ";
                if (params.wifiGrade.hasValidData() && params.wifiGrade.getLeastOccupiedChannel() > 0 &&
                        params.wifiGrade.getPrimaryApChannel() != params.wifiGrade.getLeastOccupiedChannel()) {
                    conditionalMessage = "As per our current analysis, Channel " +
                            convertChannelFrequencyToString(params.wifiGrade.getLeastOccupiedChannel()) +
                            " could provide better results for your network.";
                }
                break;
            case Condition.WIFI_CHANNEL_BAD_SIGNAL:
                //TODO: Include a line about how bad signal is impacting the download/upload bandwidth
                baseMessage = "Your signal to your wireless router is weak (about "
                        + Utils.convertSignalDbmToPercent(params.wifiGrade.getPrimaryApSignal()) + "/100) " +
                        ", this could lead to poor speeds and bad experience in streaming etc. " +
                        "\n a. If you are close to your router while doing this test (within 20ft), then your router is not " +
                        "providing enough signal. \n b. Make sure your router is not obstructed and if " +
                        "that doesn't help, you should try replacing the router. \n c. If you are " +
                        "actually far from your router during the test, then your router is not " +
                        "strong enough to cover the current testing area and you should look into " +
                        "a stronger router or a mesh Wifi solution which can provide better coverage.";
                break;
            case Condition.WIFI_INTERFACE_ON_PHONE_OFFLINE:
                baseMessage = Utils.EMPTY_STRING;
                break; //find a good explanation
            case Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE:
                baseMessage = "Your wifi on your phone seems to be in a bad state, " +
                        "since it is not able to connect to your wireless router. " +
                        "Try turning it off/on and see if that helps. ";
                break;
            case Condition.WIFI_INTERFACE_ON_PHONE_TURNED_OFF:
                baseMessage = "Wifi on your phone is turned off, so we cannot assess the " +
                        "performance of your wireless network. Try turning it on and running " +
                        "the tests again.";
                break;
            case Condition.WIFI_LINK_DHCP_ISSUE:
            case Condition.WIFI_LINK_ASSOCIATION_ISSUE:
                baseMessage = "Your phone is unable to get on your wifi network. This could be either " +
                        "due to wifi router or your phone being in a bad state. Try turning your " +
                        "phone's wifi off/on and if it still doesn't connect, then reboot your router.";
                break;
            case Condition.WIFI_LINK_AUTH_ISSUE:
                baseMessage =  "Your phone is unable to get on your wifi network due to some authentication " +
                        "issue. Make sure you have the right password (or update the wifi password " +
                        "if changed). If the problem still persists, then your router could be in a " +
                        "bad state so try rebooting the router and it might help.";
                break;
            case Condition.ROUTER_GOOD_SIGNAL_USING_SLOW_DATA_RATE:
                baseMessage =  "Your signal to your wifi router is strong, but it is using a relatively low " +
                        "speed for transferring data, which can cause laggy experience. Make sure " +
                        "Mixed mode or 802.11n mode is On if your router supports it or we would " +
                        "recommend getting a router which supports 802.11n for higher speeds.";
                break;
            case Condition.ROUTER_SOFTWARE_FAULT:
                baseMessage = getRouterFaultString(params);
                if (baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage = "Your router may be in a bad state, try rebooting it and it should put " +
                            "the router in a good state so you can connect to it.  ";
                }
                break;
            case Condition.ISP_INTERNET_DOWN:
                if (!baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage += " However, looks ";
                } else {
                    baseMessage = "Looks ";
                }
                baseMessage +=  "like your Internet service is down. " +
                        "We are unable to reach external servers for any bandwidth testing.";
                if (!params.bandwidthGrade.isp.equals(Utils.EMPTY_STRING)) {
                    conditionalMessage = "You should contact " + params.bandwidthGrade.isp + " to see if they know about any issues " +
                            "in your area";
                }
                break;
            case Condition.ISP_INTERNET_SLOW:
                if (!baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage += " However, looks ";
                } else {
                    baseMessage = "Looks ";
                }
                baseMessage += "like your Internet service could be slow at the moment. " +
                        "You are getting about " + String.format("%.2f", params.bandwidthGrade.getDownloadBandwidth()) + " Mbps download and " +
                        String.format("%.2f", params.bandwidthGrade.getUploadBandwidth()) + " Mbps upload. If these speeds seem low as per your " +
                        "contract, you should reach out to " + params.bandwidthGrade.isp + " and see why you are getting such low speeds. " +
                        "You should tell them that your wifi network latency is low but the " +
                        "latency to access Internet is high.";
                break;
            case Condition.ISP_INTERNET_SLOW_DOWNLOAD:
                baseMessage = "Looks like your Internet download speed is slow " +
                        "(around " + String.format("%.2f", params.bandwidthGrade.getDownloadBandwidth()) + " Mbps), especially given you are getting a good " +
                        "upload speed ( " + String.format("%.2f", params.bandwidthGrade.getUploadBandwidth()) + " Mbps). Since most of the data " +
                        "consumed by streaming, browsing etc. is download, you will experience slow Internet on your devices. " +
                        "If these speeds seem low as per your contract, you should " +
                        "reach out to " + params.bandwidthGrade.isp + " and see why you are getting such low speeds. " +
                        "You should tell them that your wifi network latency is low but the " +
                        "download speed is slow (esp. compared to upload).";
                break;
            case Condition.ISP_INTERNET_SLOW_UPLOAD:
                baseMessage = "Looks like your Internet upload speed is slow " +
                        "(around " + String.format("%.2f", params.bandwidthGrade.getUploadBandwidth()) +
                        " Mbps), especially given you are getting a good " +
                        "download speed (~ " + String.format("%.2f", params.bandwidthGrade.getDownloadBandwidth()) +
                        "  Mbps). You will have trouble uploading " +
                        "content like posting photos, sending email attachments etc. " +
                        "If these speeds seem low as per your contract, you should " +
                        "reach out to " + params.bandwidthGrade.isp + "  and see why you are getting such low speeds. " +
                        "You should tell them that your wifi network latency is low but the " +
                        "upload speed is slow (esp. compared to download).";
                break;
            case Condition.DNS_RESPONSE_SLOW:
                baseMessage = "Your current DNS server is acting slow to respond to queries, " +
                        "which can cause an initial lag during the load time of an app or a new webpage.";
                if (isAlternateDNSBetter(params)) {
                    conditionalMessage = "Our tests show that using other public DNSs than the one you " +
                            "are currently configured for can speed up your load times. Try changing " +
                            "your DNS server with " + params.pingGrade.getAlternativeDns() + " and re-run the test. " +
                            "You can change your DNS settings just for your phone first and see if that improves things.";
                }
                break;
            case Condition.DNS_SLOW_TO_REACH:
                if (!baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage += " However, your ";
                } else {
                    baseMessage = "Your ";
                }
                baseMessage += "current DNS server has a high latency, " +
                        "which can cause an initial lag during the load time of an app or a " +
                        "webpage.";
                break;
            case Condition.DNS_UNREACHABLE:
                if (!baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage += " However, we ";
                } else {
                    baseMessage = "we ";
                }
                baseMessage += "are unable to reach your DNS server, which means you can't access " +
                        "the Internet on your phone and other devices. This could be because the DNS " +
                        "server you have configured is down. ";
                if (isAlternateDNSBetter(params)) {
                    conditionalMessage = "Our tests show that using other public DNSs than the one you " +
                            "are currently configured for can speed up your load times. Try changing " +
                            "your DNS server with " + params.pingGrade.getAlternativeDns() + " and re-run the test. " +
                            "You can change your DNS settings just for your phone first and see if " +
                            "that improves things.";
                }
                break;
            case Condition.CABLE_MODEM_FAULT:
                baseMessage = "We think your cable modem might be faulty (It is the device which is " +
                        "provided by the Internet provider and is connected to your wireless router)" +
                        ". You should try resetting it to see if improves the performance. ";
                break;
            case Condition.CAPTIVE_PORTAL_NO_INTERNET:
                if (!baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage += " However, you ";
                } else {
                    baseMessage = "You ";
                }
                baseMessage += "are behind a captive portal -- " +
                        "basically the wifi you are connected to " + params.wifiGrade.getPrimaryApSsid() + " is managed " +
                        "by someone who restricts access unless you sign in. Currently you don't have " +
                        "access to it. Try launching a browser and it should redirect you to a " +
                        "login form. Once you are connected, you can re-run this test to see how " +
                        "your network is doing. ";
                break;
            case Condition.REMOTE_SERVER_IS_SLOW_TO_RESPOND:
                baseMessage = "Currently we believe that external servers that host different " +
                        "Internet services are a bit slow to respond to our tests. This could " +
                        "be because of failures or configuration issues in the general routing of " +
                        "packets. You can call your Internet provider to see if they have any " +
                        "information on this. ";
                break;
        }
        suggestionToReturn.append(baseMessage);
        suggestionToReturn.append(conditionalMessage);
        return suggestionToReturn.toString();
    }

    private static String convertWifiLinkModeToIssueString(@WifiState.WifiLinkMode int linkMode) {

        if (linkMode == WifiState.WifiLinkMode.FREQUENT_DISCONNECTIONS) {
            return "It seems like your phone is having trouble staying connected to the wifi network. " +
                    "It connects for short periods of time and then disconnects quickly." +
                    " If you think you should be connected, try rebooting the router (if possible) " +
                    "and hopefully it will bring your router in a good state so you can connect. ";
        }

        String issuesString = "Your phone is not currently connected to any Wifi network. ";
        switch (linkMode) {
            case WifiState.WifiLinkMode.HANGING_ON_DHCP:
                issuesString += "Specifically, it is unable to get an IP address from your wifi router. ";
                break;
            case WifiState.WifiLinkMode.HANGING_ON_SCANNING:
                issuesString += "Specifically, it is unable to find your wireless router to connect. " +
                        "Make sure the router is plugged in. ";
                break;
            case WifiState.WifiLinkMode.HANGING_ON_AUTHENTICATING:
                issuesString +=  "It is unable to get on your wifi network due to some authentication " +
                        "issue. Make sure you have the right password. ";
                break;
            case WifiState.WifiLinkMode.CONNECTING:
                issuesString += "It is trying to connect to your wifi network but is not yet successful. ";
                break;
            case WifiState.WifiLinkMode.DISCONNECTED:
                break;
        }
        issuesString += " If you think you should be connected, try rebooting the router (if possible) " +
                "and hopefully it will bring the router in a good state so you can connect to it ";
        return issuesString;
    }

    private static String getRouterFaultString(SuggestionCreatorParams params) {
        switch (params.wifiGrade.getWifiConnectivityMode()) {
            case ConnectivityAnalyzer.WifiConnectivityMode.ON_AND_DISCONNECTED:
                return convertWifiLinkModeToIssueString(params.wifiGrade.wifiLinkMode);
            case ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_OFFLINE:
                if ((params.pingGrade.hasValidData() && DataInterpreter.isGoodOrExcellentOrAverage(params.pingGrade.routerLatencyMetric)) ||
                        (params.httpGrade.hasValidData() && DataInterpreter.isGoodOrExcellentOrAverage(params.httpGrade.httpDownloadLatencyMetric))) {
                    return "Your wifi network is fine, but for some reason, connectivity to the Internet is down. " +
                            "It could be an issue with the router software being in a stalled state or you are behind a captive portal which requires sign-in (mostly public hotspots). " +
                            "If this issue is at home, try rebooting the router to see if it works. ";
                } else  if ((params.pingGrade.hasValidData() && DataInterpreter.isPoorOrAbysmal(params.pingGrade.routerLatencyMetric)) ||
                    (params.httpGrade.hasValidData() && DataInterpreter.isPoorOrAbysmal(params.httpGrade.httpDownloadLatencyMetric))) {
                return "You are connected to the router, but your link is poor (" +
                        String.format("%.2f", params.pingGrade.routerLatencyMs) + " ms), " +
                        "and you can't connect to Internet. This could be because of an issue with the router or you are behind a captive portal which requires sign-in (mostly public hotspots). " +
                        "If this issue is at home, Try rebooting the wifi router. ";
                } else if ((params.pingGrade.hasValidData() && DataInterpreter.isNonFunctional(params.pingGrade.routerLatencyMetric)) ||
                        (params.httpGrade.hasValidData() && DataInterpreter.isNonFunctional(params.httpGrade.httpDownloadLatencyMetric))) {
                    return "We can't connect to Internet. This could be because of an issue with the router or you are behind a captive portal which requires sign-in (mostly public hotspots). " +
                            "If this issue is at home, try rebooting the wifi router and it should put the router in a good state so you can connect to it. ";
                }
            case ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_ONLINE:
                if ((params.pingGrade.hasValidData() && DataInterpreter.isGoodOrExcellentOrAverage(params.pingGrade.routerLatencyMetric)) ||
                        (params.httpGrade.hasValidData() && DataInterpreter.isGoodOrExcellentOrAverage(params.httpGrade.httpDownloadLatencyMetric))) {
                    return Utils.EMPTY_STRING;
                } else  if ((params.pingGrade.hasValidData() && DataInterpreter.isPoorOrAbysmal(params.pingGrade.routerLatencyMetric)) ||
                        (params.httpGrade.hasValidData() && DataInterpreter.isPoorOrAbysmal(params.httpGrade.httpDownloadLatencyMetric))) {
                    return "You are connected to the router, but your link is poor (" +
                            String.format("%.2f", params.pingGrade.routerLatencyMs) + " ms), which could result in poor speeds. This could be because of an issue with the router. " +
                            "Try rebooting it to see if it works";
                } else if ((params.pingGrade.hasValidData() && DataInterpreter.isNonFunctional(params.pingGrade.routerLatencyMetric)) ||
                        (params.httpGrade.hasValidData() && DataInterpreter.isNonFunctional(params.httpGrade.httpDownloadLatencyMetric))) {
                    return "We can't reach your wifi router, maybe it is intermittently available. This could be because of an issue with the router. " +
                            "If possible, try rebooting it and and it should put the router in a good state so you can connect to it.  ";
                }
            case ConnectivityAnalyzer.WifiConnectivityMode.UNKNOWN:
            case ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_UNKNOWN:
                if ((params.pingGrade.hasValidData() && DataInterpreter.isGoodOrExcellentOrAverage(params.pingGrade.routerLatencyMetric)) ||
                        (params.httpGrade.hasValidData() && DataInterpreter.isGoodOrExcellentOrAverage(params.httpGrade.httpDownloadLatencyMetric))) {
                    return Utils.EMPTY_STRING;
                } else  if ((params.pingGrade.hasValidData() && DataInterpreter.isPoorOrAbysmal(params.pingGrade.routerLatencyMetric)) ||
                        (params.httpGrade.hasValidData() && DataInterpreter.isPoorOrAbysmal(params.httpGrade.httpDownloadLatencyMetric))) {
                    return "You are connected to the router, but your link is poor (" +
                            String.format("%.2f", params.pingGrade.routerLatencyMs) + " ms), which could result in poor speeds. This could be because of an issue with the router. " +
                            "Try rebooting it to see if it works";
                } else if ((params.pingGrade.hasValidData() && DataInterpreter.isNonFunctional(params.pingGrade.routerLatencyMetric)) ||
                        (params.httpGrade.hasValidData() && DataInterpreter.isNonFunctional(params.httpGrade.httpDownloadLatencyMetric))) {
                    return "We can't reach your wifi router, maybe it is intermittently available. This could be because of an issue with the router. " +
                            "If possible, try rebooting it and it should put the router in a good state so you can connect to it.  ";
                }
        }
        return Utils.EMPTY_STRING;
    }

    private static String getMessageAboutWifiLink(SuggestionCreatorParams params) {
        if ((params.wifiGrade.hasValidData() && DataInterpreter.isGoodOrExcellentOrAverage(params.wifiGrade.getPrimaryApSignalMetric())) ||
                (params.pingGrade.hasValidData() && DataInterpreter.isGoodOrExcellentOrAverage(params.pingGrade.routerLatencyMetric)) ||
                (params.httpGrade.hasValidData() && DataInterpreter.isGoodOrExcellentOrAverage(params.httpGrade.httpDownloadLatencyMetric))) {
            return "Your wifi connection to your router is fine. ";
        }
        return Utils.EMPTY_STRING;
    }

    public static String getTitleForCondition(@InferenceMap.Condition int condition,
                                                   SuggestionCreatorParams params) {
        StringBuilder suggestionToReturn = new StringBuilder();
        String baseMessage = Utils.EMPTY_STRING;
        String conditionalMessage = Utils.EMPTY_STRING;
        switch (condition) {
            case Condition.WIFI_CHANNEL_CONGESTION:
                baseMessage = "Your wifi is operating on a congested channel, " +
                        "which can cause slowness.";
                if (params.wifiGrade.hasValidData() &&
                        params.wifiGrade.getLeastOccupiedChannel() > 0 &&
                        params.wifiGrade.getLeastOccupiedChannel() != params.wifiGrade.getPrimaryApChannel()) {
                    conditionalMessage = "Try changing your channel to " + params.wifiGrade.getLeastOccupiedChannel() + " for better performance.";
                }
                break;
            case Condition.WIFI_CHANNEL_BAD_SIGNAL:
                baseMessage = "Your current signal to your wireless router is weak, only about  " +
                        Utils.convertSignalDbmToPercent(params.wifiGrade.getPrimaryApSignal()) + " %.";
                if (DataInterpreter.isPoorOrAbysmalOrNonFunctional(params.bandwidthGrade.getDownloadBandwidthMetric())
                        && DataInterpreter.isPoorOrAbysmalOrNonFunctional(params.bandwidthGrade.getUploadBandwidthMetric())) {
                    conditionalMessage = "This could be the key reason why you are getting poor download (" +
                            params.bandwidthGrade.getDownloadMbps() + " Mbps) and upload (" +
                            params.bandwidthGrade.getUploadMbps() + " Mbps) speeds. ";
                }
                conditionalMessage  += " Try moving closer to the wifi router to get better speeds.";
                break;
            case Condition.WIFI_INTERFACE_ON_PHONE_OFFLINE:
                break; //find a good explanation
            case Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE:
                baseMessage = "Your wifi on your phone seems to be in a bad state, " +
                        "try turning it off/on and see if that helps. ";
                break;
            case Condition.WIFI_INTERFACE_ON_PHONE_TURNED_OFF:
                baseMessage = "Wifi on your phone is turned off, try turning it on and running " +
                        "the tests again.";
                break;
            case Condition.ROUTER_GOOD_SIGNAL_USING_SLOW_DATA_RATE:
                baseMessage =  "Even though your signal to wifi router is strong (~" +
                        Utils.convertSignalDbmToPercent(params.wifiGrade.getPrimaryApSignal()) + " %), " +
                        " the wifi router is using low speeds for transferring data. Make sure Mixed " +
                        "mode or 802.11n mode is on if your router supports it. ";
                break;
            case Condition.ROUTER_FAULT_WIFI_OK:
                break; // Not sure what to say here
            case Condition.ROUTER_SOFTWARE_FAULT:
                baseMessage = getRouterFaultString(params);
                if (baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage = "Your router may be in a bad state, try rebooting it and it should put " +
                            "the router in a good state so you can connect to it.  ";                }
                break;
            case Condition.ISP_INTERNET_DOWN:
                baseMessage =  "Looks like your Internet service is down. ";
                if (!params.bandwidthGrade.isp.equals(Utils.EMPTY_STRING)) {
                    conditionalMessage = "Contacting " + params.bandwidthGrade.isp + " might help resolve the problem";
                }
                break;
            case Condition.ISP_INTERNET_SLOW:
                baseMessage = "Looks like your Internet service is slow. ";
                if (!params.bandwidthGrade.isp.equals(Utils.EMPTY_STRING)) {
                    conditionalMessage = "Contacting " + params.bandwidthGrade.isp + " might help resolve the problem";
                }
                break;
            case Condition.ISP_INTERNET_SLOW_DOWNLOAD:
                baseMessage = "Looks like your Internet download speed is slow. ";
                if (!params.bandwidthGrade.isp.equals(Utils.EMPTY_STRING)) {
                    conditionalMessage = "Contacting " + params.bandwidthGrade.isp + " might help resolve the problem";
                }
                break;
            case Condition.ISP_INTERNET_SLOW_UPLOAD:
                baseMessage = "Looks like your Internet upload speed is slow. ";
                if (!params.bandwidthGrade.isp.equals(Utils.EMPTY_STRING)) {
                    conditionalMessage = "Contacting " + params.bandwidthGrade.isp + " might help resolve the problem";
                }
                break;
            case Condition.DNS_RESPONSE_SLOW:
                baseMessage = "Your current DNS server is acting slow to respond to queries. ";
                conditionalMessage = getAlternateDNSMessage(params);
                break;
            case Condition.DNS_SLOW_TO_REACH:
                baseMessage = getMessageAboutWifiLink(params);
                if (!baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage += " However, your ";
                } else {
                    baseMessage = "Your ";
                }
                baseMessage += "current DNS server has a high latency, " +
                        "which can cause an initial lag during the load time of an app or a " +
                        "webpage. ";
                conditionalMessage = getAlternateDNSMessage(params);
                break;
            case Condition.DNS_UNREACHABLE:
                baseMessage = getMessageAboutWifiLink(params);
                if (!baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage += "However, we ";
                } else {
                    baseMessage += "We ";
                }
                baseMessage += "are unable to reach your DNS server, which means you can't access " +
                        "the Internet on your phone and other devices.";
                conditionalMessage = getAlternateDNSMessage(params);
                break;
            case Condition.CABLE_MODEM_FAULT:
                baseMessage = "We think your cable modem might be faulty. You should try resetting " +
                        "it to see if improves the performance. ";
                break;
            case Condition.CAPTIVE_PORTAL_NO_INTERNET:
                baseMessage = "You are behind a captive portal -- " +
                        "basically the wifi you are connected to " +
                        params.wifiGrade.getPrimaryApSsid() + " is managed by someone who " +
                        "restricts access unless you sign in.";
                break;
            case Condition.REMOTE_SERVER_IS_SLOW_TO_RESPOND:
                baseMessage = "Currently we believe that external servers that host different " +
                        "Internet services are bit slow to respond to our tests.";
                break;
        }
        suggestionToReturn.append(baseMessage);
        suggestionToReturn.append(conditionalMessage);
        return suggestionToReturn.toString();
    }

    public static String getShortSuggestionForCondition(@InferenceMap.Condition int condition,
                                                        SuggestionCreatorParams params) {
        StringBuilder suggestionToReturn = new StringBuilder();
        String baseMessage = Utils.EMPTY_STRING;
        String conditionalMessage = Utils.EMPTY_STRING;
        switch (condition) {
            case Condition.WIFI_CHANNEL_CONGESTION:
                baseMessage = "Your wifi is operating on a congested channel";
                break;
            case Condition.WIFI_CHANNEL_BAD_SIGNAL:
                baseMessage = "Your signal to your wireless router is weak (about "
                        + convertSignalDbmToPercent(params.wifiGrade.getPrimaryApSignal()) + " %) ";
                break;
            case Condition.WIFI_INTERFACE_ON_PHONE_OFFLINE:
                break; //find a good explanation
            case Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE:
                baseMessage = "Your wifi on your phone seems to be in a bad state, " +
                        "try turning it off/on and see if that helps. ";
                break;
            case Condition.WIFI_INTERFACE_ON_PHONE_TURNED_OFF:
                baseMessage = "Wifi on your phone is turned off, try turning it on and running " +
                        "the tests again.";
                break;
            case Condition.WIFI_LINK_DHCP_ISSUE:
            case Condition.WIFI_LINK_ASSOCIATION_ISSUE:
                baseMessage = "Your phone is unable to get on your wifi network. Try turning your " +
                        "phone's wifi off/on and if it still doesn't connect, then reboot your router.";
                break;
            case Condition.WIFI_LINK_AUTH_ISSUE:
                baseMessage =  "Your phone is unable to get on your wifi network due to some authentication " +
                        "issue. Make sure you have the right password. If the problem still persists, try rebooting " +
                        "the router and it might help.";
                break;
            case Condition.ROUTER_GOOD_SIGNAL_USING_SLOW_DATA_RATE:
                baseMessage =  "Even though your signal to wifi router is strong, it is using low " +
                        "speeds for transferring data. Make sure Mixed mode or 802.11n mode is On if " +
                        "your router supports it";
                break;
            case Condition.ROUTER_SOFTWARE_FAULT:
                baseMessage = getRouterFaultString(params);
                if (baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage = "Your router may be in a bad state, try rebooting it and it should put " +
                            "the router in a good state so you can connect to it.  ";                }
                break;
            case Condition.ISP_INTERNET_DOWN:
                baseMessage =  "Looks like your Internet service is down. " +
                        "We are unable to reach external servers for any bandwidth testing as well.";
                if (!params.bandwidthGrade.isp.equals(Utils.EMPTY_STRING)) {
                    conditionalMessage = "You should contact " + params.bandwidthGrade.isp + " to see if they know about any issues " +
                            "in your area. ";
                }
                break;
            case Condition.ISP_INTERNET_SLOW:
                baseMessage = "Looks like your Internet service could be slow at the moment. " +
                        "You are getting about " +
                        String.format("%.2f", params.bandwidthGrade.getDownloadMbps()) +
                        " Mbps download and " +
                        String.format("%.2f", params.bandwidthGrade.getUploadMbps()) +
                        " Mbps upload";
                break;
            case Condition.ISP_INTERNET_SLOW_DOWNLOAD:
                baseMessage = "Looks like your Internet download speed is slow " +
                        "(around " +
                        String.format("%.2f", params.bandwidthGrade.getDownloadMbps()) +
                        " Mbps), especially given you are getting a good " +
                        "upload speed ( " +
                        String.format("%.2f", params.bandwidthGrade.getUploadMbps()) + " Mbps).";
                break;
            case Condition.ISP_INTERNET_SLOW_UPLOAD:
                baseMessage = "Looks like your Internet upload speed is slow " +
                        "(around " +
                        String.format("%.2f", params.bandwidthGrade.getUploadMbps()) +
                        " Mbps), especially given you are getting a good " +
                        "download speed (~ " +
                        String.format("%.2f", params.bandwidthGrade.getDownloadMbps()) +
                        "  Mbps).";
                break;
            case Condition.DNS_RESPONSE_SLOW:
                baseMessage = getMessageAboutWifiLink(params);
                if (!baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage += " However, your ";
                } else {
                    baseMessage = "Your ";
                }
                baseMessage += "current DNS server is acting slow to respond to queries. ";
                conditionalMessage = getAlternateDNSMessage(params);
                break;
            case Condition.DNS_SLOW_TO_REACH:
                if (!baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage += " However, your ";
                } else {
                    baseMessage = "Your ";
                }
                baseMessage += "current DNS server has a high latency, " +
                        "which can cause an initial lag during the load time of an app or a " +
                        "webpage.";
                break;
            case Condition.DNS_UNREACHABLE:
                if (!baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage += " However, we ";
                } else {
                    baseMessage = "We ";
                }
                baseMessage += "are unable to reach your DNS server, which means you can't access " +
                        "the Internet on your phone and other devices.";
                conditionalMessage = getAlternateDNSMessage(params);
                break;
            case Condition.CABLE_MODEM_FAULT:
                baseMessage = "We think your cable modem might be faulty. You should try resetting " +
                        "it to see if improves the performance. ";
                break;
            case Condition.CAPTIVE_PORTAL_NO_INTERNET:
                if (!baseMessage.equals(Utils.EMPTY_STRING)) {
                    baseMessage += " However, you ";
                } else {
                    baseMessage = "You ";
                }
                baseMessage += "are behind a captive portal -- " +
                        "basically the wifi you are connected to " + params.wifiGrade.getPrimaryApSsid() + " is managed " +
                        "by someone who restricts access unless you sign in.";
                break;
            case Condition.REMOTE_SERVER_IS_SLOW_TO_RESPOND:
                baseMessage = "Currently we believe that external servers that host different " +
                        "Internet services are bit slow to respond to our tests.";
                break;
        }
        suggestionToReturn.append(baseMessage);
        suggestionToReturn.append(conditionalMessage);
        return suggestionToReturn.toString();
    }

}
