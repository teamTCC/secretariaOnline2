-- V011: Seed — Demo data for development/testing
-- Admin user: admin@ufpr.br / Admin@123456 (Argon2id hash)
-- WARNING: Change credentials before any production deployment

-- Demo admin user (password: Admin@123456)
INSERT INTO usuario (nome, email, senha_hash, senha_alterada, ativo, metadata)
VALUES (
  'Administrador Sistema',
  'admin@ufpr.br',
  '$argon2id$v=19$m=47104,t=1,p=1$placeholder_hash_replace_on_deploy',
  TRUE,
  TRUE,
  '{"aceite_lgpd_em": "2026-01-01T00:00:00Z"}'
) ON CONFLICT (email) DO NOTHING;

-- Demo curso
INSERT INTO curso (nome, sigla, ativo) VALUES
  ('Tecnologia em Análise e Desenvolvimento de Sistemas', 'TADS', TRUE),
  ('Engenharia de Software', 'ES', TRUE)
ON CONFLICT (sigla) DO NOTHING;

-- Demo periodo letivo
INSERT INTO periodo_letivo (ano, semestre, inicio, fim, ativo) VALUES
  (2026, 1, '2026-03-01', '2026-07-31', FALSE),
  (2026, 2, '2026-08-01', '2026-12-31', TRUE)
ON CONFLICT (ano, semestre) DO NOTHING;

-- Request types (19 types — the DRY core)
-- Each type stores its form_schema + workflow_json
INSERT INTO request_type (code, descricao, prazo_dias, form_schema, workflow_json) VALUES
(
  'SEGUNDA_CHAMADA',
  'Segunda chamada de prova',
  5,
  '{
    "type": "object",
    "properties": {
      "idDisciplina": {"type": "string", "format": "uuid", "title": "Disciplina", "x-ui": {"widget": "entity-select", "endpoint": "/academico/disciplinas"}},
      "dataProva": {"type": "string", "format": "date", "title": "Data da prova perdida"},
      "motivoAusencia": {"type": "string", "title": "Motivo da ausência", "enum": ["SAUDE", "LUTO", "TRABALHO", "OUTRO"]},
      "descricaoMotivo": {"type": "string", "title": "Descrição detalhada", "x-ui": {"widget": "textarea"}}
    },
    "required": ["idDisciplina", "dataProva", "motivoAusencia", "descricaoMotivo"],
    "x-required-attachments": ["ATESTADO_MEDICO"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","EM_AJUSTE","DEFERIDA","INDEFERIDA","EM_REVISAO","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_NEEDS_ACTION", "generateOneTimeToken": true},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"},
      {"from": "EM_DELIBERACAO", "to": "EM_AJUSTE", "action": "REQUEST_ADJUSTMENT", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_NEEDS_ADJUSTMENT"},
      {"from": "EM_AJUSTE", "to": "ABERTA", "action": "RESUBMIT", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante"},
      {"from": "INDEFERIDA", "to": "EM_REVISAO", "action": "REQUEST_REVIEW", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante and request.allowsReview"}
    ]
  }'
),
(
  'TRANCAMENTO_DISCIPLINA',
  'Trancamento de disciplina',
  10,
  '{
    "type": "object",
    "properties": {
      "disciplinas": {"type": "array", "title": "Disciplinas para trancar", "items": {"type": "object", "properties": {"idDisciplina": {"type": "string", "format": "uuid"}}, "required": ["idDisciplina"]}, "minItems": 1, "x-ui": {"widget": "multi-select-table"}},
      "justificativa": {"type": "string", "title": "Justificativa", "minLength": 20, "x-ui": {"widget": "textarea"}}
    },
    "required": ["disciplinas", "justificativa"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","EM_AJUSTE","DEFERIDA","INDEFERIDA","EM_REVISAO","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"},
      {"from": "EM_DELIBERACAO", "to": "EM_AJUSTE", "action": "REQUEST_ADJUSTMENT", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_AJUSTE", "to": "ABERTA", "action": "RESUBMIT", "requiresAuthority": ["request.open"]}
    ]
  }'
),
(
  'DECLARACAO_MATRICULA',
  'Declaração de matrícula',
  3,
  '{
    "type": "object",
    "properties": {
      "finalidade": {"type": "string", "title": "Finalidade", "enum": ["BOLSA", "CONVENIO", "OUTRO"], "x-ui": {"widget": "select"}},
      "observacoes": {"type": "string", "title": "Observações", "x-ui": {"widget": "textarea"}}
    },
    "required": ["finalidade"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","DEFERIDA","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"}
    ]
  }'
)
ON CONFLICT (code) DO NOTHING;
