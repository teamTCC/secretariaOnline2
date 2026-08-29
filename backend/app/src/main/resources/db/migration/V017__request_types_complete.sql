-- V017: Seed remaining 16 RequestType entries (V011 seeded 3: SEGUNDA_CHAMADA, TRANCAMENTO_DISCIPLINA, DECLARACAO_MATRICULA)
-- Together these 19 types are the DRY core — adding a type is inserting 1 row, never creating classes.
-- All workflow_json follow the full 9-state machine unless the type has a simpler lifecycle.

INSERT INTO request_type (code, descricao, prazo_dias, form_schema, workflow_json) VALUES

-- 1. ADIANTAMENTO_PERIODO
(
  'ADIANTAMENTO_PERIODO',
  'Adiantamento de período',
  15,
  '{
    "type": "object",
    "properties": {
      "semestre": {"type": "string", "title": "Semestre pretendido", "x-ui": {"widget": "select"},
        "enum": ["2026/1", "2026/2", "2027/1", "2027/2"]},
      "justificativa": {"type": "string", "title": "Justificativa", "minLength": 30,
        "x-ui": {"widget": "textarea", "rows": 6}},
      "disciplinasDesejadas": {
        "type": "array", "title": "Disciplinas desejadas",
        "items": {
          "type": "object",
          "properties": {
            "idDisciplina": {"type": "string", "format": "uuid", "title": "Disciplina",
              "x-ui": {"widget": "entity-select", "endpoint": "/academico/disciplinas"}}
          },
          "required": ["idDisciplina"]
        },
        "minItems": 1,
        "x-ui": {"widget": "multi-select-table"}
      }
    },
    "required": ["semestre", "justificativa", "disciplinasDesejadas"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","EM_AJUSTE","DEFERIDA","INDEFERIDA","EM_REVISAO","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_NEEDS_ACTION"},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"},
      {"from": "EM_DELIBERACAO", "to": "EM_AJUSTE", "action": "REQUEST_ADJUSTMENT", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_NEEDS_ADJUSTMENT"},
      {"from": "EM_AJUSTE", "to": "ABERTA", "action": "RESUBMIT", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante"},
      {"from": "INDEFERIDA", "to": "EM_REVISAO", "action": "REQUEST_REVIEW", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante and request.allowsReview"}
    ]
  }'
),

-- 2. APROVEITAMENTO_DISCIPLINA
(
  'APROVEITAMENTO_DISCIPLINA',
  'Aproveitamento de disciplina',
  15,
  '{
    "type": "object",
    "properties": {
      "idDisciplinaAlvo": {"type": "string", "format": "uuid", "title": "Disciplina a ser aproveitada",
        "x-ui": {"widget": "entity-select", "endpoint": "/academico/disciplinas"}},
      "instituicaoOrigem": {"type": "string", "title": "Instituição de origem", "maxLength": 200},
      "disciplinaOrigem": {"type": "string", "title": "Nome da disciplina de origem", "maxLength": 200},
      "cargaHorariaOrigem": {"type": "integer", "title": "Carga horária (horas)", "minimum": 1, "maximum": 500},
      "notaOrigem": {"type": "number", "title": "Nota obtida", "minimum": 0, "maximum": 10},
      "justificativa": {"type": "string", "title": "Justificativa", "minLength": 30, "x-ui": {"widget": "textarea"}}
    },
    "required": ["idDisciplinaAlvo", "instituicaoOrigem", "disciplinaOrigem", "cargaHorariaOrigem"],
    "x-required-attachments": ["HISTORICO_ORIGEM", "EMENTA_DISCIPLINA"]
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

-- 3. TRANCAMENTO_PERIODO
(
  'TRANCAMENTO_PERIODO',
  'Trancamento de período',
  10,
  '{
    "type": "object",
    "properties": {
      "semestre": {"type": "string", "title": "Semestre a trancar", "x-ui": {"widget": "select"},
        "enum": ["2026/1", "2026/2", "2027/1"]},
      "justificativa": {"type": "string", "title": "Justificativa", "minLength": 30, "x-ui": {"widget": "textarea"}},
      "tipoTrancamento": {"type": "string", "title": "Tipo", "enum": ["NORMAL", "ESPECIAL"],
        "x-ui": {"widget": "select"}}
    },
    "required": ["semestre", "justificativa", "tipoTrancamento"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","EM_AJUSTE","DEFERIDA","INDEFERIDA","EM_REVISAO","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_NEEDS_ACTION"},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"},
      {"from": "EM_DELIBERACAO", "to": "EM_AJUSTE", "action": "REQUEST_ADJUSTMENT", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_AJUSTE", "to": "ABERTA", "action": "RESUBMIT", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante"},
      {"from": "INDEFERIDA", "to": "EM_REVISAO", "action": "REQUEST_REVIEW", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante and request.allowsReview"}
    ]
  }'
),

-- 4. COLACAO_SEM_SOLENIDADE
(
  'COLACAO_SEM_SOLENIDADE',
  'Colação sem solenidade',
  20,
  '{
    "type": "object",
    "properties": {
      "motivoJustificado": {"type": "string", "title": "Motivo justificado", "minLength": 50,
        "x-ui": {"widget": "textarea"}},
      "dataPreferencial": {"type": "string", "format": "date", "title": "Data preferencial",
        "x-ui": {"widget": "date-picker"}}
    },
    "required": ["motivoJustificado"],
    "x-required-attachments": ["COMPROVANTE_MOTIVO"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","EM_AJUSTE","DEFERIDA","INDEFERIDA","EM_REVISAO","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_NEEDS_ACTION"},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"},
      {"from": "EM_DELIBERACAO", "to": "EM_AJUSTE", "action": "REQUEST_ADJUSTMENT", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_AJUSTE", "to": "ABERTA", "action": "RESUBMIT", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante"},
      {"from": "INDEFERIDA", "to": "EM_REVISAO", "action": "REQUEST_REVIEW", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante and request.allowsReview"}
    ]
  }'
),

