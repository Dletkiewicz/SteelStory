-- Spring Session JDBC 4.1.1 schema-postgresql.sql, with schema-qualified owner-prefixed names.
-- Framework epoch-millisecond columns intentionally remain BIGINT.
CREATE TABLE steelstory.identity_sessions (
    PRIMARY_ID CHAR(36) NOT NULL PRIMARY KEY,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100)
);

CREATE UNIQUE INDEX identity_sessions_id ON steelstory.identity_sessions (SESSION_ID);
CREATE INDEX identity_sessions_expiry ON steelstory.identity_sessions (EXPIRY_TIME);
CREATE INDEX identity_sessions_principal ON steelstory.identity_sessions (PRINCIPAL_NAME);

CREATE TABLE steelstory.identity_sessions_attributes (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BYTEA NOT NULL,
    PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES steelstory.identity_sessions(PRIMARY_ID) ON DELETE CASCADE
);
