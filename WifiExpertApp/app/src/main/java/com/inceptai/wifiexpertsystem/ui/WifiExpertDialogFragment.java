package com.inceptai.wifiexpertsystem.ui;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.inceptai.wifiexpertsystem.BuildConfig;
import com.inceptai.wifiexpertsystem.DobbyActivity;
import com.inceptai.wifiexpertsystem.DobbyApplication;
import com.inceptai.wifiexpertsystem.database.DobbyDatabaseBackend;
import com.inceptai.wifiexpertsystem.R;
import com.inceptai.wifiexpertsystem.analytics.DobbyAnalytics;
import com.inceptai.wifiexpertsystem.database.model.FeedbackRecord;

import java.util.ArrayList;

import javax.inject.Inject;

import static com.inceptai.wifiexpertsystem.utils.Utils.WIFIEXPERT_FLAVOR;


public class WifiExpertDialogFragment extends DialogFragment {
    public static final int DIALOG_SHOW_SUGGESTIONS = 1001;
    public static final int DIALOG_SHOW_FEEDBACK_FORM = 1002;
    public static final int DIALOG_SHOW_ABOUT_AND_PRIVACY_POLICY = 1003;
    public static final int DIALOG_SHOW_LOCATION_PERMISSION_REQUEST = 1004;
    public static final int DIALOG_SHOW_LOCATION_AND_OVERDRAW_PERMISSION_REQUEST = 1005;
    public static final int DIALOG_SHOW_ACCESSIBILITY_PERMISSION_REQUEST = 1006;

    public static final String DIALOG_SUGGESTION_TILTE = "suggestionTitle";
    public static final String DIALOG_SUGGESTION_LIST = "suggestionList";
    public static final String DIALOG_TYPE = "type";
    public static final String PARENT_VIEW_ID = "parentViewId";
    private static final String PRIVACY_POLICY = "Please click <a href=\"http://inceptai.com/privacy/\"> here </a> to read about our privacy policy.";
    private static final String ABOUT_STRING = "This app is offered by InceptAI. Copyright &#169; 2017. For detailed feedback or questions, email us at <a href=\"mailto:hello@obiai.tech\">hello@obiai.tech</a>.";
    private View rootView;
    private String suggestionTitle;
    private ArrayList<String> suggestionList;

    @Inject
    DobbyDatabaseBackend dobbyDatabaseBackend;
    @Inject
    DobbyAnalytics dobbyAnalytics;

    private DobbyActivity dobbyActivity;

    public WifiExpertDialogFragment() {
    }


    public void setDobbyActivity(DobbyActivity dobbyActivity) {
        this.dobbyActivity = dobbyActivity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().setCanceledOnTouchOutside(false);
        return super.onCreateView(inflater, container, savedInstanceState);
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
            case DIALOG_SHOW_LOCATION_PERMISSION_REQUEST:
                return createLocationPermissionRequestDialog(bundle);
            case DIALOG_SHOW_LOCATION_AND_OVERDRAW_PERMISSION_REQUEST:
                return createLocationAndOverdrawPermissionRequestDialog(bundle);
            case DIALOG_SHOW_ACCESSIBILITY_PERMISSION_REQUEST:
                return createAccessibilityPermissionRequestDialog(bundle);
        }
        return new AlertDialog.Builder(getActivity()).create();
    }

    public static WifiExpertDialogFragment forSuggestion(String title, ArrayList<String> suggestionList) {
        WifiExpertDialogFragment fragment = new WifiExpertDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_SHOW_SUGGESTIONS);
        bundle.putString(DIALOG_SUGGESTION_TILTE, title);
        bundle.putStringArrayList(DIALOG_SUGGESTION_LIST, suggestionList);
        fragment.setArguments(bundle);
        return fragment;
    }


    public static WifiExpertDialogFragment forAboutAndPrivacyPolicy() {
        WifiExpertDialogFragment fragment = new WifiExpertDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_SHOW_ABOUT_AND_PRIVACY_POLICY);
        fragment.setArguments(bundle);
        return fragment;
    }


    public static WifiExpertDialogFragment forFeedback(int parentViewId) {
        WifiExpertDialogFragment fragment = new WifiExpertDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_SHOW_FEEDBACK_FORM);
        bundle.putInt(PARENT_VIEW_ID, parentViewId);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static WifiExpertDialogFragment forLocationPermission(DobbyActivity dobbyActivity)  {
        WifiExpertDialogFragment fragment = new WifiExpertDialogFragment();
        fragment.setDobbyActivity(dobbyActivity);
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_SHOW_LOCATION_PERMISSION_REQUEST);
        fragment.setArguments(bundle);
        return fragment;
    }


    public static WifiExpertDialogFragment forLocationAndOverdrawPermission(DobbyActivity dobbyActivity)  {
        WifiExpertDialogFragment fragment = new WifiExpertDialogFragment();
        fragment.setDobbyActivity(dobbyActivity);
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_SHOW_LOCATION_AND_OVERDRAW_PERMISSION_REQUEST);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static WifiExpertDialogFragment forAccessibilityPermission(DobbyActivity dobbyActivity)  {
        WifiExpertDialogFragment fragment = new WifiExpertDialogFragment();
        fragment.setDobbyActivity(dobbyActivity);
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_SHOW_ACCESSIBILITY_PERMISSION_REQUEST);
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

    private Dialog createLocationPermissionRequestDialog(Bundle bundle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.location_permission_dialog_fragment, null);
        FrameLayout nextFl = (FrameLayout) rootView.findViewById(R.id.bottom_next_fl);
        nextFl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dobbyActivity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    dobbyActivity.requestLocationPermission();
                }
                dismiss();
            }
        });
        builder.setView(rootView);
        return builder.create();
    }


    private Dialog createLocationAndOverdrawPermissionRequestDialog(Bundle bundle) {
        //Preconditions.checkArgument(BuildConfig.FLAVOR.equals(WIFIEXPERT_FLAVOR));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.location_and_overdraw_permission_dialog_fragment, null);
        FrameLayout nextFl = (FrameLayout) rootView.findViewById(R.id.bottom_next_fl);
        nextFl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dobbyActivity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    dobbyActivity.requestLocationAndOverdrawPermission();
                }
                dismiss();
            }
        });
        builder.setView(rootView);
        return builder.create();
    }


    private Dialog createAccessibilityPermissionRequestDialog(Bundle bundle) {
        Preconditions.checkArgument(BuildConfig.FLAVOR.equalsIgnoreCase(WIFIEXPERT_FLAVOR));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.accessibility_permission_dialog_fragment, null);
        FrameLayout nextFl = (FrameLayout) rootView.findViewById(R.id.bottom_next_fl);
        nextFl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dobbyActivity != null) {
                    dobbyActivity.takeUserToAccessibilitySetting();
                }
                dismiss();
            }
        });
        builder.setView(rootView);
        return builder.create();
    }

    @NonNull
    private Dialog createPrivacyPolicyDialog(Bundle bundle) {
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
                dobbyDatabaseBackend.writeFeedbackToDatabase(feedbackRecord);
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
