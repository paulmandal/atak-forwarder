package com.paulmandal.atak.forwarder.plugin.ui.tabs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.LifecycleOwner;

import com.geeksville.mesh.MeshProtos;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.ChannelTabViewModel;

import java.util.HashMap;
import java.util.Map;

public class ChannelTab extends RelativeLayout {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + ChannelTab.class.getSimpleName();

    private TextView mChannelName;
    private TextView mPskHash;
    private TextView mModemConfig;
    
    private TextView mChannelNameLabel;
    private EditText mChannelNameEditText;
    private RadioGroup mModemSettingRadioGroup;
    private TextView mPskStatusTextView;

    private ImageView mChannelQr;

    private FrameLayout mQrScannerContainer;

    private Button mScanQrButton;
    private Button mAbortQrScanButton;
    private Button mEditChannelButton;
    private Button mSaveChannelButton;
    private Button mShowQrButton;
    private Button mHideQrButton;
    private Button mGenPskButton;

    private Map<Integer, MeshProtos.ChannelSettings.ModemConfig> mRadioButtonToModemSettingMap = new HashMap() {{
        put(R.id.radio_button_short_range, MeshProtos.ChannelSettings.ModemConfig.Bw125Cr45Sf128);
        put(R.id.radio_button_medium_range, MeshProtos.ChannelSettings.ModemConfig.Bw500Cr45Sf128);
        put(R.id.radio_button_long_range, MeshProtos.ChannelSettings.ModemConfig.Bw31_25Cr48Sf512);
        put(R.id.radio_button_very_long_range, MeshProtos.ChannelSettings.ModemConfig.Bw125Cr48Sf4096);
    }};

    public ChannelTab(Context context) {
        this(context, null);
    }
    
    public ChannelTab(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public ChannelTab(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.channel_layout, this);

        mChannelName = findViewById(R.id.channel_name);
        mPskHash = findViewById(R.id.psk_hash);
        mModemConfig = findViewById(R.id.modem_config);
        
        mChannelNameLabel = findViewById(R.id.label_channel_name);
        mChannelNameEditText = findViewById(R.id.edittext_channel_name);
        mChannelQr = findViewById(R.id.channel_qr);
        mQrScannerContainer = findViewById(R.id.qr_scanner_container);
        mModemSettingRadioGroup = findViewById(R.id.radio_group_modem_setting);
        mPskStatusTextView = findViewById(R.id.textview_psk_status);
        mModemSettingRadioGroup = findViewById(R.id.radio_group_modem_setting);

        mScanQrButton = findViewById(R.id.button_scan_channel_qr);
        mAbortQrScanButton = findViewById(R.id.button_abort_scan_qr);
        mEditChannelButton = findViewById(R.id.button_edit_channel);
        mSaveChannelButton = findViewById(R.id.button_save_channel);
        mShowQrButton = findViewById(R.id.button_show_qr);
        mHideQrButton = findViewById(R.id.button_hide_qr);
        mGenPskButton = findViewById(R.id.button_gen_psk);
    }

