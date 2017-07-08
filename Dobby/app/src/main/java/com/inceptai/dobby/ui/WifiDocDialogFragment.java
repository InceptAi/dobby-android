package com.inceptai.dobby.ui;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.inceptai.dobby.BuildConfig;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.MainActivity;
import com.inceptai.dobby.R;
import com.inceptai.dobby.analytics.DobbyAnalytics;
import com.inceptai.dobby.database.FeedbackDatabaseWriter;
import com.inceptai.dobby.database.FeedbackRecord;

import java.util.ArrayList;

import javax.inject.Inject;

import static com.inceptai.dobby.utils.Utils.WIFIDOC_FLAVOR;


public class WifiDocDialogFragment extends DialogFragment {
    public static final int DIALOG_SHOW_SUGGESTIONS = 1001;
    public static final int DIALOG_SHOW_FEEDBACK_FORM = 1002;
    public static final int DIALOG_SHOW_ABOUT_AND_PRIVACY_POLICY = 1003;
    public static final int DIALOG_EXPERT_ONBOARDING = 1004;
    public static final int DIALOG_SHOW_SIMPLE_FEEDBACK = 1005;
    public static final int DIALOG_SHOW_LOCATION_PERMISSION_REQUEST = 1006;

    public static final String DIALOG_PAYLOAD = "payload";
    public static final String DIALOG_SUGGESTION_TILTE = "suggestionTitle";
    public static final String DIALOG_SUGGESTION_LIST = "suggestionList";
    public static final String USER_UUID = "userUuid";
    public static final String DIALOG_TYPE = "type";
    public static final String APP_NAME = "appName";
    public static final String APP_ICON = "appIcon";
    public static final String PARENT_VIEW_ID = "parentViewId";
    public static final String VERSION_TEXT = "App Version: ";

    private static final String PRIVACY_POLICY = "Please click <a href=\"http://inceptai.com/privacy/\"> here </a> to read about our privacy policy.";
    private static final String ABOUT_STRING = "This app is offered by InceptAI. Copyright &#169; 2017. For detailed feedback or questions, email us at <a href=\"mailto:hello@obiai.tech\">hello@obiai.tech</a>.";
    private View rootView;
    private String suggestionTitle;
    private ArrayList<String> suggestionList;

    @Inject
    FeedbackDatabaseWriter feedbackDatabaseWriter;
    @Inject
    DobbyAnalytics dobbyAnalytics;

    private WifiDocMainFragment wifiDocMainFragment;
    private MainActivity mainActivity;

    public WifiDocDialogFragment() {
    }

