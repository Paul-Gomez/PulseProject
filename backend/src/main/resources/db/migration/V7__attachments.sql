CREATE TABLE attachments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id   UUID NOT NULL REFERENCES channels (id) ON DELETE CASCADE,
    message_id   UUID REFERENCES messages (id) ON DELETE SET NULL,
    uploaded_by  UUID NOT NULL REFERENCES users (id),
    storage_key  VARCHAR(500) NOT NULL UNIQUE,
    file_name    VARCHAR(255) NOT NULL,
    mime_type    VARCHAR(100) NOT NULL,
    size_bytes   BIGINT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_attachments_message_id ON attachments (message_id);
CREATE INDEX idx_attachments_channel_id ON attachments (channel_id);
