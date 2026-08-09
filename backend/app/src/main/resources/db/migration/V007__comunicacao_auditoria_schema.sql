-- V007: Comunicação, Notificações e Auditoria Schema

CREATE TABLE communication (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_autor        UUID NOT NULL REFERENCES usuario(id),
    titulo          VARCHAR(200) NOT NULL,
    conteudo        TEXT NOT NULL,
    tipo            VARCHAR(20) NOT NULL,
    audiencia       JSONB NOT NULL DEFAULT '{}',
    published_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_communication_autor ON communication(id_autor);
CREATE INDEX idx_communication_published ON communication(published_at) WHERE published_at IS NOT NULL;

CREATE TABLE communication_delivery (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_communication    UUID NOT NULL REFERENCES communication(id) ON DELETE CASCADE,
    id_usuario          UUID NOT NULL REFERENCES usuario(id),
    canal               VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    delivered_at        TIMESTAMPTZ,
    read_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comm_delivery_communication ON communication_delivery(id_communication);
CREATE INDEX idx_comm_delivery_usuario ON communication_delivery(id_usuario);

CREATE TABLE notification_preference (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_usuario      UUID NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE CASCADE,
    email_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE outbox_event (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    event_type      VARCHAR(100) NOT NULL,
    aggregate_type  VARCHAR(60) NOT NULL,
    aggregate_id    UUID NOT NULL,
    payload         JSONB NOT NULL DEFAULT '{}',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count   INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_status ON outbox_event(status);
CREATE INDEX idx_outbox_next_attempt ON outbox_event(next_attempt_at) WHERE status = 'PENDING';

CREATE TABLE audit_log (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    id_ator     UUID,
    acao        VARCHAR(100) NOT NULL,
    alvo_tipo   VARCHAR(60),
    alvo_id     UUID,
    ip          VARCHAR(45),
    user_agent  TEXT,
    resultado   VARCHAR(20) NOT NULL,
    payload     JSONB NOT NULL DEFAULT '{}'
);

CREATE INDEX idx_audit_log_ator ON audit_log(id_ator);
CREATE INDEX idx_audit_log_acao ON audit_log(acao);
CREATE INDEX idx_audit_log_at ON audit_log(at);
CREATE INDEX idx_audit_log_alvo ON audit_log(alvo_tipo, alvo_id);
