package com.mobility.hack.security;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {

    private final TokenManager tokenManager;

    public CryptoManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public String encrypt(String plainText) throws Exception {
        String weakKey = tokenManager.getWeakKey();
        if (weakKey == null) {
            throw new IllegalStateException("Encryption key is not available.");
        }

        SecretKeySpec secretKey = new SecretKeySpec(weakKey.getBytes("UTF-8"), "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
    }

    public String decrypt(String encryptedText) throws Exception {
        String weakKey = tokenManager.getWeakKey();
        if (weakKey == null) {
            throw new IllegalStateException("Decryption key is not available.");
        }

        SecretKeySpec secretKey = new SecretKeySpec(weakKey.getBytes("UTF-8"), "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decodedBytes = Base64.decode(encryptedText, Base64.NO_WRAP);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, "UTF-8");
    }
}
