package com.ulsee.mower.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {

    public static String convertMD5(String inStr) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(inStr.getBytes(), 0, inStr.length());
            String secured = new BigInteger(1, digest.digest()).toString(16);
            return secured.toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
