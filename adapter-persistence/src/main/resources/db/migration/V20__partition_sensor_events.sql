ALTER TABLE sensor_events RENAME TO sensor_events_unpartitioned;
ALTER INDEX sensor_events_device_type_at_idx RENAME TO sensor_events_unpartitioned_device_idx;
ALTER INDEX sensor_events_fridge_at_idx RENAME TO sensor_events_unpartitioned_fridge_idx;

CREATE TABLE sensor_events (
    id UUID NOT NULL,
    device_id UUID NOT NULL REFERENCES devices (id) ON DELETE CASCADE,
    fridge_id UUID NOT NULL REFERENCES fridges (id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('DOOR', 'TEMPERATURE', 'WEIGHT')),
    door_state TEXT CHECK (door_state IN ('OPEN', 'CLOSED')),
    celsius NUMERIC(5, 2),
    grams NUMERIC(10, 2),
    stable BOOLEAN,
    scale_mode TEXT CHECK (scale_mode IN ('FRIDGE', 'COOKING')),
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);

CREATE INDEX sensor_events_device_type_at_idx ON sensor_events (device_id, type, occurred_at DESC);
CREATE INDEX sensor_events_fridge_at_idx ON sensor_events (fridge_id, occurred_at DESC);

CREATE TABLE sensor_events_default PARTITION OF sensor_events DEFAULT;

CREATE FUNCTION ensure_sensor_event_partitions(first_month DATE, months_ahead INT)
RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
    month DATE := date_trunc('month', first_month)::date;
    last_month DATE := (date_trunc('month', now()) + make_interval(months => months_ahead))::date;
    created INT := 0;
    name TEXT;
BEGIN
    WHILE month <= last_month LOOP
        name := 'sensor_events_' || to_char(month, 'YYYY_MM');
        IF to_regclass(name) IS NULL THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF sensor_events FOR VALUES FROM (%L) TO (%L)',
                name, month, (month + INTERVAL '1 month')::date);
            created := created + 1;
        END IF;
        month := (month + INTERVAL '1 month')::date;
    END LOOP;
    RETURN created;
END;
$$;

SELECT ensure_sensor_event_partitions(
    COALESCE((SELECT min(occurred_at)::date FROM sensor_events_unpartitioned), now()::date), 3);

INSERT INTO sensor_events SELECT * FROM sensor_events_unpartitioned;

DROP TABLE sensor_events_unpartitioned;
