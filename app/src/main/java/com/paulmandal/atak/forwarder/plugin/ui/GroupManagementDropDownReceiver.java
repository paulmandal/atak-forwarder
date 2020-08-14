package com.paulmandal.atak.forwarder.plugin.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.MessageQueue;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.group.GroupInfo;
import com.paulmandal.atak.forwarder.group.GroupTracker;
import com.paulmandal.atak.forwarder.group.UserInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GroupManagementDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener,
        GroupTracker.UpdateListener,
        MessageQueue.Listener,
        CommHardware.ScanListener {
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
    private Button mScanOrUnpair;
    private TextView mConnectionStatusTextView;

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

        // Set up tabs
        TabHost tabs = (TabHost) mTemplateView.findViewById(R.id.tab_host);
        tabs.setup();
        TabHost.TabSpec spec = tabs.newTabSpec("tab_settings");
        spec.setContent(R.id.tab_settings);
        spec.setIndicator("Settings");
        tabs.addTab(spec);
        spec = tabs.newTabSpec("tab_advanced");
        spec.setContent(R.id.tab_advanced);
        spec.setIndicator("Advanced");
        tabs.addTab(spec);

        // Set up the rest of the UI

        mGroupIdTextView = (TextView) mTemplateView.findViewById(R.id.textview_group_id);
        mMessageQueueLengthTextView = (TextView)mTemplateView.findViewById(R.id.textview_message_queue_length);
        mCreateGroupButton = (Button) mTemplateView.findViewById(R.id.button_create_group);
        mGroupMembersListView = (ListView) mTemplateView.findViewById(R.id.listview_group_members);

        mConnectionStatusTextView = (TextView) mTemplateView.findViewById(R.id.textview_connection_status);

        Button clearData = (Button) mTemplateView.findViewById(R.id.button_clear_data);
        Button broadcastDiscovery = (Button) mTemplateView.findViewById(R.id.button_broadcast_discovery);
        Button clearMessageCache = (Button) mTemplateView.findViewById(R.id.button_clear_message_cache);
        Button clearMessageQueue = (Button) mTemplateView.findViewById(R.id.button_clear_message_queue);
        Button setCachePurgeTime = (Button) mTemplateView.findViewById(R.id.button_set_message_purge_time_ms);
        mScanOrUnpair = (Button) mTemplateView.findViewById(R.id.button_scan_or_unpair);

        EditText cachePurgeTimeMins = (EditText) mTemplateView.findViewById(R.id.edittext_purge_time_mins);

        mMessageQueueLengthTextView.setText(String.format(Locale.getDefault(), "%d", messageQueue.getQueueSize()));
        cachePurgeTimeMins.setText(String.format(Locale.getDefault(), "%d", mCotMessageCache.getCachePurgeTimeMs() / 60000));

        broadcastDiscovery.setOnClickListener((View v) -> {
            Toast.makeText(mPluginContext, "Broadcasting discovery message", Toast.LENGTH_SHORT).show();
            commHardware.broadcastDiscoveryMessage();
        });

        clearData.setOnClickListener((View v) -> {
            Toast.makeText(mPluginContext, "Clearing all plugin data", Toast.LENGTH_LONG).show();
            mGroupTracker.clearData();
            mCotMessageCache.clearData();
            mMessageQueue.clearData();
            updateUi();
        });

        clearMessageCache.setOnClickListener((View v) -> {
            Toast.makeText(mPluginContext, "Clearing duplicate message cache", Toast.LENGTH_SHORT).show();
            mCotMessageCache.clearData();
        });

        clearMessageQueue.setOnClickListener((View v) -> {
            Toast.makeText(mPluginContext, "Clearing outgoing message queue", Toast.LENGTH_SHORT).show();
            mMessageQueue.clearData();
        });

        setCachePurgeTime.setOnClickListener((View v) -> {
            Toast.makeText(mPluginContext, "Set duplicate message cache TTL", Toast.LENGTH_SHORT).show();
            String cachePurgeTimeMinsStr = cachePurgeTimeMins.getText().toString();
            if (cachePurgeTimeMinsStr.equals("")) {
                return;
            }
            int cachePurgeTimeMs = Integer.parseInt(cachePurgeTimeMinsStr) * 60000;
            mCotMessageCache.setCachePurgeTimeMs(cachePurgeTimeMs);
        });

        mScanOrUnpair.setOnClickListener(mScanClickListener);

        mGroupTracker.setUpdateListener(this);
        messageQueue.setListener(this);
        commHardware.setScanListener(this);
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

    private View.OnClickListener mScanClickListener = (View v) -> mCommHardware.connect();

    private View.OnClickListener mUnpairClickListener = (View v) -> mCommHardware.forgetDevice();

    private void updateUi() {
        setEditModeAndUiForGroup();
        setupListView();
    }

    private void setEditModeAndUiForGroup() {
        GroupInfo groupInfo = mGroupTracker.getGroup();
        if (groupInfo != null) {
            mGroupIdTextView.setText(String.format(Locale.getDefault(), "%d", groupInfo.groupId));
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
        mActivity.runOnUiThread(() -> mMessageQueueLengthTextView.setText(String.format(Locale.getDefault(), "%d", size)));
    }

    @Override
    public void onScanStarted() {
        Toast.makeText(mPluginContext, "Scanning for comm device", Toast.LENGTH_SHORT).show();
        mConnectionStatusTextView.setText(R.string.connection_status_scanning);
        mScanOrUnpair.setOnClickListener(null);
    }

    @Override
    public void onScanTimeout() {
        Toast.makeText(mPluginContext, "Scanning for comm device timed out, ready device and then rescan in settings menu!", Toast.LENGTH_LONG).show();
        mConnectionStatusTextView.setText(R.string.connection_status_timeout);
        mScanOrUnpair.setOnClickListener(mScanClickListener);
        mScanOrUnpair.setText(R.string.scan);
    }

    @Override
    public void onDeviceConnected() {
        Toast.makeText(mPluginContext, "Comm device connected", Toast.LENGTH_SHORT).show();
        mConnectionStatusTextView.setText(R.string.connection_status_connected);
        mScanOrUnpair.setOnClickListener(mUnpairClickListener);
        mScanOrUnpair.setText(R.string.unpair);
    }

    @Override
    public void onDeviceDisconnected() {
        Toast.makeText(mPluginContext, "Comm device disconnected", Toast.LENGTH_SHORT).show();
        mConnectionStatusTextView.setText(R.string.connection_status_disconnected);
        mScanOrUnpair.setOnClickListener(mScanClickListener);
        mScanOrUnpair.setText(R.string.scan);
    }
}
