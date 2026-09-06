-- V003: Acadêmico Module Schema

CREATE TABLE curso (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    nome            VARCHAR(200) NOT NULL,
    sigla           VARCHAR(20) NOT NULL UNIQUE,
    id_coordenador  UUID REFERENCES usuario(id),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_curso_coordenador ON curso(id_coordenador);

CREATE TABLE disciplina (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_curso                UUID NOT NULL REFERENCES curso(id),
    codigo                  VARCHAR(20) NOT NULL,
    nome                    VARCHAR(200) NOT NULL,
    carga_horaria_total     INTEGER NOT NULL,
    creditos                INTEGER NOT NULL,
    ativa                   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (id_curso, codigo)
);

CREATE INDEX idx_disciplina_curso ON disciplina(id_curso);
CREATE INDEX idx_disciplina_nome_trgm ON disciplina USING GIN(nome gin_trgm_ops);

CREATE TABLE periodo_letivo (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    ano         SMALLINT NOT NULL,
    semestre    SMALLINT NOT NULL CHECK (semestre IN (1, 2)),
    inicio      DATE NOT NULL,
    fim         DATE NOT NULL,
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (ano, semestre)
);

CREATE TABLE calendario_academico (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_periodo_letivo   UUID NOT NULL REFERENCES periodo_letivo(id),
    id_request_type     UUID,
    descricao           VARCHAR(300) NOT NULL,
    prazo_inicio        DATE,
    prazo_fim           DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_calendario_periodo ON calendario_academico(id_periodo_letivo);
