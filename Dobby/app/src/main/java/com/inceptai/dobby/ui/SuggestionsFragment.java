package com.inceptai.dobby.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.suggest.LocalSnippet;
import com.inceptai.dobby.utils.DobbyLog;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class SuggestionsFragment extends Fragment {
    public static final String TAG = "SuggestionsFragment";
    private static final String ARG_PARAM1 = "param1";
    private LocalSnippet localSnippet;
    private NestedScrollView nestedScrollView;
    private CardView bandwidthCardview;

    private TextView overallBwTv;
    private TextView uploadTv;
    private TextView downloadTv;


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
        View view = inflater.inflate(R.layout.fragment_suggestions, container, false);
        nestedScrollView = (NestedScrollView) view.findViewById(R.id.suggestions_scroll_view);

        if (localSnippet != null) {
            fillSuggestions(inflater);
        }
        return view;
    }

    private void fetchViewInstances(View cardView) {
        overallBwTv = (TextView) cardView.findViewById(R.id.overall_bw_tv);
        uploadTv = (TextView) cardView.findViewById(R.id.upload_bw_tv);
        downloadTv = (TextView) cardView.findViewById(R.id.download_bw_tv);
    }

    private void fillSuggestions(LayoutInflater inflater) {
        bandwidthCardview = (CardView) inflater.inflate(R.layout.bandwidth_suggestions_card, null);
        nestedScrollView.addView(bandwidthCardview);
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
}
