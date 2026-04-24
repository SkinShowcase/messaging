ALTER TABLE message
    ADD COLUMN IF NOT EXISTS sender_id_enc TEXT,
    ADD COLUMN IF NOT EXISTS recipient_id_enc TEXT;
