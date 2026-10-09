ALTER TABLE inventory_movements ADD COLUMN food_key TEXT;
ALTER TABLE inventory_movements ADD COLUMN expires_on DATE;

CREATE INDEX inventory_movements_household_type_at_idx
    ON inventory_movements (household_id, type, at);

ALTER TABLE food_catalog ADD COLUMN reference_price_cop_per_kg NUMERIC(12, 2)
    CHECK (reference_price_cop_per_kg > 0);

UPDATE food_catalog AS f SET reference_price_cop_per_kg = p.price
FROM (VALUES
    ('milk', 4800), ('yogurt', 14000), ('cheese', 32000), ('butter', 38000), ('egg', 12000),
    ('chicken breast', 22000), ('ground beef', 30000), ('tuna', 45000), ('rice', 4800),
    ('pasta', 9000), ('bread', 10000), ('black beans', 11000), ('lentils', 9500),
    ('tomato', 5500), ('onion', 4500), ('carrot', 3800), ('potato', 3200), ('avocado', 9000),
    ('banana', 3500), ('plantain', 3600), ('arepa', 12000), ('soup', 15000),
    ('soy sauce', 25000), ('orange juice', 7000)
) AS p (name_key, price)
WHERE f.name_key = p.name_key;

CREATE TABLE food_prices (
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    food_key TEXT NOT NULL,
    price_per_kg NUMERIC(12, 2) NOT NULL CHECK (price_per_kg > 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (household_id, food_key)
);
