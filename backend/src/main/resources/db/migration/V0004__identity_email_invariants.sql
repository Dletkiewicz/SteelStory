-- Enforce the application's canonical ASCII subset, not the full email RFC.
-- Existing invalid rows cause migration failure; never rewrite account identities silently.
ALTER TABLE steelstory.identity_accounts
    ADD CONSTRAINT identity_accounts_email_separator CHECK (
        length(email) - length(replace(email, '@', '')) = 1
    ),
    ADD CONSTRAINT identity_accounts_email_local_part CHECK (
        split_part(email, '@', 1) ~ '^[a-z0-9!#$%&''*+/=?^_`{|}~.-]{1,64}$'
        AND left(email, 1) <> '.'
        AND right(split_part(email, '@', 1), 1) <> '.'
        AND position('..' in split_part(email, '@', 1)) = 0
    ),
    ADD CONSTRAINT identity_accounts_email_domain CHECK (
        -- Nonempty DNS-style labels, 1–63 characters, separated by at least one dot.
        split_part(email, '@', 2) ~ '^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?([.][a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$'
    );
