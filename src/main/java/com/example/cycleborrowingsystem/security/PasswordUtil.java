package com.example.cycleborrowingsystem.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtil {
    private static final SecureRandom RNG = new SecureRandom();
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    private PasswordUtil() {}

    public static String generateSalt() {
        byte[] s = new byte[16];
        RNG.nextBytes(s);
        return Base64.getEncoder().encodeToString(s);
    }

    public static String hashPassword(char[] password, String salt) throws Exception {
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        PBEKeySpec spec = new PBEKeySpec(password, saltBytes, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyPassword(char[] password, String salt, String expectedHash) {
        try {
            String h = hashPassword(password, salt);
            return h.equals(expectedHash);
        } catch (Exception e) {
            return false;
        }
    }
}
