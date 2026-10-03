CREATE TABLE devices (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    fridge_id UUID NOT NULL REFERENCES fridges (id) ON DELETE CASCADE,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    kind TEXT NOT NULL CHECK (kind IN ('ESP32_DOOR_TEMP', 'ESP32_SCALE', 'SIMULATOR')),
    api_key_hash BYTEA NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX devices_household_idx ON devices (household_id);
