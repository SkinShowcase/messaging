-- Идемпотентно для повторного запуска (одна БД для нескольких сервисов / пересоздание контейнеров).
CREATE TABLE IF NOT EXISTS message (
    id          UUID PRIMARY KEY,
    sender_id   VARCHAR(32) NOT NULL,
    recipient_id VARCHAR(32) NOT NULL,
    text        VARCHAR(4096) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_message_sender_recipient ON message (sender_id, recipient_id);
CREATE INDEX IF NOT EXISTS idx_message_recipient_sender ON message (recipient_id, sender_id);
CREATE INDEX IF NOT EXISTS idx_message_created_at ON message (created_at);
