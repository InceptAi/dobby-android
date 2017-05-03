package com.inceptai.dobby.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.SuggestionCreator;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;


public class WifiDocDialogFragment extends DialogFragment {
    public static final int DIALOG_SHOW_SUGGESTIONS = 1001;
    public static final int DIALOG_SHOW_FEEDBACK_FORM = 1002;
    public static final int DIALOG_SHOW_PRIVACY_POLICY = 1003;

    public static final String DIALOG_PAYLOAD = "payload";
    public static final String DIALOG_SUGGESTION_TILTE = "suggestionTitle";
    public static final String DIALOG_SUGGESTION_LIST = "suggestionList";
    public static final String DIALOG_TYPE = "type";

    private View rootView;
    private String suggestionTitle;
    private ArrayList<String> suggestionList;

    public WifiDocDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        int dialogType = bundle.getInt(DIALOG_TYPE);
        switch (dialogType) {
            case DIALOG_SHOW_SUGGESTIONS:
                return createSuggestionsDialog(bundle);
            case DIALOG_SHOW_PRIVACY_POLICY:
                break;
            case DIALOG_SHOW_FEEDBACK_FORM:
                break;
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

    private Dialog createSuggestionsDialog(Bundle bundle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.more_suggestions_dialog_fragment, null);
        ListView listView = (ListView) rootView.findViewById(R.id.more_suggest_listview);
        suggestionList = bundle.getStringArrayList(DIALOG_SUGGESTION_LIST);
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, suggestionList);
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

    private Dialog createFeedbackFormDialog() {
        return null;
    }

    private Dialog createPrivacyPolicyDialog() {
        return null;
    }
}
