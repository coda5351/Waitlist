package com.waitlist.model;

import java.security.SecureRandom;

/**
 * Shared utility for generating the random 6‑character codes used by
 * {@link Entry} and {@link Account} (and potentially other entities).
 */
public final class CodeGenerator {
    private CodeGenerator() {
        // utility class
    }

    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom CODE_RANDOM = new SecureRandom();

    public static String generateCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int idx = CODE_RANDOM.nextInt(CODE_CHARS.length());
            sb.append(CODE_CHARS.charAt(idx));
        }
        return sb.toString();
    }
}
