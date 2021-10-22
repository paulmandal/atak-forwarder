package com.paulmandal.atak.forwarder.plugin.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.text.InputFilter;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.atakmap.android.gui.PanEditTextPreference;
import com.atakmap.android.gui.PanListPreference;
import com.atakmap.android.gui.PanPreference;
import com.atakmap.android.gui.PanSwitchPreference;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.channel.ChannelConfig;
import com.paulmandal.atak.forwarder.comm.meshtastic.DiscoveryBroadcastEventHandler;
import com.paulmandal.atak.forwarder.helpers.ChannelJsonException;
import com.paulmandal.atak.forwarder.helpers.ChannelJsonHelper;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.helpers.PskHelper;
import com.paulmandal.atak.forwarder.helpers.QrHelper;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.DestroyableSharedPrefsListener;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.ArrayList;
import java.util.List;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ChannelButtons extends DestroyableSharedPrefsListener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + ChannelButtons.class.getSimpleName();

    private static final int MAX_CHANNELS = ForwarderConstants.MAX_CHANNELS;
    private static final List<ChannelConfig> DEFAULT_CHANNEL_CONFIGS = PreferencesDefaults.DEFAULT_CHANNEL_CONFIGS;

    private final SharedPreferences mSharedPreferences;
    private final Context mSettingsMenuContext;
    private final Context mPluginContext;
    private final HashHelper mHashHelper;
    private final PskHelper mPskHelper;
    private final ChannelJsonHelper mChannelJsonHelper;
    private final PreferenceCategory mCategoryChannels;

    private List<ChannelConfig> mChannelConfigs;

    public ChannelButtons(List<Destroyable> destroyables,
                          SharedPreferences sharedPreferences,
                          Context settingsMenuContext,
                          Context pluginContext,
                          DiscoveryBroadcastEventHandler discoveryBroadcastEventHandler,
                          HashHelper hashHelper,
                          PskHelper pskHelper,
                          QrHelper qrHelper,
                          ChannelJsonHelper channelJsonHelper,
                          Logger logger,
                          PreferenceCategory categoryChannels,
                          Preference addChannel,
                          Preference showChannelQr,
                          Preference scanChannelQr,
                          Preference saveChannelToFile,
                          Preference readChannelFromFile) {
        super(destroyables,
                sharedPreferences,
                new String[] {},
                new String[] {
                        PreferencesKeys.KEY_CHANNEL_DATA,
                });

        mSharedPreferences = sharedPreferences;
        mSettingsMenuContext = settingsMenuContext;
        mPluginContext = pluginContext;
        mHashHelper = hashHelper;
        mPskHelper = pskHelper;
        mChannelJsonHelper = channelJsonHelper;
        mCategoryChannels = categoryChannels;

        addChannel.setOnPreferenceClickListener((Preference preference) -> {
            if (mChannelConfigs.size() < MAX_CHANNELS) {
                mChannelConfigs.add(new ChannelConfig("New " + mChannelConfigs.size(), ForwarderConstants.DEFAULT_CHANNEL_PSK, 1, false));
                saveChannels();
            } else {
                Toast.makeText(settingsMenuContext, "Reached maximum channel count: " + MAX_CHANNELS, Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        showChannelQr.setOnPreferenceClickListener((Preference preference) -> {
            String channelConfigsStr;
            try {
                channelConfigsStr = channelJsonHelper.toJson(mChannelConfigs);
            } catch (ChannelJsonException e) {
                Toast.makeText(settingsMenuContext, "Error generating channel JSON!", Toast.LENGTH_SHORT).show();
                return true;
            }
            byte[] payload = channelConfigsStr.getBytes();

            WindowManager windowManager = (WindowManager) settingsMenuContext.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            int width = (int) ((float) displayMetrics.widthPixels * 0.9);

            Bitmap bm = null;
            try {
                bm = qrHelper.encodeAsBitmap(payload, width);
            } catch (WriterException e) {
                e.printStackTrace();
            }

            if (bm != null) {
                ImageView iv = new ImageView(settingsMenuContext);
                iv.setImageBitmap(bm);

                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(settingsMenuContext)
                        .setView(iv)
                        .setNegativeButton(pluginContext.getResources().getString(R.string.cancel), (DialogInterface dialog, int whichButton) -> dialog.cancel());

                alertDialog.show();
            }
            return true;
        });

        scanChannelQr.setOnPreferenceClickListener((Preference preference) -> {
            ZXingScannerView scannerView = new ZXingScannerView(pluginContext);

            List<BarcodeFormat> possibleFormats = new ArrayList<>();
            possibleFormats.add(BarcodeFormat.QR_CODE);

            scannerView.setFormats(possibleFormats);

            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(settingsMenuContext)
                    .setView(scannerView)
                    .setNegativeButton(pluginContext.getResources().getString(R.string.cancel), (DialogInterface dialog, int whichButton) -> {
                        scannerView.stopCamera();
                        dialog.cancel();
                    });

            AlertDialog dialog = alertDialog.show();

            scannerView.setResultHandler((Result rawResult) -> {
                String resultText = rawResult.getText();
                byte[] resultBytes = Base64.decode(resultText, Base64.DEFAULT);
                String json = new String(resultBytes);

                try {
                    mChannelConfigs = mChannelJsonHelper.listFromJson(json);
                    saveChannels();

                    discoveryBroadcastEventHandler.broadcastDiscoveryMessage(true);
                } catch (ChannelJsonException e) {
                    Toast.makeText(settingsMenuContext, "Error reading QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    e.printStackTrace();
                }

                scannerView.stopCamera();

                dialog.dismiss();
            });
            scannerView.startCamera();

            return true;
        });

//        saveChannelToFile.setOnPreferenceClickListener((Preference preference) -> {
//            // TODO: implement
//            return true;
//        });
//
//        readChannelFromFile.setOnPreferenceClickListener((Preference preference) -> {
//            // TODO: implement
//            return true;
//        });

        complexUpdate(sharedPreferences, "");
    }

    @Override
    protected void updateSettings(SharedPreferences sharedPreferences) {
        // Do nothing
    }

    @Override
    protected void complexUpdate(SharedPreferences sharedPreferences, String key) {
        // This is in complexUpdate so mCategoryChannels is available when it gets called
        String channelJson = sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_DATA, PreferencesDefaults.DEFAULT_CHANNEL_DATA);
        try {
            mChannelConfigs = mChannelJsonHelper.listFromJson(channelJson);

            updateChannels();
        } catch (ChannelJsonException e) {
            // do nothing
        }
    }

    private void updateChannels() {
        mCategoryChannels.removeAll();

        for (ChannelConfig channelConfig : mChannelConfigs) {
            PanPreference dividerPreference = new PanPreference(mSettingsMenuContext);
            dividerPreference.setTitle(channelConfig.name + " " + mPluginContext.getResources().getString(R.string.click_to_delete));
            dividerPreference.setOnPreferenceClickListener((Preference preference) -> {
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(mSettingsMenuContext)
                        .setTitle(mPluginContext.getResources().getString(R.string.delete_channel))
                        .setMessage(mPluginContext.getResources().getString(R.string.delete_channel_message))
                        .setPositiveButton(mPluginContext.getResources().getString(R.string.ok), (DialogInterface dialog, int whichButton) -> {
                            mChannelConfigs.remove(channelConfig);

                            if (mChannelConfigs.size() == 0) {
                                mChannelConfigs = new ArrayList<>();
                                for (ChannelConfig config : DEFAULT_CHANNEL_CONFIGS) {
                                    mChannelConfigs.add(config.clone());
                                }
                            }

                            saveChannels();
                        })
                        .setNegativeButton(mPluginContext.getResources().getString(R.string.cancel), (DialogInterface dialog, int whichButton) -> dialog.cancel());

                alertDialog.show();
                return true;
            });

            mCategoryChannels.addPreference(dividerPreference);

            PanEditTextPreference channelNamePreference = new PanEditTextPreference(mSettingsMenuContext);
            channelNamePreference.setTitle(mPluginContext.getResources().getString(R.string.channel_name) + " " + channelConfig.name);
            channelNamePreference.setDialogMessage(mPluginContext.getResources().getString(R.string.enter_channel_name));
            channelNamePreference.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(11)
            }, false);
            channelNamePreference.setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
                String channelName = (String)newValue;
                channelConfig.name = channelName;
                saveChannels();
                return true;
            });
            mCategoryChannels.addPreference(channelNamePreference);

            PanPreference channelPskPreference = new PanPreference(mSettingsMenuContext);
            channelPskPreference.setTitle(channelConfig.name + " " + mPluginContext.getResources().getString(R.string.channel_psk)  + " " + mHashHelper.hashFromBytes(channelConfig.psk));
            channelPskPreference.setOnPreferenceClickListener((Preference preference) -> {
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(mSettingsMenuContext)
                        .setTitle(mPluginContext.getResources().getString(R.string.warning))
                        .setMessage(mPluginContext.getResources().getString(R.string.generate_psk_warning))
                        .setPositiveButton(mPluginContext.getResources().getString(R.string.ok), (DialogInterface dialog, int whichButton) -> {
                            channelConfig.psk = mPskHelper.genPskBytes();
                            saveChannels();
                        })
                        .setNegativeButton(mPluginContext.getResources().getString(R.string.cancel), (DialogInterface dialog, int whichButton) -> dialog.cancel());

                alertDialog.show();
                return true;
            });
            mCategoryChannels.addPreference(channelPskPreference);

            PanListPreference channelModePreference = new PanListPreference(mPluginContext);
            channelModePreference.setTitle(channelConfig.name + " " + mPluginContext.getResources().getString(R.string.channel_mode)  + " " + channelConfig.modemConfig);
            channelModePreference.setEntries(R.array.channel_modes);
            channelModePreference.setEntryValues(R.array.channel_mode_values);
            channelModePreference.setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
                channelConfig.modemConfig = Integer.parseInt((String) newValue);
                saveChannels();
                return true;
            });
            mCategoryChannels.addPreference(channelModePreference);

            PanSwitchPreference channelDefaultPreference = new PanSwitchPreference(mSettingsMenuContext);
            channelDefaultPreference.setTitle(channelConfig.name + " " + mPluginContext.getResources().getString(R.string.channel_default));
            channelDefaultPreference.setChecked(channelConfig.isDefault);
            channelDefaultPreference.setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
                channelConfig.isDefault = (boolean) newValue;
                for (ChannelConfig testChannelConfig : mChannelConfigs) {
                    if (testChannelConfig != channelConfig) {
                        testChannelConfig.isDefault = false;
                    }
                }
                saveChannels();
                return true;
            });
            mCategoryChannels.addPreference(channelDefaultPreference);
        }
    }

    private void saveChannels() {
        String channelJson;

        try {
            channelJson = mChannelJsonHelper.toJson(mChannelConfigs);
        } catch (ChannelJsonException e) {
            return;
        }

        mSharedPreferences.edit()
                .putString(PreferencesKeys.KEY_CHANNEL_DATA, channelJson)
                .apply();
    }
}
