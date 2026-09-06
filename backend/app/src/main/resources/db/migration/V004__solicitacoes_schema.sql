-- V004: Solicitações Module Schema (Generic Workflow Engine)

CREATE TABLE request_type (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    code            VARCHAR(60) NOT NULL UNIQUE,
    descricao       VARCHAR(200) NOT NULL,
    form_schema     JSONB NOT NULL DEFAULT '{}',
    workflow_json   JSONB NOT NULL DEFAULT '{}',
    prazo_dias      INTEGER NOT NULL DEFAULT 10,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE request (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    numero_anual    INTEGER NOT NULL,
    ano             SMALLINT NOT NULL,
    id_request_type UUID NOT NULL REFERENCES request_type(id),
    request_type_code VARCHAR(60) NOT NULL,
    id_solicitante  UUID NOT NULL REFERENCES usuario(id),
    id_curso        UUID NOT NULL REFERENCES curso(id),
    estado          VARCHAR(30) NOT NULL DEFAULT 'ABERTA',
    dados           JSONB NOT NULL DEFAULT '{}',
    parecer         TEXT,
    prazo_em        TIMESTAMPTZ,
    concluded_at    TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (numero_anual, ano, id_curso)
);

CREATE INDEX idx_request_solicitante ON request(id_solicitante);
CREATE INDEX idx_request_tipo ON request(id_request_type);
CREATE INDEX idx_request_curso ON request(id_curso);
CREATE INDEX idx_request_estado ON request(estado) WHERE deleted_at IS NULL;
CREATE INDEX idx_request_curso_estado ON request(id_curso, estado) WHERE deleted_at IS NULL;
CREATE INDEX idx_request_prazo ON request(prazo_em) WHERE concluded_at IS NULL AND deleted_at IS NULL;
CREATE INDEX idx_request_dados ON request USING GIN(dados);

CREATE TABLE request_event (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_request      UUID NOT NULL REFERENCES request(id),
    tipo            VARCHAR(50) NOT NULL,
    estado_anterior VARCHAR(30) NOT NULL,
    estado_novo     VARCHAR(30) NOT NULL,
    id_ator         UUID NOT NULL REFERENCES usuario(id),
    parecer         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_request_event_request ON request_event(id_request);

CREATE TABLE request_attachment (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_request      UUID NOT NULL REFERENCES request(id),
    categoria       VARCHAR(100) NOT NULL,
    storage_key     VARCHAR(500) NOT NULL,
    sha256          VARCHAR(64) NOT NULL,
    nome_original   VARCHAR(300) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    tamanho_bytes   BIGINT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_request_attachment_request ON request_attachment(id_request);
