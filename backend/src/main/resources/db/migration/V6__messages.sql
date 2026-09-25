CREATE TABLE messages (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id        UUID NOT NULL REFERENCES channels (id) ON DELETE CASCADE,
    author_id         UUID NOT NULL REFERENCES users (id),
    content           TEXT NOT NULL,
    parent_message_id UUID REFERENCES messages (id) ON DELETE SET NULL,
    edited_at         TIMESTAMPTZ,
    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_channel_id ON messages (channel_id, created_at);
CREATE INDEX idx_messages_parent_id ON messages (parent_message_id);

CREATE TABLE message_reactions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    emoji      VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (message_id, user_id, emoji)
);

CREATE TABLE message_mentions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id         UUID NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    mentioned_user_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_message_mentions_user_id ON message_mentions (mentioned_user_id);
