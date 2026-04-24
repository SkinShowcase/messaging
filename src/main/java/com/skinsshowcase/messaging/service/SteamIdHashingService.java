package com.skinsshowcase.messaging.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class SteamIdHashingService {

    public String sha256(String steamId) {
        if (steamId == null || steamId.isBlank()) {
            return null;
        }
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(steamId.trim().getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder(bytes.length * 2);
            for (var b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
