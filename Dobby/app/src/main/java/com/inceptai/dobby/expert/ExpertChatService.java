package com.inceptai.dobby.expert;

/**
 * Created by arunesh on 6/1/17.
 */

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.inceptai.dobby.BuildConfig;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

/**
 * A "service" that connects to the expert chat system.
 * This is NOT an Android service.
 */
public class ExpertChatService implements ChildEventListener {

    public static final String CHAT_ROOM_CHILD_BASE_DOBBY = "dobby_chat_rooms";
    public static final String CHAT_ROOM_CHILD_BASE_WIFI_TESTER = "wifitester_chat_rooms";

    private String userUuid;
    private String chatRoomChild;
    private DatabaseReference firebaseDatabaseReference;
    private ChatCallback chatCallback;

    public interface ChatCallback {
        void onMessageAvailable(ExpertChat expertChat);
    }

    private ExpertChatService(String userUuid, String chatRoomChildBase) {
        this.userUuid = userUuid;
        this.chatRoomChild = chatRoomChildBase + "/" + "user_" + userUuid;
        DobbyLog.i("Using chat room ID: " + chatRoomChild);
        initialize();
    }

    public static ExpertChatService newInstance(String userUuid) {
        String chatRoomChild;
        if (BuildConfig.FLAVOR.equals(Utils.WIFIDOC_FLAVOR)) {
            chatRoomChild = CHAT_ROOM_CHILD_BASE_WIFI_TESTER;
        } else {
            chatRoomChild = CHAT_ROOM_CHILD_BASE_DOBBY;
        }
        return new ExpertChatService(userUuid, chatRoomChild);
    }

    public void setCallback(ChatCallback callback) {
        this.chatCallback = callback;
    }

    public void disconnect() {
        firebaseDatabaseReference.removeEventListener(this);
    }
    
    private void initialize() {
        firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child(chatRoomChild);
        firebaseDatabaseReference.addChildEventListener(this);
    }

    public void pushData(ExpertChat expertChat) {
        firebaseDatabaseReference.push().setValue(expertChat);
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        if (chatCallback != null) {
            ExpertChat expertChat = parse(dataSnapshot);
            chatCallback.onMessageAvailable(expertChat);
            DobbyLog.i("Got chat message: " + expertChat .getText());
        }
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {

    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }

    private static ExpertChat parse(DataSnapshot dataSnapshot) {
        ExpertChat expertChat = dataSnapshot.getValue(ExpertChat.class);
        return expertChat;
    }
}
