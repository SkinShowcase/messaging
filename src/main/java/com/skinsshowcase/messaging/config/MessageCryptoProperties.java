package com.skinsshowcase.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.message-crypto")
public class MessageCryptoProperties {

    /**
     * Base64-encoded 256-bit key (32 bytes) for AES.
     */
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
