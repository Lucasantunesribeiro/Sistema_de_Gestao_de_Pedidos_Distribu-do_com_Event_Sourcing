-- Event store: versão sequencial por stream (aggregate_type, aggregate_id).
-- Base da concorrência otimista (append com versão esperada) e do replay em ordem determinística.
ALTER TABLE domain_events ADD COLUMN IF NOT EXISTS aggregate_version BIGINT;

-- Backfill de eventos existentes: numera cada stream na ordem em que foi gravado.
UPDATE domain_events d
SET aggregate_version = numbered.rn
FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY aggregate_type, aggregate_id ORDER BY created_at, id) AS rn
    FROM domain_events
) numbered
WHERE d.id = numbered.id
  AND d.aggregate_version IS NULL;

ALTER TABLE domain_events ALTER COLUMN aggregate_version SET NOT NULL;

-- Dois writers que leram a mesma versão não conseguem gravar a mesma posição do stream.
ALTER TABLE domain_events
    ADD CONSTRAINT uq_domain_events_stream_version UNIQUE (aggregate_type, aggregate_id, aggregate_version);

COMMENT ON COLUMN domain_events.aggregate_version IS 'Posição do evento no stream (1..n) por aggregate_type + aggregate_id';
