CREATE TABLE roles (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name    VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE permissions (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code    VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

INSERT INTO roles (name) VALUES
    ('OWNER'), ('ADMIN'), ('MODERATOR'), ('MEMBER'), ('GUEST');

INSERT INTO permissions (code) VALUES
    ('WORKSPACE_EDIT'), ('WORKSPACE_DELETE'),
    ('CHANNEL_CREATE'), ('CHANNEL_EDIT'), ('CHANNEL_DELETE'),
    ('MEMBER_INVITE'), ('MEMBER_REMOVE'), ('MEMBER_BAN'), ('MEMBER_MANAGE_ROLES'),
    ('MESSAGE_DELETE_ANY'),
    ('FILE_UPLOAD');

-- OWNER: todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'OWNER';

-- ADMIN: todo excepto borrar el workspace
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN' AND p.code <> 'WORKSPACE_DELETE';

-- MODERATOR: moderación de contenido y miembros, sin gestión estructural
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MODERATOR' AND p.code IN ('MESSAGE_DELETE_ANY', 'MEMBER_REMOVE', 'MEMBER_BAN', 'FILE_UPLOAD');

-- MEMBER: solo subir archivos (el resto de acciones básicas de mensajería no requieren permiso explícito)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MEMBER' AND p.code = 'FILE_UPLOAD';

-- GUEST: ningún permiso adicional
