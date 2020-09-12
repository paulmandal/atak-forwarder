package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.group.UserInfo;

import java.util.List;

import eo.view.batterymeter.BatteryMeterView;

public class GroupMemberDataAdapter extends ArrayAdapter<UserInfo> {
    private final List<UserInfo> mUsers;
    private final Context mContext;

    public GroupMemberDataAdapter(Context context, List<UserInfo> users) {
        super(context, R.layout.group_member_list_view_item, users);
        mContext = context;
        mUsers = users;
    }

    static class ViewHolder {
        protected TextView callsign;
        protected BatteryMeterView batteryMeterView;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            LayoutInflater inflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.group_member_list_view_item, parent, false);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.callsign = (TextView) view.findViewById(R.id.textview_callsign);
            viewHolder.batteryMeterView = (BatteryMeterView) view.findViewById(R.id.battery_meter);
            view.setTag(viewHolder);
        } else {
            view = convertView;
        }
        UserInfo userInfo = mUsers.get(position);

        ViewHolder holder = (ViewHolder) view.getTag();
        holder.callsign.setText(userInfo.callsign);
        holder.batteryMeterView.setChargeLevel(userInfo.batteryPercentage);
        return view;
    }
}