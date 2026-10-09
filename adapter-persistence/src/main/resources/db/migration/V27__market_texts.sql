INSERT INTO translations (locale, key, text) VALUES
    ('es-CO', 'market.add-title', 'Agregar a la lista'),
    ('en', 'market.add-title', 'Add to the list'),
    ('es-CO', 'market.aisles', 'Por pasillo'),
    ('en', 'market.aisles', 'By aisle'),
    ('es-CO', 'market.bar-label', 'Gastado {spent} y planeado {planned} de {monthly}'),
    ('en', 'market.bar-label', 'Spent {spent} and planned {planned} of {monthly}'),
    ('es-CO', 'market.budget-hint', 'Define cuánto gastas en comida al mes; HayPaComer pone primero lo que pide el plan y sugiere alimentos permitidos más baratos.'),
    ('en', 'market.budget-hint', 'Set how much you spend on food each month; HayPaComer puts what the plan needs first and suggests cheaper allowed foods.'),
    ('es-CO', 'market.budget-missing', 'Escribe el monto mensual'),
    ('en', 'market.budget-missing', 'Write the monthly amount'),
    ('es-CO', 'market.change-budget', 'Cambiar el presupuesto'),
    ('en', 'market.change-budget', 'Change the budget'),
    ('es-CO', 'market.per-kilo', '{price} el kilo'),
    ('en', 'market.per-kilo', '{price} per kg'),
    ('es-CO', 'market.spent', 'gastado este mes'),
    ('en', 'market.spent', 'spent this month')
ON CONFLICT (locale, key) DO UPDATE SET text = EXCLUDED.text, updated_at = now();
