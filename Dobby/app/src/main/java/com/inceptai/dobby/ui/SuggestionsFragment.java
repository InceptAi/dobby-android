package com.inceptai.dobby.ui;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.suggest.LocalSnippet;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class SuggestionsFragment extends Fragment {
    public static final String TAG = "SuggestionsFragment";
    private static final String ARG_PARAM1 = "param1";
    private LocalSnippet localSnippet;
    private ScrollView scrollView;
    private CardView bandwidthCardview;

    private TextView overallBwTv;
    private TextView uploadTv;
    private TextView downloadTv;
    private Toolbar toolbar;

    private FrameLayout shareResultsFl;
    private FrameLayout contactExpertFl;

    public SuggestionsFragment() {
        // Required empty public constructor
    }

    public void setSuggestions(LocalSnippet localSnippet) {
        this.localSnippet = localSnippet;
    }

    public static SuggestionsFragment newInstance(String param1) {
        SuggestionsFragment fragment = new SuggestionsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_suggestions_2, container, false);
        scrollView = (ScrollView) view.findViewById(R.id.suggestions_scroll_view);
        toolbar = (Toolbar) view.findViewById(R.id.suggestions_toolbar);

        shareResultsFl = (FrameLayout) view.findViewById(R.id.share_results_fl);
        shareResultsFl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareResults();
            }
        });

        contactExpertFl = (FrameLayout) view.findViewById(R.id.contact_expert_fl);
        contactExpertFl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contactExpertUi();
            }
        });

        if (localSnippet != null) {
            fillSuggestions(inflater);
        }
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (toolbar != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.suggestion_fragment_title);
        } else {
            Toast.makeText(getContext(), "Null toolbar", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchViewInstances(View cardView) {
        overallBwTv = (TextView) cardView.findViewById(R.id.overall_bw_tv);
        uploadTv = (TextView) cardView.findViewById(R.id.upload_bw_tv);
        downloadTv = (TextView) cardView.findViewById(R.id.download_bw_tv);
    }

    private void fillSuggestions(LayoutInflater inflater) {
        bandwidthCardview = (CardView) inflater.inflate(R.layout.bandwidth_suggestions_card, null);
        scrollView.addView(bandwidthCardview);
        fetchViewInstances(bandwidthCardview);

        ArrayList<Pair<String, String>> stringList = localSnippet.getStrings();
        DobbyLog.i("StringList size = " + stringList.size());
        if (stringList.size() >= 1) {
            overallBwTv.setText(stringList.get(0).first);
        }
        if (stringList.size() >= 2) {
            uploadTv.setText(stringList.get(1).first);
        }
        if (stringList.size() >= 3) {
            downloadTv.setText(stringList.get(1).first);
        }
    }

    private void contactExpertUi() {
        Snackbar.make(getView(), "Not implemented yet.", Snackbar.LENGTH_SHORT).show();
    }

    private void shareResults() {
        Intent shareIntent = ShareCompat.IntentBuilder.from(getActivity())
                .setType("text/html")
                .setHtmlText("YOUR RESULTS LOOK GOOD.")
                .setSubject("Definitely read this")
                .getIntent();
        startActivity(shareIntent);
    }
}
