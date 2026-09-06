-- V012: Service Records, FAQ, Support Tickets, FCM Device Tokens

-- Registro de atendimentos (secretaria registra presença física ou atendimento)
CREATE TABLE service_record (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_secretario   UUID NOT NULL REFERENCES usuario(id),
    id_aluno        UUID NOT NULL REFERENCES usuario(id),
    tipo            VARCHAR(50) NOT NULL DEFAULT 'PRESENCIAL',
    assunto         VARCHAR(300) NOT NULL,
    descricao       TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_service_record_aluno ON service_record(id_aluno);
CREATE INDEX idx_service_record_secretario ON service_record(id_secretario);
CREATE INDEX idx_service_record_created ON service_record(created_at DESC);

-- FAQ items
CREATE TABLE faq_item (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    categoria   VARCHAR(100) NOT NULL,
    pergunta    VARCHAR(500) NOT NULL,
    resposta    TEXT NOT NULL,
    ordem       INTEGER NOT NULL DEFAULT 0,
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_faq_ativo ON faq_item(ativo) WHERE ativo = TRUE;
CREATE INDEX idx_faq_categoria ON faq_item(categoria);
-- Full text search support
CREATE INDEX idx_faq_pergunta_trgm ON faq_item USING GIN(pergunta gin_trgm_ops);

-- Tickets de suporte
CREATE TABLE support_ticket (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_usuario  UUID NOT NULL REFERENCES usuario(id),
    assunto     VARCHAR(300) NOT NULL,
    descricao   TEXT NOT NULL,
    estado      VARCHAR(20) NOT NULL DEFAULT 'ABERTO',
    resposta    TEXT,
    id_atendente UUID REFERENCES usuario(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_support_ticket_usuario ON support_ticket(id_usuario);
CREATE INDEX idx_support_ticket_estado ON support_ticket(estado);

-- Tokens FCM para push notifications
CREATE TABLE device_fcm_token (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_usuario  UUID NOT NULL REFERENCES usuario(id),
    fcm_token   VARCHAR(500) NOT NULL,
    plataforma  VARCHAR(20) NOT NULL DEFAULT 'ANDROID',
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (id_usuario, fcm_token)
);

CREATE INDEX idx_fcm_token_usuario ON device_fcm_token(id_usuario);
CREATE INDEX idx_fcm_token_ativo ON device_fcm_token(id_usuario) WHERE ativo = TRUE;
