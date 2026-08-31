-- Flyway SQL callback (not a versioned migration).
-- V001's uuid_generate_v7() emits 28 hex chars; seeds from V010+ need a valid UUID.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'uuid_generate_v7') THEN
    EXECUTE $fn$
      CREATE OR REPLACE FUNCTION public.uuid_generate_v7()
      RETURNS uuid
      LANGUAGE sql
      AS $f$
        SELECT encode(
          set_bit(set_bit(
            overlay(uuid_send(gen_random_uuid()) placing
              substring(int8send(floor(extract(epoch from clock_timestamp()) * 1000)::bigint) from 3)
              from 1 for 6),
            52, 1), 53, 1), 'hex')::uuid;
      $f$;
    $fn$;
  END IF;
END
$$;
