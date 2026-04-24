package com.skinsshowcase.messaging.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@Getter
public class MessagingTcpProperties {

    private final int port;

    public MessagingTcpProperties(@Min(1) @Max(65535) @Value("${messaging.tcp.port}") int port) {
        this.port = port;
    }
}
