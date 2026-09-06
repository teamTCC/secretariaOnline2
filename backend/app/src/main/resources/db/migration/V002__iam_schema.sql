-- V002: IAM Module Schema
-- Tables: usuario, role, authority, role_authority, usuario_role,
--         refresh_token, jti_blacklist, password_history

CREATE TABLE authority (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    descricao   VARCHAR(200) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE role (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    code        VARCHAR(50) NOT NULL UNIQUE,
    descricao   VARCHAR(200) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE role_authority (
    id_role         UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    id_authority    UUID NOT NULL REFERENCES authority(id) ON DELETE CASCADE,
    PRIMARY KEY (id_role, id_authority)
);

CREATE TABLE usuario (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    nome                VARCHAR(200) NOT NULL,
    email               CITEXT NOT NULL UNIQUE,
    grr                 VARCHAR(20) UNIQUE,
    senha_hash          VARCHAR(300) NOT NULL,
    senha_alterada      BOOLEAN NOT NULL DEFAULT FALSE,
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    bloqueado_ate       TIMESTAMPTZ,
    tentativas_falhas   INTEGER NOT NULL DEFAULT 0,
    metadata            JSONB NOT NULL DEFAULT '{}',
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_usuario_grr ON usuario(grr) WHERE grr IS NOT NULL;
CREATE INDEX idx_usuario_nome_trgm ON usuario USING GIN(nome gin_trgm_ops);

CREATE TABLE usuario_role (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_usuario  UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    id_role     UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    escopo      JSONB NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (id_usuario, id_role)
);

CREATE INDEX idx_usuario_role_usuario ON usuario_role(id_usuario);

CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    value       VARCHAR(128) NOT NULL UNIQUE,
    id_usuario  UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_token_usuario ON refresh_token(id_usuario);
CREATE INDEX idx_refresh_token_expires ON refresh_token(expires_at) WHERE revoked_at IS NULL;

CREATE TABLE jti_blacklist (
    jti         VARCHAR(100) PRIMARY KEY,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jti_blacklist_expires ON jti_blacklist(expires_at);

CREATE TABLE password_history (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_usuario  UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    senha_hash  VARCHAR(300) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_history_usuario ON password_history(id_usuario);
