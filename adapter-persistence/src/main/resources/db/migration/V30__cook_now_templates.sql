INSERT INTO translations (locale, key, text) VALUES
    ('es-CO', 'now.cook-lead.one', 'Revisa {n} receta (tuyas y de la casa HayPaComer) contra los gramos de la nevera y las alergias de la casa.'),
    ('en', 'now.cook-lead.one', 'Checks {n} recipe (yours and HayPaComer''s) against the grams in the fridge and the allergies at home.'),
    ('es-CO', 'now.cook-lead.other', 'Revisa {n} recetas (tuyas y de la casa HayPaComer) contra los gramos de la nevera y las alergias de la casa.'),
    ('en', 'now.cook-lead.other', 'Checks {n} recipes (yours and HayPaComer''s) against the grams in the fridge and the allergies at home.')
ON CONFLICT (locale, key) DO UPDATE SET text = EXCLUDED.text, updated_at = now();
