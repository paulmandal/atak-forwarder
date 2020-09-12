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
import android.widget.TextView;

import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.group.ChannelTracker;
import com.paulmandal.atak.forwarder.plugin.ui.QrHelper;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ChannelTab {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + ChannelTab.class.getSimpleName();

    private Context mAtakContext;

    private CommHardware mCommHardware;

    private ChannelTracker mChannelTracker;
    private QrHelper mQrHelper;

    private TextView mChannelNameLabel;
    private EditText mChannelNameEditText;
    private ImageView mChannelQr;
    private FrameLayout mQrScannerContainer;

    private Button mShowOrHideQrButton;
    private Button mScanQrButton;
    private Button mEditOrSaveButton;

    private View.OnClickListener mShowQrOnClickListener;
    private View.OnClickListener mHideQrOnClickListener;

    private View.OnClickListener mEditChannelOnClickListener;
    private View.OnClickListener mSaveChannelOnClickListener;

    private enum ScreenMode {
        DEFAULT,
        SCAN_QR,
        EDIT_CHANNEL,
        SHOW_QR
    }

    public ChannelTab(Context atakContext,
                      CommHardware commHardware,
                      ChannelTracker channelTracker,
                      QrHelper qrHelper) {
        mAtakContext = atakContext;
        mCommHardware = commHardware;
        mChannelTracker = channelTracker;
        mQrHelper = qrHelper;
    }

    public void init(View templateView) {
        mChannelNameLabel = templateView.findViewById(R.id.label_channel_name);
        mChannelNameEditText = templateView.findViewById(R.id.edittext_channel_name);
        mChannelQr = templateView.findViewById(R.id.channel_qr);
        mQrScannerContainer = templateView.findViewById(R.id.qr_scanner_container);

        /*
         * Show/Hide QR
         */
        mShowQrOnClickListener = (View v) -> {
            setMode(ScreenMode.SHOW_QR);

            try {
                Bitmap bm = mQrHelper.encodeAsBitmap(mChannelTracker.getPsk());
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

            Button b = (Button) v;
            b.setText(R.string.save);
            v.setOnClickListener(mSaveChannelOnClickListener);
        };

        mSaveChannelOnClickListener = (View v) -> {
            setMode(ScreenMode.DEFAULT);

            // TODO: store settings

            Button b = (Button) v;
            b.setText(R.string.edit_channel);
            v.setOnClickListener(mEditChannelOnClickListener);
        };

        mEditOrSaveButton = templateView.findViewById(R.id.button_edit_or_save_channel);
        mEditOrSaveButton.setOnClickListener(mEditChannelOnClickListener);

        /*
         * Scan QR
         */
        mScanQrButton = templateView.findViewById(R.id.button_scan_channel_qr);
        mScanQrButton.setOnClickListener((View v) -> {
            setMode(ScreenMode.SCAN_QR);

            ZXingScannerView scannerView = new ZXingScannerView(mAtakContext);
            scannerView.setResultHandler((Result rawResult) -> {
                setMode(ScreenMode.DEFAULT);

                // Do something with the result here
                // TODO: store settings
                String resultText = rawResult.getText();
                byte[] resultBytes = Base64.decode(resultText, Base64.DEFAULT);
                Log.e(TAG, " read bytes: " + QrHelper.toBinaryString(resultBytes));
                Log.e(TAG, rawResult.getText()); // Prints scan results
                Log.e(TAG, rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode, pdf417 etc.)

                // If you would like to resume scanning, call this method below:
                scannerView.stopCamera();
                mQrScannerContainer.removeView(scannerView);
            }); // Register ourselves as a handler for scan results.
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
                mShowOrHideQrButton.setVisibility(View.GONE);
                mScanQrButton.setVisibility(View.GONE);
                break;
        }
    }

}
