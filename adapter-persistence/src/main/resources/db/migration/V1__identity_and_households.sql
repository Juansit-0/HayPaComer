CREATE TABLE users (
    id UUID PRIMARY KEY,
    email TEXT NOT NULL UNIQUE CHECK (email = lower(email)),
    password_hash TEXT NOT NULL,
    display_name TEXT NOT NULL CHECK (length(trim(display_name)) > 0),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash BYTEA NOT NULL UNIQUE,
    family_id UUID NOT NULL,
    replaced_by UUID REFERENCES refresh_tokens (id),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    user_agent TEXT,
    ip INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX refresh_tokens_family_idx ON refresh_tokens (family_id);
CREATE INDEX refresh_tokens_user_idx ON refresh_tokens (user_id);

CREATE TABLE user_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('VERIFY_EMAIL', 'RESET_PASSWORD')),
    token_hash BYTEA NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);

CREATE TABLE login_attempts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email TEXT NOT NULL,
    user_id UUID REFERENCES users (id) ON DELETE SET NULL,
    ip INET,
    success BOOLEAN NOT NULL,
    at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX login_attempts_email_at_idx ON login_attempts (email, at DESC);

CREATE TABLE households (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    currency CHAR(3) NOT NULL,
    timezone TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE household_members (
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    member_id UUID NOT NULL UNIQUE,
    role TEXT NOT NULL CHECK (role IN ('OWNER', 'MEMBER', 'GUEST')),
    joined_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (household_id, user_id)
);

CREATE UNIQUE INDEX household_members_one_owner_idx
    ON household_members (household_id) WHERE role = 'OWNER';
CREATE INDEX household_members_user_idx ON household_members (user_id);

CREATE TABLE household_invitations (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('MEMBER', 'GUEST')),
    token_hash BYTEA NOT NULL UNIQUE,
    invited_by UUID NOT NULL REFERENCES users (id),
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ
);

CREATE TABLE allergens (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code TEXT NOT NULL UNIQUE
);

INSERT INTO allergens (code) VALUES
    ('GLUTEN'), ('CRUSTACEANS'), ('EGGS'), ('FISH'), ('PEANUTS'), ('SOY'), ('MILK'),
    ('TREE_NUTS'), ('CELERY'), ('MUSTARD'), ('SESAME'), ('SULPHITES'), ('LUPIN'), ('MOLLUSCS');

CREATE TABLE member_profiles (
    member_id UUID PRIMARY KEY REFERENCES household_members (member_id) ON DELETE CASCADE,
    diet TEXT NOT NULL DEFAULT 'OMNIVORE'
        CHECK (diet IN ('OMNIVORE', 'PESCATARIAN', 'VEGETARIAN', 'VEGAN'))
);

CREATE TABLE profile_allergens (
    member_id UUID NOT NULL REFERENCES member_profiles (member_id) ON DELETE CASCADE,
    allergen_id SMALLINT NOT NULL REFERENCES allergens (id),
    PRIMARY KEY (member_id, allergen_id)
);

CREATE TABLE profile_avoided_foods (
    member_id UUID NOT NULL REFERENCES member_profiles (member_id) ON DELETE CASCADE,
    food_key TEXT NOT NULL,
    PRIMARY KEY (member_id, food_key)
);

CREATE TABLE audit_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_user_id UUID REFERENCES users (id) ON DELETE SET NULL,
    household_id UUID REFERENCES households (id) ON DELETE CASCADE,
    action TEXT NOT NULL,
    entity TEXT NOT NULL,
    entity_id UUID,
    detail JSONB,
    at TIMESTAMPTZ NOT NULL DEFAULT now()
);
