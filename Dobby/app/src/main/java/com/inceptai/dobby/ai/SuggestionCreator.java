package com.inceptai.dobby.ai;

import com.inceptai.dobby.ai.InferenceMap.Condition;
import com.inceptai.dobby.utils.Utils;

import java.util.ArrayList;
import java.util.List;
/**
 * Created by vivek on 4/19/17.
 */

public class SuggestionCreator {

    private static String NO_CONDITION_MESSAGE = "We don't see any key issues with your Wifi network. " +
            "Since wifi network problems are sometimes transient, it might be good if you run " +
            "this test a few times so we can catch an issue if it shows up. Wish we can be more helpful here.";

    private static String MULTIPLE_CONDITIONS_PREFIX = "There a few things which can be causing problems for your network.";

    public static String getSuggestionForConditions(@InferenceMap.Condition int[] conditionList, int listLength) {
        List<String> suggestionList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int index=0; index < conditionList.length && index < listLength; index++) {
            suggestionList.add(getSuggestionForCondition(conditionList[index]));
        }
        if (suggestionList.size() == 0) {
            sb.append(NO_CONDITION_MESSAGE);
        } else if (suggestionList.size() == 1) {
            sb.append(suggestionList.get(0));
        } else {
            //Multiple conditions
            sb.append(MULTIPLE_CONDITIONS_PREFIX + "\n");
            int index = 1;
            for(String suggestion: suggestionList) {
                sb.append(index + ". " + suggestion + "\n");
            }
        }
        return sb.toString();
    }

    public static String getSuggestionForCondition(@InferenceMap.Condition int condition) {
        switch (condition) {
            case Condition.WIFI_CHANNEL_CONGESTION:
                return "Your wifi is operating on channel {OWN_CHANNEL_PARAM} which is congested. " +
                        "This means there a lot of other Wifi networks near " +
                        "you which are also operating on the same channel as yours. " +
                        "You can mitigate it by changing the channel on which your router is " +
                        "operating. As per my current analysis, Channel {BEST_CHANNEL_PARAM} " +
                        "could provide better results for your network.";
            case Condition.WIFI_CHANNEL_BAD_SIGNAL:
                return "Your signal to your wireless router is very weak, this could lead to poor " +
                        "speeds and bad experience in streaming etc. If you are close to your " +
                        "router while doing this test (within 20ft), then your router is not " +
                        "providing enough signal. Make sure your router is not obstructed and if " +
                        "that doesn't help, you should try replacing the router. If you are " +
                        "actually far from your router during the test, then your router is not " +
                        "strong enough to cover the current testing area and you should look into " +
                        "a stronger router or a mesh Wifi solution which can provide better coverage.";
            case Condition.WIFI_INTERFACE_ON_PHONE_OFFLINE:
                return ""; //find a good explanation
            case Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE:
                return "Your wifi on your phone seems to be in a bad state, " +
                        "since it is not able to connect to your wireless router. " +
                        "Try turning it off/on and see if that helps. ";
            case Condition.WIFI_INTERFACE_ON_PHONE_TURNED_OFF:
                return "Wifi on your phone is turned off, so we cannot assess the " +
                        "performance of your wireless network. Try turning it on and running " +
                        "the tests again.";
            case Condition.WIFI_LINK_DHCP_ISSUE:
            case Condition.WIFI_LINK_ASSOCIATION_ISSUE:
                return "Your phone is unable to get on your wifi network. This could be either " +
                        "due to wifi router or your phone being in a bad state. Try turning your " +
                        "phone's wifi off/on and it doesn't connect, then reboot your router.";
            case Condition.WIFI_LINK_AUTH_ISSUE:
                return "Your phone is unable to get on your wifi network due to some authentication " +
                        "issue. Make sure you have the right password (or update the wifi password " +
                        "if changed). If the problem still persists, then your router could be in a " +
                        "bad state so try rebooting the router and it might help.";
            case Condition.ROUTER_GOOD_SIGNAL_USING_SLOW_DATA_RATE:
                return "Your signal to your wifi router is strong, but it is using a relatively low " +
                        "speed for transferring data, which can cause laggy experience. Make sure " +
                        "Mixed mode or 802.11n mode is On if your router supports it or we would " +
                        "recommend getting a router which supports 802.11n for higher speeds.";
            case Condition.ROUTER_FAULT_WIFI_OK:
                return ""; // Not sure what to say here
            case Condition.ROUTER_WIFI_INTERFACE_FAULT:
            case Condition.ROUTER_SOFTWARE_FAULT:
                return "Seems like your router could be in a bad state, since your phone is unable " +
                        "to connect to the wifi. Can you try rebooting the router and hopefully.";
            case Condition.ISP_INTERNET_SLOW_DNS_OK:
                return "ISP_INTERNET_SLOW_DNS_OK";
            case Condition.ISP_INTERNET_DOWN:
                return "Your wifi is fine but looks like your Internet service is down. " +
                        "We are unable to reach external servers for any bandwidth testing as well." +
                        "You should contact {PARAM_ISP} to see if they know about any outage " +
                        "in your area";
            case Condition.ISP_INTERNET_SLOW:
                return "Your wifi is fine but looks like your Internet service is really slow. " +
                        "You are getting about {PARAM_DOWNLOAD} Mbps download and {PARAM_UPLOAD} " +
                        "Mbps upload. If these speeds seem low as per your contract, you should " +
                        "reach out to {PARAM_ISP} and see why you are getting such low speeds. " +
                        "You should tell them that your wifi network latency is low but the " +
                        "latency to access Internet is very high.";
            case Condition.ISP_INTERNET_SLOW_DOWNLOAD:
                return "Your wifi is fine but looks like your Internet download speed is very low " +
                        "(around {PARAM_DOWNLOAD} Mbps), especially given you are getting a good " +
                        "upload speed. Since most of the data consumed by streaming, browsing etc. " +
                        "is download, you will experience slow Internet on your devices. " +
                        "If these speeds seem low as per your contract, you should " +
                        "reach out to {PARAM_ISP} and see why you are getting such low speeds. " +
                        "You should tell them that your wifi network latency is low but the " +
                        "download speed is very low (esp. compared to upload).";
            case Condition.ISP_INTERNET_SLOW_UPLOAD:
                return "Your wifi is fine but looks like your Internet upload speed is very low " +
                        "(around {PARAM_UPLOAD} Mbps), especially given you are getting a good " +
                        "download speed (~{PARAM_DOWNLOAD} Mbps). You will have trouble uploading " +
                        "content like posting photos, sending email attachments etc. " +
                        "If these speeds seem low as per your contract, you should " +
                        "reach out to {PARAM_ISP} and see why you are getting such low speeds. " +
                        "You should tell them that your wifi network latency is low but the " +
                        "upload speed is very low (esp. compared to download).";
            case Condition.DNS_RESPONSE_SLOW:
                return "Your wifi network is fine but your current DNS server is acting slow to respond to queries, " +
                        "which can cause an initial lag during the load time of an app or a " +
                        "webpage. You can change your DNS server with one of these listed here " +
                        "and re-run the test. You can change your DNS settings in the router " +
                        "settings page or in the app accompanying your wireless router.";
            case Condition.DNS_SLOW_TO_REACH:
                return "Your wifi network is fine but your current DNS server has a high latency, " +
                        "which can cause an initial lag during the load time of an app or a " +
                        "webpage. You can change your DNS server with one of these listed here " +
                        "and re-run the test. You can change your DNS settings in the router " +
                        "settings page or in the app accompanying your wireless router.";
            case Condition.DNS_UNREACHABLE:
                return "Your wifi network is fine but we are unable to reach your DNS server, which means you can't access " +
                        "the Internet on your phone and other devices. This could be because the DNS " +
                        "server you have configured is down. We would recommend changing your DNS " +
                        "server to one of the following listed here and re-run the test to see " +
                        "if you get connectivity restored. You can change your DNS settings in the router " +
                        "settings page or in the app accompanying your wireless router.";
            case Condition.CABLE_MODEM_FAULT:
                return "We think your cable modem might be faulty (It is the device which is " +
                        "provided by the Internet provider and is connected to your wireless router)" +
                        ". You should try resetting it to see if improves the performance. ";
            case Condition.CAPTIVE_PORTAL_NO_INTERNET:
                return "You are behind a captive portal -- " +
                        "basically the wifi you are connected to {WIFI_PARAM} is managed by s" +
                        "omeone who restricts access unless you sign in. Currently you don't have " +
                        "access to it. Try launching a browser and it should redirect you to a " +
                        "login form. Once you are connected, you can re-run this test to see how " +
                        "your network is doing. ";
            case Condition.REMOTE_SERVER_IS_SLOW_TO_RESPOND:
                return "Currently we believe that external servers that host different " +
                        "Internet services are a bit slow to respond to our tests. This could " +
                        "be because of failures or configuration issues in the general routing of " +
                        "packets. You can call your Internet provider to see if they have any " +
                        "information on this. ";
        }

        return Utils.EMPTY_STRING;
    }

    public static void sortConditionInOrderOfRelevance(List<InferenceMap.Condition> conditionList) {
        //Sort the incoming suggestions
    }


}
