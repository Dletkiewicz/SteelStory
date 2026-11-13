CREATE TABLE steelstory.character_account_guards (
    account_id UUID PRIMARY KEY
);

CREATE TABLE steelstory.character_characters (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES steelstory.character_account_guards(account_id),
    name VARCHAR(20) NOT NULL,
    canonical_name TEXT COLLATE "C" GENERATED ALWAYS AS
        (translate(name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')) STORED,
    character_class TEXT NOT NULL CHECK (character_class IN ('WARRIOR', 'ARCHER', 'MAGE')),
    starting_strength INTEGER NOT NULL CHECK (starting_strength >= 5),
    starting_dexterity INTEGER NOT NULL CHECK (starting_dexterity >= 5),
    starting_intelligence INTEGER NOT NULL CHECK (starting_intelligence >= 5),
    starting_constitution INTEGER NOT NULL CHECK (starting_constitution >= 5),
    starting_luck INTEGER NOT NULL CHECK (starting_luck >= 5),
    trained_strength INTEGER NOT NULL CHECK (trained_strength >= 0),
    trained_dexterity INTEGER NOT NULL CHECK (trained_dexterity >= 0),
    trained_intelligence INTEGER NOT NULL CHECK (trained_intelligence >= 0),
    trained_constitution INTEGER NOT NULL CHECK (trained_constitution >= 0),
    trained_luck INTEGER NOT NULL CHECK (trained_luck >= 0),
    level INTEGER NOT NULL CHECK (level >= 1),
    experience BIGINT NOT NULL CHECK (experience >= 0),
    unspent_training_points INTEGER NOT NULL CHECK (unspent_training_points >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL CHECK (updated_at >= created_at),
    CONSTRAINT character_name_format CHECK (
        char_length(name) BETWEEN 3 AND 20 AND name COLLATE "C" ~ '^[A-Za-z0-9]+( [A-Za-z0-9]+)*$'
    ),
    CONSTRAINT character_name_unique UNIQUE (canonical_name),
    CONSTRAINT character_starting_points CHECK (
        starting_strength::BIGINT + starting_dexterity + starting_intelligence + starting_constitution + starting_luck = 35
    )
);

CREATE INDEX character_owner_order ON steelstory.character_characters(account_id, created_at, id);
