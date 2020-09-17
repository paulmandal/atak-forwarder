package com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.geeksville.mesh.MeshProtos;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.channel.ChannelTracker;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.plugin.ui.QrHelper;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.HashHelper;

import java.security.SecureRandom;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ChannelTabViewModel implements MeshtasticCommHardware.ChannelSettingsListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + ChannelTabViewModel.class.getSimpleName();

    public enum ScreenMode {
        DEFAULT,
        SCAN_QR,
        EDIT_CHANNEL,
        SHOW_QR
    }

    private static final int PSK_LENGTH = Config.PSK_LENGTH;

    private Context mPluginContext;
    private Context mAtakContext;

    private CommHardware mCommHardware;

    private ChannelTracker mChannelTracker;
    private QrHelper mQrHelper;
    private HashHelper mHashHelper;

    private MutableLiveData<ScreenMode> mScreenMode = new MutableLiveData<>();
    private MutableLiveData<String> mChannelName = new MutableLiveData<>();
    private MutableLiveData<Byte[]> mPsk = new MutableLiveData<>();
    private MutableLiveData<MeshProtos.ChannelSettings.ModemConfig> mModemConfig = new MutableLiveData<>();
    private MutableLiveData<String> mPskHash = new MutableLiveData<>();
    private MutableLiveData<Boolean> mIsPskFresh = new MutableLiveData<>();
    private MutableLiveData<Bitmap> mChannelQr = new MutableLiveData<>();

    public ChannelTabViewModel(Context pluginContext,
                               Context atakContext,
                               MeshtasticCommHardware commHardware,
                               ChannelTracker channelTracker,
                               QrHelper qrHelper,
                               HashHelper hashHelper) {
        mPluginContext = pluginContext;
        mAtakContext = atakContext;
        mCommHardware = commHardware;
        mChannelTracker = channelTracker;
        mQrHelper = qrHelper;
        mHashHelper = hashHelper;

        mIsPskFresh.setValue(false);
        commHardware.setChannelSettingsListener(this);
        mScreenMode.setValue(ScreenMode.DEFAULT);
    }

    @Override
    public void onChannelSettingsUpdated(String channelName, byte[] psk, MeshProtos.ChannelSettings.ModemConfig modemConfig) {
        Log.d(TAG, "onChannelSettingsUpdated: " + channelName);
        Byte[] pskByte = null;
        if (psk != null) {
            pskByte = new Byte[psk.length];
            for (int i = 0 ; i < psk.length ; i++) {
                pskByte[i] = psk[i];
            }

            byte[] channelNameBytes = channelName.getBytes();
            byte modemConfigByte = (byte) modemConfig.getNumber();

            byte[] payload = new byte[psk.length + 1 + channelNameBytes.length];

            System.arraycopy(psk, 0, payload, 0, psk.length);
            payload[psk.length] = modemConfigByte;
            System.arraycopy(channelNameBytes, 0, payload, psk.length + 1, channelNameBytes.length);

            try {
                Bitmap bm = mQrHelper.encodeAsBitmap(payload);
                if (bm != null) {
                    mChannelQr.setValue(bm);
                }
            } catch (WriterException e) {
                e.printStackTrace();
            }
        }

        mChannelName.setValue(channelName);
        mPsk.setValue(pskByte);
        mModemConfig.setValue(modemConfig);
        mPskHash.setValue(mHashHelper.hashFromBytes(psk));
   }

    @NonNull
    public LiveData<ScreenMode> getScreenMode() {
        return mScreenMode;
    }

    @Nullable
    public LiveData<String> getChannelName() {
        return mChannelName;
    }

    @Nullable
    public LiveData<MeshProtos.ChannelSettings.ModemConfig> getModemConfig() {
        return mModemConfig;
    }

    @Nullable
    public LiveData<String> getPskHash() {
        return mPskHash;
    }

    @NonNull
    public LiveData<Boolean> isPskFresh() {
        return mIsPskFresh;
    }

    @Nullable
    public LiveData<Bitmap> getChannelQr() {
        return mChannelQr;
    }

    public void showQr(ImageView channelQr) {
        mScreenMode.setValue(ScreenMode.SHOW_QR);
    }

    public void startQrScan(FrameLayout qrScannerContainer) {
        mScreenMode.setValue(ScreenMode.SCAN_QR);

        ZXingScannerView scannerView = new ZXingScannerView(mPluginContext);
        scannerView.setResultHandler((Result rawResult) -> {
            mScreenMode.setValue((ScreenMode.DEFAULT));

            String resultText = rawResult.getText();
            byte[] resultBytes = Base64.decode(resultText, Base64.DEFAULT);

            byte[] psk = new byte[PSK_LENGTH];
            byte[] channelNameBytes = new byte[resultBytes.length - PSK_LENGTH - 1];

            System.arraycopy(resultBytes, 0, psk, 0, PSK_LENGTH);
            int modemConfigValue = resultBytes[PSK_LENGTH];
            System.arraycopy(resultBytes, PSK_LENGTH + 1, channelNameBytes, 0, resultBytes.length - PSK_LENGTH - 1);

            MeshProtos.ChannelSettings.ModemConfig modemConfig = MeshProtos.ChannelSettings.ModemConfig.valueOf(modemConfigValue);

            Toast.makeText(mAtakContext, "Updated channel settings from QR", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Updating channel settings: " + new String(channelNameBytes) + ", " + modemConfig + ", " + mHashHelper.hashFromBytes(psk));

            mCommHardware.updateChannelSettings(new String(channelNameBytes), psk, modemConfig);
            mCommHardware.broadcastDiscoveryMessage();

            scannerView.stopCamera();
            qrScannerContainer.removeView(scannerView);
        });
        qrScannerContainer.addView(scannerView);
        scannerView.startCamera();
    }

    public void hideQr() {
        mScreenMode.setValue(ScreenMode.DEFAULT);
    }

    public void startEditChannel() {
        mScreenMode.setValue(ScreenMode.EDIT_CHANNEL);
    }

    public void genPsk() {
        SecureRandom random = new SecureRandom();
        byte[] psk = new byte[PSK_LENGTH];
        random.nextBytes(psk);

        Byte[] pskByte = new Byte[PSK_LENGTH];
        System.arraycopy(psk, 0, pskByte, 0, PSK_LENGTH);

        mPsk.setValue(pskByte);
        mIsPskFresh.setValue(true);
        mPskHash.setValue(mHashHelper.hashFromBytes(psk));
    }

    public void saveChannelSettings(String channelName, MeshProtos.ChannelSettings.ModemConfig modemConfig) {
        mScreenMode.setValue(ScreenMode.DEFAULT);

        Byte[] pskByte = mPsk.getValue();
        byte[] psk = new byte[pskByte.length];
        System.arraycopy(pskByte, 0, psk, 0, pskByte.length);

        mChannelTracker.clearData();
        mCommHardware.updateChannelSettings(channelName, psk, modemConfig);
        mCommHardware.broadcastDiscoveryMessage();
    }

    public void clearData() {
        mScreenMode.postValue(ScreenMode.DEFAULT);
        mChannelName.postValue(null);
        mPsk.postValue(null);
        mModemConfig.postValue(null);
        mIsPskFresh.postValue(false);
    }
}
