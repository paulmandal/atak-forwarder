package com.paulmandal.atak.forwarder.plugin.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.MessageQueue;
import com.paulmandal.atak.forwarder.comm.interfaces.CommHardware;
import com.paulmandal.atak.forwarder.group.GroupInfo;
import com.paulmandal.atak.forwarder.group.GroupTracker;
import com.paulmandal.atak.forwarder.group.UserInfo;

import java.util.ArrayList;
import java.util.List;

public class GroupManagementDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener, GroupTracker.UpdateListener, MessageQueue.Listener {
    public static final String TAG = "ATAKDBG." + GroupManagementDropDownReceiver.class.getSimpleName();
    public static final String SHOW_PLUGIN = "com.paulmandal.atak.forwarder.SHOW_PLUGIN";

    private final View mTemplateView;
    private final Context mPluginContext;

    private Activity mActivity;
    private GroupTracker mGroupTracker;
    private CommHardware mCommHardware;
    private CotMessageCache mCotMessageCache;
    private MessageQueue mMessageQueue;
    private EditMode mEditMode;

    private List<UserInfo> mUsers;
    private List<UserInfo> mModifiedUsers;

    private TextView mGroupIdTextView;
    private TextView mMessageQueueLengthTextView;
    private ListView mGroupMembersListView;
    private Button mCreateGroupButton;

    public GroupManagementDropDownReceiver(final MapView mapView,
                                           final Context context,
                                           final Activity activity,
                                           final GroupTracker groupTracker,
                                           final CommHardware commHardware,
                                           final CotMessageCache cotMessageCache,
                                           final MessageQueue messageQueue) {
        super(mapView);
        mPluginContext = context;
        mActivity = activity;
        mGroupTracker = groupTracker;
        mCommHardware = commHardware;
        mCotMessageCache = cotMessageCache;
        mMessageQueue = messageQueue;

        // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
        // In this case, using it is not necessary - but I am putting it here to remind
        // developers to look at this Inflator
        mTemplateView = PluginLayoutInflater.inflate(context, R.layout.main_layout, null);

        mGroupIdTextView = (TextView) mTemplateView.findViewById(R.id.textview_group_id);
        mMessageQueueLengthTextView = (TextView)mTemplateView.findViewById(R.id.textview_message_queue_length);
        mCreateGroupButton = (Button) mTemplateView.findViewById(R.id.button_create_group);
        mGroupMembersListView = (ListView) mTemplateView.findViewById(R.id.listview_group_members);

        Button clearData = (Button) mTemplateView.findViewById(R.id.button_clear_data);
        Button broadcastDiscovery = (Button) mTemplateView.findViewById(R.id.button_broadcast_discovery);
        Button clearMessageCache = (Button) mTemplateView.findViewById(R.id.button_clear_message_cache);
        Button setCachePurgeTime = (Button) mTemplateView.findViewById(R.id.button_set_message_purge_time_ms);

        EditText cachePurgeTimeMins = (EditText) mTemplateView.findViewById(R.id.edittext_purge_time_mins);

        mMessageQueueLengthTextView.setText("" + messageQueue.getQueueSize());
        cachePurgeTimeMins.setText("" + (mCotMessageCache.getCachePurgeTimeMs() / 60000));

        broadcastDiscovery.setOnClickListener((View v) -> {
            Toast.makeText(mPluginContext, "Broadcasting discovery message", Toast.LENGTH_SHORT).show();
            commHardware.broadcastDiscoveryMessage();
        });

        clearData.setOnClickListener((View v) -> {
            mGroupTracker.clearData();
            mCotMessageCache.clearData();
            mMessageQueue.clearData();
        });

        clearMessageCache.setOnClickListener((View v) -> mCotMessageCache.clearData());

        setCachePurgeTime.setOnClickListener((View v) -> {
            String cachePurgeTimeMinsStr = cachePurgeTimeMins.getText().toString();
            if (cachePurgeTimeMinsStr.equals("")) {
                return;
            }
            int cachePurgeTimeMs = Integer.parseInt(cachePurgeTimeMinsStr) * 60000;
            mCotMessageCache.setCachePurgeTimeMs(cachePurgeTimeMs);
        });

        mGroupTracker.setUpdateListener(this);
        messageQueue.setListener(this);
    }