-- 5. REVISAO_NOTA
(
  'REVISAO_NOTA',
  'Revisão de nota',
  10,
  '{
    "type": "object",
    "properties": {
      "idDisciplina": {"type": "string", "format": "uuid", "title": "Disciplina",
        "x-ui": {"widget": "entity-select", "endpoint": "/academico/disciplinas?enrolled=true"}},
      "tipoAvaliacao": {"type": "string", "title": "Tipo de avaliação",
        "enum": ["PROVA1", "PROVA2", "SUBSTITUTIVA", "TRABALHO", "OUTRO"],
        "x-ui": {"widget": "select"}},
      "notaAtual": {"type": "number", "title": "Nota atual", "minimum": 0, "maximum": 10},
      "notaEsperada": {"type": "number", "title": "Nota esperada", "minimum": 0, "maximum": 10},
      "justificativa": {"type": "string", "title": "Justificativa detalhada", "minLength": 30,
        "x-ui": {"widget": "textarea"}}
    },
    "required": ["idDisciplina", "tipoAvaliacao", "notaAtual", "justificativa"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","EM_AJUSTE","DEFERIDA","INDEFERIDA","EM_REVISAO","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_NEEDS_ACTION", "generateOneTimeToken": true},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"},
      {"from": "EM_DELIBERACAO", "to": "EM_AJUSTE", "action": "REQUEST_ADJUSTMENT", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_AJUSTE", "to": "ABERTA", "action": "RESUBMIT", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante"},
      {"from": "INDEFERIDA", "to": "EM_REVISAO", "action": "REQUEST_REVIEW", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante and request.allowsReview"}
    ]
  }'
),

-- 6. INCLUSAO_DISCIPLINA
(
  'INCLUSAO_DISCIPLINA',
  'Inclusão de disciplina',
  5,
  '{
    "type": "object",
    "properties": {
      "disciplinas": {
        "type": "array", "title": "Disciplinas para incluir",
        "items": {
          "type": "object",
          "properties": {
            "idDisciplina": {"type": "string", "format": "uuid", "title": "Disciplina",
              "x-ui": {"widget": "entity-select", "endpoint": "/academico/disciplinas"}},
            "turma": {"type": "string", "title": "Turma", "maxLength": 10}
          },
          "required": ["idDisciplina"]
        },
        "minItems": 1,
        "x-ui": {"widget": "multi-select-table"}
      },
      "justificativa": {"type": "string", "title": "Justificativa", "x-ui": {"widget": "textarea"}}
    },
    "required": ["disciplinas"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","DEFERIDA","INDEFERIDA","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"}
    ]
  }'
),

