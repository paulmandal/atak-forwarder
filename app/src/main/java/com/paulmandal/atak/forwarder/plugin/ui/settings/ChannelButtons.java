package com.paulmandal.atak.forwarder.plugin.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.Preference;
import android.text.InputFilter;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.ImageView;

import com.atakmap.android.gui.PanEditTextPreference;
import com.atakmap.android.gui.PanListPreference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.meshtastic.DiscoveryBroadcastEventHandler;
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

    private static final int PSK_LENGTH = ForwarderConstants.PSK_LENGTH;

    private String mChannelName;
    private int mMode;
    private byte[] mPsk;

    public ChannelButtons(List<Destroyable> destroyables,
                          SharedPreferences sharedPreferences,
                          Context settingsMenuContext,
                          Context pluginContext,
                          DiscoveryBroadcastEventHandler discoveryBroadcastEventHandler,
                          HashHelper hashHelper,
                          PskHelper pskHelper,
                          QrHelper qrHelper,
                          Logger logger,
                          Preference channelName,
                          Preference channelMode,
                          Preference channelPsk,
                          Preference showChannelQr,
                          Preference scanChannelQr,
                          Preference saveChannelToFile,
                          Preference readChannelFromFile) {
        super(destroyables,
                sharedPreferences,
                new String[] {
                        PreferencesKeys.KEY_CHANNEL_NAME,
                        PreferencesKeys.KEY_CHANNEL_MODE,
                        PreferencesKeys.KEY_CHANNEL_PSK
                },
                new String[]{});
        PanEditTextPreference editTextPreferenceChannelName = (PanEditTextPreference) channelName;
        editTextPreferenceChannelName.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(11)
        }, false);

        PanListPreference listPreferenceChannelMode = (PanListPreference) channelMode;
        listPreferenceChannelMode.setEntries(R.array.channel_modes);
        listPreferenceChannelMode.setEntryValues(R.array.channel_mode_values);

        channelPsk.setOnPreferenceClickListener((Preference preference) -> {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(settingsMenuContext)
                    .setTitle(pluginContext.getResources().getString(R.string.warning))
                    .setMessage(pluginContext.getResources().getString(R.string.generate_psk_warning))
                    .setPositiveButton(pluginContext.getResources().getString(R.string.ok), (DialogInterface dialog, int whichButton) -> {
                        String pskBase64 = pskHelper.genPsk();

                        preference.getEditor().putString(PreferencesKeys.KEY_CHANNEL_PSK, pskBase64).commit();
                    })
                    .setNegativeButton(pluginContext.getResources().getString(R.string.cancel), (DialogInterface dialog, int whichButton) -> dialog.cancel());

            alertDialog.show();
            return true;
        });

        showChannelQr.setOnPreferenceClickListener((Preference preference) -> {
            byte[] channelNameBytes = mChannelName.getBytes();
            byte modemConfigByte = (byte) mMode;

            byte[] payload = new byte[mPsk.length + 1 + channelNameBytes.length];

            System.arraycopy(mPsk, 0, payload, 0, mPsk.length);
            payload[mPsk.length] = modemConfigByte;
            System.arraycopy(channelNameBytes, 0, payload, mPsk.length + 1, channelNameBytes.length);

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

                byte[] psk = new byte[PSK_LENGTH];
                byte[] channelNameBytes = new byte[resultBytes.length - PSK_LENGTH - 1];

                System.arraycopy(resultBytes, 0, psk, 0, PSK_LENGTH);
                int modemConfigValue = resultBytes[PSK_LENGTH];
                System.arraycopy(resultBytes, PSK_LENGTH + 1, channelNameBytes, 0, resultBytes.length - PSK_LENGTH - 1);

                preference.getEditor()
                        .putString(PreferencesKeys.KEY_CHANNEL_NAME, new String(channelNameBytes))
                        .putString(PreferencesKeys.KEY_CHANNEL_MODE, Integer.toString(modemConfigValue))
                        .putString(PreferencesKeys.KEY_CHANNEL_PSK, Base64.encodeToString(psk, Base64.DEFAULT))
                        .apply();

                discoveryBroadcastEventHandler.broadcastDiscoveryMessage(true);

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
    }

    @Override
    protected void updateSettings(SharedPreferences sharedPreferences) {
        mChannelName = sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_NAME, PreferencesDefaults.DEFAULT_CHANNEL_NAME);
        mMode = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_MODE, PreferencesDefaults.DEFAULT_CHANNEL_MODE));
        mPsk = Base64.decode(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_PSK, PreferencesDefaults.DEFAULT_CHANNEL_PSK), Base64.DEFAULT);
    }

    @Override
    protected void complexUpdate(SharedPreferences sharedPreferences, String key) {
        // Do nothing
    }
}
