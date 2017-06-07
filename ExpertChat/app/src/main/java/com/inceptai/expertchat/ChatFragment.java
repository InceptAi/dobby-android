package com.inceptai.expertchat;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ChatFragment extends Fragment {
    public static final String FRAGMENT_TAG = "ChatFragment";
    private static final String USER_ID = "param1";

    private String userId;


    public ChatFragment() {
        // Required empty public constructor
    }

    /**
     * @param param1 Parameter 1.
     * @return A new instance of Bundle.
     */
    // TODO: Rename and change types and number of parameters
    public static Bundle getArgumentBundle(String param1) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(USER_ID, param1);
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(USER_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView textView = new TextView(getActivity());
        textView.setText(R.string.hello_blank_fragment);
        return textView;
    }

}