-- 7. EXCLUSAO_DISCIPLINA
(
  'EXCLUSAO_DISCIPLINA',
  'Exclusão de disciplina',
  5,
  '{
    "type": "object",
    "properties": {
      "disciplinas": {
        "type": "array", "title": "Disciplinas para excluir",
        "items": {
          "type": "object",
          "properties": {
            "idDisciplina": {"type": "string", "format": "uuid", "title": "Disciplina",
              "x-ui": {"widget": "entity-select", "endpoint": "/academico/disciplinas?enrolled=true"}}
          },
          "required": ["idDisciplina"]
        },
        "minItems": 1,
        "x-ui": {"widget": "multi-select-table"}
      },
      "justificativa": {"type": "string", "title": "Justificativa", "x-ui": {"widget": "textarea"}}
    },
    "required": ["disciplinas"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","DEFERIDA","INDEFERIDA","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"}
    ]
  }'
),

-- 8. MATRICULA_DISCIPLINA_ISOLADA
(
  'MATRICULA_DISCIPLINA_ISOLADA',
  'Matrícula em disciplina isolada',
  10,
  '{
    "type": "object",
    "properties": {
      "idDisciplina": {"type": "string", "format": "uuid", "title": "Disciplina",
        "x-ui": {"widget": "entity-select", "endpoint": "/academico/disciplinas"}},
      "semestre": {"type": "string", "title": "Semestre", "x-ui": {"widget": "select"},
        "enum": ["2026/1", "2026/2", "2027/1", "2027/2"]},
      "justificativa": {"type": "string", "title": "Justificativa", "minLength": 20,
        "x-ui": {"widget": "textarea"}}
    },
    "required": ["idDisciplina", "semestre", "justificativa"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","EM_AJUSTE","DEFERIDA","INDEFERIDA","EM_REVISAO","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_NEEDS_ACTION"},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"},
      {"from": "EM_DELIBERACAO", "to": "EM_AJUSTE", "action": "REQUEST_ADJUSTMENT", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_AJUSTE", "to": "ABERTA", "action": "RESUBMIT", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante"},
      {"from": "INDEFERIDA", "to": "EM_REVISAO", "action": "REQUEST_REVIEW", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante and request.allowsReview"}
    ]
  }'
),

-- 9. MATRICULA_DISCIPLINA_ELETIVA
(
  'MATRICULA_DISCIPLINA_ELETIVA',
  'Matrícula em disciplina eletiva',
  10,
  '{
    "type": "object",
    "properties": {
      "idDisciplina": {"type": "string", "format": "uuid", "title": "Disciplina eletiva",
        "x-ui": {"widget": "entity-select", "endpoint": "/academico/disciplinas?tipo=ELETIVA"}},
      "semestre": {"type": "string", "title": "Semestre", "enum": ["2026/1","2026/2","2027/1","2027/2"],
        "x-ui": {"widget": "select"}},
      "justificativa": {"type": "string", "title": "Justificativa", "minLength": 20,
        "x-ui": {"widget": "textarea"}}
    },
    "required": ["idDisciplina", "semestre"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","DEFERIDA","INDEFERIDA","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"}
    ]
  }'
),

-- 10. APROVEITAMENTO_ESTAGIO
(
  'APROVEITAMENTO_ESTAGIO',
  'Aproveitamento de estágio',
  15,
  '{
    "type": "object",
    "properties": {
      "empresa": {"type": "string", "title": "Empresa", "maxLength": 200},
      "cnpj": {"type": "string", "title": "CNPJ", "pattern": "^\\d{14}$"},
      "periodo": {"type": "string", "title": "Período (ex: 2025/1)", "maxLength": 20},
      "cargaHoraria": {"type": "integer", "title": "Carga horária total (horas)", "minimum": 1},
      "atividadesRealizadas": {"type": "string", "title": "Descrição das atividades", "minLength": 100,
        "x-ui": {"widget": "textarea"}}
    },
    "required": ["empresa", "periodo", "cargaHoraria", "atividadesRealizadas"],
    "x-required-attachments": ["TERMO_ESTAGIO", "RELATORIO_FINAL", "AVALIACAO_EMPRESA"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","EM_AJUSTE","DEFERIDA","INDEFERIDA","EM_REVISAO","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_NEEDS_ACTION"},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"},
      {"from": "EM_DELIBERACAO", "to": "EM_AJUSTE", "action": "REQUEST_ADJUSTMENT", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_AJUSTE", "to": "ABERTA", "action": "RESUBMIT", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante"},
      {"from": "INDEFERIDA", "to": "EM_REVISAO", "action": "REQUEST_REVIEW", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante and request.allowsReview"}
    ]
  }'
),

