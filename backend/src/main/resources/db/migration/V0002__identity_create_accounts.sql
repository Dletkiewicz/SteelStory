CREATE TABLE steelstory.identity_accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(254) COLLATE "C" NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT identity_accounts_email_unique UNIQUE (email),
    CONSTRAINT identity_accounts_email_canonical CHECK (
        email = lower(btrim(email)) AND email ~ '^[!-~]+@[a-z0-9.-]+$'
    ),
    CONSTRAINT identity_accounts_password_hash CHECK (password_hash LIKE '$argon2id$v=19$%'),
    CONSTRAINT identity_accounts_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT identity_accounts_timestamps CHECK (updated_at >= created_at)
);
