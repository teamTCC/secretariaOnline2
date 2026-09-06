-- PostgreSQL extensions required by SecretariaOnline2
-- This script runs automatically on first container start

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "citext";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- UUIDv7 function (time-sortable, no sequence lock contention)
CREATE OR REPLACE FUNCTION uuid_generate_v7() RETURNS uuid AS $$
DECLARE
  unix_ts_ms bigint;
  uuid_bytes bytea;
BEGIN
  unix_ts_ms := (extract(epoch from clock_timestamp()) * 1000)::bigint;
  uuid_bytes := decode(lpad(to_hex(unix_ts_ms), 12, '0'), 'hex') ||
                decode(lpad(to_hex(floor(random() * (2^62))::bigint::bigint), 16, '0'), 'hex');
  uuid_bytes := set_byte(uuid_bytes, 6, (get_byte(uuid_bytes, 6) & 15) | 112);
  uuid_bytes := set_byte(uuid_bytes, 8, (get_byte(uuid_bytes, 8) & 63) | 128);
  RETURN encode(uuid_bytes, 'hex')::uuid;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE secretaria_dev TO secretaria;
