CREATE TABLE scale_calibrations (
    device_id UUID PRIMARY KEY REFERENCES devices (id) ON DELETE CASCADE,
    offset_counts BIGINT NOT NULL,
    counts_per_gram NUMERIC(18, 6) CHECK (counts_per_gram <> 0),
    tared_at TIMESTAMPTZ NOT NULL,
    calibrated_at TIMESTAMPTZ
);
