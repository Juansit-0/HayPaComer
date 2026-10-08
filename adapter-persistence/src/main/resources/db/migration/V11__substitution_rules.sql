CREATE TABLE substitution_rules (
    id UUID PRIMARY KEY,
    from_food_id UUID NOT NULL REFERENCES food_catalog (id) ON DELETE CASCADE,
    to_food_id UUID NOT NULL REFERENCES food_catalog (id) ON DELETE CASCADE,
    ratio NUMERIC(6, 3) NOT NULL CHECK (ratio > 0),
    max_g NUMERIC(10, 2) NOT NULL CHECK (max_g > 0),
    position INT NOT NULL,
    CHECK (from_food_id <> to_food_id),
    UNIQUE (from_food_id, to_food_id)
);

CREATE INDEX substitution_rules_from_idx ON substitution_rules (from_food_id, position);

INSERT INTO substitution_rules (id, from_food_id, to_food_id, ratio, max_g, position)
SELECT gen_random_uuid(), f.id, t.id, r.ratio, r.max_g, r.position
FROM (VALUES
    ('chicken breast', 'tuna', 1.0, 300, 1),
    ('chicken breast', 'ground beef', 1.0, 300, 2),
    ('ground beef', 'lentils', 1.5, 300, 3),
    ('ground beef', 'black beans', 1.5, 300, 4),
    ('milk', 'yogurt', 1.0, 250, 5),
    ('rice', 'pasta', 1.0, 300, 6),
    ('pasta', 'rice', 1.0, 300, 7),
    ('black beans', 'lentils', 1.0, 500, 8),
    ('lentils', 'black beans', 1.0, 500, 9),
    ('potato', 'plantain', 1.0, 500, 10),
    ('arepa', 'bread', 0.7, 450, 11)
) AS r (from_key, to_key, ratio, max_g, position)
JOIN food_catalog f ON f.name_key = r.from_key
JOIN food_catalog t ON t.name_key = r.to_key;
