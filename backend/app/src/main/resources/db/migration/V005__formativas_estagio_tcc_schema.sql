-- V005: Formativas, Estágio e TCC Schema

CREATE TABLE formative_activity (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_aluno                UUID NOT NULL REFERENCES usuario(id),
    titulo                  VARCHAR(200) NOT NULL,
    descricao               TEXT,
    categoria               VARCHAR(50) NOT NULL,
    carga_horaria           DOUBLE PRECISION NOT NULL,
    data_realizacao         DATE NOT NULL,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    parecer_revisor         TEXT,
    id_revisor              UUID REFERENCES usuario(id),
    storage_key_comprovante VARCHAR(500),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_formative_activity_aluno ON formative_activity(id_aluno);
CREATE INDEX idx_formative_activity_estado ON formative_activity(estado);

CREATE TABLE formative_entry (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_aluno        UUID NOT NULL REFERENCES usuario(id),
    id_activity     UUID REFERENCES formative_activity(id),
    id_evento       UUID,
    horas_aprovadas DOUBLE PRECISION NOT NULL,
    aprovado_em     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_formative_entry_aluno ON formative_entry(id_aluno);

CREATE TABLE internship (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_aluno                UUID NOT NULL REFERENCES usuario(id),
    id_supervisor           UUID REFERENCES usuario(id),
    empresa                 VARCHAR(200) NOT NULL,
    cargo                   VARCHAR(100) NOT NULL,
    carga_horaria_semanal   INTEGER NOT NULL,
    inicio                  DATE NOT NULL,
    fim                     DATE,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'EM_ANDAMENTO',
    observacoes             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_internship_aluno ON internship(id_aluno);
CREATE INDEX idx_internship_supervisor ON internship(id_supervisor);

CREATE TABLE internship_document (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_internship   UUID NOT NULL REFERENCES internship(id) ON DELETE CASCADE,
    tipo            VARCHAR(50) NOT NULL,
    storage_key     VARCHAR(500) NOT NULL,
    sha256          VARCHAR(64) NOT NULL,
    nome_original   VARCHAR(300) NOT NULL,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_internship_doc_internship ON internship_document(id_internship);

CREATE TABLE tcc (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_orientador   UUID NOT NULL REFERENCES usuario(id),
    titulo          VARCHAR(300) NOT NULL,
    id_curso        UUID NOT NULL REFERENCES curso(id),
    estado          VARCHAR(20) NOT NULL DEFAULT 'EM_ANDAMENTO',
    data_defesa     DATE,
    storage_key_pdf VARCHAR(500),
    hash_sha256_pdf VARCHAR(64),
    nota_final      DOUBLE PRECISION,
    aprovado        BOOLEAN,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tcc_orientador ON tcc(id_orientador);
CREATE INDEX idx_tcc_estado ON tcc(estado);

CREATE TABLE tcc_member (
    id_tcc      UUID NOT NULL REFERENCES tcc(id) ON DELETE CASCADE,
    id_aluno    UUID NOT NULL REFERENCES usuario(id),
    papel       VARCHAR(20) NOT NULL DEFAULT 'AUTOR',
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id_tcc, id_aluno)
);

CREATE TABLE tcc_examiner (
    id_tcc          UUID NOT NULL REFERENCES tcc(id) ON DELETE CASCADE,
    id_professor    UUID NOT NULL REFERENCES usuario(id),
    papel           VARCHAR(30) NOT NULL DEFAULT 'BANCA',
    nota            DOUBLE PRECISION,
    PRIMARY KEY (id_tcc, id_professor)
);
