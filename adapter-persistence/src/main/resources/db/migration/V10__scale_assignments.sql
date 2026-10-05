CREATE TABLE scale_assignments (
    device_id UUID PRIMARY KEY REFERENCES devices (id) ON DELETE CASCADE,
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    food_item_id UUID NOT NULL REFERENCES food_items (id) ON DELETE CASCADE,
    assigned_by UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL
);
