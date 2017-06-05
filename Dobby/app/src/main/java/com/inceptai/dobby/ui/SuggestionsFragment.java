package com.inceptai.dobby.ui;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.suggest.LocalSummary;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class SuggestionsFragment extends Fragment {
    public static final String TAG = "SuggestionsFragment";
    private static final String ARG_PARAM1 = "param1";
    private LocalSummary localSummary;
    private CardView bandwidthCardview;
    private CardView overallSummaryCv;
    private CardView suggestionsCv;
    private ListView suggestionsLv;

    private TextView overallBwTv;
    private TextView uploadTv;
    private TextView downloadTv;
    private Toolbar toolbar;
    private Button moreButton;
    private TextView suggestionsTitleTv;

    private FrameLayout shareResultsFl;
    private FrameLayout contactExpertFl;

    public SuggestionsFragment() {
        // Required empty public constructor
    }

    public void setSuggestions(LocalSummary localSummary) {
        this.localSummary = localSummary;
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

        if (localSummary != null) {
            fillSuggestions2(view);
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

    private void fillSuggestions2(View rootView) {
        overallSummaryCv = (CardView) rootView.findViewById(R.id.wd_result_summary_inc_cv);
        fetchOverallSummaryViews(overallSummaryCv);

        overallBwTv.setText(localSummary.getOverall());
        uploadTv.setText(String.format("%2.1f", localSummary.getSummary().getUploadMbps()));
        downloadTv.setText(String.format("%2.1f", localSummary.getSummary().getDownloadMbps()));

        suggestionsCv = (CardView) rootView.findViewById(R.id.wd_suggestions_inc_cv);
        suggestionsLv = (ListView) suggestionsCv.findViewById(R.id.wd_suggest_listview);
        suggestionsTitleTv = (TextView) suggestionsCv.findViewById(R.id.wd_suggest_view_title_tv);

        ArrayList<String> tempList = new ArrayList<>();
        tempList.add("We performed speed tests, DNS pings and wifi tests on your network and did not see anything amiss.");
        tempList.add("Since wifi network problems are sometimes transient, " +
        "it might be good if you run this test a few times so we can catch an issue " +
                "if it shows up. Hope this helps :)");
        tempList.add("Your signal to your wireless router is weak (about "
                +  "78/100) " +
                ", this could lead to poor speeds and bad experience in streaming etc. " +
                "\n a. If you are close to your router while doing this test (within 20ft), then your router is not " +
                "providing enough signal. \n b. Make sure your router is not obstructed and if " +
                "that doesn't help, you should try replacing the router. \n c. If you are " +
                "actually far from your router during the test, then your router is not " +
                "strong enough to cover the current testing area and you should look into " +
                "a stronger router or a mesh Wifi solution which can provide better coverage.");
        fillSuggestions("There a few things which can be causing problems for your network", tempList);
    }

    private void fillSuggestions(String title, List<String> list) {
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(getContext(), R.layout.custom_simple_list_item, list);
        suggestionsLv.setAdapter(itemsAdapter);
        suggestionsTitleTv.setText(title);
        suggestionsCv.requestLayout();
    }

    private void fetchOverallSummaryViews(View rootView) {
        overallBwTv = (TextView) rootView.findViewById(R.id.top_summary_tv);
        uploadTv = (TextView) rootView.findViewById(R.id.upload_bw_tv);
        downloadTv = (TextView) rootView.findViewById(R.id.download_bw_tv);
        moreButton = (Button) rootView.findViewById(R.id.more_button);
    }

    private void contactExpertUi() {
        startActivity(new Intent(getContext(), ExpertChatActivity.class));
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
