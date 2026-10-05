CREATE TABLE conversations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    is_group         BOOLEAN NOT NULL DEFAULT FALSE,
    name             VARCHAR(100),
    -- Only set for 1:1 chats ("smallerId:biggerId"); the unique constraint stops two people
    -- who open the chat at the same moment from ending up with two conversations.
    direct_key       VARCHAR(80) UNIQUE,
    created_by       UUID NOT NULL REFERENCES users (id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE conversation_members (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    last_read_at    TIMESTAMPTZ,
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (conversation_id, user_id)
);

CREATE INDEX idx_conversation_members_user_id ON conversation_members (user_id);

CREATE TABLE direct_messages (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id   UUID NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    author_id         UUID NOT NULL REFERENCES users (id),
    content           TEXT NOT NULL,
    parent_message_id UUID REFERENCES direct_messages (id) ON DELETE SET NULL,
    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_direct_messages_conversation ON direct_messages (conversation_id, created_at);
