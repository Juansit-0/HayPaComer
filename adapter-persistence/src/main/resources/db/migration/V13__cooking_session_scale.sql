ALTER TABLE cooking_sessions
    ADD COLUMN scale_device_id UUID REFERENCES devices (id) ON DELETE SET NULL;
