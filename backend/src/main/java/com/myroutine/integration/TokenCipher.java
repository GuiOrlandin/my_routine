package com.myroutine.integration;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM para refresh tokens em {@code google_tokens}.
 * Formato persistido: Base64(IV 12 bytes ‖ ciphertext ‖ tag).
 */
public class TokenCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final String base64Key;
    private final SecureRandom secureRandom = new SecureRandom();
    private volatile SecretKey secretKey;

    public TokenCipher(String base64Key) {
        this.base64Key = base64Key;
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, resolveKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao criptografar refresh token", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, resolveKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Falha ao descriptografar refresh token", e);
        }
    }

    private SecretKey resolveKey() {
        SecretKey existing = secretKey;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (secretKey == null) {
                if (base64Key == null || base64Key.isBlank()) {
                    throw new IllegalStateException(
                            "google.token-encryption-key (GOOGLE_TOKEN_ENCRYPTION_KEY) é obrigatória");
                }
                byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim());
                if (keyBytes.length != 32) {
                    throw new IllegalStateException(
                            "GOOGLE_TOKEN_ENCRYPTION_KEY deve ser Base64 de exatamente 32 bytes (AES-256)");
                }
                secretKey = new SecretKeySpec(keyBytes, "AES");
            }
            return secretKey;
        }
    }
}
