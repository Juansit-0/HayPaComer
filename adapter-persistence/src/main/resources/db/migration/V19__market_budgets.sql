CREATE TABLE market_budgets (
    household_id UUID PRIMARY KEY REFERENCES households (id) ON DELETE CASCADE,
    monthly_amount NUMERIC(14, 2) NOT NULL CHECK (monthly_amount > 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
