package com.inceptai.dobby.ui;

import android.app.Dialog;
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
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.R;
import com.inceptai.dobby.database.FeedbackDatabaseWriter;
import com.inceptai.dobby.database.FeedbackRecord;

import java.util.ArrayList;

import javax.inject.Inject;


public class WifiDocDialogFragment extends DialogFragment {
    public static final int DIALOG_SHOW_SUGGESTIONS = 1001;
    public static final int DIALOG_SHOW_FEEDBACK_FORM = 1002;
    public static final int DIALOG_SHOW_ABOUT_AND_PRIVACY_POLICY = 1003;

    public static final String DIALOG_PAYLOAD = "payload";
    public static final String DIALOG_SUGGESTION_TILTE = "suggestionTitle";
    public static final String DIALOG_SUGGESTION_LIST = "suggestionList";
    public static final String DIALOG_TYPE = "type";

    private static final String[] PRIVACY_POLICY_LIST = {
            "In addition, the Application may collect certain information automatically, including," +
                    "but not limited to, the type of mobile device you use, your mobile devices unique device ID," +
                    "the IP address of your mobile device, your mobile operating system, the type of mobile Internet" +
                    "browsers you use, and information about the way you use the Application.",
            "This Application does not collect precise information about the location of your mobile device. ",
            "If you have any questions regarding privacy while using the Application, or have questions about" +
                    "our practices, please contact us via email at hello@obiai.tech.",
            "By using the Application, you are consenting to our processing of your information as" +
                    "set forth in this Privacy Policy now and as amended by us. \"Processing,” means" +
                    "using cookies on a computer/hand held device or using or touching information in any way, " +
                    " including, but not limited to, collecting, storing, deleting, using, combining and disclosing " +
                    "information, all of which activities will take place in the United States. If you reside outside " +
                    "the United States your information will be transferred, processed and stored there under United States privacy standards. "
    };

    private static final String PRIVACY_POLICY = "Please click <a href=\"http://inceptai.com/privacy.html\"> here </a> to read about our privacy policy.";
    private View rootView;
    private String suggestionTitle;
    private ArrayList<String> suggestionList;

    @Inject
    FeedbackDatabaseWriter feedbackDatabaseWriter;

    public WifiDocDialogFragment() {
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
                return createFeedbackFormDialog();
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

    public static WifiDocDialogFragment forFeedback() {
        WifiDocDialogFragment fragment = new WifiDocDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_TYPE, DIALOG_SHOW_FEEDBACK_FORM);
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

//    private Dialog createPrivacyPolicyDialog(Bundle bundle) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        LayoutInflater inflater = getActivity().getLayoutInflater();
//        rootView = inflater.inflate(R.layout.privacy_policy_dialog_fragment, null);
//        ListView listView = (ListView) rootView.findViewById(R.id.privacy_policy_listview);
//        ArrayAdapter<String> itemsAdapter =
//                new ArrayAdapter<String>(getContext(), R.layout.custom_simple_list_item,
//                        new ArrayList<String>(Arrays.asList(PRIVACY_POLICY_LIST)));
//        listView.setAdapter(itemsAdapter);
//        Button dismissButton = (Button) rootView.findViewById(R.id.privacy_policy_dismiss_button);
//        dismissButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dismiss();
//            }
//        });
//        builder.setView(rootView);
//        return builder.create();
//    }

    @NonNull
    private Dialog createPrivacyPolicyDialog(Bundle bundle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.privacy_policy_dialog_fragment, null);
        TextView aboutTv = (TextView) rootView.findViewById(R.id.about_tv);
        TextView privacyTv = (TextView) rootView. findViewById(R.id.privacy_tv);
        makeHtmlFriendly(privacyTv, PRIVACY_POLICY);
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

    private Dialog createFeedbackFormDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.feedback_dialog_fragment, null);
        Button submitButton = (Button) rootView.findViewById(R.id.feedback_submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FrameLayout fl = (FrameLayout) getActivity().findViewById(R.id.wifi_doc_placeholder_fl);
                //Write the feedback to database
                FeedbackRecord feedbackRecord = createFeedbackRecord(rootView);
                feedbackDatabaseWriter.writeFeedbackToDatabase(feedbackRecord);
                Snackbar.make(fl, "Feedback submitted. Thanks for your comments !", Snackbar.LENGTH_SHORT).show();
                dismiss();
            }
        });
        Button cancelButton = (Button) rootView.findViewById(R.id.feedback_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FrameLayout fl = (FrameLayout) getActivity().findViewById(R.id.wifi_doc_placeholder_fl);
                Snackbar.make(fl, "Feedback cancelled.", Snackbar.LENGTH_SHORT).show();
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
