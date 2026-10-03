ALTER TABLE inventory_snapshots
    ADD COLUMN kind TEXT NOT NULL DEFAULT 'MANUAL' CHECK (kind IN ('MANUAL', 'UNDO')),
    ADD COLUMN command_id UUID,
    ADD COLUMN actor_user_id UUID REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN used_at TIMESTAMPTZ,
    ADD COLUMN seq BIGINT GENERATED ALWAYS AS IDENTITY;

CREATE INDEX inventory_snapshots_household_kind_at_idx
    ON inventory_snapshots (household_id, kind, at DESC);
