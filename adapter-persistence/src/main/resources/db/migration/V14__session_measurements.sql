ALTER TABLE cooking_sessions
    ADD COLUMN measured_g NUMERIC(10, 2) CHECK (measured_g >= 0);
