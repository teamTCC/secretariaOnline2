-- V019: immutable RequestType snapshots on publish + stamp on request instances.

CREATE TABLE request_type_version (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    id_request_type   UUID NOT NULL REFERENCES request_type(id),
    version           INTEGER NOT NULL,
    form_schema       JSONB NOT NULL,
    workflow_json     JSONB NOT NULL,
    published_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (id_request_type, version)
);

CREATE INDEX idx_request_type_version_type ON request_type_version(id_request_type);

INSERT INTO request_type_version (id_request_type, version, form_schema, workflow_json)
SELECT id, 1, form_schema, workflow_json
FROM request_type
WHERE ativo = TRUE;

ALTER TABLE request
    ADD COLUMN id_request_type_version UUID REFERENCES request_type_version(id);

UPDATE request r
SET id_request_type_version = v.id
FROM request_type_version v
WHERE v.id_request_type = r.id_request_type
  AND v.version = 1;

CREATE INDEX idx_request_type_version_fk ON request(id_request_type_version);
