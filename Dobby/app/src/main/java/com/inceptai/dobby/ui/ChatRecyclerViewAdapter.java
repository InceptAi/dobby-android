package com.inceptai.dobby.ui;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.inceptai.dobby.R;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.List;

/**
 * Created by arunesh on 3/28/17.
 */

public class ChatRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatEntry> entryList;
    private Context context;

    public ChatRecyclerViewAdapter(Context context, List<ChatEntry> entryList) {
        this.context = context;
        this.entryList = entryList;
    }

    @Override
    public int getItemViewType(int position) {
        ChatEntry entry = entryList.get(position);
        return entry.getEntryType();
    }

    public void addEntryAtBottom(ChatEntry entry) {
        ChatEntry lastChatEntry = null;
        if (entryList.size() >= 1) {
            lastChatEntry = entryList.get(entryList.size() - 1);
        }
        if (lastChatEntry != null && lastChatEntry.isTestStatusMessage()) {
            entryList.remove(entryList.size() - 1);
            entryList.add(entry);
        } else {
            entryList.add(entry);
        }
        notifyItemChanged(entryList.size() - 1);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == ChatEntry.DOBBY_CHAT) {
            View v = inflater.inflate(R.layout.dobby_chat, parent, false);
            viewHolder = new DobbyChatViewHolder(v);
        } else if (viewType == ChatEntry.USER_CHAT) {
            View v = inflater.inflate(R.layout.user_chat, parent, false);
            viewHolder = new UserChatViewHolder(v);
        } else if (viewType == ChatEntry.EXPERT_CHAT) {
            View v = inflater.inflate(R.layout.expert_chat, parent, false);
            viewHolder = new ExpertChatViewHolder(v);
        } else if (viewType == ChatEntry.RT_GRAPH) {
            View v = inflater.inflate(R.layout.rt_graph, parent, false);
            viewHolder = new RtGraphViewHolder(v);
        } else if (viewType == ChatEntry.BW_RESULTS_GAUGE_CARDVIEW) {
            View v = inflater.inflate(R.layout.bandwidth_results_cardview, parent, false);
            viewHolder = new BandwidthResultsCardViewHolder(v);
        } else if (viewType == ChatEntry.PING_RESULTS_CARDVIEW) {
            View v = inflater.inflate(R.layout.ping_results_cardview, parent, false);
            viewHolder = new PingResultsViewHolder(v);
        } else if (viewType == ChatEntry.OVERALL_NETWORK_CARDVIEW) {
            View v = inflater.inflate(R.layout.overall_network_cardview, parent, false);
            viewHolder = new OverallNetworkResultsViewHolder(v);
        } else {
            View v = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            viewHolder = new RecyclerViewSimpleTextViewHolder(v);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      switch (holder.getItemViewType()) {
          case ChatEntry.DOBBY_CHAT:
              DobbyChatViewHolder vh1 = (DobbyChatViewHolder) holder;
              configureDobbyViewHolder(vh1, position);
              break;

          case ChatEntry.USER_CHAT:
              UserChatViewHolder vh2 = (UserChatViewHolder) holder;
              configureUserViewHolder(vh2, position);
              break;

          case ChatEntry.EXPERT_CHAT:
              ExpertChatViewHolder expertChatViewHolder = (ExpertChatViewHolder) holder;
              configureExpertViewHolder(expertChatViewHolder, position);
              break;

          case ChatEntry.RT_GRAPH:
              RtGraphViewHolder vh4 = (RtGraphViewHolder) holder;
              configureRtGraphViewHolder(vh4, position);
              break;

          case ChatEntry.BW_RESULTS_GAUGE_CARDVIEW:
              BandwidthResultsCardViewHolder viewHolder = (BandwidthResultsCardViewHolder) holder;
              configureBandwidthResultsViewHolder(viewHolder, position);
              break;

          case ChatEntry.PING_RESULTS_CARDVIEW:
              PingResultsViewHolder pingResultsViewHolder = (PingResultsViewHolder) holder;
              configurePingResultsViewHolder(context, pingResultsViewHolder, position);
              break;

          case ChatEntry.OVERALL_NETWORK_CARDVIEW:
              OverallNetworkResultsViewHolder overallNetworkResultsViewHolder = (OverallNetworkResultsViewHolder) holder;
              configureOverallNetworkResultsViewHolder(context, overallNetworkResultsViewHolder, position);
              break;

          default:
              RecyclerViewSimpleTextViewHolder vh = (RecyclerViewSimpleTextViewHolder) holder;
              configureDefaultViewHolder(vh, position);
              break;
      }
    }

    private void configureDefaultViewHolder(RecyclerViewSimpleTextViewHolder holder, int position) {
        holder.getLabel().setText("This is a dummy message");
    }

    private void configureDobbyViewHolder(DobbyChatViewHolder dobbyChatViewHolder, int position) {
        ChatEntry entry = entryList.get(position);
        dobbyChatViewHolder.getDobbyChatTextView().setText(entryList.get(position).getText());
    }

    private void configureUserViewHolder(UserChatViewHolder userChatViewHolder, int position) {
        userChatViewHolder.getUserChatTv().setText(entryList.get(position).getText());
    }

    private void configureExpertViewHolder(ExpertChatViewHolder expertChatViewHolder, int position) {
        ChatEntry entry = entryList.get(position);
        expertChatViewHolder.getExpertChatTextView().setText(entryList.get(position).getText());
    }

    private void configureBandwidthResultsViewHolder(BandwidthResultsCardViewHolder viewHolder, int position) {
        viewHolder.showResults(entryList.get(position).getUploadMbps(), entryList.get(position).getDownloadMbps());
    }

    private void configurePingResultsViewHolder(Context context, PingResultsViewHolder viewHolder, int position) {
        viewHolder.setPingResults(context, entryList.get(position).getPingGrade());
    }

    private void configureOverallNetworkResultsViewHolder(Context context, OverallNetworkResultsViewHolder viewHolder, int position) {
        ChatEntry entry = entryList.get(position);
        viewHolder.setResults(context, entry.getWifiGrade(), entry.getIspName(), entry.getRouterIp());
    }

    private void configureRtGraphViewHolder(RtGraphViewHolder viewHolder, int position) {
        ChatEntry entry = entryList.get(position);
        GraphData graphData = entry.getGraphData();
        if (graphData != null) {
            viewHolder.setGraphData(graphData);
        } else {
            DobbyLog.e("Error: Attempting to draw chart with null GraphData.");
        }
    }

    @Override
    public int getItemCount() {
        return entryList.size();
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        int position = holder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        ChatEntry entry = entryList.get(position);
        if (entry != null && entry.getEntryType() == ChatEntry.RT_GRAPH) {
            // Special logic to unbind this view from its data source.
            if (holder instanceof RtGraphViewHolder) {
                ((RtGraphViewHolder) holder).unlinkDataSource();
            }
        }
     }
}
