CREATE TABLE market_items (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    food_id UUID NOT NULL REFERENCES food_catalog (id),
    grams_needed NUMERIC(10, 2) NOT NULL CHECK (grams_needed > 0),
    source TEXT NOT NULL CHECK (source IN ('MANUAL', 'RECIPE', 'PLAN', 'AGENT_CONFIRMED')),
    added_by UUID REFERENCES users (id) ON DELETE SET NULL,
    added_at TIMESTAMPTZ NOT NULL,
    checked_at TIMESTAMPTZ,
    position INT NOT NULL
);

CREATE UNIQUE INDEX market_items_one_pending_per_food_idx
    ON market_items (household_id, food_id) WHERE checked_at IS NULL;
CREATE INDEX market_items_household_idx ON market_items (household_id, position);
