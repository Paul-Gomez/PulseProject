CREATE TABLE workspace_bans (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    banned_by    UUID NOT NULL REFERENCES users (id),
    reason       VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (workspace_id, user_id)
);

CREATE INDEX idx_workspace_bans_workspace_id ON workspace_bans (workspace_id);
