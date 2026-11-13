ALTER TABLE steelstory.character_characters
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD CONSTRAINT character_deletion_timestamp CHECK (
        deleted_at IS NULL OR (deleted_at >= created_at AND deleted_at = updated_at)
    );

-- Keep character_name_unique unchanged: names remain reserved after deletion.
CREATE INDEX character_active_owner_order
    ON steelstory.character_characters(account_id, created_at, id)
    WHERE deleted_at IS NULL;
