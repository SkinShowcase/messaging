package com.skinsshowcase.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.transport-security")
public class TransportSecurityProperties {

    private boolean requireSecureTransport = true;

    public boolean isRequireSecureTransport() {
        return requireSecureTransport;
    }

    public void setRequireSecureTransport(boolean requireSecureTransport) {
        this.requireSecureTransport = requireSecureTransport;
    }
}
