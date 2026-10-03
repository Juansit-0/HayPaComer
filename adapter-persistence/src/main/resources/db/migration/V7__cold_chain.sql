CREATE TABLE cold_chains (
    fridge_id UUID PRIMARY KEY REFERENCES fridges (id) ON DELETE CASCADE,
    phase TEXT NOT NULL CHECK (phase IN ('NORMAL', 'WARMING', 'UNDER_REVIEW')),
    since TIMESTAMPTZ,
    peak_c NUMERIC(5, 2),
    recovered BOOLEAN NOT NULL DEFAULT TRUE,
    last_c NUMERIC(5, 2),
    last_reading_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cold_incidents (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fridge_id UUID NOT NULL REFERENCES fridges (id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL,
    max_c NUMERIC(5, 2) NOT NULL,
    reviewed_by UUID REFERENCES users (id) ON DELETE SET NULL,
    UNIQUE (fridge_id, started_at)
);