    @SuppressLint("DefaultLocale")
    public void bind(LifecycleOwner lifecycleOwner,
                     ChannelTabViewModel channelTabViewModel,
                     Context pluginContext,
                     Context atakContext) {
        channelTabViewModel.getChannelName().observe(lifecycleOwner, channelName -> {
            mChannelName.setText(channelName != null ? String.format("#%s", channelName) : null);

            // Do not update channel info while editing
            if (channelTabViewModel.getScreenMode().getValue() == ChannelTabViewModel.ScreenMode.EDIT_CHANNEL) {
                return;
            }
            mChannelNameEditText.setText(channelName);
        });
        channelTabViewModel.getPskHash().observe(lifecycleOwner, pskHash -> mPskHash.setText(pskHash));
        channelTabViewModel.getModemConfig().observe(lifecycleOwner, modemConfig -> {
            mModemConfig.setText(modemConfig != null ? String.format("%d", modemConfig.getNumber()) : null);

            // Do not update channel info while editing
            if (channelTabViewModel.getScreenMode().getValue() == ChannelTabViewModel.ScreenMode.EDIT_CHANNEL) {
                return;
            }
            mModemSettingRadioGroup.check(getRadioButtonForModemConfig(modemConfig));
        });
        channelTabViewModel.getScreenMode().observe(lifecycleOwner, this::setScreenMode);
        channelTabViewModel.isPskFresh().observe(lifecycleOwner, isPskFresh -> {
            int textResId = isPskFresh ? R.string.using_new_psk : R.string.reusing_old_psk;
            int textColor = isPskFresh ? pluginContext.getResources().getColor(R.color.green) : pluginContext.getResources().getColor(R.color.red);
            mPskStatusTextView.setText(textResId);
            mPskStatusTextView.setTextColor(textColor);
        });
        channelTabViewModel.getChannelQr().observe(lifecycleOwner, channelQr -> mChannelQr.setImageBitmap(channelQr));

        mShowQrButton.setOnClickListener(v -> {
            if (mChannelName.getText().toString().equals("null")) {
                Toast.makeText(atakContext, "Channel settings not yet available, check in the Settings tab", Toast.LENGTH_SHORT).show();
                return;
            }

            channelTabViewModel.showQr();
        });

        mScanQrButton.setOnClickListener(v -> {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(atakContext)
                    .setTitle(pluginContext.getResources().getString(R.string.warning))
                    .setMessage(pluginContext.getResources().getString(R.string.start_qr_scan_dialog))
                    .setPositiveButton(pluginContext.getResources().getString(R.string.ok), (DialogInterface dialog, int whichButton) -> channelTabViewModel.startQrScan(mQrScannerContainer))
                    .setNegativeButton(pluginContext.getResources().getString(R.string.cancel), (DialogInterface dialog, int whichButton) -> dialog.cancel());

            alertDialog.show();
        });

        mAbortQrScanButton.setOnClickListener(v -> channelTabViewModel.abortQrScan(mQrScannerContainer));
        
        mHideQrButton.setOnClickListener(v -> channelTabViewModel.hideQr());

        mEditChannelButton.setOnClickListener(v -> {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(atakContext)
                    .setTitle(pluginContext.getResources().getString(R.string.warning))
                    .setMessage(pluginContext.getResources().getString(R.string.start_channel_edit_dialog))
                    .setPositiveButton(pluginContext.getResources().getString(R.string.ok), (DialogInterface dialog, int whichButton) -> channelTabViewModel.startEditChannel())
                    .setNegativeButton(pluginContext.getResources().getString(R.string.cancel), (DialogInterface dialog, int whichButton) -> dialog.cancel());

            alertDialog.show();
        });

        mGenPskButton.setOnClickListener(v -> channelTabViewModel.genPsk());

        mSaveChannelButton.setOnClickListener(v -> {
            String channelName = mChannelNameEditText.getText().toString();
            MeshProtos.ChannelSettings.ModemConfig modemConfig = mRadioButtonToModemSettingMap.get(mModemSettingRadioGroup.getCheckedRadioButtonId());

            channelTabViewModel.saveChannelSettings(channelName, modemConfig);
            Toast.makeText(atakContext, "Saved channel settings", Toast.LENGTH_SHORT).show();
        });
    }

    private void setScreenMode(ChannelTabViewModel.ScreenMode screenMode) {
        switch (screenMode) {
            case DEFAULT:
                mAbortQrScanButton.setVisibility(INVISIBLE);
                mChannelNameLabel.setVisibility(INVISIBLE);
                mChannelNameEditText.setVisibility(INVISIBLE);
                mChannelQr.setVisibility(INVISIBLE);
                mModemSettingRadioGroup.setVisibility(INVISIBLE);
                mPskStatusTextView.setVisibility(INVISIBLE);
                mHideQrButton.setVisibility(INVISIBLE);
                mSaveChannelButton.setVisibility(INVISIBLE);
                mGenPskButton.setVisibility(INVISIBLE);

                mChannelName.setVisibility(VISIBLE);
                mPskHash.setVisibility(VISIBLE);
                mModemConfig.setVisibility(VISIBLE);

                mShowQrButton.setVisibility(VISIBLE);
                mScanQrButton.setVisibility(VISIBLE);
                mEditChannelButton.setVisibility(VISIBLE);
                break;
            case SCAN_QR:
                mAbortQrScanButton.setVisibility(VISIBLE);
                
                mShowQrButton.setVisibility(INVISIBLE);
                mHideQrButton.setVisibility(INVISIBLE);
                mEditChannelButton.setVisibility(INVISIBLE);
                mSaveChannelButton.setVisibility(INVISIBLE);
                break;
            case SHOW_QR:
                mChannelQr.setVisibility(VISIBLE);
                mHideQrButton.setVisibility(VISIBLE);

                mShowQrButton.setVisibility(INVISIBLE);
                mScanQrButton.setVisibility(INVISIBLE);
                mEditChannelButton.setVisibility(INVISIBLE);
                mSaveChannelButton.setVisibility(INVISIBLE);
                break;
            case EDIT_CHANNEL:
                mChannelNameLabel.setVisibility(VISIBLE);
                mChannelNameEditText.setVisibility(VISIBLE);
                mModemSettingRadioGroup.setVisibility(VISIBLE);
                mPskStatusTextView.setVisibility(VISIBLE);
                mGenPskButton.setVisibility(VISIBLE);
                mSaveChannelButton.setVisibility(VISIBLE);

                mEditChannelButton.setVisibility(INVISIBLE);
                mShowQrButton.setVisibility(INVISIBLE);
                mHideQrButton.setVisibility(INVISIBLE);
                mScanQrButton.setVisibility(INVISIBLE);
                break;
        }
    }

    private int getRadioButtonForModemConfig(MeshProtos.ChannelSettings.ModemConfig modemConfig) {
        for (Map.Entry<Integer, MeshProtos.ChannelSettings.ModemConfig> entry : mRadioButtonToModemSettingMap.entrySet()) {
            if (entry.getValue().equals(modemConfig)) {
                return entry.getKey();
            }
        }
        return 0;
    }
}