-- 11. APROVEITAMENTO_ATIVIDADE_COMPLEMENTAR
(
  'APROVEITAMENTO_ATIVIDADE_COMPLEMENTAR',
  'Aproveitamento de atividade complementar',
  15,
  '{
    "type": "object",
    "properties": {
      "tipoAtividade": {"type": "string", "title": "Tipo de atividade",
        "enum": ["CURSO","CONGRESSO","PUBLICACAO","MONITORIA","PROJETO_PESQUISA","EXTENSAO","OUTRO"],
        "x-ui": {"widget": "select"}},
      "titulo": {"type": "string", "title": "Título da atividade", "maxLength": 300},
      "cargaHoraria": {"type": "integer", "title": "Carga horária (horas)", "minimum": 1, "maximum": 500},
      "dataRealizacao": {"type": "string", "format": "date", "title": "Data de realização",
        "x-ui": {"widget": "date-picker"}},
      "descricao": {"type": "string", "title": "Descrição", "minLength": 20, "x-ui": {"widget": "textarea"}}
    },
    "required": ["tipoAtividade", "titulo", "cargaHoraria", "dataRealizacao"],
    "x-required-attachments": ["COMPROVANTE_ATIVIDADE"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","EM_AJUSTE","DEFERIDA","INDEFERIDA","EM_REVISAO","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_NEEDS_ACTION"},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"},
      {"from": "EM_DELIBERACAO", "to": "EM_AJUSTE", "action": "REQUEST_ADJUSTMENT", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_AJUSTE", "to": "ABERTA", "action": "RESUBMIT", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante"},
      {"from": "INDEFERIDA", "to": "EM_REVISAO", "action": "REQUEST_REVIEW", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante and request.allowsReview"}
    ]
  }'
),

-- 12. JUSTIFICATIVA_FALTA
(
  'JUSTIFICATIVA_FALTA',
  'Justificativa de falta',
  3,
  '{
    "type": "object",
    "properties": {
      "idDisciplina": {"type": "string", "format": "uuid", "title": "Disciplina",
        "x-ui": {"widget": "entity-select", "endpoint": "/academico/disciplinas?enrolled=true"}},
      "dataFalta": {"type": "string", "format": "date", "title": "Data da falta",
        "x-ui": {"widget": "date-picker"}},
      "motivoAusencia": {"type": "string", "title": "Motivo",
        "enum": ["SAUDE", "LUTO", "TRABALHO", "JUDICIAL", "OUTRO"],
        "x-ui": {"widget": "select"}},
      "descricaoMotivo": {"type": "string", "title": "Descrição", "minLength": 20,
        "x-ui": {"widget": "textarea"}}
    },
    "required": ["idDisciplina", "dataFalta", "motivoAusencia", "descricaoMotivo"],
    "x-required-attachments": ["COMPROVANTE_AUSENCIA"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","DEFERIDA","INDEFERIDA","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_NEEDS_ACTION", "generateOneTimeToken": true},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"}
    ]
  }'
),

-- 13. HISTORICO_ESCOLAR
(
  'HISTORICO_ESCOLAR',
  'Emissão de histórico escolar',
  5,
  '{
    "type": "object",
    "properties": {
      "finalidade": {"type": "string", "title": "Finalidade",
        "enum": ["CONCURSO", "POS_GRADUACAO", "REINGRESSO", "TRANSFERENCIA", "OUTRO"],
        "x-ui": {"widget": "select"}},
      "vias": {"type": "integer", "title": "Número de vias", "minimum": 1, "maximum": 5, "default": 1},
      "observacoes": {"type": "string", "title": "Observações", "x-ui": {"widget": "textarea"}}
    },
    "required": ["finalidade", "vias"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","DEFERIDA","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"}
    ]
  }'
),

