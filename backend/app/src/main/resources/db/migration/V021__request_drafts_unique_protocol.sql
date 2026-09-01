-- Drafts share numero_anual = 0 as a placeholder until submit assigns a public protocol.
-- The old UNIQUE (numero_anual, ano, id_curso) allowed only one RASCUNHO per curso per year (HTTP 500).
-- Official protocols remain unique; drafts are excluded from the index.

ALTER TABLE request DROP CONSTRAINT IF EXISTS request_numero_anual_ano_id_curso_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_request_protocolo_oficial
    ON request (numero_anual, ano, id_curso)
    WHERE estado <> 'RASCUNHO'
      AND deleted_at IS NULL
      AND numero_anual > 0;
