package com.skinsshowcase.messaging;

import com.skinsshowcase.messaging.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        JwtProperties.class,
        com.skinsshowcase.messaging.config.AdminApiProperties.class,
        com.skinsshowcase.messaging.config.MessageCryptoProperties.class,
        com.skinsshowcase.messaging.config.TransportSecurityProperties.class
})
public class MessagingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagingApplication.class, args);
    }
}
