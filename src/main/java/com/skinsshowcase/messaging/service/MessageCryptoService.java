package com.skinsshowcase.messaging.service;

import com.skinsshowcase.messaging.config.MessageCryptoProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class MessageCryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private final byte[] keyBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    public MessageCryptoService(MessageCryptoProperties properties) {
        var keyRaw = properties.getKey();
        if (keyRaw == null || keyRaw.isBlank()) {
            throw new IllegalStateException("app.message-crypto.key must be configured");
        }
        var decoded = Base64.getDecoder().decode(keyRaw.trim());
        if (decoded.length != 32) {
            throw new IllegalStateException("app.message-crypto.key must be a base64-encoded 32-byte key");
        }
        this.keyBytes = decoded;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            var iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            var keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            var encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            var payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt chat message", e);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        try {
            var payload = Base64.getDecoder().decode(ciphertext);
            if (payload.length <= GCM_IV_BYTES) {
                throw new IllegalArgumentException("Ciphertext payload is too short");
            }
            var iv = new byte[GCM_IV_BYTES];
            var encrypted = new byte[payload.length - GCM_IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, GCM_IV_BYTES);
            System.arraycopy(payload, GCM_IV_BYTES, encrypted, 0, encrypted.length);

            var cipher = Cipher.getInstance(TRANSFORMATION);
            var keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            var plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Backward compatibility: historical rows may contain plaintext before encryption rollout.
            return ciphertext;
        }
    }
}