    public void setWifiDocMainFragment(WifiDocMainFragment mainFragment) {
        this.wifiDocMainFragment = mainFragment;
    }


    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ((DobbyApplication) getActivity().getApplication()).getProdComponent().inject(this);
        Bundle bundle = getArguments();
        int dialogType = bundle.getInt(DIALOG_TYPE);
        switch (dialogType) {
            case DIALOG_SHOW_SUGGESTIONS:
                return createSuggestionsDialog(bundle);
            case DIALOG_SHOW_ABOUT_AND_PRIVACY_POLICY:
                return createPrivacyPolicyDialog(bundle);
            case DIALOG_SHOW_FEEDBACK_FORM:
                return createFeedbackFormDialogNoToast(bundle);
            case DIALOG_EXPERT_ONBOARDING:
                return createExpertOnboardingDialog(bundle);
            case DIALOG_SHOW_SIMPLE_FEEDBACK:
                return createSimpleFeedbackForm(bundle);
            case DIALOG_SHOW_LOCATION_PERMISSION_REQUEST:
                return createLocationPermissionRequestDialog(bundle);
        }
        return new AlertDialog.Builder(getActivity()).create();
    }

    public static WifiDocDialogFragment forSuggestion(String title, ArrayList<String> suggestionList) {
        WifiDocDialogFragment fragment = new WifiDocDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_SHOW_SUGGESTIONS);
        bundle.putString(DIALOG_SUGGESTION_TILTE, title);
        bundle.putStringArrayList(DIALOG_SUGGESTION_LIST, suggestionList);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static WifiDocDialogFragment forAboutAndPrivacyPolicy() {
        WifiDocDialogFragment fragment = new WifiDocDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_SHOW_ABOUT_AND_PRIVACY_POLICY);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static WifiDocDialogFragment forExpertOnBoarding() {
        WifiDocDialogFragment fragment = new WifiDocDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_EXPERT_ONBOARDING);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static WifiDocDialogFragment forFeedback(int parentViewId) {
        WifiDocDialogFragment fragment = new WifiDocDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_SHOW_FEEDBACK_FORM);
        bundle.putInt(PARENT_VIEW_ID, parentViewId);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static WifiDocDialogFragment forSimpleFeedback(String userUuid) {
        WifiDocDialogFragment fragment = new WifiDocDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_SHOW_SIMPLE_FEEDBACK);
        bundle.putString(USER_UUID, userUuid);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static WifiDocDialogFragment forLocationPermission(WifiDocMainFragment mainFragment)  {
        WifiDocDialogFragment fragment = new WifiDocDialogFragment();
        fragment.setWifiDocMainFragment(mainFragment);
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_SHOW_LOCATION_PERMISSION_REQUEST);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static WifiDocDialogFragment forDobbyLocationPermission(MainActivity mainActivity)  {
        WifiDocDialogFragment fragment = new WifiDocDialogFragment();
        fragment.setMainActivity(mainActivity);
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_SHOW_LOCATION_PERMISSION_REQUEST);
        fragment.setArguments(bundle);
        return fragment;
    }

    private Dialog createSuggestionsDialog(Bundle bundle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.more_suggestions_dialog_fragment, null);
        ListView listView = (ListView) rootView.findViewById(R.id.more_suggest_listview);
        suggestionList = bundle.getStringArrayList(DIALOG_SUGGESTION_LIST);
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(getContext(), R.layout.custom_simple_list_item, suggestionList);
        listView.setAdapter(itemsAdapter);
        Button dismissButton = (Button) rootView.findViewById(R.id.more_suggestions_dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        builder.setView(rootView);
        return builder.create();
    }

    private Dialog createExpertOnboardingDialog(Bundle bundle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.expert_onboarding_dialog_fragment, null);
        FrameLayout continueFl = (FrameLayout) rootView.findViewById(R.id.bottom_continue_fl);
        continueFl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        builder.setView(rootView);
        return builder.create();
    }

    private Dialog createSimpleFeedbackForm(Bundle bundle) {
        final String userUuid = bundle.getString(USER_UUID);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.simple_feedback_dialog_fragment, null);
        ImageView yesIv = (ImageView) rootView.findViewById(R.id.yes_feedback_iv);
        ImageView noIv = (ImageView) rootView.findViewById(R.id.no_feedback_iv);
        yesIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedbackDatabaseWriter.writeSimpleFeedbackAsync(userUuid, true);
                View rootView = getActivity().findViewById(android.R.id.content);
                if (rootView != null) {
                    Snackbar.make(rootView, "Thanks for your feedback !", Snackbar.LENGTH_SHORT).show();
                }
                dobbyAnalytics.wifiTesterSimpleFeedbackPositive();
                dismiss();
            }
        });

        noIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedbackDatabaseWriter.writeSimpleFeedbackAsync(userUuid, false);
                View rootView = getActivity().findViewById(android.R.id.content);
                if (rootView != null) {
                    Snackbar.make(rootView, "Thanks for your feedback !", Snackbar.LENGTH_SHORT).show();
                }
                dobbyAnalytics.setWifiTesterSimpleFeedbackNegative();
                dismiss();
            }
        });
        builder.setView(rootView);
        dobbyAnalytics.wifiTesterSimpleFeedbackShown();
        return builder.create();
    }

    private Dialog createLocationPermissionRequestDialog(Bundle bundle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        if (BuildConfig.FLAVOR.equals(WIFIDOC_FLAVOR)) {
            rootView = inflater.inflate(R.layout.location_permission_dialog_fragment, null);
        } else {
            rootView = inflater.inflate(R.layout.location_permission_dialog_fragment_dobby, null);
        }
        FrameLayout nextFl = (FrameLayout) rootView.findViewById(R.id.bottom_next_fl);
        nextFl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BuildConfig.FLAVOR.equals(WIFIDOC_FLAVOR)) {
                    if (wifiDocMainFragment != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        wifiDocMainFragment.requestLocationPermission();
                    }
                } else {
                    if (mainActivity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mainActivity.requestLocationPermission();
                    }
                }
                dismiss();
            }
        });
        builder.setView(rootView);
        return builder.create();
    }

    @NonNull
    private Dialog createPrivacyPolicyDialog(Bundle bundle) {
        //String appName = bundle.getString(APP_NAME);
        //String appIcon = bundle.getString(APP_ICON);
        String versionString = "App Version:" + BuildConfig.VERSION_NAME;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.privacy_policy_dialog_fragment, null);
        TextView aboutTv = (TextView) rootView.findViewById(R.id.about_tv);
        TextView privacyTv = (TextView) rootView. findViewById(R.id.privacy_tv);
        TextView versionTv = (TextView) rootView.findViewById(R.id.version_tv);
        versionTv.setText(versionString);
        makeHtmlFriendly(privacyTv, PRIVACY_POLICY);
        makeHtmlFriendly(aboutTv, ABOUT_STRING);
        Button dismissButton = (Button) rootView.findViewById(R.id.privacy_policy_dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        builder.setView(rootView);
        return builder.create();
    }

    private void makeHtmlFriendly(TextView tv, String text) {
        tv.setText(Html.fromHtml(text));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private Dialog createFeedbackFormDialog(Bundle bundle) {
        final int parentViewId = bundle.getInt(PARENT_VIEW_ID);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.feedback_dialog_fragment, null);
        Button submitButton = (Button) rootView.findViewById(R.id.feedback_submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //FrameLayout fl = (FrameLayout) getActivity().findViewById(R.id.wifi_doc_placeholder_fl);
                FrameLayout fl = (FrameLayout) getActivity().findViewById(parentViewId);
                Snackbar.make(fl, "Thanks for your feedback !", Snackbar.LENGTH_SHORT).show();
                //Write the feedback to database
                FeedbackRecord feedbackRecord = createFeedbackRecord(rootView);
                feedbackDatabaseWriter.writeFeedbackToDatabase(feedbackRecord);
                dismiss();
            }
        });
        Button cancelButton = (Button) rootView.findViewById(R.id.feedback_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //FrameLayout fl = (FrameLayout) getActivity().findViewById(R.id.wifi_doc_placeholder_fl);
                FrameLayout fl = (FrameLayout) getActivity().findViewById(parentViewId);
                Snackbar.make(fl, "Feedback cancelled.", Snackbar.LENGTH_SHORT).show();
                dismiss();
            }
        });
        builder.setView(rootView);
        return builder.create();
    }

    private Dialog createFeedbackFormDialogNoToast(Bundle bundle) {
        final int parentViewId = bundle.getInt(PARENT_VIEW_ID);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.feedback_dialog_fragment, null);
        Button submitButton = (Button) rootView.findViewById(R.id.feedback_submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Write the feedback to database
                FeedbackRecord feedbackRecord = createFeedbackRecord(rootView);
                feedbackDatabaseWriter.writeFeedbackToDatabase(feedbackRecord);
                dismiss();
            }
        });
        Button cancelButton = (Button) rootView.findViewById(R.id.feedback_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        builder.setView(rootView);
        return builder.create();
    }

    private FeedbackRecord createFeedbackRecord(View rootView) {
        FeedbackRecord feedbackRecord = new FeedbackRecord(((DobbyApplication) getActivity().getApplication()).getUserUuid());
        RadioGroup helpfulRg = (RadioGroup) rootView.findViewById(R.id.helpful_rb);
        int id = helpfulRg.getCheckedRadioButtonId();
        switch (id) {
            case R.id.radio_helpful_yes:
                feedbackRecord.setHelpfulScore(FeedbackRecord.HelpfulScore.HELPFUL);
                break;
            case R.id.radio_helpful_maybe:
                feedbackRecord.setHelpfulScore(FeedbackRecord.HelpfulScore.MAYBE);
                break;
            case R.id.radio_helpful_no:
                feedbackRecord.setHelpfulScore(FeedbackRecord.HelpfulScore.NOT_HELPFUL);
                break;
            default:
                feedbackRecord.setHelpfulScore(FeedbackRecord.HelpfulScore.UNKNOWN);
        }
        RadioGroup recommendRg = (RadioGroup) rootView.findViewById(R.id.recommend_rg);
        id = recommendRg.getCheckedRadioButtonId();
        switch (id) {
            case R.id.radio_recommend_yes:
                feedbackRecord.setPromotionScore(FeedbackRecord.PromotionScore.YES);
                break;
            case R.id.radio_recommend_maybe:
                feedbackRecord.setPromotionScore(FeedbackRecord.PromotionScore.MAYBE);
                break;
            case R.id.radio_recommend_no:
                feedbackRecord.setPromotionScore(FeedbackRecord.PromotionScore.NO);
                break;
            default:
                feedbackRecord.setPromotionScore(FeedbackRecord.PromotionScore.UNKNOWN);
        }
        EditText commentsEt = (EditText) rootView.findViewById(R.id.detailed_comment_edittext);
        String comments = commentsEt.getText().toString();
        if (comments != null && !comments.isEmpty()) {
            feedbackRecord.setUserFeedback(comments);
        }
        EditText userEmail = (EditText) rootView.findViewById(R.id.email_et);
        String emailAddress = userEmail.getText().toString();
        if (!emailAddress.isEmpty()) {
            feedbackRecord.setEmailAddress(emailAddress);
        }
        return feedbackRecord;
    }
}
