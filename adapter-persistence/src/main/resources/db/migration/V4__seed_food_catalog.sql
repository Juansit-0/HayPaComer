INSERT INTO food_catalog (id, name, name_key, category, default_unit, grams_per_ml, grams_per_piece,
                          perishable, shelf_days)
VALUES
    (gen_random_uuid(), 'Milk', 'milk', 'DAIRY', 'MILLILITER', 1.03, NULL, TRUE, 7),
    (gen_random_uuid(), 'Yogurt', 'yogurt', 'DAIRY', 'GRAM', NULL, 125, TRUE, 10),
    (gen_random_uuid(), 'Cheese', 'cheese', 'DAIRY', 'GRAM', NULL, NULL, TRUE, 21),
    (gen_random_uuid(), 'Butter', 'butter', 'DAIRY', 'GRAM', NULL, NULL, TRUE, 30),
    (gen_random_uuid(), 'Egg', 'egg', 'EGGS', 'PIECE', NULL, 50, TRUE, 21),
    (gen_random_uuid(), 'Chicken breast', 'chicken breast', 'POULTRY', 'GRAM', NULL, NULL, TRUE, 2),
    (gen_random_uuid(), 'Ground beef', 'ground beef', 'MEAT', 'GRAM', NULL, NULL, TRUE, 2),
    (gen_random_uuid(), 'Tuna', 'tuna', 'FISH', 'GRAM', NULL, 130, TRUE, 3),
    (gen_random_uuid(), 'Rice', 'rice', 'GRAIN', 'GRAM', NULL, NULL, FALSE, 365),
    (gen_random_uuid(), 'Pasta', 'pasta', 'GRAIN', 'GRAM', NULL, NULL, FALSE, 365),
    (gen_random_uuid(), 'Bread', 'bread', 'GRAIN', 'PIECE', NULL, 30, TRUE, 4),
    (gen_random_uuid(), 'Black beans', 'black beans', 'LEGUME', 'GRAM', NULL, NULL, FALSE, 365),
    (gen_random_uuid(), 'Lentils', 'lentils', 'LEGUME', 'GRAM', NULL, NULL, FALSE, 365),
    (gen_random_uuid(), 'Tomato', 'tomato', 'VEGETABLE', 'PIECE', NULL, 120, TRUE, 7),
    (gen_random_uuid(), 'Onion', 'onion', 'VEGETABLE', 'PIECE', NULL, 150, TRUE, 30),
    (gen_random_uuid(), 'Carrot', 'carrot', 'VEGETABLE', 'PIECE', NULL, 70, TRUE, 21),
    (gen_random_uuid(), 'Potato', 'potato', 'VEGETABLE', 'PIECE', NULL, 170, TRUE, 30),
    (gen_random_uuid(), 'Avocado', 'avocado', 'FRUIT', 'PIECE', NULL, 200, TRUE, 5),
    (gen_random_uuid(), 'Banana', 'banana', 'FRUIT', 'PIECE', NULL, 120, TRUE, 6),
    (gen_random_uuid(), 'Plantain', 'plantain', 'FRUIT', 'PIECE', NULL, 280, TRUE, 7),
    (gen_random_uuid(), 'Arepa', 'arepa', 'PREPARED', 'PIECE', NULL, 90, TRUE, 5),
    (gen_random_uuid(), 'Soup', 'soup', 'PREPARED', 'MILLILITER', 1.0, NULL, TRUE, 3),
    (gen_random_uuid(), 'Soy sauce', 'soy sauce', 'CONDIMENT', 'MILLILITER', 1.2, NULL, FALSE, 365),
    (gen_random_uuid(), 'Orange juice', 'orange juice', 'BEVERAGE', 'MILLILITER', 1.04, NULL, TRUE, 7);

INSERT INTO food_allergens (food_id, allergen_id)
SELECT f.id, a.id FROM food_catalog f JOIN allergens a ON
    (f.name_key IN ('milk', 'yogurt', 'cheese', 'butter') AND a.code = 'MILK')
    OR (f.name_key = 'egg' AND a.code = 'EGGS')
    OR (f.name_key = 'tuna' AND a.code = 'FISH')
    OR (f.name_key IN ('pasta', 'bread') AND a.code = 'GLUTEN')
    OR (f.name_key = 'soy sauce' AND a.code IN ('SOY', 'GLUTEN'));