    public void disposeImpl() {
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(SHOW_PLUGIN)) {
            updateUi();

            List<Long> gIdsForGroup = new ArrayList<>();
            mCreateGroupButton.setOnClickListener((View v) -> {
                if (mEditMode == EditMode.ADD_USERS) {
                    List<Long> newGidsForGroup = new ArrayList<>();

                    StringBuilder usernamesForOutput = new StringBuilder();
                    boolean first = true;

                    for (int i = 0; i < mUsers.size(); i++) {
                        UserInfo originalUser = mUsers.get(i);
                        UserInfo modifiedUser = mModifiedUsers.get(i);
                        if (originalUser.isInGroup) {
                            gIdsForGroup.add(originalUser.gId);
                        }
                        if (!originalUser.isInGroup && modifiedUser.isInGroup) {
                            gIdsForGroup.add(originalUser.gId);
                            newGidsForGroup.add(originalUser.gId);

                            usernamesForOutput.append(first ? "" : ", ");
                            usernamesForOutput.append(originalUser.callsign);
                            first = false;
                        }
                    }

                    Toast.makeText(mPluginContext, "Adding users to group: " + usernamesForOutput, Toast.LENGTH_SHORT).show();
                    mCommHardware.addToGroup(gIdsForGroup, newGidsForGroup);
                } else {
                    StringBuilder usernamesForOutput = new StringBuilder();
                    boolean first = true;

                    for (UserInfo user : mModifiedUsers) {
                        if (user.isInGroup) {
                            gIdsForGroup.add(user.gId);

                            usernamesForOutput.append(first ? "" : ", ");
                            usernamesForOutput.append(user.callsign);
                            first = false;
                        }
                    }
                    Toast.makeText(mPluginContext, "Creating group with users: " + usernamesForOutput, Toast.LENGTH_SHORT).show();
                    mCommHardware.createGroup(gIdsForGroup);
                }
            });
            showDropDown(mTemplateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false);
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

    @Override
    public void onUsersUpdated() {
        Toast.makeText(mPluginContext, "User list updated", Toast.LENGTH_SHORT).show();
        updateUi();
    }

    @Override
    public void onGroupUpdated() {
        Toast.makeText(mPluginContext, "Group membership updated", Toast.LENGTH_SHORT).show();
        updateUi();
    }

    private void updateUi() {
        setEditModeAndUiForGroup();
        setupListView();
    }

    private void setEditModeAndUiForGroup() {
        GroupInfo groupInfo = mGroupTracker.getGroup();
        if (groupInfo != null) {
            mGroupIdTextView.setText(Long.toString(groupInfo.groupId));
            mCreateGroupButton.setText(R.string.add_to_group);
            mEditMode = EditMode.ADD_USERS;
        } else {
            mEditMode = EditMode.NEW_GROUP;
        }
    }

    private void setupListView() {
        mUsers = mGroupTracker.getUsers();
        mModifiedUsers = new ArrayList<>(mUsers.size());
        for (UserInfo user : mUsers) {
            mModifiedUsers.add(user.clone());
        }

        GroupMemberDataAdapter groupMemberDataAdapter = new GroupMemberDataAdapter(mPluginContext, mModifiedUsers, mEditMode);
        mGroupMembersListView.setAdapter(groupMemberDataAdapter);
    }

    @Override
    public void onMessageQueueSizeChanged(int size) {
        mActivity.runOnUiThread(() -> mMessageQueueLengthTextView.setText("" + size));
    }
}
