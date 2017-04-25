package com.inceptai.dobby.ai;

import com.inceptai.dobby.ai.InferenceMap.Condition;
import com.inceptai.dobby.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static com.inceptai.dobby.utils.Utils.convertSignalDbmToPercent;

/**
 * Created by vivek on 4/19/17.
 */

public class SuggestionCreator {
    static final String MULTIPLE_CONDITIONS_PREFIX = "There a few things which can be causing problems for your network.";
    static final String NO_CONDITION_MESSAGE = "We performed speed tests, DNS pings and wifi tests on your network and did not see anything amiss.";

    public static class Suggestion {
        String title;
        List<String> longSuggestionList;
        List<String> shortSuggestionList;

        Suggestion() {
            title = Utils.EMPTY_STRING;
            longSuggestionList = new ArrayList<>();
            shortSuggestionList = new ArrayList<>();
        }

        public String getTitle() {
            return title;
        }

        public List<String> getLongSuggestionList() {
            return longSuggestionList;
        }

        public List<String> getShortSuggestionList() {
            return shortSuggestionList;
        }

        private String convertListToStringMessage(boolean useLongSuggestions) {
            //Use short suggestion by default.
            List<String> suggestionList;
            if (useLongSuggestions) {
                suggestionList = longSuggestionList;
            } else {
                suggestionList = shortSuggestionList;
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getTitle());
            sb.append("\n");
            //Use short suggestion by default.
            sb.append(convertListToStringMessage(false));
            return sb.toString();
        }
    }

    public static class SuggestionCreatorParams {
        int currentWifiChannel;
        int bestWifiChannel;
        int currentSignal;
        double downloadBandwidthMbps;
        double uploadBandwidthMbps;
        String isp;
        String currentWifiSSID;
        String alternateDNS;

        SuggestionCreatorParams() {
            currentWifiChannel = 0;
            bestWifiChannel = 0;
            currentSignal = 0;
            downloadBandwidthMbps = -1;
            uploadBandwidthMbps = -1;
            isp = Utils.EMPTY_STRING;
            currentWifiSSID = Utils.EMPTY_STRING;
            alternateDNS = Utils.EMPTY_STRING;
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

    static Suggestion getSuggestionForConditions(List<Integer> conditionList,
                                                    SuggestionCreatorParams params,
                                                    boolean getLongSuggestions) {
        Suggestion suggestionToReturn = new Suggestion();
        suggestionToReturn.title = getTitle(conditionList, params);
        List<String> longSuggestionList = new ArrayList<>();
        List<String> shortSuggestionList = new ArrayList<>();
        for (int index=0; index < conditionList.size(); index++) {
            @Condition int condition  = conditionList.get(index);
            longSuggestionList.add(getSuggestionForCondition(condition, params));
            shortSuggestionList.add(getShortSuggestionForCondition(condition, params));
        }
        return suggestionToReturn;
    }

    public static String getSuggestionString(List<Integer> conditionList,
                                              SuggestionCreatorParams params,
                                              boolean getLongSuggestions) {
        final String NO_CONDITION_MESSAGE = "We don't see any key issues with your Wifi network. " +
                "We performed speed tests, pings and wifi scans on your network and didn't find any issues." +
                "You are getting " + String.format("%.2f", params.downloadBandwidthMbps) +
                " Mbps download / " + String.format("%.2f", params.uploadBandwidthMbps) +
                " Mbps upload speed. You connection to your wifi is about " +
                Utils.convertSignalDbmToPercent(params.currentSignal) +
                ". Since wifi network problems are sometimes transient, it might be good if you run " +
                "this test a few times so we can catch an issue if it shows up. Hope this helps :)";

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
            titleToReturn = getShortSuggestionForCondition(condition, params);
        } else {
            titleToReturn = "We don't see any key issues with your Wifi network. " +
                    "We performed speed tests, pings and wifi scans on your network and didn't find any issues." +
                    "You are getting " + String.format("%.2f", params.downloadBandwidthMbps) +
                    " Mbps download / " + String.format("%.2f", params.uploadBandwidthMbps) +
                    " Mbps upload speed. You connection to your wifi is about " + Utils.convertSignalDbmToPercent(params.currentSignal) +
                    ". Since wifi network problems are sometimes transient, it might be good if you run " +
                    "this test a few times so we can catch an issue if it shows up. Hope this helps :)";
        }
        return titleToReturn;
    }

