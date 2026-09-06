-- V006: Presença em Eventos Schema (v4.1 — modes + windows + certificates)

CREATE TABLE event_attendance (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    titulo              VARCHAR(300) NOT NULL,
    descricao           TEXT,
    id_organizador      UUID NOT NULL REFERENCES usuario(id),
    id_curso            UUID REFERENCES curso(id),
    attendance_mode     VARCHAR(20) NOT NULL CHECK (attendance_mode IN ('QR_SINGLE','QR_DUAL','SECRET_SINGLE','SECRET_DUAL')),
    estado              VARCHAR(20) NOT NULL DEFAULT 'AGENDADO' CHECK (estado IN ('AGENDADO','EM_ANDAMENTO','CONCLUIDO','CANCELADO')),
    ch_creditadas       DOUBLE PRECISION NOT NULL,
    inicio_em           TIMESTAMPTZ NOT NULL,
    fim_em              TIMESTAMPTZ NOT NULL,
    validation_windows  JSONB NOT NULL DEFAULT '[]',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_attendance_organizador ON event_attendance(id_organizador);
CREATE INDEX idx_event_attendance_curso ON event_attendance(id_curso);
CREATE INDEX idx_event_attendance_estado ON event_attendance(estado);
CREATE INDEX idx_event_attendance_fim ON event_attendance(fim_em) WHERE estado = 'EM_ANDAMENTO';

CREATE TABLE attendance_session (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_evento           UUID NOT NULL REFERENCES event_attendance(id) ON DELETE CASCADE,
    id_aluno            UUID NOT NULL REFERENCES usuario(id),
    device_uuid         VARCHAR(100),
    entry_confirmed_at  TIMESTAMPTZ,
    exit_confirmed_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (id_evento, id_aluno)
);

CREATE INDEX idx_attendance_session_evento ON attendance_session(id_evento);
CREATE INDEX idx_attendance_session_aluno ON attendance_session(id_aluno);

CREATE TABLE certificate (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_aluno            UUID NOT NULL REFERENCES usuario(id),
    id_evento           UUID NOT NULL REFERENCES event_attendance(id),
    hash_sha256         VARCHAR(64) NOT NULL UNIQUE,
    signature_ed25519   VARCHAR(200) NOT NULL,
    storage_key         VARCHAR(500) NOT NULL,
    ch_creditadas       DOUBLE PRECISION NOT NULL,
    issued_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_certificate_aluno ON certificate(id_aluno);
CREATE INDEX idx_certificate_hash ON certificate(hash_sha256);
