ALTER TABLE recipes ALTER COLUMN household_id DROP NOT NULL;
ALTER TABLE recipes ADD COLUMN is_template BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE recipes ADD COLUMN cloned_from UUID REFERENCES recipes (id) ON DELETE SET NULL;
ALTER TABLE recipes ADD CONSTRAINT recipes_owner_check CHECK (is_template OR household_id IS NOT NULL);

ALTER TABLE weekly_plans ADD COLUMN cloned_from UUID REFERENCES weekly_plans (id) ON DELETE SET NULL;

INSERT INTO recipes (id, household_id, name, servings, minutes, source, is_template) VALUES
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d01', NULL, 'Arepa with cheese', 2, 15, 'TEMPLATE', TRUE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d02', NULL, 'Rice and black beans', 2, 40, 'TEMPLATE', TRUE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d03', NULL, 'Pasta with tomato', 2, 25, 'TEMPLATE', TRUE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d04', NULL, 'Scrambled eggs with tomato', 2, 10, 'TEMPLATE', TRUE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d05', NULL, 'Potato and egg tortilla', 2, 35, 'TEMPLATE', TRUE);

INSERT INTO recipe_requirements (recipe_id, food_id, position, grams, optional)
SELECT r.recipe_id::uuid, f.id, r.position, r.grams, r.optional
FROM (VALUES
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d01', 'arepa', 0, 180, FALSE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d01', 'cheese', 1, 60, FALSE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d01', 'butter', 2, 10, TRUE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d02', 'rice', 0, 150, FALSE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d02', 'black beans', 1, 200, FALSE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d02', 'onion', 2, 75, TRUE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d03', 'pasta', 0, 200, FALSE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d03', 'tomato', 1, 240, FALSE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d03', 'onion', 2, 75, TRUE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d04', 'egg', 0, 150, FALSE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d04', 'tomato', 1, 120, TRUE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d05', 'potato', 0, 340, FALSE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d05', 'egg', 1, 200, FALSE),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d05', 'onion', 2, 75, TRUE)
) AS r (recipe_id, food_key, position, grams, optional)
JOIN food_catalog f ON f.name_key = r.food_key;

INSERT INTO recipe_steps (recipe_id, position, instruction, timer_seconds) VALUES
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d01', 1, 'Toast the arepas on a hot pan', 480),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d01', 2, 'Open them and fill with cheese', NULL),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d02', 1, 'Cook the rice', 1080),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d02', 2, 'Warm the beans with the onion', 600),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d03', 1, 'Boil the pasta', 600),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d03', 2, 'Cook the tomato and onion into a sauce', 900),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d04', 1, 'Scramble the eggs with the tomato', 300),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d05', 1, 'Fry the potato and onion', 1200),
    ('7a1e0c52-6b0f-4d4e-9d1a-0f6c1b2a3d05', 2, 'Add the beaten eggs and set both sides', 600);
