-- Events waiting to be relayed to the broker (shared foundation, transactional outbox). Standard SQL.
CREATE TABLE outbox_event (
    id           VARCHAR(36)              PRIMARY KEY,
    topic        VARCHAR(249)             NOT NULL,
    message_key  VARCHAR(255),
    payload      TEXT                     NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX outbox_event_unpublished ON outbox_event (published_at, created_at);
