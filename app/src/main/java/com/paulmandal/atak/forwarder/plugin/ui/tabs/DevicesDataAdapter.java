package com.paulmandal.atak.forwarder.plugin.ui.tabs;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticDevice;

import java.util.List;

public class DevicesDataAdapter extends ArrayAdapter<MeshtasticDevice> {
    private final List<MeshtasticDevice> mDevices;
    private final Context mContext;

    @Nullable
    private final String mCommDeviceAddress;

    public DevicesDataAdapter(Context context, List<MeshtasticDevice> devices, String commDeviceAddress) {
        super(context, R.layout.devices_list_view_item, devices);
        mContext = context;
        mDevices = devices;
        mCommDeviceAddress = commDeviceAddress;
    }

    static class ViewHolder {
        protected TextView deviceName;
        protected TextView deviceAddress;
        protected Switch commDevice;
    }

    @Override
    @NonNull
    @SuppressLint("MissingPermission")
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            LayoutInflater inflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.devices_list_view_item, parent, false);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.deviceName = view.findViewById(R.id.textview_device_name);
            viewHolder.deviceAddress = view.findViewById(R.id.textview_device_address);
            viewHolder.commDevice = view.findViewById(R.id.switch_comm_device);
            view.setTag(viewHolder);
        } else {
            view = convertView;
        }

        MeshtasticDevice device = mDevices.get(position);

        ViewHolder holder = (ViewHolder) view.getTag();
        String deviceAddress = device.address;
        holder.deviceName.setText(device.name);
        holder.deviceAddress.setText(deviceAddress);
        holder.commDevice.setChecked(deviceAddress.equals(mCommDeviceAddress));
        return view;
    }
}
