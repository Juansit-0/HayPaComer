ALTER TABLE food_items
    ADD COLUMN expiry_source TEXT
        CHECK (expiry_source IN ('USER', 'LABEL', 'AI_SUGGESTED', 'ESTIMATED')),
    ADD COLUMN opened_on DATE;

UPDATE food_items SET expiry_source = 'USER' WHERE expires_on IS NOT NULL;

ALTER TABLE food_catalog
    ADD COLUMN fridge_days INT CHECK (fridge_days >= 0),
    ADD COLUMN door_days INT CHECK (door_days >= 0),
    ADD COLUMN freezer_days INT CHECK (freezer_days >= 0),
    ADD COLUMN opened_days INT CHECK (opened_days >= 0);

UPDATE food_catalog AS f
SET fridge_days = v.fridge, door_days = v.door, freezer_days = v.freezer, opened_days = v.opened
FROM (VALUES
    ('arepa', 5, 4, 90, 3),
    ('avocado', 5, 3, 120, 1),
    ('banana', 6, 5, 90, 1),
    ('black beans', 365, 365, 365, 365),
    ('bread', 4, 4, 90, 4),
    ('butter', 30, 30, 180, 30),
    ('carrot', 21, 14, 300, 21),
    ('cheese', 21, 14, 120, 7),
    ('chicken breast', 2, 1, 270, 2),
    ('egg', 21, 14, 120, 21),
    ('ground beef', 2, 1, 120, 2),
    ('lentils', 365, 365, 365, 365),
    ('milk', 7, 5, 90, 4),
    ('onion', 30, 21, 240, 7),
    ('orange juice', 7, 7, 240, 5),
    ('pasta', 365, 365, 365, 365),
    ('plantain', 7, 5, 90, 2),
    ('potato', 30, 21, 300, 5),
    ('rice', 365, 365, 365, 365),
    ('soup', 3, 3, 90, 3),
    ('soy sauce', 365, 365, 365, 365),
    ('tomato', 7, 5, 90, 3),
    ('tuna', 2, 1, 90, 1),
    ('yogurt', 10, 7, 60, 5)
) AS v (name_key, fridge, door, freezer, opened)
WHERE f.name_key = v.name_key;

INSERT INTO settings (key, kind, scope, value, min_value, max_value, description) VALUES
    ('food.expiry-margin-days', 'INTEGER', 'HOUSEHOLD', '3', 0, 30,
     'Extra days accepted beyond the usual shelf life before a typed expiry date is rejected');

INSERT INTO translations (locale, key, text) VALUES
    ('es-CO', 'setting.food.expiry-margin-days',
     'Días extra que se aceptan sobre la vida útil normal antes de rechazar una fecha escrita');

INSERT INTO message_templates (locale, source, target) VALUES
    ('es-CO', 'Expiry date not possible', 'Fecha de vencimiento imposible'),
    ('es-CO', '{} lasts about {} days in the fridge, so a date {} days away is not possible; check the label or let HayPaComer estimate it',
     '{} dura unos {} días en la nevera, así que una fecha a {} días no es posible; revisa la etiqueta o deja que HayPaComer la estime'),
    ('es-CO', '{} lasts about {} days in the fridge door, so a date {} days away is not possible; check the label or let HayPaComer estimate it',
     '{} dura unos {} días en la puerta de la nevera, así que una fecha a {} días no es posible; revisa la etiqueta o deja que HayPaComer la estime'),
    ('es-CO', '{} lasts about {} days in the freezer, so a date {} days away is not possible; check the label or let HayPaComer estimate it',
     '{} dura unos {} días en el congelador, así que una fecha a {} días no es posible; revisa la etiqueta o deja que HayPaComer la estime'),
    ('es-CO', '{} lasts about {} days once opened, so a date {} days away is not possible; check the label or let HayPaComer estimate it',
     '{} dura unos {} días una vez abierto, así que una fecha a {} días no es posible; revisa la etiqueta o deja que HayPaComer la estime'),
    ('es-CO', 'Unknown storage {}', 'Lugar de almacenamiento desconocido: {}');
