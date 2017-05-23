package com.inceptai.dobby.ui;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.suggest.LocalSnippet;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.ArrayList;

public class SuggestionActivity extends AppCompatActivity {

    private LocalSnippet localSnippet;
    private ScrollView scrollView;
    private CardView bandwidthCardview;

    private TextView overallBwTv;
    private TextView uploadTv;
    private TextView downloadTv;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestion);
        // Find the toolbar view inside the activity layout
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        scrollView = (ScrollView) findViewById(R.id.suggestions_scroll_view);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (localSnippet != null) {
            fillSuggestions(getLayoutInflater());
        }
    }

    public void setSuggestions(LocalSnippet localSnippet) {
        this.localSnippet = localSnippet;
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
}
