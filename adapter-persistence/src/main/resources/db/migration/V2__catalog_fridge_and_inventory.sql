CREATE TABLE food_catalog (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    name_key TEXT NOT NULL UNIQUE,
    category TEXT NOT NULL,
    default_unit TEXT NOT NULL,
    grams_per_ml NUMERIC(10, 4) CHECK (grams_per_ml > 0),
    grams_per_piece NUMERIC(10, 2) CHECK (grams_per_piece > 0),
    perishable BOOLEAN NOT NULL,
    shelf_days INT NOT NULL CHECK (shelf_days >= 0)
);

CREATE INDEX food_catalog_name_key_prefix_idx ON food_catalog (name_key text_pattern_ops);

CREATE TABLE food_allergens (
    food_id UUID NOT NULL REFERENCES food_catalog (id) ON DELETE CASCADE,
    allergen_id SMALLINT NOT NULL REFERENCES allergens (id),
    PRIMARY KEY (food_id, allergen_id)
);

CREATE TABLE fridges (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    min_c NUMERIC(4, 1) NOT NULL DEFAULT 1.0,
    max_c NUMERIC(4, 1) NOT NULL DEFAULT 5.0,
    door_alert_seconds INT NOT NULL DEFAULT 40
);

CREATE INDEX fridges_household_idx ON fridges (household_id);

CREATE TABLE zones (
    id UUID PRIMARY KEY,
    fridge_id UUID NOT NULL REFERENCES fridges (id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('SHELF', 'DOOR', 'DRAWER', 'FREEZER')),
    position INT NOT NULL
);

CREATE TABLE trays (
    id UUID PRIMARY KEY,
    zone_id UUID NOT NULL REFERENCES zones (id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    position INT NOT NULL CHECK (position >= 0)
);

CREATE TABLE food_items (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    tray_id UUID NOT NULL REFERENCES trays (id) ON DELETE CASCADE,
    food_id UUID NOT NULL REFERENCES food_catalog (id),
    owner_member_id UUID REFERENCES household_members (member_id) ON DELETE SET NULL,
    visibility TEXT NOT NULL DEFAULT 'SHARED' CHECK (visibility IN ('SHARED', 'ASK_FIRST', 'PRIVATE')),
    quantity_g NUMERIC(10, 2) NOT NULL CHECK (quantity_g >= 0),
    tare_g NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (tare_g >= 0),
    expires_on DATE,
    position INT NOT NULL,
    label_code TEXT UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX food_items_household_expiry_idx ON food_items (household_id, expires_on);
CREATE INDEX food_items_tray_idx ON food_items (tray_id);

CREATE TABLE food_access_grants (
    food_item_id UUID NOT NULL REFERENCES food_items (id) ON DELETE CASCADE,
    grantee_member_id UUID NOT NULL REFERENCES household_members (member_id) ON DELETE CASCADE,
    granted_by UUID NOT NULL REFERENCES household_members (member_id),
    expires_at TIMESTAMPTZ,
    PRIMARY KEY (food_item_id, grantee_member_id)
);

CREATE TABLE inventory_movements (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    food_item_id UUID NOT NULL,
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    user_id UUID REFERENCES users (id) ON DELETE SET NULL,
    type TEXT NOT NULL CHECK (type IN ('ADD', 'CONSUME', 'ADJUST', 'DISCARD', 'RESTORE')),
    delta_g NUMERIC(10, 2) NOT NULL,
    source TEXT NOT NULL CHECK (source IN ('SCALE', 'MANUAL', 'AGENT_CONFIRMED')),
    command_id UUID NOT NULL UNIQUE,
    at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX inventory_movements_household_at_idx ON inventory_movements (household_id, at DESC);

CREATE TABLE inventory_snapshots (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    payload JSONB NOT NULL,
    reason TEXT NOT NULL,
    at TIMESTAMPTZ NOT NULL DEFAULT now()
);
