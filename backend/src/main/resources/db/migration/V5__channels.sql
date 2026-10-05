CREATE TABLE channels (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    name         VARCHAR(80) NOT NULL,
    type         VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    is_private   BOOLEAN NOT NULL DEFAULT FALSE,
    created_by   UUID NOT NULL REFERENCES users (id),
    position     INTEGER NOT NULL DEFAULT 0,
    archived_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (workspace_id, name)
);

CREATE TABLE channel_members (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL REFERENCES channels (id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (channel_id, user_id)
);

CREATE INDEX idx_channels_workspace_id ON channels (workspace_id);
CREATE INDEX idx_channel_members_channel_id ON channel_members (channel_id);
CREATE INDEX idx_channel_members_user_id ON channel_members (user_id);