-- 14. DIPLOMA
(
  'DIPLOMA',
  'Solicitação de diploma',
  20,
  '{
    "type": "object",
    "properties": {
      "nomeCompleto": {"type": "string", "title": "Nome completo conforme diploma", "maxLength": 200},
      "enderecoEntrega": {"type": "string", "title": "Endereço para envio (se postal)", "maxLength": 500,
        "x-ui": {"widget": "textarea"}},
      "modalidadeRetirada": {"type": "string", "title": "Modalidade de retirada",
        "enum": ["PRESENCIAL", "POSTAL"],
        "x-ui": {"widget": "select"}}
    },
    "required": ["nomeCompleto", "modalidadeRetirada"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","EM_DELIBERACAO","EM_AJUSTE","DEFERIDA","INDEFERIDA","EM_REVISAO","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_TRIAGEM", "to": "EM_DELIBERACAO", "action": "FORWARD_TO_DELIBERATOR", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_NEEDS_ACTION"},
      {"from": "EM_DELIBERACAO", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_DELIBERACAO", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate"], "notifyTemplate": "REQUEST_DENIED"},
      {"from": "EM_DELIBERACAO", "to": "EM_AJUSTE", "action": "REQUEST_ADJUSTMENT", "requiresAuthority": ["request.deliberate"]},
      {"from": "EM_AJUSTE", "to": "ABERTA", "action": "RESUBMIT", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante"},
      {"from": "INDEFERIDA", "to": "EM_REVISAO", "action": "REQUEST_REVIEW", "requiresAuthority": ["request.open"], "guard": "actor.id == request.idSolicitante and request.allowsReview"}
    ]
  }'
),

-- 15. AUTORIZACAO_IMAGEM
(
  'AUTORIZACAO_IMAGEM',
  'Autorização de uso de imagem',
  5,
  '{
    "type": "object",
    "properties": {
      "finalidadeUso": {"type": "string", "title": "Finalidade do uso da imagem", "minLength": 20,
        "x-ui": {"widget": "textarea"}},
      "vigencia": {"type": "string", "title": "Vigência",
        "enum": ["1_ANO", "5_ANOS", "INDEFINIDA"],
        "x-ui": {"widget": "select"}},
      "aceiteTermos": {"type": "boolean", "title": "Declaro que li e aceito os termos de uso de imagem"}
    },
    "required": ["finalidadeUso", "vigencia", "aceiteTermos"]
  }',
  '{
    "initial": "ABERTA",
    "states": ["RASCUNHO","ABERTA","EM_TRIAGEM","DEFERIDA","INDEFERIDA","ARQUIVADA"],
    "transitions": [
      {"from": "ABERTA", "to": "EM_TRIAGEM", "action": "ASSIGN", "requiresAuthority": ["request.deliberate", "image_authorization.review"]},
      {"from": "EM_TRIAGEM", "to": "DEFERIDA", "action": "DEFER", "requiresAuthority": ["request.deliberate", "image_authorization.review"], "notifyTemplate": "REQUEST_DEFERRED"},
      {"from": "EM_TRIAGEM", "to": "INDEFERIDA", "action": "DENY", "requiresAuthority": ["request.deliberate", "image_authorization.review"], "notifyTemplate": "REQUEST_DENIED"}
    ]
  }'
),

-- 16. ATESTADO_FREQUENCIA
(
  'ATESTADO_FREQUENCIA',
  'Atestado de frequência',
  3,
  '{
    "type": "object",
    "properties": {
      "semestre": {"type": "string", "title": "Semestre de referência",
        "enum": ["2026/1", "2026/2", "2025/2"],
        "x-ui": {"widget": "select"}},
      "finalidade": {"type": "string", "title": "Finalidade",
        "enum": ["BOLSA", "CONVENIO", "BENEFICIO_SOCIAL", "OUTRO"],
        "x-ui": {"widget": "select"}},
      "observacoes": {"type": "string", "title": "Observações", "x-ui": {"widget": "textarea"}}
    },
    "required": ["semestre", "finalidade"]
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

-- Mark all 19 types as active
UPDATE request_type SET ativo = TRUE
WHERE code IN (
  'ADIANTAMENTO_PERIODO', 'APROVEITAMENTO_DISCIPLINA', 'TRANCAMENTO_DISCIPLINA',
  'TRANCAMENTO_PERIODO', 'COLACAO_SEM_SOLENIDADE', 'REVISAO_NOTA', 'SEGUNDA_CHAMADA',
  'INCLUSAO_DISCIPLINA', 'EXCLUSAO_DISCIPLINA', 'MATRICULA_DISCIPLINA_ISOLADA',
  'MATRICULA_DISCIPLINA_ELETIVA', 'APROVEITAMENTO_ESTAGIO', 'APROVEITAMENTO_ATIVIDADE_COMPLEMENTAR',
  'JUSTIFICATIVA_FALTA', 'DECLARACAO_MATRICULA', 'HISTORICO_ESCOLAR',
  'DIPLOMA', 'AUTORIZACAO_IMAGEM', 'ATESTADO_FREQUENCIA'
);