    private static String getSuggestionForCondition(@InferenceMap.Condition int condition,
                                                   SuggestionCreatorParams params) {
        StringBuilder suggestionToReturn = new StringBuilder();
        String baseMessage = Utils.EMPTY_STRING;
        String conditionalMessage = Utils.EMPTY_STRING;
        switch (condition) {
            case Condition.WIFI_CHANNEL_CONGESTION:
                baseMessage = "Your wifi is operating on Channel " +
                        convertChannelFrequencyToString(params.currentWifiChannel) +
                        " which is congested. This means there a lot of other Wifi networks near " +
                        "you which are also operating on the same channel as yours. ";
                if (params.bestWifiChannel > 0) {
                    conditionalMessage = "As per our current analysis, Channel " +
                            convertChannelFrequencyToString(params.bestWifiChannel) +
                            " could provide better results for your network.";
                }
                break;
            case Condition.WIFI_CHANNEL_BAD_SIGNAL:
                baseMessage = "Your signal to your wireless router is very weak (about "
                        + Utils.convertSignalDbmToPercent(params.currentSignal) + " %) " +
                        ", this could lead to poor speeds and bad experience in streaming etc. " +
                        "If you are close to your router while doing this test (within 20ft), then your router is not " +
                        "providing enough signal. Make sure your router is not obstructed and if " +
                        "that doesn't help, you should try replacing the router. If you are " +
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
            case Condition.ROUTER_FAULT_WIFI_OK:
                baseMessage = Utils.EMPTY_STRING;
                return ""; // Not sure what to say here
            case Condition.ROUTER_WIFI_INTERFACE_FAULT:
            case Condition.ROUTER_SOFTWARE_FAULT:
                baseMessage =  "Seems like your router could be in a bad state, since your phone is unable " +
                        "to connect to the wifi. Can you try rebooting the router and hopefully it will connect without problem.";
                break;
            case Condition.ISP_INTERNET_DOWN:
                baseMessage =  "Your wifi is fine but looks like your Internet service is down. " +
                        "We are unable to reach external servers for any bandwidth testing as well.";
                if (!params.isp.equals(Utils.EMPTY_STRING)) {
                    conditionalMessage = "You should contact " + params.isp + " to see if they know about any outage " +
                            "in your area";
                }
                break;
            case Condition.ISP_INTERNET_SLOW:
                baseMessage = "Your wifi is fine but looks like your Internet service is really slow. " +
                        "You are getting about " + String.format("%.2f", params.downloadBandwidthMbps) + " Mbps download and " +
                        String.format("%.2f", params.uploadBandwidthMbps) + " Mbps upload. If these speeds seem low as per your " +
                        "contract, you should reach out to " + params.isp + " and see why you are getting such low speeds. " +
                        "You should tell them that your wifi network latency is low but the " +
                        "latency to access Internet is very high.";
                break;
            case Condition.ISP_INTERNET_SLOW_DOWNLOAD:
                baseMessage = "Your wifi is fine but looks like your Internet download speed is very low " +
                        "(around " + String.format("%.2f", params.downloadBandwidthMbps) + " Mbps), especially given you are getting a good " +
                        "upload speed ( " + String.format("%.2f", params.uploadBandwidthMbps) + " Mbps. Since most of the data " +
                        "consumed by streaming, browsing etc. is download, you will experience slow Internet on your devices. " +
                        "If these speeds seem low as per your contract, you should " +
                        "reach out to " + params.isp + " and see why you are getting such low speeds. " +
                        "You should tell them that your wifi network latency is low but the " +
                        "download speed is very low (esp. compared to upload).";
                break;
            case Condition.ISP_INTERNET_SLOW_UPLOAD:
                baseMessage = "Your wifi is fine but looks like your Internet upload speed is very low " +
                        "(around " + String.format("%.2f", params.uploadBandwidthMbps) + " Mbps), especially given you are getting a good " +
                        "download speed (~ " + String.format("%.2f", params.downloadBandwidthMbps) + "  Mbps). You will have trouble uploading " +
                        "content like posting photos, sending email attachments etc. " +
                        "If these speeds seem low as per your contract, you should " +
                        "reach out to " + params.isp + "  and see why you are getting such low speeds. " +
                        "You should tell them that your wifi network latency is low but the " +
                        "upload speed is very low (esp. compared to download).";
                break;
            case Condition.DNS_RESPONSE_SLOW:
                baseMessage = "Your wifi network is fine but your current DNS server is acting slow to respond to queries, " +
                        "which can cause an initial lag during the load time of an app or a new webpage.";
                if (!params.alternateDNS.equals(Utils.EMPTY_STRING)) {
                    conditionalMessage = "Our tests show that using other public DNSs than the one you " +
                            "are currently configured for can speed up your load times. Try changing " +
                            "your DNS server with " + params.alternateDNS + " and re-run the test. " +
                            "You can change your DNS settings just for your phone first and see if that improves things.";
                }
                break;
            case Condition.DNS_SLOW_TO_REACH:
                baseMessage = "Your wifi network is fine but your current DNS server has a high latency, " +
                        "which can cause an initial lag during the load time of an app or a " +
                        "webpage.";
                break;
            case Condition.DNS_UNREACHABLE:
                baseMessage = "Your wifi network is fine but we are unable to reach your DNS server, which means you can't access " +
                        "the Internet on your phone and other devices. This could be because the DNS " +
                        "server you have configured is down. ";
                if (!params.alternateDNS.equals(Utils.EMPTY_STRING)) {
                    conditionalMessage = "Our tests show that using other public DNSs than the one you " +
                            "are currently configured for can speed up your load times. Try changing " +
                            "your DNS server with " + params.alternateDNS + " and re-run the test. " +
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
                baseMessage = "You are behind a captive portal -- " +
                        "basically the wifi you are connected to " + params.currentWifiSSID + " is managed " +
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


    public static String getShortSuggestionForCondition(@InferenceMap.Condition int condition,
                                                   SuggestionCreatorParams params) {
        StringBuilder suggestionToReturn = new StringBuilder();
        String baseMessage = Utils.EMPTY_STRING;
        String conditionalMessage = Utils.EMPTY_STRING;
        switch (condition) {
            case Condition.WIFI_CHANNEL_CONGESTION:
                baseMessage = "Your wifi is operating on a very congested channel";
                break;
            case Condition.WIFI_CHANNEL_BAD_SIGNAL:
                baseMessage = "Your signal to your wireless router is very weak (about "
                        + convertSignalDbmToPercent(params.currentSignal) + " %) ";
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
            case Condition.ROUTER_FAULT_WIFI_OK:
                return ""; // Not sure what to say here
            case Condition.ROUTER_WIFI_INTERFACE_FAULT:
            case Condition.ROUTER_SOFTWARE_FAULT:
                baseMessage =  "Seems like your router could be in a bad state, since your phone is unable " +
                        "to connect to the wifi. Can you try rebooting the router and hopefully it will connect without problem.";
                break;
            case Condition.ISP_INTERNET_DOWN:
                baseMessage =  "Your wifi is fine but looks like your Internet service is down. " +
                        "We are unable to reach external servers for any bandwidth testing as well.";
                if (!params.isp.equals(Utils.EMPTY_STRING)) {
                    conditionalMessage = "You should contact " + params.isp + " to see if they know about any outage " +
                            "in your area";
                }
                break;
            case Condition.ISP_INTERNET_SLOW:
                baseMessage = "Your wifi is fine but looks like your Internet service is really slow. " +
                        "You are getting about " + String.format("%.2f", params.downloadBandwidthMbps) + " Mbps download and " +
                        String.format("%.2f", params.uploadBandwidthMbps) + " Mbps upload";
                break;
            case Condition.ISP_INTERNET_SLOW_DOWNLOAD:
                baseMessage = "Your wifi is fine but looks like your Internet download speed is very low " +
                        "(around " + String.format("%.2f", params.downloadBandwidthMbps) + " Mbps), especially given you are getting a good " +
                        "upload speed ( " + String.format("%.2f", params.uploadBandwidthMbps) + " Mbps";
                break;
            case Condition.ISP_INTERNET_SLOW_UPLOAD:
                baseMessage = "Your wifi is fine but looks like your Internet upload speed is very low " +
                        "(around " + String.format("%.2f", params.uploadBandwidthMbps) + " Mbps), especially given you are getting a good " +
                        "download speed (~ " + String.format("%.2f", params.downloadBandwidthMbps) + "  Mbps).";
                break;
            case Condition.DNS_RESPONSE_SLOW:
                baseMessage = "Your wifi network is fine but your current DNS server is acting slow to respond to queries. ";
                if (!params.alternateDNS.equals(Utils.EMPTY_STRING)) {
                    conditionalMessage = "Try using " +  params.alternateDNS + " and re-run the test. ";
                }
                break;
            case Condition.DNS_SLOW_TO_REACH:
                baseMessage = "Your wifi network is fine but your current DNS server has a high latency, " +
                        "which can cause an initial lag during the load time of an app or a " +
                        "webpage.";
                break;
            case Condition.DNS_UNREACHABLE:
                baseMessage = "Your wifi network is fine but we are unable to reach your DNS server, which means you can't access " +
                        "the Internet on your phone and other devices.";
                if (!params.alternateDNS.equals(Utils.EMPTY_STRING)) {
                    conditionalMessage = "Try chaing primary DNS to " +  params.alternateDNS +
                            " which is working and re-run the test. ";
                }
                break;
            case Condition.CABLE_MODEM_FAULT:
                baseMessage = "We think your cable modem might be faulty. You should try resetting " +
                        "it to see if improves the performance. ";
                break;
            case Condition.CAPTIVE_PORTAL_NO_INTERNET:
                baseMessage = "You are behind a captive portal -- " +
                        "basically the wifi you are connected to " + params.currentWifiSSID + " is managed " +
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
