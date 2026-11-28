package com.kaleidoscopedoll.network;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public final class DollPackEncryption {
    public static final int KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BYTES = 16;
    public static final int OVERHEAD = NONCE_BYTES + TAG_BYTES;
    private static final SecureRandom RANDOM = new SecureRandom();

    private DollPackEncryption() {
    }

    public static byte[] newKey() {
        byte[] key = new byte[KEY_BYTES];
        RANDOM.nextBytes(key);
        return key;
    }

    public static byte[] seal(byte[] archive, byte[] key) throws IOException {
        byte[] nonce = new byte[NONCE_BYTES];
        RANDOM.nextBytes(nonce);
        byte[] output = new byte[archive.length + OVERHEAD];
        System.arraycopy(nonce, 0, output, 0, NONCE_BYTES);
        try {
            cipher(Cipher.ENCRYPT_MODE, key, nonce).doFinal(archive, 0, archive.length, output, NONCE_BYTES);
            return output;
        } catch (GeneralSecurityException e) {
            throw new IOException("Could not encrypt server doll ZIP", e);
        }
    }

    public static byte[] open(byte[] archive, byte[] key) throws IOException {
        if (archive.length < OVERHEAD) {
            throw new IOException("Truncated encrypted server doll ZIP");
        }
        try {
            return cipher(Cipher.DECRYPT_MODE, key, archive).doFinal(archive, NONCE_BYTES, archive.length - NONCE_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IOException("Server doll ZIP authentication failed", e);
        }
    }

    private static Cipher cipher(int mode, byte[] key, byte[] nonce) throws GeneralSecurityException {
        if (key.length != KEY_BYTES) {
            throw new GeneralSecurityException("Invalid server doll encryption key size");
        }
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BYTES * 8, nonce, 0, NONCE_BYTES));
        return cipher;
    }
}
