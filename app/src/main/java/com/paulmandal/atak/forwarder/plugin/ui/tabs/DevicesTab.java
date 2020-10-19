package com.paulmandal.atak.forwarder.plugin.ui.tabs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;

import com.atakmap.android.gui.PluginSpinner;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticDevice;
import com.paulmandal.atak.forwarder.nonatak.NonAtakStationCotGenerator;
import com.paulmandal.atak.forwarder.plugin.ui.EditTextValidator;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.DevicesTabViewModel;

import java.util.Arrays;
import java.util.List;

public class DevicesTab extends ConstraintLayout {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + DevicesTab.class.getSimpleName();

    private Context mAtakContext;
    private MeshtasticDevice mTargetDevice;

    public DevicesTab(Context context) {
        this(context, null);
    }

    public DevicesTab(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DevicesTab(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.devices_layout, this);
    }

    @SuppressLint("MissingPermission")
    public void bind(LifecycleOwner lifecycleOwner,
                     DevicesTabViewModel devicesTabViewModel,
                     Context pluginContext,
                     Context atakContext) {
        Button refreshButton = findViewById(R.id.button_refresh_connected_devices);
        refreshButton.setOnClickListener(v -> devicesTabViewModel.refreshDevices());

        Button setCommDeviceButton = findViewById(R.id.button_set_comm_device);
        setCommDeviceButton.setOnClickListener(v -> {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(atakContext)
                    .setTitle(pluginContext.getResources().getString(R.string.warning))
                    .setMessage(pluginContext.getResources().getString(R.string.set_comm_device_dialog))
                    .setPositiveButton(pluginContext.getResources().getString(R.string.ok), (DialogInterface dialog, int whichButton) -> devicesTabViewModel.setCommDeviceAddress(mTargetDevice))
                    .setNegativeButton(pluginContext.getResources().getString(R.string.cancel), (DialogInterface dialog, int whichButton) -> dialog.cancel());

            alertDialog.show();
        });

        EditText deviceCallsignEditText = findViewById(R.id.edittext_device_callsign);
        deviceCallsignEditText.addTextChangedListener(new EditTextValidator(deviceCallsignEditText) {
            @Override
            public void validate(TextView textView, String text) {
                if (text == null || text.isEmpty()) {
                    textView.setError("You must enter a device callsign");
                    return;
                }

                textView.setError(null);
            }
        });

        EditText pliIntervalSEditText = findViewById(R.id.edittext_pli_interval_s);
        pliIntervalSEditText.addTextChangedListener(new EditTextValidator(pliIntervalSEditText) {
            @Override
            public void validate(TextView textView, String text) {
                try {
                    int pliIntervalS = Integer.parseInt(text);

                    if (pliIntervalS > 0) {
                        textView.setError(null);
                        return;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                textView.setError("PLI Interval must be > 0s");
            }
        });

        EditText screenShutoffDelaySEditText = findViewById(R.id.edittext_screen_shutoff_delay_s);
        screenShutoffDelaySEditText.addTextChangedListener(new EditTextValidator(screenShutoffDelaySEditText) {
            @Override
            public void validate(TextView textView, String text) {
                try {
                    int screenShutoffDelayS = Integer.parseInt(text);

                    if (screenShutoffDelayS > 0) {
                        textView.setError(null);
                        return;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                textView.setError("Screen shutoff delay must be > 0s");
            }
        });

        PluginSpinner teamSpinner = findViewById(R.id.spinner_team);
        ArrayAdapter<String> teamArrayAdapter = new ArrayAdapter<>(pluginContext, R.layout.plugin_spinner_item, Arrays.asList(NonAtakStationCotGenerator.TEAMS));
        teamArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamSpinner.setAdapter(teamArrayAdapter);

        PluginSpinner roleSpinner = findViewById(R.id.spinner_role);
        ArrayAdapter<String> roleArrayAdapter = new ArrayAdapter<>(pluginContext, R.layout.plugin_spinner_item, Arrays.asList(NonAtakStationCotGenerator.ROLES));
        roleArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleArrayAdapter);

        Button writeToNonAtakButton = findViewById(R.id.button_write_to_non_atak);
        writeToNonAtakButton.setOnClickListener(v -> {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(atakContext)
                    .setTitle(pluginContext.getResources().getString(R.string.warning))
                    .setMessage(pluginContext.getResources().getString(R.string.write_to_non_atak_dialog))
                    .setPositiveButton(pluginContext.getResources().getString(R.string.ok), (DialogInterface dialog, int whichButton) -> maybeWriteToNonAtatkDevice(devicesTabViewModel, mTargetDevice, deviceCallsignEditText.getText().toString(), teamSpinner.getSelectedItemPosition(), roleSpinner.getSelectedItemPosition(), Integer.parseInt(pliIntervalSEditText.getText().toString()), Integer.parseInt(screenShutoffDelaySEditText.getText().toString())))
                    .setNegativeButton(pluginContext.getResources().getString(R.string.cancel), (DialogInterface dialog, int whichButton) -> dialog.cancel());
            alertDialog.show();
        });

        PluginSpinner devicesSpinner = findViewById(R.id.spinner_devices);
        TextView commDevice = findViewById(R.id.textview_comm_device);

        devicesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mTargetDevice = (MeshtasticDevice) devicesSpinner.getAdapter().getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        devicesTabViewModel.getMeshtasticDevices().observe(lifecycleOwner, meshtasticDevices -> {
            String commDeviceAddress = devicesTabViewModel.getCommDeviceAddress().getValue();

            maybeUpdateCommDevice(commDevice, commDeviceAddress, meshtasticDevices);
            updateDevicesAdapter(devicesSpinner, pluginContext, meshtasticDevices);
        });

        devicesTabViewModel.getCommDeviceAddress().observe(lifecycleOwner, commDeviceAddress -> {
            List<MeshtasticDevice> meshtasticDevices = devicesTabViewModel.getMeshtasticDevices().getValue();

            if (meshtasticDevices == null) {
                return;
            }

            maybeUpdateCommDevice(commDevice, commDeviceAddress, meshtasticDevices);
        });

        ProgressBar deviceWriteProgressBar = findViewById(R.id.progressbar_writing_to_device);
        devicesTabViewModel.getNonAtakDeviceWriteInProgress().observe(lifecycleOwner, nonAtakDeviceWriteInProgress -> deviceWriteProgressBar.setVisibility(nonAtakDeviceWriteInProgress ? View.VISIBLE : View.GONE));

        mAtakContext = atakContext;
    }

    @SuppressLint("MissingPermission")
    private void maybeUpdateCommDevice(TextView commDevice, String commDeviceAddress, List<MeshtasticDevice> meshtasticDevices) {
        if (commDeviceAddress != null) {
            for (MeshtasticDevice device : meshtasticDevices) {
                String deviceAddress = device.address;
                if (deviceAddress.equals(commDeviceAddress)) {
                    commDevice.setText(String.format("%s - %s", device.name, deviceAddress));
                }
            }
        }
    }

    private void updateDevicesAdapter(PluginSpinner devicesSpinner, Context pluginContext, List<MeshtasticDevice> meshtasticDevices) {
        ArrayAdapter<MeshtasticDevice> devicesDataAdapter = new ArrayAdapter<>(pluginContext, R.layout.plugin_spinner_item, meshtasticDevices);
        devicesDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        devicesSpinner.setAdapter(devicesDataAdapter);
    }

    private void maybeWriteToNonAtatkDevice(DevicesTabViewModel devicesTabViewModel, MeshtasticDevice targetDevice, String deviceCallsign, int teamIndex, int roleIndex, int refreshIntervalS, int screenShutoffDelayS) {
        if (targetDevice == null) {
            Toast.makeText(mAtakContext, "You must select a device to write to", Toast.LENGTH_SHORT).show();
            return;
        }

        if (deviceCallsign == null || deviceCallsign.isEmpty()) {
            Toast.makeText(mAtakContext, "You must enter a device callsign", Toast.LENGTH_SHORT).show();
            return;
        }

        if (refreshIntervalS < 1) {
            Toast.makeText(mAtakContext, "Refresh interval must be > 0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (screenShutoffDelayS < 1) {
            Toast.makeText(mAtakContext, "Screen shutoff delay must be > 0", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(mAtakContext, "Writing to non-ATAK device", Toast.LENGTH_SHORT).show();
        devicesTabViewModel.writeToNonAtak(targetDevice, deviceCallsign, teamIndex, roleIndex, refreshIntervalS, screenShutoffDelayS);
    }
}
