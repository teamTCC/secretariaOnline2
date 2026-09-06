-- V014: Admin FGAC extras, colação/diplomas, kanban, importações, templates, ciência de atendimento

INSERT INTO authority (code, descricao) VALUES
  ('diploma.register',                'Registrar colação e diplomas'),
  ('alumni.list',                     'Listar egressos'),
  ('report.view_coordinator',         'Relatórios analíticos de coordenação'),
  ('report.view_secretary',           'Estatísticas da secretaria'),
  ('iam.manage_authorities',          'Editar matriz role × authority'),
  ('import.run',                      'Importar dados em lote (CSV)'),
  ('task.manage',                     'Gerenciar tarefas internas da secretaria'),
  ('communication.manage_templates',  'Gerenciar templates de comunicação'),
  ('request_type.manage',             'Editar tipos de solicitação e workflow'),
  ('user.export_own_data',            'Exportar próprios dados (LGPD)'),
  ('service_record.view_own',         'Ver e dar ciência aos próprios atendimentos'),
  ('image_authorization.review',      'Revisar autorizações de uso de imagem'),
  ('export.run',                      'Exportar dados em lote (CSV)')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'SECRETARIO' AND a.code IN (
  'diploma.register', 'alumni.list', 'report.view_secretary',
  'import.run', 'task.manage', 'user.export_own_data',
  'image_authorization.review', 'export.run'
) ON CONFLICT DO NOTHING;

INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'COORDENADOR' AND a.code IN (
  'report.view_coordinator', 'alumni.list', 'diploma.register', 'user.export_own_data'
) ON CONFLICT DO NOTHING;

INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'ALUNO' AND a.code IN (
  'user.export_own_data', 'service_record.view_own'
) ON CONFLICT DO NOTHING;

INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;

ALTER TABLE service_record
    ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'PENDENTE_CIENCIA',
    ADD COLUMN IF NOT EXISTS acknowledged_at TIMESTAMPTZ;

CREATE TABLE graduation_record (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_aluno        UUID NOT NULL REFERENCES usuario(id),
    id_curso        UUID REFERENCES curso(id),
    data_colacao    DATE,
    estado          VARCHAR(30) NOT NULL DEFAULT 'COLOCADO',
    delivered_at    TIMESTAMPTZ,
    delivered_by    UUID REFERENCES usuario(id),
    observacao      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_graduation_aluno ON graduation_record(id_aluno);
CREATE INDEX idx_graduation_estado ON graduation_record(estado);

CREATE TABLE secretary_task (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    titulo          VARCHAR(200) NOT NULL,
    descricao       TEXT,
    estado          VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    id_assignee     UUID REFERENCES usuario(id),
    prioridade      VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    prazo_em        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_secretary_task_estado ON secretary_task(estado);

CREATE TABLE import_job (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    kind            VARCHAR(40) NOT NULL,
    filename        VARCHAR(300) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'VALIDATED',
    total_rows      INTEGER NOT NULL DEFAULT 0,
    success_count   INTEGER NOT NULL DEFAULT 0,
    error_count     INTEGER NOT NULL DEFAULT 0,
    rows_payload    JSONB NOT NULL DEFAULT '[]',
    errors          JSONB NOT NULL DEFAULT '[]',
    id_ator         UUID NOT NULL REFERENCES usuario(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_import_job_ator ON import_job(id_ator);

CREATE TABLE communication_template (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    codigo          VARCHAR(80) NOT NULL UNIQUE,
    titulo          VARCHAR(200) NOT NULL,
    assunto         VARCHAR(300) NOT NULL,
    corpo           TEXT NOT NULL,
    canal           VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    versao          INTEGER NOT NULL DEFAULT 1,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE communication_template_revision (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_template     UUID NOT NULL REFERENCES communication_template(id) ON DELETE CASCADE,
    versao          INTEGER NOT NULL,
    assunto         VARCHAR(300) NOT NULL,
    corpo           TEXT NOT NULL,
    id_autor        UUID REFERENCES usuario(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (id_template, versao)
);

CREATE TABLE notification_log (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    event_type      VARCHAR(100) NOT NULL,
    aggregate_id    UUID NOT NULL,
    id_usuario      UUID,
    canal           VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    status          VARCHAR(20) NOT NULL DEFAULT 'SENT',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notification_log_aggregate ON notification_log(aggregate_id);

CREATE TABLE export_job (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    kind            VARCHAR(40) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PRONTO',
    filename        VARCHAR(300) NOT NULL,
    storage_key     VARCHAR(400) NOT NULL,
    id_ator         UUID NOT NULL REFERENCES usuario(id),
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_export_job_ator ON export_job(id_ator);
