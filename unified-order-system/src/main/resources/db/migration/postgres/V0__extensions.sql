-- PostgreSQL-only extension setup
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_catalog.pg_extension WHERE extname = 'uuid-ossp') THEN
        RETURN;
    END IF;
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
    ALTER EXTENSION "uuid-ossp" SET SCHEMA public;
END
$$;
