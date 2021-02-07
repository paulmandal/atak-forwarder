package com.paulmandal.atak.forwarder.plugin.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.channel.TrackerUserInfo;
import com.paulmandal.atak.forwarder.channel.UserInfo;

import java.util.List;

import eo.view.batterymeter.BatteryMeterView;

public class GroupMemberDataAdapter extends ArrayAdapter<UserInfo> {
    private final List<UserInfo> mUsers;
    private final Context mAtakContext;
    private final Context mPluginContext;

    public GroupMemberDataAdapter(Context atakContext, Context pluginContext, List<UserInfo> users) {
        super(pluginContext, R.layout.group_member_list_view_item, users);
        mAtakContext = atakContext;
        mPluginContext = pluginContext;
        mUsers = users;
    }

    static class ViewHolder {
        protected TextView callsign;
        protected ImageView takIcon;
        protected BatteryMeterView batteryMeterView;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            LayoutInflater inflator = (LayoutInflater) mPluginContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.group_member_list_view_item, parent, false);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.callsign = view.findViewById(R.id.textview_callsign);
            viewHolder.takIcon = view.findViewById(R.id.imageview_tak_icon);
            viewHolder.takIcon.setImageDrawable(mAtakContext.getResources().getDrawable(com.atakmap.app.R.drawable.ic_atak_launcher));
            viewHolder.batteryMeterView = view.findViewById(R.id.battery_meter);
            view.setTag(viewHolder);
        } else {
            view = convertView;
        }
        UserInfo userInfo = mUsers.get(position);

        ViewHolder holder = (ViewHolder) view.getTag();
        holder.callsign.setText(userInfo.callsign);
        holder.takIcon.setVisibility(userInfo instanceof TrackerUserInfo ? View.GONE : View.VISIBLE);
        holder.batteryMeterView.setChargeLevel(userInfo.batteryPercentage);
        return view;
    }
}
