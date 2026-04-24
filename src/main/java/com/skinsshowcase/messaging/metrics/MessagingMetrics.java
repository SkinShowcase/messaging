package com.skinsshowcase.messaging.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Метрики messaging: отправка сообщений, чтение истории и списка чатов.
 */
@Component
public class MessagingMetrics {

    private final MeterRegistry registry;

    public MessagingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordMessageSent(String channel) {
        registry.counter("messaging.messages.sent",
                "channel", channel).increment();
    }

    public void recordChatHistoryRequest() {
        registry.counter("messaging.chat.history.requests").increment();
    }

    public void recordChatListRequest() {
        registry.counter("messaging.chat.list.requests").increment();
    }
}
