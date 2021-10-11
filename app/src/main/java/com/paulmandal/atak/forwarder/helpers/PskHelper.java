package com.paulmandal.atak.forwarder.helpers;

import android.util.Base64;

import com.paulmandal.atak.forwarder.ForwarderConstants;

import java.security.SecureRandom;

public class PskHelper {
    public byte[] genPskBytes() {
        SecureRandom sr = new SecureRandom();
        byte[] psk = new byte[ForwarderConstants.PSK_LENGTH];
        sr.nextBytes(psk);
        return psk;
    }

    public String genPsk() {
        return Base64.encodeToString(genPskBytes(), Base64.DEFAULT);
    }
}
