CREATE TABLE sensor_events (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices (id) ON DELETE CASCADE,
    fridge_id UUID NOT NULL REFERENCES fridges (id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('DOOR', 'TEMPERATURE', 'WEIGHT')),
    door_state TEXT CHECK (door_state IN ('OPEN', 'CLOSED')),
    celsius NUMERIC(5, 2),
    grams NUMERIC(10, 2),
    stable BOOLEAN,
    scale_mode TEXT CHECK (scale_mode IN ('FRIDGE', 'COOKING')),
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX sensor_events_device_type_at_idx ON sensor_events (device_id, type, occurred_at DESC);
CREATE INDEX sensor_events_fridge_at_idx ON sensor_events (fridge_id, occurred_at DESC);
