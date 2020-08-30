package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.group.UserInfo;

import java.util.List;

public class GroupMemberDataAdapter extends ArrayAdapter<UserInfo> {
    private final List<UserInfo> mUsers;
    private final Context mContext;
    private final EditMode mEditMode;

    public GroupMemberDataAdapter(Context context, List<UserInfo> users, EditMode editMode) {
        super(context, R.layout.group_member_list_view_item, users);
        mContext = context;
        mUsers = users;
        mEditMode = editMode;
    }

    static class ViewHolder {
        protected TextView callsign;
        protected Switch inOutSwitch;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            LayoutInflater inflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.group_member_list_view_item, parent);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.callsign = (TextView) view.findViewById(R.id.textview_callsign);
            viewHolder.inOutSwitch = (Switch) view.findViewById(R.id.switch_in_out);
            viewHolder.inOutSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
                UserInfo user = (UserInfo) viewHolder.inOutSwitch.getTag();
                if (mEditMode == EditMode.ADD_USERS && user.isInGroup && !buttonView.isChecked()) {
                    // Reset checkbox for users already in the group
                    buttonView.setChecked(true);
                    return;
                }
                user.isInGroup = buttonView.isChecked();
            });
            view.setTag(viewHolder);
            viewHolder.inOutSwitch.setTag(mUsers.get(position));
        } else {
            view = convertView;
            ((ViewHolder) view.getTag()).inOutSwitch.setTag(mUsers.get(position));
        }
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.callsign.setText(mUsers.get(position).callsign);
        holder.inOutSwitch.setChecked(mUsers.get(position).isInGroup);
        return view;
    }
}