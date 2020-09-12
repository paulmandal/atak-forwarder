package com.paulmandal.atak.forwarder.plugin.ui;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.paulmandal.atak.forwarder.Config;

public class QrHelper {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + QrHelper.class.getSimpleName();

    public static int COLOR_WHITE = 0xFFFFFFFF;
    public static int COLOR_BLACK = 0xFF000000;

    private static final int WIDTH = 512;

    public Bitmap encodeAsBitmap(byte[] input) throws WriterException {
        Log.e(TAG, " wrote bytes: " + toBinaryString(input));
        String base64 = Base64.encodeToString(input, Base64.DEFAULT);

        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(base64, BarcodeFormat.QR_CODE, WIDTH, WIDTH, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? COLOR_BLACK : COLOR_WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, WIDTH, 0, 0, w, h);
        return bitmap;
    }

    // TODO: move this or remove it
    public static String toBinaryString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            sb.append(" ");
        }
        return sb.toString();
    }
}
