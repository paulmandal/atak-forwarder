package com.paulmandal.atak.forwarder.helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class HashHelper {
    public String hashFromBytes(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            bytes = md.digest(bytes);

            Formatter formatter  = new Formatter();
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }

            String hash = formatter.toString();
            return hash.substring(hash.length() - 8);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
