ALTER TABLE steelstory.character_characters ADD COLUMN appearance_gender VARCHAR(6);

-- Preserve the appearance shown by the original class-only portraits, including retained deleted rows.
UPDATE steelstory.character_characters
SET appearance_gender = CASE WHEN character_class = 'ARCHER' THEN 'FEMALE' ELSE 'MALE' END;

ALTER TABLE steelstory.character_characters
    ALTER COLUMN appearance_gender SET NOT NULL,
    ADD CONSTRAINT character_appearance_gender CHECK (appearance_gender IN ('MALE', 'FEMALE'));
