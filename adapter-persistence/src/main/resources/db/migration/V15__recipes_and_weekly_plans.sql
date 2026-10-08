CREATE TABLE recipes (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    servings INT NOT NULL CHECK (servings >= 1),
    minutes INT NOT NULL CHECK (minutes >= 1),
    source TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX recipes_household_idx ON recipes (household_id);

CREATE TABLE recipe_requirements (
    recipe_id UUID NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    food_id UUID NOT NULL REFERENCES food_catalog (id),
    position INT NOT NULL,
    grams NUMERIC(10, 2) NOT NULL CHECK (grams > 0),
    optional BOOLEAN NOT NULL,
    PRIMARY KEY (recipe_id, food_id)
);

CREATE TABLE recipe_steps (
    recipe_id UUID NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    position INT NOT NULL CHECK (position >= 1),
    instruction TEXT NOT NULL,
    timer_seconds BIGINT CHECK (timer_seconds > 0),
    weigh_food_id UUID REFERENCES food_catalog (id),
    weigh_grams NUMERIC(10, 2) CHECK (weigh_grams > 0),
    PRIMARY KEY (recipe_id, position)
);

CREATE TABLE weekly_plans (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    week_start DATE NOT NULL,
    UNIQUE (household_id, week_start)
);

CREATE TABLE plan_entries (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES weekly_plans (id) ON DELETE CASCADE,
    day SMALLINT NOT NULL CHECK (day BETWEEN 1 AND 7),
    meal TEXT NOT NULL CHECK (meal IN ('LUNCH', 'DINNER')),
    recipe_id UUID NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    servings INT NOT NULL CHECK (servings >= 1),
    needs_shopping BOOLEAN NOT NULL,
    UNIQUE (plan_id, day, meal)
);
