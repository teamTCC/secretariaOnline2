-- V018: BaseEntity.updated_at em request_event e request_attachment
-- Hibernate mapeia ambas via BaseEntity; sem a coluna o INSERT de anexos/eventos falha.
-- ROLLBACK: ALTER TABLE request_event DROP COLUMN updated_at;
--           ALTER TABLE request_attachment DROP COLUMN updated_at;

ALTER TABLE request_event
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE request_attachment
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
