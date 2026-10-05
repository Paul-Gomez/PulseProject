CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- No foreign keys: an audit trail has to outlive the things it talks about (a deleted channel, a banned user).
    workspace_id   UUID NOT NULL,
    actor_id       UUID,
    actor_username VARCHAR(50),
    action         VARCHAR(50) NOT NULL,
    resource_type  VARCHAR(30) NOT NULL,
    resource_id    UUID,
    details        VARCHAR(500),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_workspace_created ON audit_logs (workspace_id, created_at DESC);

INSERT INTO permissions (code) VALUES ('AUDIT_VIEW');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE p.code = 'AUDIT_VIEW' AND r.name IN ('OWNER', 'ADMIN');
