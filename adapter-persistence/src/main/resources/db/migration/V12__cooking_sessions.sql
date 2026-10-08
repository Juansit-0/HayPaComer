CREATE TABLE cooking_sessions (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    started_by UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipe JSONB NOT NULL,
    state TEXT NOT NULL CHECK (state IN ('PREPARING', 'COOKING', 'PAUSED', 'FINISHED', 'ABANDONED')),
    current_step INT NOT NULL CHECK (current_step >= 0),
    servings INT NOT NULL CHECK (servings >= 1),
    started_at TIMESTAMPTZ NOT NULL,
    state_since TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX cooking_sessions_one_active_idx ON cooking_sessions (household_id)
    WHERE state IN ('PREPARING', 'COOKING', 'PAUSED');

CREATE TABLE session_step_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES cooking_sessions (id) ON DELETE CASCADE,
    position INT NOT NULL CHECK (position >= 1),
    measured_g NUMERIC(10, 2),
    at TIMESTAMPTZ NOT NULL,
    UNIQUE (session_id, position)
);
