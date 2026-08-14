-- V015: beyond-MVP — config de curso, histórico, certificados PDF, contato, busca, export async

INSERT INTO authority (code, descricao) VALUES
  ('course.config', 'Configurar parâmetros acadêmicos do próprio curso')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'COORDENADOR' AND a.code = 'course.config'
ON CONFLICT DO NOTHING;

INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'ADMIN' AND a.code = 'course.config'
ON CONFLICT DO NOTHING;

ALTER TABLE curso
    ADD COLUMN IF NOT EXISTS horas_formativas_minimas INTEGER NOT NULL DEFAULT 120,
    ADD COLUMN IF NOT EXISTS duracao_calendario VARCHAR(20) NOT NULL DEFAULT '15_SEMANAS',
    ADD COLUMN IF NOT EXISTS banca_membros_externos INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS banca_modalidade VARCHAR(20) NOT NULL DEFAULT 'PRESENCIAL',
    ADD COLUMN IF NOT EXISTS regimento TEXT;

CREATE TABLE IF NOT EXISTS historico_escolar (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_aluno        UUID NOT NULL REFERENCES usuario(id),
    id_disciplina   UUID NOT NULL REFERENCES disciplina(id),
    estado          VARCHAR(20) NOT NULL DEFAULT 'CURSANDO',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (id_aluno, id_disciplina)
);
CREATE INDEX IF NOT EXISTS idx_historico_aluno ON historico_escolar(id_aluno);

ALTER TABLE certificate
    ALTER COLUMN id_evento DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS origem VARCHAR(20) NOT NULL DEFAULT 'EVENTO',
    ADD COLUMN IF NOT EXISTS id_activity UUID REFERENCES formative_activity(id);

ALTER TABLE graduation_record
    ADD COLUMN IF NOT EXISTS livro VARCHAR(40),
    ADD COLUMN IF NOT EXISTS folha VARCHAR(40),
    ADD COLUMN IF NOT EXISTS ata VARCHAR(80),
    ADD COLUMN IF NOT EXISTS id_periodo UUID REFERENCES periodo_letivo(id),
    ADD COLUMN IF NOT EXISTS diploma_storage_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS diploma_hash_sha256 VARCHAR(64);

ALTER TABLE export_job
    ADD COLUMN IF NOT EXISTS error_message TEXT;

ALTER TABLE service_record
    ALTER COLUMN id_secretario DROP NOT NULL;

CREATE TABLE IF NOT EXISTS contact_message (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    nome            VARCHAR(200) NOT NULL,
    email           VARCHAR(200) NOT NULL,
    assunto         VARCHAR(300) NOT NULL,
    mensagem        TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'NOVO',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_usuario_nome_trgm ON usuario USING GIN (nome gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_usuario_email_trgm ON usuario USING GIN (email gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_event_titulo_trgm ON event_attendance USING GIN (titulo gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_request_type_trgm ON request USING GIN (request_type_code gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_curso_nome_trgm ON curso USING GIN (nome gin_trgm_ops);

INSERT INTO communication_template (codigo, titulo, assunto, corpo, canal)
VALUES
  ('solicitacoes.transicionada', 'Atualização de solicitação',
   'Atualização na sua solicitação — SecretariaOnline',
   '<html><body><h2>Sua solicitação foi atualizada</h2><p>Olá, <strong>{{nome}}</strong>!</p><p>A solicitação <strong>{{tipo}}</strong> mudou para o estado <strong>{{estadoNovo}}</strong>.</p>{{parecerHtml}}<p>Acesse o portal para ver os detalhes.</p><br><p>— Equipe SecretariaOnline UFPR</p></body></html>',
   'EMAIL'),
  ('atendimentos.created', 'Atendimento registrado',
   'Novo atendimento registrado',
   '<html><body><h2>Atendimento registrado</h2><p>Olá, <strong>{{nome}}</strong>!</p><p>A secretaria registrou um atendimento: <strong>{{assunto}}</strong>.</p><p>Acesse <em>Meus atendimentos</em> para dar ciência.</p><br><p>— Equipe SecretariaOnline UFPR</p></body></html>',
   'EMAIL'),
  ('graduations.confirmed', 'Colação de grau',
   'Colação de grau confirmada',
   '<html><body><h2>Colação de grau</h2><p>Olá, <strong>{{nome}}</strong>!</p><p>Sua colação de grau foi confirmada. Seu perfil agora é de egresso.</p><br><p>— Equipe SecretariaOnline UFPR</p></body></html>',
   'EMAIL'),
  ('imports.completed', 'Importação CSV',
   'Importação CSV concluída',
   '<html><body><h2>Importação concluída</h2><p>Olá, <strong>{{nome}}</strong>!</p><p>O job de importação terminou com status <strong>{{status}}</strong>.</p><br><p>— Equipe SecretariaOnline UFPR</p></body></html>',
   'EMAIL'),
  ('exports.ready', 'Exportação pronta',
   'Exportação pronta para download',
   '<html><body><h2>Exportação pronta</h2><p>Olá, <strong>{{nome}}</strong>!</p><p>O arquivo <strong>{{kind}}</strong> está disponível para download.</p><br><p>— Equipe SecretariaOnline UFPR</p></body></html>',
   'EMAIL'),
  ('contato.recebido', 'Contato público',
   'Nova mensagem de contato — {{assunto}}',
   '<html><body><h2>Mensagem de contato</h2><p><strong>De:</strong> {{nome}} &lt;{{email}}&gt;</p><p><strong>Assunto:</strong> {{assunto}}</p><p>{{mensagem}}</p></body></html>',
   'EMAIL')
ON CONFLICT (codigo) DO NOTHING;
