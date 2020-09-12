package com.paulmandal.atak.forwarder.plugin.ui.tabs;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.geeksville.mesh.MeshProtos;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.group.ChannelTracker;
import com.paulmandal.atak.forwarder.plugin.ui.QrHelper;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ChannelTab {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + ChannelTab.class.getSimpleName();

    private static final int PSK_LENGTH = Config.PSK_LENGTH;

    private Context mPluginContext;

    private CommHardware mCommHardware;

    private ChannelTracker mChannelTracker;
    private QrHelper mQrHelper;

    private RadioGroup mModemSettingRadioGroup;

    private TextView mChannelNameLabel;
    private EditText mChannelNameEditText;
    private ImageView mChannelQr;
    private FrameLayout mQrScannerContainer;
    private TextView mPskStatusTextView;

    private Button mShowOrHideQrButton;
    private Button mScanQrButton;
    private Button mEditOrSaveButton;

    private View.OnClickListener mShowQrOnClickListener;
    private View.OnClickListener mHideQrOnClickListener;

    private View.OnClickListener mEditChannelOnClickListener;
    private View.OnClickListener mSaveChannelOnClickListener;

    private byte[] mPsk;

    Map<Integer, MeshProtos.ChannelSettings.ModemConfig> mRadioButtonToModemSettingMap = new HashMap() {{
        put(R.id.radio_button_short_range, MeshProtos.ChannelSettings.ModemConfig.Bw125Cr45Sf128);
        put(R.id.radio_button_medium_range, MeshProtos.ChannelSettings.ModemConfig.Bw500Cr45Sf128);
        put(R.id.radio_button_long_range, MeshProtos.ChannelSettings.ModemConfig.Bw31_25Cr48Sf512);
        put(R.id.radio_button_very_long_range, MeshProtos.ChannelSettings.ModemConfig.Bw125Cr48Sf4096);
    }};

    private enum ScreenMode {
        DEFAULT,
        SCAN_QR,
        EDIT_CHANNEL,
        SHOW_QR
    }

    public ChannelTab(Context pluginContext,
                      CommHardware commHardware,
                      ChannelTracker channelTracker,
                      QrHelper qrHelper) {
        mPluginContext = pluginContext;
        mCommHardware = commHardware;
        mChannelTracker = channelTracker;
        mQrHelper = qrHelper;
    }

    public void init(View templateView) {
        mChannelNameLabel = templateView.findViewById(R.id.label_channel_name);
        mChannelNameEditText = templateView.findViewById(R.id.edittext_channel_name);
        mChannelQr = templateView.findViewById(R.id.channel_qr);
        mQrScannerContainer = templateView.findViewById(R.id.qr_scanner_container);
        mModemSettingRadioGroup = templateView.findViewById(R.id.radio_group_modem_setting);
        mPskStatusTextView = templateView.findViewById(R.id.textview_psk_status);

        /*
         * Show/Hide QR
         */
        mShowQrOnClickListener = (View v) -> {
            setMode(ScreenMode.SHOW_QR);

            byte[] channelName = mChannelTracker.getChannelName().getBytes();
            byte[] psk = mChannelTracker.getPsk();
            byte modemConfig = (byte) mChannelTracker.getModemConfig().getNumber();

            Log.e(TAG, "channelName.len: " + channelName.length);
            Log.e(TAG, "psk.len: " + psk.length);

            byte[] payload = new byte[psk.length + 1 + channelName.length];
            for (int i = 0; i < psk.length; i++) {
                payload[i] = psk[i];
            }
            payload[psk.length] = modemConfig;

            for (int i = psk.length + 1, j = 0; j < channelName.length; i++, j++) {
                payload[i] = channelName[j];
            }

            Log.e(TAG, " wrote bytes: " + QrHelper.toBinaryString(payload));
            Log.e(TAG, "  cname: " + new String(channelName) + ", psk: " + QrHelper.toBinaryString(psk));

            try {
                Bitmap bm = mQrHelper.encodeAsBitmap(payload);
                if (bm != null) {
                    mChannelQr.setImageBitmap(bm);
                }
            } catch (WriterException e) {
                e.printStackTrace();
            }

            mChannelQr.setVisibility(View.VISIBLE);

            Button b = (Button) v;
            b.setText(R.string.hide_channel_qr);
            v.setOnClickListener(mHideQrOnClickListener);
        };

        mHideQrOnClickListener = (View v) -> {
            setMode(ScreenMode.DEFAULT);

            mChannelQr.setVisibility(View.GONE);

            Button b = (Button) v;
            b.setText(R.string.show_channel_qr);
            v.setOnClickListener(mShowQrOnClickListener);
        };

        mShowOrHideQrButton = templateView.findViewById(R.id.button_show_or_hide_channel_qr);
        mShowOrHideQrButton.setOnClickListener(mShowQrOnClickListener);

        /*
         * Edit / Save Channel
         */
        mEditChannelOnClickListener = (View v) -> {
            setMode(ScreenMode.EDIT_CHANNEL);

            mChannelNameEditText.setText(mChannelTracker.getChannelName());
            mModemSettingRadioGroup.check(getRadioButtonForModemConfig(mChannelTracker.getModemConfig()));
            mPsk = mChannelTracker.getPsk();

            mPskStatusTextView.setText(R.string.reusing_old_psk);
            mPskStatusTextView.setTextColor(mPluginContext.getResources().getColor(R.color.red));

            Button b = (Button) v;
            b.setText(R.string.save);
            v.setOnClickListener(mSaveChannelOnClickListener);
        };

        mSaveChannelOnClickListener = (View v) -> {
            setMode(ScreenMode.DEFAULT);

            MeshProtos.ChannelSettings.ModemConfig modemConfig = mRadioButtonToModemSettingMap.get(mModemSettingRadioGroup.getCheckedRadioButtonId());

            mChannelTracker.clearData();
            mCommHardware.updateChannelSettings(mChannelNameEditText.getText().toString(), mPsk, modemConfig);

            Button b = (Button) v;
            b.setText(R.string.edit_channel);
            v.setOnClickListener(mEditChannelOnClickListener);
        };

        mEditOrSaveButton = templateView.findViewById(R.id.button_edit_or_save_channel);
        mEditOrSaveButton.setOnClickListener(mEditChannelOnClickListener);

        Button genPsk = templateView.findViewById(R.id.button_gen_psk);
        genPsk.setOnClickListener((View v) -> {
            SecureRandom random = new SecureRandom();
            byte[] psk = new byte[PSK_LENGTH];
            random.nextBytes(psk);

            mPsk = psk;

            mPskStatusTextView.setText(R.string.using_new_psk);
            mPskStatusTextView.setTextColor(mPluginContext.getResources().getColor(R.color.green));
        });

        /*
         * Edit Radio Button
         */
        mModemSettingRadioGroup = templateView.findViewById(R.id.radio_group_modem_setting);

        /*
         * Scan QR
         */
        mScanQrButton = templateView.findViewById(R.id.button_scan_channel_qr);
        mScanQrButton.setOnClickListener((View v) -> {
            setMode(ScreenMode.SCAN_QR);

            ZXingScannerView scannerView = new ZXingScannerView(mPluginContext);
            scannerView.setResultHandler((Result rawResult) -> {
                setMode(ScreenMode.DEFAULT);

                String resultText = rawResult.getText();
                byte[] resultBytes = Base64.decode(resultText, Base64.DEFAULT);

                byte[] psk = new byte[PSK_LENGTH];
                for (int i = 0; i < PSK_LENGTH; i++) {
                    psk[i] = resultBytes[i];
                }

                int modemConfigValue = resultBytes[PSK_LENGTH];

                byte[] channelNameBytes = new byte[resultBytes.length - PSK_LENGTH - 1];
                for (int i = PSK_LENGTH + 1, j = 0; i < resultBytes.length; i++, j++) {
                    channelNameBytes[j] = resultBytes[i];
                }
                MeshProtos.ChannelSettings.ModemConfig modemConfig = MeshProtos.ChannelSettings.ModemConfig.valueOf(modemConfigValue);

                mCommHardware.updateChannelSettings(new String(channelNameBytes), psk, modemConfig);
                Log.e(TAG, " read bytes: " + QrHelper.toBinaryString(resultBytes));
                Log.e(TAG, "  cname: " + new String(channelNameBytes) + ", psk: " + QrHelper.toBinaryString(psk));

                scannerView.stopCamera();
                mQrScannerContainer.removeView(scannerView);
            });
            mQrScannerContainer.addView(scannerView);
            scannerView.startCamera();
        });
    }

    private void setMode(ScreenMode mode) {
        switch (mode) {
            case DEFAULT:
                mChannelNameLabel.setVisibility(View.GONE);
                mChannelNameEditText.setVisibility(View.GONE);
                mChannelQr.setVisibility(View.GONE);
                mModemSettingRadioGroup.setVisibility(View.GONE);
                mPskStatusTextView.setVisibility(View.GONE);

                mShowOrHideQrButton.setVisibility(View.VISIBLE);
                mScanQrButton.setVisibility(View.VISIBLE);
                mEditOrSaveButton.setVisibility(View.VISIBLE);
                break;
            case SCAN_QR:
                mShowOrHideQrButton.setVisibility(View.GONE);
                mEditOrSaveButton.setVisibility(View.GONE);
                break;
            case SHOW_QR:
                mScanQrButton.setVisibility(View.GONE);
                mEditOrSaveButton.setVisibility(View.GONE);
                break;
            case EDIT_CHANNEL:
                mChannelNameLabel.setVisibility(View.VISIBLE);
                mChannelNameEditText.setVisibility(View.VISIBLE);
                mModemSettingRadioGroup.setVisibility(View.VISIBLE);
                mPskStatusTextView.setVisibility(View.VISIBLE);

                mShowOrHideQrButton.setVisibility(View.GONE);
                mScanQrButton.setVisibility(View.GONE);
                break;
        }
    }

    public int getRadioButtonForModemConfig(MeshProtos.ChannelSettings.ModemConfig modemConfig) {
        for (Map.Entry<Integer, MeshProtos.ChannelSettings.ModemConfig> entry : mRadioButtonToModemSettingMap.entrySet()) {
            if (entry.getValue().equals(modemConfig)) {
                return entry.getKey();
            }
        }
        return 0;
    }
}
